"""Members of this file/module are related to the loading of performance statistics
from various data "stores" (such as from files or AWS S3)"""
from functools import reduce
from statistics import mean
from dataclasses import Field, fields
import time
from abc import ABC, abstractmethod
import json
import os
from typing import Any, Dict, List, Optional
from common.stats.aggregated_stats import AggregatedStats, StatsMetadata, TaskStats
from common.stats.stats_config import StatsComparisonType, StatsConfiguration, StatsStorageType

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
from gevent import monkey
monkey.patch_all()
import boto3


class StatsLoader(ABC):
    """Loads AggregatedStats depending on what type of comparison is requested"""

    def __init__(self, stats_config: StatsConfiguration, metadata: StatsMetadata) -> None:
        self.stats_config = stats_config
        self.metadata = metadata

    def load(self) -> Optional[AggregatedStats]:
        """Loads an AggregatedStats instance constructed based on what type of comparison is required

        Returns:
            AggregatedStats: An AggregatedStats instance representing the set of stats requested to load
        """
        is_avg_compare = self.stats_config.compare == StatsComparisonType.AVERAGE
        return self.load_average() if is_avg_compare else self.load_previous()

    @abstractmethod
    def load_previous(self) -> Optional[AggregatedStats]:
        """Loads an AggregatedStats instance constructed based on the most recent, previous test suite runs' 
        stats under the tag specified by the user

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of the previous test suite run
        """
        pass

    @abstractmethod
    def load_average(self) -> Optional[AggregatedStats]:
        """Loads an AggregatedStats instance constructed based on the the average of all of the previous test suite
        runs' stats under the tag specified by the user

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of all specified previous test suite runs
        """
        pass

    @staticmethod
    def create(stats_config: StatsConfiguration, metadata: StatsMetadata) -> 'StatsLoader':
        """Construct a new concrete instance of StatsLoader that will load from the appropriate store as
        specified in stats_config

        Args:
            stats_config (StatsConfiguration): The configuration specified for storing and comparing statistics

        Returns:
            StatsLoader: A concrete instance of StatsLoader that will load from the store specified in configuration
        """
        return StatsFileLoader(stats_config, metadata) if stats_config.store == StatsStorageType.FILE else StatsAthenaLoader(stats_config, metadata)


class StatsFileLoader(StatsLoader):
    """Child class of StatsLoader that loads aggregated task stats from the local file system through JSON files"""

    def load_previous(self) -> Optional[AggregatedStats]:
        # Get a list of all AggregatedStats from stats.json files under path
        stats_list = self.__load_stats_from_files()

        # Filter those that don't match the config and current run's metadata
        filtered_stats = [stats for stats in stats_list
                          if self.__verify_metadata(stats.metadata)]

        # Sort them based upon timestamp, greater to lower
        filtered_stats.sort(key=lambda stats: stats.metadata.timestamp,
                            reverse=True)

        # Take the first item, if it exists -- this is the most recent, previous run
        return filtered_stats[0] if filtered_stats else None

    def load_average(self) -> Optional[AggregatedStats]:
        stats_list = self.__load_stats_from_files()
        verified_stats = [stats for stats in stats_list
                          if self.__verify_metadata(stats.metadata)]

        return _get_average_all_stats(verified_stats)

    def __load_stats_from_files(self, suffix: str = '.stats.json') -> List[AggregatedStats]:
        path = self.stats_config.path
        stats_files = [os.path.join(path, file)
                       for file in os.listdir(path) if file.endswith(suffix)]

        aggregated_stats_list = []
        for stats_file in stats_files:
            with open(stats_file) as json_file:
                aggregated_stats_list.append(
                    AggregatedStats(**json.load(json_file)))

        return aggregated_stats_list

    def __verify_metadata(self, loaded_metadata: StatsMetadata):
        return all([
            loaded_metadata.environment == self.stats_config.env,
            loaded_metadata.tag == self.stats_config.comp_tag,
            loaded_metadata.num_total_users == self.metadata.num_total_users,
            loaded_metadata.num_users_per_second == self.metadata.num_users_per_second,
            loaded_metadata.stats_reset_after_spawn == self.metadata.stats_reset_after_spawn,
            # Pick some delta that the runtimes should be under -- in this case, we're using 1 second
            loaded_metadata.total_runtime - self.metadata.total_runtime < 1.0
        ])


