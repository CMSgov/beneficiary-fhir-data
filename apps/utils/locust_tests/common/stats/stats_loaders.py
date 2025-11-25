"""Members of this file/module are related to the loading of performance statistics
from various data "stores" (such as from files or AWS S3).
"""

import json
import time
from abc import ABC, abstractmethod
from dataclasses import fields
from functools import cmp_to_key, reduce
from pathlib import Path
from statistics import mean
from typing import Any

from common.stats.aggregated_stats import (
    AggregatedStats,
    FinalCompareResult,
    StatsMetadata,
    TaskStats,
)
from common.stats.stats_config import (
    StatsComparisonType,
    StatsConfiguration,
    StatsStorageType,
)
from common.validation import ValidationResult
from gevent import monkey

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
monkey.patch_all()
import boto3  # noqa: E402

AthenaQueryRowResult = dict[str, list[dict[str, str]]]
"""Type representing a single row result from the result of an Athena query"""

TOTAL_RUNTIME_DELTA = 3.0
"""The delta under which two AggregatedStats instances are considered able to
be compared"""


class StatsLoader(ABC):
    """Loads AggregatedStats depending on what type of comparison is requested."""

    def __init__(self, stats_config: StatsConfiguration, metadata: StatsMetadata) -> None:
        self.stats_config = stats_config
        self.metadata = metadata

    def load(self) -> AggregatedStats | None:
        """Load an AggregatedStats instance constructed based on what type of comparison is
        required.

        Returns:
            AggregatedStats: An AggregatedStats instance representing the set of stats requested to
            load
        """
        is_avg_compare = self.stats_config.stats_compare == StatsComparisonType.AVERAGE
        return self.load_average() if is_avg_compare else self.load_previous()

    @abstractmethod
    def load_previous(self) -> AggregatedStats | None:
        """Load an AggregatedStats instance constructed based on the most recent, previous test
        suite runs' stats under the tag specified by the user.

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of the previous test
            suite run
        """

    @abstractmethod
    def load_average(self) -> AggregatedStats | None:
        """Load an AggregatedStats instance constructed based on the the average of all of the
        previous test suite runs' stats under the tag specified by the user.

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of all specified
            previous test suite runs
        """

    @staticmethod
    def create(stats_config: StatsConfiguration, metadata: StatsMetadata) -> "StatsLoader":
        """Construct a new concrete instance of StatsLoader that will load from the appropriate
        store as specified in stats_config.

        Args:
            stats_config (StatsConfiguration): The configuration specified for storing and comparing
            statistics

        Returns:
            StatsLoader: A concrete instance of StatsLoader that will load from the store specified
            in configuration
        """
        return (
            StatsFileLoader(stats_config, metadata)
            if stats_config.stats_store == StatsStorageType.FILE
            else StatsAthenaLoader(stats_config, metadata)
        )


class StatsFileLoader(StatsLoader):
    """Child class of StatsLoader that loads aggregated task stats from the local file system
    through JSON files.
    """

    def load_previous(self) -> AggregatedStats | None:
        # Get a list of all AggregatedStats from stats.json files under path
        stats_list = self.__load_stats_from_files()

        # Filter those that don't match the config and current run's metadata
        filtered_stats = [
            stats
            for stats in stats_list
            if stats.metadata and self.__verify_metadata(stats.metadata)
        ]

        # Sort them based upon timestamp, greater to lower
        filtered_stats.sort(key=lambda stats: stats.metadata.timestamp, reverse=True)  # type: ignore

        # Take the first item, if it exists -- this is the most recent, previous run
        return filtered_stats[0] if filtered_stats else None

    def load_average(self) -> AggregatedStats | None:
        stats_list = self.__load_stats_from_files()
        verified_stats = [
            stats
            for stats in stats_list
            if stats.metadata and self.__verify_metadata(stats.metadata)
        ]
        limited_stats = sorted(
            verified_stats,
            key=cmp_to_key(
                lambda item1, item2: item1.metadata.timestamp - item2.metadata.timestamp  # type: ignore
            ),
            reverse=True,
        )[: self.stats_config.stats_compare_load_limit]

        return _get_average_all_stats(limited_stats)

    def __load_stats_from_files(self, suffix: str = ".stats.json") -> list[AggregatedStats]:
        path = (
            self.stats_config.stats_store_file_path
            if self.stats_config and self.stats_config.stats_store_file_path
            else ""
        )
        stats_files = [path / file for file in Path(path).iterdir() if file.name.endswith(suffix)]

        aggregated_stats_list = []
        for stats_file in stats_files:
            with Path(stats_file).open(encoding="utf-8") as json_file:
                aggregated_stats_list.append(AggregatedStats(**json.load(json_file)))

        return aggregated_stats_list

    def __verify_metadata(self, loaded_metadata: StatsMetadata) -> bool:
        return all(
            [
                self.stats_config.stats_compare_tag in loaded_metadata.tags,
                loaded_metadata.hash == self.metadata.hash,
                loaded_metadata.compare_result
                in (FinalCompareResult.NOT_APPLICABLE, FinalCompareResult.PASSED),
                loaded_metadata.validation_result
                in (ValidationResult.NOT_APPLICABLE, ValidationResult.PASSED),
                # Pick some delta that the runtimes should be under -- in this case, we're using 3
                # seconds
                # TODO: Determine the right delta for checking for matching runtimes
                loaded_metadata.total_runtime - self.metadata.total_runtime < TOTAL_RUNTIME_DELTA,
            ]
        )


