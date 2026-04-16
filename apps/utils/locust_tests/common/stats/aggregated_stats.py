"""Members of this file/module should be related to the collection of performance statistics during
a test run as well as the representation of those statistics via dataclasses or other suitable
objects.
"""

import hashlib
import time
from dataclasses import dataclass
from enum import StrEnum
from typing import Any

from common.validation import ValidationResult
from locust.env import Environment
from locust.stats import PERCENTILES_TO_REPORT, StatsEntry

ResponseTimePercentiles = dict[str, int | float]
"""A type representing a dictionary of stringified percentile keys to their integer or
floating-point values"""


class StatsCollector:
    """Used to collect a snapshot of aggregated performance statistics of all tasks, or endpoints,
    that ran in the current Locust environment.
    """

    def __init__(
        self,
        locust_env: Environment,
        stats_tags: list[str],
        running_env: str,
    ) -> None:
        """Create a new instance of StatsCollector given the current Locust environment and a list
        of percentiles to report.

        Args:
            locust_env (Environment): Current Locust environment
            stats_tag (str): A string which tags the output JSON; used to distinguish between
            separate test runs
            running_env (str): A string which represents the current testing environment.
            May be test, prod-sbx, prod, or any ephemeral environment whose name ends with one of
            those values, e.g [TICKET_NUM]-test. Case-insensitive.
        """
        super().__init__()

        self.locust_env = locust_env
        self.stats_tags = stats_tags
        self.running_env = running_env

    def __sort_stats(self, stats: dict[Any, StatsEntry]) -> list[StatsEntry]:
        """Extract a sorted list of StatsEntrys from a dictionary of StatsEntrys.

        Args:
            stats (Dict[Any, StatsEntry]): Dictionary of Locust StatsEntrys

        Returns:
            List[StatsEntry]: A sorted list of Locust StatsEntrys
        """
        return [stats[key] for key in sorted(stats.keys())]

    def __get_task_stats_list(self) -> list["TaskStats"]:
        """Return a list of TaskStats representing the performance statistics of _all_ Locust tasks
        that ran.

        Returns:
            List[TaskStats]: A List of TaskStats that represent the performance statistics of all
            Locust tasks
        """
        stats = self.locust_env.stats
        return [
            TaskStats.from_stats_entry(stats_entry)
            for stats_entry in self.__sort_stats(stats.entries)
        ]

    def collect_stats(self) -> "AggregatedStats":
        """Return an AggregatedStats instance representing a snapshot of the
        aggregated performance statistics of the current Locust environment at current time.

        Returns:
            AggregatedStats: An instance of AggregatedStats representing a snapshot of all stats at
            the current time
        """
        tasks = self.__get_task_stats_list()
        return AggregatedStats(
            metadata=StatsMetadata.from_locust_env(
                timestamp=int(time.time()),
                tags=self.stats_tags,
                environment=self.running_env,
                tasks_names=[task.task_name for task in tasks],
                locust_env=self.locust_env,
            ),
            totals=TaskStats.from_stats_entry(self.locust_env.stats.total),
            tasks=tasks,
        )