class StatsAthenaLoader(StatsLoader):
    """Child class of StatsLoader that loads aggregated task stats from S3 via Athena"""

    def __init__(self, stats_config: StatsConfiguration, metadata: StatsMetadata) -> None:
        self.client = boto3.client('athena', region_name='us-east-1')

        super().__init__(stats_config, metadata)

    def load_previous(self) -> Optional[AggregatedStats]:
        query = (
            f'SELECT cast(tasks as JSON) FROM "bfd"."{self.stats_config.athena_tbl}" '
            f'WHERE {self.__get_where_clause()} ORDER BY metadata.timestamp DESC '
            'LIMIT 1'
        )

        queried_stats = self.__get_stats_from_query(query)
        return queried_stats[0] if queried_stats else None

    def load_average(self) -> Optional[AggregatedStats]:
        query = (
            f'SELECT cast(tasks as JSON) FROM "bfd"."{self.stats_config.athena_tbl}" '
            f'WHERE {self.__get_where_clause()}'
        )

        queried_stats = self.__get_stats_from_query(query)
        return _get_average_all_stats(queried_stats)

    def __get_stats_from_query(self, query: str) -> List[AggregatedStats]:
        query_result = self.__run_query(query)
        raw_json_list = self.__get_raw_json_list(query_result)
        return self.__stats_from_json_list(raw_json_list)

    def __start_athena_query(self, query: str) -> Dict[str, Any]:
        return self.client.start_query_execution(
            QueryString=query,
            # The database should _always_ be "bfd", so we're hardcoding it here
            QueryExecutionContext={
                'Database': 'bfd'
            },
            # This method requires an OutputLocation, so we're using the "adhoc"
            # path defined in the BFD Insights data organization standards to
            # store query results
            ResultConfiguration={
                'OutputLocation': f's3://{self.stats_config.bucket}/adhoc/query_results/test_performance_stats/'
            },
            # The workgroup should also always be "bfd" if we're targeting the "bfd"
            # database
            WorkGroup='bfd'
        )

    def __get_athena_query_status(self, query_execution_id: str) -> str:
        # See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/athena.html#Athena.Client.get_query_execution
        # for the structure of the returned Dict
        return self.client.get_query_execution(
            QueryExecutionId=query_execution_id
        )['QueryExecution']['Status']['State']

    def __get_athena_query_result(self, query_execution_id: str) -> Dict[str, Any]:
        # See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/athena.html#Athena.Client.get_query_results
        # for the structure of the returned Dict
        return self.client.get_query_results(
            QueryExecutionId=query_execution_id
        )['ResultSet']['Rows']

    def __run_query(self, query: str, max_retries: int = 10) -> Optional[Dict[str, Any]]:
        start_response = self.__start_athena_query(query)
        query_execution_id = start_response['QueryExecutionId']

        for try_number in range(0, max_retries - 1):
            # Exponentially back-off from hitting the API to ensure we don't hit the API limit
            # See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            time.sleep((2**try_number * 100.0) / 1000.0)

            status = self.__get_athena_query_status(query_execution_id)

            if status == 'SUCCEEDED':
                break
            elif status == 'FAILED' or status == 'CANCELLED':
                raise RuntimeError(
                    f'Query failed to complete -- status returned was {status}')

        return self.__get_athena_query_result(query_execution_id)

    def __get_where_clause(self) -> str:
        # The following StatsMetadata fields need to be excluded from having their
        # equality check being auto-generated as they either should not be checked
        # (i.e. timestamp) or require a different type of check
        fields_to_exclude = ['timestamp', 'tag', 'total_runtime']
        filtered_fields = [field for field in fields(StatsMetadata)
                           if not field.name in fields_to_exclude]
        # Automatically generate a list of equality checks for all of the fields that are
        # necessary to validate to ensure that stats can be compared
        generated_checks = [self.__generate_check_str(field)
                            for field in filtered_fields]
        explicit_checks = [
            f"metadata.tag='{self.stats_config.comp_tag}'",
            f"(metadata.total_runtime - {self.metadata.total_runtime}) < 1.0",
        ]

        return ' AND '.join(generated_checks + explicit_checks)

    def __generate_check_str(self, field: Field) -> str:
        instance_value = getattr(self.metadata, field.name)
        # Anything that's a string should be surrounded by single quotes to denote it as a string
        # in SQL. Otherwise, no quotes should surround it
        rhs_operand = f"'{instance_value}'" if issubclass(
            field.type, str) else f"{instance_value}"

        return f"metadata.{field.name}={rhs_operand}"

    def __get_raw_json_list(self, query_result: List[Dict[str, List[Dict[str, str]]]]) -> List[str]:
        # The data is returned as an array of dicts, each with a 'Data' key. These 'Data'
        # dicts values are arrays of dicts with the key being the data type and the value being
        # the actual returned result. The first dict in the array is the column names, and subsequent
        # items are the data

        # We make a few assumptions:
        # 1. The first item is always the column names
        # 2. The data returned is always of the type `VarCharValue`
        # 3. We are only retrieving a single column
        return [item['Data'][0]['VarCharValue'] for item in query_result[1:]]

    def __stats_from_json_list(self, raw_json_list: List[str]) -> List[AggregatedStats]:
        # This is bad, but Athena does not have any way to sanely export structs in such
        # a way that we can use a standard parser (JSON, CSV, etc.); either we export in
        # their JSON-ish proprietary format and keep the names of fields but have no way
        # to use a standard parser AND lose type information OR we can export "as JSON" but
        # Athena opts to export structs as JSON arrays without field names. Given that we
        # can use a JSON parser to parse a JSON array and we can assume stable order, we are
        # sticking with getting results as a JSON array and working from there.

        # The serialization from a TaskStats array will give a list of values, so the serialized
        # list will be a list of lists of lists (in inner to outer order:
        # TaskStats -> AggregatedStats -> List[AggregatedStats])
        serialized_list: List[List[List[Any]]] = [json.loads(json_str) for json_str
                                                  in raw_json_list]
        # The metadata is unnecessary here since by the time we've gotten here the metadata for each of the
        # tasks we're serializing here has already been checked
        return [AggregatedStats(metadata=None,
                                tasks=[TaskStats.from_list(values_list) for values_list in agg_tasks_list])
                for agg_tasks_list in serialized_list]