class StatsAthenaLoader(StatsLoader):
    """Child class of StatsLoader that loads aggregated task stats from S3 via Athena."""

    def __init__(self, stats_config: StatsConfiguration, metadata: StatsMetadata) -> None:
        self.client = boto3.client("athena", region_name="us-east-1")

        super().__init__(stats_config, metadata)

    def load_previous(self) -> AggregatedStats | None:
        query = (
            f"SELECT cast(totals as JSON), cast(tasks as JSON) "
            f'FROM "{self.stats_config.stats_store_s3_database}"."{self.stats_config.stats_store_s3_table}" '
            f"WHERE {self.__get_where_clause()} ORDER BY metadata.timestamp DESC "
            "LIMIT 1"
        )

        queried_stats = self.__get_stats_from_query(query)
        return queried_stats[0] if queried_stats else None

    def load_average(self) -> AggregatedStats | None:
        query = (
            f"SELECT cast(totals as JSON), cast(tasks as JSON) "
            f'FROM "{self.stats_config.stats_store_s3_database}"."{self.stats_config.stats_store_s3_table}" '
            f"WHERE {self.__get_where_clause()} "
            "ORDER BY metadata.timestamp DESC "
            f"LIMIT {self.stats_config.stats_compare_load_limit}"
        )

        queried_stats = self.__get_stats_from_query(query)
        return _get_average_all_stats(queried_stats)

    def __get_stats_from_query(self, query: str) -> list[AggregatedStats]:
        query_result = self.__run_query(query)
        if not query_result:
            raise RuntimeError("Athena query result was empty or query failed")

        raw_json_data = self.__get_raw_json_data(query_result)
        return self.__stats_from_json_data(raw_json_data)

    def __start_athena_query(self, query: str) -> dict[str, Any]:
        return self.client.start_query_execution(
            QueryString=query,
            QueryExecutionContext={"Database": self.stats_config.stats_store_s3_database},
            WorkGroup=self.stats_config.stats_store_s3_workgroup,
        )

    def __get_athena_query_status(self, query_execution_id: str) -> str:
        # See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/athena.html#Athena.Client.get_query_execution
        # for the structure of the returned Dict
        return self.client.get_query_execution(QueryExecutionId=query_execution_id)[
            "QueryExecution"
        ]["Status"]["State"]

    def __get_athena_query_result(self, query_execution_id: str) -> list[AthenaQueryRowResult]:
        # See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/athena.html#Athena.Client.get_query_results
        # for the structure of the returned Dict
        return self.client.get_query_results(QueryExecutionId=query_execution_id)["ResultSet"][
            "Rows"
        ]

    def __run_query(self, query: str, max_retries: int = 10) -> list[AthenaQueryRowResult] | None:
        start_response = self.__start_athena_query(query)
        query_execution_id = start_response["QueryExecutionId"]

        for try_number in range(max_retries - 1):
            # Exponentially back-off from hitting the API to ensure we don't hit the API limit
            # See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            time.sleep((2**try_number * 100.0) / 1000.0)

            status = self.__get_athena_query_status(query_execution_id)

            if status == "SUCCEEDED":
                break
            if status == "FAILED" or status == "CANCELLED":
                raise RuntimeError(f"Query failed to complete -- status returned was {status}")

        return self.__get_athena_query_result(query_execution_id)

    def __get_where_clause(self) -> str:
        explicit_checks = [
            f"contains(metadata.tags, '{self.stats_config.stats_compare_tag}')",
            f"metadata.hash='{self.metadata.hash}'",
            (
                f"(metadata.compare_result='{FinalCompareResult.NOT_APPLICABLE.value}' OR "
                f"metadata.compare_result='{FinalCompareResult.PASSED.value}')"
            ),
            (
                f"(metadata.validation_result='{ValidationResult.NOT_APPLICABLE.value}' OR "
                f"metadata.validation_result='{ValidationResult.PASSED.value}')"
            ),
            # TODO: Determine the right delta for checking for matching runtimes
            f"(metadata.total_runtime - {self.metadata.total_runtime}) < {TOTAL_RUNTIME_DELTA}",
        ]

        return " AND ".join(explicit_checks)

    def __get_raw_json_data(
        self, query_result: list[dict[str, list[dict[str, str]]]]
    ) -> list[tuple[str, str]]:
        # The data is returned as an array of dicts, each with a 'Data' key. These 'Data' dicts
        # values are arrays of dicts with the key being the data type and the value being the actual
        # returned result. The first 'Data' dict in the array is the column names, and subsequent
        # 'Data' dict entries in the array are actual values

        # We make a few assumptions:
        # 1. The first 'Data' dict in the array is always the column names
        # 2. The data returned is always of the type `VarCharValue`
        # 3. We are only retrieving two columns
        raw_data = [item["Data"] for item in query_result[1:]]
        return [(data[0]["VarCharValue"], data[1]["VarCharValue"]) for data in raw_data]

    def __stats_from_json_data(self, raw_json_data: list[tuple[str, str]]) -> list[AggregatedStats]:
        # Deserializing from a tuple of raw JSON objects; first tuple is a raw JSON object string
        # representing the aggregated totals and second tuple is a raw JSON list of objects
        # representing the statistics for each task
        serialized_tuples: list[tuple[dict[str, Any], list[dict[str, Any]]]] = [
            (json.loads(raw_json_totals), json.loads(raw_json_tasks))
            for raw_json_totals, raw_json_tasks in raw_json_data
        ]
        # The metadata is unnecessary here since by the time we've gotten here the metadata for each
        # of the tasks we're serializing here has already been checked
        return [
            AggregatedStats(
                totals=TaskStats(**totals_as_dict),
                tasks=[TaskStats(**task_vals_dict) for task_vals_dict in tasks_as_lists],
            )
            for totals_as_dict, tasks_as_lists in serialized_tuples
        ]