@dataclass
class TaskStats:
    """Dataclass representing the performance statistics of a given Locust task."""

    task_name: str
    """The name specified by the task"""
    request_method: str
    """The HTTP request method (i.e. POST, GET) used by the Task"""
    num_requests: int
    """The total number of requests of this Task sent during the test run"""
    num_failures: int
    """The total number of failures received by this Task during the test run"""
    median_response_time: int
    """The median response time, in milliseconds, of each of this Task's requests"""
    average_response_time: float
    """The average response time, in milliseconds, of each of this Task's requests"""
    min_response_time: int
    """The fastest response time, in milliseconds, out of all this Task's requests"""
    max_response_time: int
    """The slowest respone time, in milliseconds, out of all this Task's requests"""
    total_reqs_per_second: float
    """The average number of requests-per-second of this Task's requests over the test run"""
    total_fails_per_sec: float
    """The average number of failures-per-second of this Task's requests over the test run"""
    response_time_percentiles: ResponseTimePercentiles
    """A dictionary of response time percentiles indicating the percentage of requests that
    completed in a particular timeframe"""

    @classmethod
    def from_stats_entry(cls, stats_entry: StatsEntry) -> "TaskStats":
        """Construct an instance of TaskStats from a Locust StatsEntry
        instance.

        Args:
            stats_entry (StatsEntry): A Locust StatsEntry instance encapsulating a Task's stats

        Returns:
            TaskStats: A TaskStats dataclass instance that encapsulates the most important stats of
            a given Task
        """
        return cls(
            task_name=str(stats_entry.name),
            request_method=str(stats_entry.method),
            num_requests=int(stats_entry.num_requests),
            num_failures=int(stats_entry.num_failures),
            median_response_time=int(stats_entry.median_response_time),
            average_response_time=float(stats_entry.avg_response_time),
            min_response_time=int(stats_entry.min_response_time or 0),
            max_response_time=int(stats_entry.max_response_time),
            total_reqs_per_second=float(stats_entry.total_rps),
            total_fails_per_sec=float(stats_entry.total_fail_per_sec),
            response_time_percentiles=cls.__get_percentiles_dict(stats_entry),
        )

    @classmethod
    def __get_percentiles_dict(cls, stats_entry: StatsEntry) -> ResponseTimePercentiles:
        """Return a dictionary of response time percentiles indicating the percentage of requests
        that completed in a particular timeframe.

        Args:
            stats_entry (StatsEntry): The Locust StatsEntry object which encodes a particular task's
            statistics

        Returns:
            Dict[str, int]: A dictionary of response time percentiles
        """
        if not stats_entry.num_requests:
            # If there were no requests made, simply return a dictionary with 0
            # for each of its values
            return {str(k): 0 for k in PERCENTILES_TO_REPORT}

        return {
            str(percentile): int(stats_entry.get_response_time_percentile(percentile) or 0)
            for percentile in PERCENTILES_TO_REPORT
        }


class FinalCompareResult(StrEnum):
    """Enum that indicates the _overall_ result of a given AggregatedStats' comparison with a
    baseline AggregatedStats. Used to filter out runs that did not pass comparison against a
    baseline, but may want to be stored for future analysis.
    """

    NOT_APPLICABLE = "NOT_APPLICABLE"
    """Indicates the run either was explicitly not compared against a baseline (i.e. the user did
    not specify to compare against anything) or there was no such baseline to compare against (i.e.
    it was the first ever run)"""
    PASSED = "PASSED"
    """Indicates that the run passed comparison against a baseline; this means _all_ stats
    passed comparison for the totals _and_ each task"""
    FAILED = "FAILED"
    """Indicates that the run failed comparison against a baseline; this means at least _one_ stat
    exceeded its failure threshold for its percent ratio against the baseline's equivalent stat"""