def _bucket_tasks_by_name(all_stats: List[AggregatedStats]) -> Dict[str, List[TaskStats]]:
    tasks_by_name: Dict[str, List[TaskStats]] = {}
    for stats in all_stats:
        for task in stats.tasks:
            if not task.task_name in tasks_by_name:
                tasks_by_name[task.task_name] = []

            tasks_by_name[task.task_name].append(task)

    return tasks_by_name


def _get_average_task_stats(all_tasks: List[TaskStats]) -> Optional[TaskStats]:
    if not all_tasks:
        return None

    if not all(x.task_name == all_tasks[0].task_name for x in all_tasks):
        raise ValueError('The list of TaskStats must be for the same task')

    # Exclude fields that are not statistics and the response time percentiles
    # dict which will be handled on its own later
    fields_to_exclude = ['task_name',
                         'request_method', 'response_time_percentiles']
    stats_to_average = [field.name for field in fields(TaskStats)
                        if not field.name in fields_to_exclude]
    # Calculate the mean automatically for every matching stat in the list of
    # all stats, and then put the mean in a dict
    avg_task_stats = {stat_name: mean(getattr(task, stat_name) for task in all_tasks)
                      for stat_name in stats_to_average}

    # Get the common keys between all of the response time percentile dicts in
    # the list of task stats
    common_percents = reduce(lambda prev, next: prev & next,
                             (task.response_time_percentiles.keys() for task in all_tasks))
    # Do the same thing as above but for each entry in each response time percentile dict --
    # get the mean of each percentile across all tasks and make it the value of a new
    # percentile dict
    avg_task_percents = {p: mean(task.response_time_percentiles[p] for task in all_tasks)
                         for p in common_percents}

    return TaskStats(task_name=all_tasks[0].task_name, request_method=all_tasks[0].request_method,
                     response_time_percentiles=avg_task_percents, **avg_task_stats)


def _get_average_all_stats(all_stats: List[AggregatedStats]) -> Optional[AggregatedStats]:
    partitioned_task_stats = _bucket_tasks_by_name(all_stats)
    averaged_tasks = [_get_average_task_stats(tasks)
                      for tasks in partitioned_task_stats.values()]

    # With an averaged aggregated stats there really is no such thing as metadata
    # since it's the result of many
    return AggregatedStats(metadata=None, tasks=averaged_tasks) if averaged_tasks else None
