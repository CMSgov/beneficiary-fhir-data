"""
Much of this file is adapted from equivalent Locust code, particularly locust.stats.StatsCSV
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass
import re
from typing import Dict, List, Optional
from enum import Enum
import json
import os
import time
from locust.env import Environment
from locust.stats import StatsEntry, sort_stats, PERCENTILES_TO_REPORT

PERCENTILES_TO_REPORT = PERCENTILES_TO_REPORT
"""A list of floating-point percentiles to report when generating JSON performance reports"""


class StatsStorageType(Enum):
    """Enumeration for each available type of storage for JSON stats"""
    FILE = 1
    S3 = 2


class StatsEnvironment(Enum):
    """Enumeration for each possible test running environment"""
    TEST = 1
    PROD = 2


@dataclass
class StatsStorageConfig(ABC):
    """Abstract dataclass that holds data about where and how aggregated JSON performance statistics are stored"""
    stats_environment: StatsEnvironment
    """The test running environment from which the statistics will be collected"""
    tag: str
    """A simple string tag that is used to partition collected statistics when stored"""

    @abstractmethod
    def to_arg_str(self) -> str:
        """Returns an "arg string" representation of this StatsStorageConfig instance. Follows the format
        <STORAGE_TYPE>:<RUNNING_ENVIRONMENT>:<TAG>:<PATH_OR_BUCKET> 
        Used to serialize this object to config.

        Returns:
            str: The "arg string" representation of this object.
        """
        return

    @staticmethod
    def from_arg_str(arg_str: str):
        """Constructs a concrete instance of StatsStorageConfig from a given "arg string" in the format of
        "<STORAGE_TYPE>:<RUNNING_ENVIRONMENT>:<TAG>:<PATH_OR_BUCKET>".

        Args:
            arg_str (str): The "arg string" representation of this object.

        Raises:
            ValueError: Raised if the passed string does not follow the proper format.

        Returns:
            StatsStorageConfig: Returns a concrete instance of StatsStorageConfig with the values specified in the "arg string".
        """
        items = arg_str.split(':')
        if len(items) != 4:
            raise ValueError(
                'Input must follow format: "<STORAGE_TYPE>:<RUNNING_ENVIRONMENT>:<TAG>:<PATH_OR_BUCKET>"') from None

        try:
            storage_type = StatsStorageType[items[0].upper()]
        except KeyError:
            raise ValueError(
                'STORAGE_TYPE must be either "file" or "s3"') from None

        try:
            stats_environment = StatsEnvironment[items[1].upper()]
        except KeyError:
            raise ValueError(
                'RUNNING_ENVIRONMENT must be either "TEST" or "PROD"') from None

        tag = items[2]
        if re.fullmatch('[a-z0-9_]+', tag) == None:
            raise ValueError(
                'TAG must only consist of lower-case letters, numbers and the "_" character') from None

        if storage_type == StatsStorageType.FILE:
            return StatsFileStorageConfig(stats_environment, tag, items[3])
        else:
            return StatsS3StorageConfig(stats_environment, tag, items[3])


@dataclass
class StatsFileStorageConfig(StatsStorageConfig):
    """Concrete dataclass inheriting from StatsStorageConfig distinguishing a config for storing
    JSON files to a local file"""
    file_path: str

    def to_arg_str(self) -> str:
        return f'file:{self.stats_environment.name}:{self.tag}:{self.file_path}'


@dataclass
class StatsS3StorageConfig(StatsStorageConfig):
    """Concrete dataclass inheriting from StatsStorageConfig distinguishing a config for storing
    JSON files to an S3 Bucket"""
    bucket: str

    def to_arg_str(self) -> str:
        return f's3:{self.stats_environment.name}:{self.tag}:{self.bucket}'


class StatsJson(object):
    """Class to generate performance statistics in JSON format"""

    def __init__(self, locust_env: Environment, percentiles_to_report: List[float], stats_tag: str, running_env: StatsEnvironment = StatsEnvironment.TEST) -> None:
        """Creates a new instance of StatsJson given the current Locust environment and a list of percentiles to report.

        Args:
            environment (Environment): Current Locust environment
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
        return f"{int(percentile * 100) if (percentile * 100).is_integer() else round(100 * percentile, 6)}%"

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

    def get_stats_json(self, pretty_print: bool = False) -> str:
        """Returns a JSON-formatted string that encodes the performance statistics of all Locust tasks in the current environment

        Args:
            pretty_print (bool, optional): A boolean which if True will generate the JSON in a more human-readable format. Defaults to False.

        Returns:
            str: A JSON-formatted string that encodes the performance statistics of the current Locust run
        """
        full_dict = {**{
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

        return json.dumps(full_dict, indent=(4 if pretty_print else None))


class StatsJsonFileWriter(object):
    def __init__(self, stats_json: StatsJson) -> None:
        """Creates a new instance of StatsJsonFileWriter given a StatsJson object

        Args:
            stats_json (StatsJson): A StatsJson object that encodes the aggregated performance statistics of all Locust tasks in the current environment
        """
        super().__init__()

        self.stats_json = stats_json

    def write(self, path: str = '', pretty_print: bool = False) -> None:
        """Writes the JSON-formatted statistics to the given path

        Args:
            path (str, optional): The _parent_ path of the file to write to disk. Defaults to ''.
            pretty_print (bool, optional): A boolean which if True will write the JSON in a more human-readable format. Defaults to False.
        """
        with open(os.path.join(path, f'{self.stats_json.running_env.name}-{self.stats_json.stats_tag}-{int(time.time())}.json'), 'x') as json_file:
            json_file.write(self.stats_json.get_stats_json(
                pretty_print=pretty_print))
