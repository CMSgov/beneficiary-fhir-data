"""
Much of this file is adapted from equivalent Locust code, particularly locust.stats.StatsCSV.
Code in this file relates to the storing and retrieval of performance statistics in a given Locust
environment.
"""
from locust.stats import StatsEntry, sort_stats, PERCENTILES_TO_REPORT
from locust.env import Environment
import time
import os
import json
from enum import Enum
from typing import Dict, List, Optional
import re
from dataclasses import dataclass
from abc import ABC, abstractmethod

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
from gevent import monkey
monkey.patch_all()
import boto3

PERCENTILES_TO_REPORT = PERCENTILES_TO_REPORT
"""A list of floating-point percentiles to report when generating JSON performance reports"""


class StatsStorageType(Enum):
    """Enumeration for each available type of storage for JSON stats"""
    FILE = 1
    """Indicates that aggregated statistics will be stored to a local file"""
    S3 = 2
    """Indicates that aggregated statistics will be stored to an S3 bucket"""


class StatsEnvironment(Enum):
    """Enumeration for each possible test running environment"""
    TEST = 1
    """Indicates that the running environment is in testing, using testing resources"""
    PROD = 2
    """Indicates that the running environment is in production, using production resources"""


@dataclass
class StatsStorageConfig(ABC):
    """Abstract dataclass that holds data about where and how aggregated performance statistics are stored"""
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
        # Tag must follow the BFD Insights data convention constraints for
        # partition/folders names, as it is used as a partition folder when uploading
        # to S3
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
    statistics files to a local file"""
    file_path: str
    """The parent path of the statistics file that will be written to disk"""

    def to_arg_str(self) -> str:
        return f'file:{self.stats_environment.name}:{self.tag}:{self.file_path}'


@dataclass
class StatsS3StorageConfig(StatsStorageConfig):
    """Concrete dataclass inheriting from StatsStorageConfig distinguishing a config for storing
    statistics files to an S3 Bucket"""
    bucket: str
    """The AWS S3 Bucket that statistics will be written to"""

    def to_arg_str(self) -> str:
        return f's3:{self.stats_environment.name}:{self.tag}:{self.bucket}'


class AggregatedStats(object):
    """Represents a snapshot of aggregated performance statistics of all tasks, or endpoints, that
    ran in the current Locust environment"""

    def __init__(self, locust_env: Environment, percentiles_to_report: List[float], stats_tag: str, running_env: StatsEnvironment = StatsEnvironment.TEST) -> None:
        """Creates a new instance of AggregatedStats given the current Locust environment and a list of percentiles to report.

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


class StatsJsonFileWriter(object):
    """Writes an AggegratedStats instance to a specified directory path in JSON format"""

    def __init__(self, stats: AggregatedStats) -> None:
        """Creates a new instance of StatsJsonFileWriter given an AggregatedStats object

        Args:
            stats (AggregatedStats): An AggregatedStats object that encodes the aggregated performance statistics of all Locust tasks in the current environment
        """
        super().__init__()

        self.stats = stats

    def write(self, path: str = '') -> None:
        """Writes the JSON-formatted statistics to the given path

        Args:
            path (str, optional): The _parent_ path of the file to write to disk. Defaults to ''.
        """
        with open(os.path.join(path, f'{self.stats.running_env.name}-{self.stats.stats_tag}-{int(time.time())}.json'), 'x') as json_file:
            json_file.write(json.dumps(self.stats.all_stats, indent=4))


class StatsJsonS3Writer(object):
    """Writes an AggegratedStats instance to a specified S3 bucket in JSON format"""

    def __init__(self, stats: AggregatedStats) -> None:
        """Creates a new instance of StatsJsonS3Writer given an AggregatedStats object

        Args:
            stats (AggregatedStats): An AggregatedStats object that encodes the aggregated performance statistics of all Locust tasks in the current environment
        """
        super().__init__()

        self.stats = stats
        self.s3 = boto3.client('s3')

    def write(self, bucket: str) -> None:
        """Writes the JSON-formatted statistics to the given S3 bucket to a pre-determined path
        following BFD Insights data organization standards

        Args:
            bucket (str): The S3 bucket in AWS to write the JSON to
        """
        self.s3.put_object(
            Bucket=bucket, Key=f'databases/bfd/test_performance_stats/env={self.stats.running_env.name}/tag={self.stats.stats_tag}/{int(time.time())}.json', Body=json.dumps(self.stats.all_stats))