@dataclass
class StatsMetadata:
    """A dataclass encoding metadata that is necessary when comparing snapshots of aggregated
    performance stats.
    """

    timestamp: int
    """A timestamp indicating the time a stats snapshot was collected"""
    tags: list[str]
    """A list of simple string tags that partition or bucket emitted statistics"""
    environment: str
    """The environment that the stats were collected from"""
    stats_reset_after_spawn: bool
    """Indicates whether the test run's stats were reset after all users were spawned"""
    num_total_users: int
    """The number of users spawned running Tasks during the test run"""
    num_users_per_second: float
    """The number of users spawned per second when the test run started"""
    requested_runtime: int
    """The runtime requested by the user via --spawned-runtime or --runtime"""
    total_runtime: float
    """The actual, total runtime of the test run"""
    user_classes: list[str]
    """A List of the user classes ran during the test run"""
    hash: str
    """A hash that encodes various information about the running tests to ensure that comparisons
    can be made. Two (or more) AggregatedStats instances having the same hash means that they are
    comparable"""
    compare_result: FinalCompareResult = FinalCompareResult.NOT_APPLICABLE
    """Indicates the result of comparison against a baseline. Used to filter out failures when
    doing comparisons during load"""
    validation_result: ValidationResult = ValidationResult.NOT_APPLICABLE
    """Indicates the result of validation (checking >0 failures, and static percentile SLAs). Used
    to filter out failing runs when doing comparisons"""

    @classmethod
    def from_locust_env(
        cls,
        timestamp: int,
        tags: list[str],
        environment: str,
        tasks_names: list[str],
        locust_env: Environment,
    ) -> "StatsMetadata":
        """Construct an instance of StatsMetadata by computing its fields from
        a given Locust environment.

        Args:
            timestamp (int): A Unix timestamp indicating the time that the stats were collected
            tag (str): A simple string tag that is used as a partitioning tag
            environment (str): The environment that the test run was started in
            tasks_names (str): A List of the names of all of the tasks that ran
            locust_env (Environment): The current Locust environment

        Raises:
            ValueError: If no parsed_options exist on locust_env, or if there is no
            last_request_timestamp

        Returns:
            StatsMetadata: A StatsMetadata instance encapsulating all of the necessary metadata to
            store and compare statistics
        """
        if not locust_env.parsed_options:
            raise ValueError(
                "Parsed options did not exist on Locust environment -- is Locust being ran as a"
                " library?"
            )

        if not locust_env.stats.last_request_timestamp:
            raise ValueError("No requests were ran, stats cannot be aggregated")

        ran_user_classes = [user_class.__name__ for user_class in locust_env.user_classes]
        num_users = locust_env.parsed_options.num_users
        spawn_rate = locust_env.parsed_options.spawn_rate
        stats_reset_after_spawn = locust_env.reset_stats
        requested_runtime = int(
            locust_env.parsed_options.spawned_runtime or locust_env.parsed_options.runtime
        )

        return cls(
            timestamp,
            tags,
            environment,
            stats_reset_after_spawn=stats_reset_after_spawn,
            num_total_users=num_users,
            num_users_per_second=spawn_rate,
            requested_runtime=requested_runtime,
            total_runtime=locust_env.stats.last_request_timestamp - locust_env.stats.start_time,
            user_classes=ran_user_classes,
            hash=cls.__generate_hash_str(
                user_classes_names=ran_user_classes,
                tasks_names=tasks_names,
                num_users=num_users,
                requested_runtime=requested_runtime,
                spawn_rate=spawn_rate,
                stats_reset_after_spawn=stats_reset_after_spawn,
                environment=environment,
            ),
        )

    @classmethod
    def __generate_hash_str(
        cls,
        user_classes_names: list[str],
        tasks_names: list[str],
        num_users: int,
        requested_runtime: int,
        spawn_rate: int,
        stats_reset_after_spawn: bool,
        environment: str,
    ) -> str:
        # The value of the pepper doesn't matter, it's just here so that if anything that composes
        # the hash doesn't change but some external factor (like the data the Regression Suite uses)
        # does, we can forcefully change the hash so that a new baseline can be generated
        pepper = "pepper1"
        # Generate a SHA256 hash string from various bits of information
        str_to_hash = "".join(
            [
                "".join(sorted(user_classes_names)),
                "".join(sorted(tasks_names)),
                str(num_users),
                str(requested_runtime),
                str(spawn_rate),
                str(stats_reset_after_spawn),
                str(environment),
                pepper,
            ]
        )
        return hashlib.sha256(str.encode(str_to_hash, encoding="utf-8")).hexdigest()


@dataclass
class AggregatedStats:
    """A dataclass encoding the entirety of performance statistics for every Locust Task along with
    metadata necessary for comparison and storage.
    """

    metadata: StatsMetadata | None
    """An instance of StatsMetadata that encapsulates the necessary metadata about the set of Task
    statistics"""
    totals: TaskStats
    """The aggregated totals of performance statistics for every task ran"""
    tasks: list[TaskStats]
    """A list of TaskStats where each entry represents the performance statistics of each Task"""

    def __init__(
        self,
        totals: TaskStats | dict[str, Any],
        tasks: list[TaskStats] | list[dict[str, Any]],
        metadata: StatsMetadata | dict[str, Any] | None = None,
    ) -> None:
        # Support conversion directly from a nested dictionary, such as when loading from JSON files
        # or from Athena
        if isinstance(metadata, dict):
            self.metadata = StatsMetadata(**metadata)
        else:
            self.metadata = metadata

        if isinstance(totals, dict):
            self.totals = TaskStats(**totals)
        else:
            self.totals = totals

        if all(isinstance(x, dict) for x in tasks):
            self.tasks = [TaskStats(**task_dict) for task_dict in tasks]  # type: ignore
        else:
            self.tasks = tasks  # type: ignore
