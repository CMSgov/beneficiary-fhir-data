from locust.stats import StatsEntry, sort_stats, PERCENTILES_TO_REPORT
from locust.env import Environment
import time
from typing import Dict, List

from common.stats.stats_config import StatsEnvironment

# We are re-exporting Locust's default percentiles list here so that consumers of members
# of this file do not need to also import from the locust.stats module if they want to use
# the default reporting percentiles
PERCENTILES_TO_REPORT = PERCENTILES_TO_REPORT
"""A list of floating-point percentiles to report when generating JSON performance reports"""


class AggregatedStats(object):
    """Represents a snapshot of aggregated performance statistics of all tasks, or endpoints, that
    ran in the current Locust environment"""

    def __init__(self, locust_env: Environment, percentiles_to_report: List[float], stats_tag: str, running_env: StatsEnvironment = StatsEnvironment.TEST) -> None:
        """Creates a new instance of AggregatedStats given the current Locust environment and a list of percentiles to report.

        Args:
            locust_env (Environment): Current Locust environment
            percentiles_to_report (List[float]): List of percentiles to report in the generated JSON
            stats_tag (str): A string which tags the output JSON; used to distinguish between separate test runs
            running_env (StatsEnvironment, optional): A StatsEnvironment enum which represents the current testing environment; either TEST or PROD. Defaults to TEST.
        """
        super().__init__()
        self.locust_env = locust_env
        self.percentiles_to_report = percentiles_to_report
        self.percentiles_na = ["N/A"] * len(self.percentiles_to_report)

        self.stats_tag = stats_tag
        self.running_env = running_env

    def _get_readable_percentile(self, percentile: float) -> str:
        """Returns a human-readable percent string for a given value from 0-1

        Args:
            percentile (float): Floating-point percentile number between 0-1

        Returns:
            str: A human-readable percent string (i.e. "100%", "95%") for the given percentile number
        """
        return f"{int(percentile * 100) if (percentile * 100).is_integer() else round(100 * percentile, 6)}"

    def _get_percentiles_dict(self, stats_entry: StatsEntry) -> Dict[str, int]:
        """Returns a dictionary of a human-readable percentile string to its reported value

        Args:
            stats_entry (StatsEntry): The Locust StatsEntry object which encodes a particular task's statistics

        Returns:
            Dict[str, int]: A dictionary of human-readable percentile strings to the percent response time
        """
        if not stats_entry.num_requests:
            return self.percentiles_na

        return {self._get_readable_percentile(percentile): int(stats_entry.get_response_time_percentile(percentile) or 0) for percentile in self.percentiles_to_report}

    def _get_stats_entry_dict(self, stats_entry: StatsEntry) -> Dict[str, any]:
        """Returns a dictionary representation of a StatsEntry object

        Args:
            stats_entry (StatsEntry): The Locust StatsEntry object which encodes a particular task's statistics

        Returns:
            Dict[str, any]: A dictionary that represents most of the statistics exposed by StatsEntry, including percentiles
        """
        return {**{
            'name': stats_entry.name,
            'requestMethod': stats_entry.method,
            'numRequests': stats_entry.num_requests,
            'numFailures': stats_entry.num_failures,
            'medianResponseTime': stats_entry.median_response_time,
            'avgResponseTime': stats_entry.avg_response_time,
            'minResponseTime': stats_entry.min_response_time or 0,
            'maxResponseTime': stats_entry.max_response_time,
            'avgContentLength': stats_entry.avg_content_length,
            'totalRequestsPerSecond': stats_entry.total_rps,
            'totalFailuresPerSecond': stats_entry.total_fail_per_sec
        }, **self._get_percentiles_dict(stats_entry)}

    def _get_stats_entries_list(self) -> List[Dict[str, any]]:
        """Returns a list of dictionaries representing the performance statistics of _all_ Locust tasks that ran

        Returns:
            List[Dict[str, any]]: A List of Dicts that represent the performance statistics of all Locust tasks
        """
        stats = self.locust_env.stats
        return [self._get_stats_entry_dict(stats_entry) for stats_entry in sort_stats(stats.entries)]

    @property
    def all_stats(self) -> Dict[str, any]:
        """A property that returns a snapshot Dict of the current stats of aggregated performance statistics of the current
        Locust environment.

        Returns:
            Dict[str, any]: A dictionary of the aggregated performance statistics of all endpoints
        """
        return {**{
            'timestamp': int(time.time()),
            'tag': self.stats_tag,
            'environment': self.running_env.name,
            'statsResetAfterSpawn': self.locust_env.reset_stats,
            'numUsers': self.locust_env.parsed_options.num_users,
            'usersPerSecond': self.locust_env.parsed_options.spawn_rate,
            # We cannot get the user provided runtime directly; however, we can compute a more exact
            # runtime by subtracting the start time from the last request's time
            'runtime': self.locust_env.stats.last_request_timestamp - self.locust_env.stats.start_time
        }, **{
            'endpoints': self._get_stats_entries_list()
        }}