def _bucket_tasks_by_name(all_stats: list[AggregatedStats]) -> dict[str, list[TaskStats]]:
    tasks_by_name: dict[str, list[TaskStats]] = {}
    for stats in all_stats:
        for task in stats.tasks:
            if task.task_name not in tasks_by_name:
                tasks_by_name[task.task_name] = []

            tasks_by_name[task.task_name].append(task)

    return tasks_by_name


def _get_average_task_stats(all_tasks: list[TaskStats]) -> TaskStats:
    if not all_tasks:
        raise ValueError("The list of tasks to average must not be empty")

    if not all(x.task_name == all_tasks[0].task_name for x in all_tasks):
        raise ValueError("The list of TaskStats must be for the same task")

    # Exclude fields that are not statistics and the response time percentiles
    # dict which will be handled on its own later
    fields_to_exclude = ["task_name", "request_method", "response_time_percentiles"]
    stats_to_average = [
        field.name for field in fields(TaskStats) if field.name not in fields_to_exclude
    ]
    # Calculate the mean automatically for every matching stat in the list of
    # all stats, and then put the mean in a dict
    avg_task_stats = {
        stat_name: mean(getattr(task, stat_name) for task in all_tasks)
        for stat_name in stats_to_average
    }

    # Get the common keys between all of the response time percentile dicts in
    # the list of task stats
    common_percents = reduce(
        lambda prev, next: prev & next,
        (task.response_time_percentiles.keys() for task in all_tasks),
    )
    # Do the same thing as above but for each entry in each response time percentile dict --
    # get the mean of each percentile across all tasks and make it the value of a new
    # percentile dict
    avg_task_percents = {
        p: mean(task.response_time_percentiles[p] for task in all_tasks) for p in common_percents
    }

    return TaskStats(
        task_name=all_tasks[0].task_name,
        request_method=all_tasks[0].request_method,
        response_time_percentiles=avg_task_percents,
        **avg_task_stats,
    )


def _get_average_all_stats(all_stats: list[AggregatedStats]) -> AggregatedStats | None:
    partitioned_task_stats = _bucket_tasks_by_name(all_stats)
    try:
        averaged_tasks = [
            _get_average_task_stats(tasks) for tasks in partitioned_task_stats.values()
        ]
        averaged_totals = _get_average_task_stats([stat.totals for stat in all_stats])
    except ValueError:
        return None

    return AggregatedStats(totals=averaged_totals, tasks=averaged_tasks) if averaged_tasks else None
