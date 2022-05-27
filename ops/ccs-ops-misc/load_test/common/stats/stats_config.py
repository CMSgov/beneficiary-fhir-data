from enum import Enum
import re
from dataclasses import dataclass
from abc import ABC, abstractmethod


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
