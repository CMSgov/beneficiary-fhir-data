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
    def to_key_val_str(self) -> str:
        """Returns a key-value string representation of this StatsStorageConfig instance.
        Used to serialize this object to config.

        Returns:
            str: The key-value string representation of this object.
        """
        return

    @staticmethod
    def from_key_val_str(key_val_str: str):
        """Constructs a concrete instance of StatsStorageConfig from a given string in key-value format seperated
        by semi-colons ("key1=value1;key2=value2").

        Args:
            key_val_str (str): The key-value representation of this object.

        Raises:
            ValueError: Raised if the passed string does not follow the proper format.

        Returns:
            StatsStorageConfig: Returns a concrete instance of StatsStorageConfig with the values specified in the key-value string.
        """
        key_vals_list = key_val_str.split(';')
        # Create a dictionary from the list of split key-value pairs by parsing each
        # "key=value" string in the list into {'key': 'value'}.
        # Empty values are simply considered to be empty strings.
        config_dict = {k: str(v or '') for k, v in (
            key_val.split('=') for key_val in key_vals_list)}

        # Check for required parameters, like type, tag, environment
        if not set(['type', 'tag', "env"]).issubset(set(config_dict.keys())):
            raise ValueError('type, tag, and env must be specified') from None

        try:
            storage_type = StatsStorageType[config_dict['type'].upper()]
        except KeyError:
            raise ValueError(
                '"type" must be either "file" or "s3"') from None

        try:
            stats_environment = StatsEnvironment[config_dict['env'].upper()]
        except KeyError:
            raise ValueError(
                '"env" must be either "TEST" or "PROD"') from None

        tag = config_dict['tag']
        # Tag must follow the BFD Insights data convention constraints for
        # partition/folders names, as it is used as a partition folder when uploading
        # to S3
        if re.fullmatch('[a-z0-9_]+', tag) == None or tag == '':
            raise ValueError(
                '"tag" must only consist of lower-case letters, numbers and the "_" character') from None

        if storage_type == StatsStorageType.FILE:
            return StatsFileStorageConfig(stats_environment, tag, config_dict.get('path') or '')
        else:
            if not 'bucket' in config_dict:
                raise ValueError(
                    '"bucket" must be specified if "type" is "s3"') from None

            return StatsS3StorageConfig(stats_environment, tag, config_dict['bucket'])


@dataclass
class StatsFileStorageConfig(StatsStorageConfig):
    """Concrete dataclass inheriting from StatsStorageConfig distinguishing a config for storing
    statistics files to a local file"""
    file_path: str
    """The parent path of the statistics file that will be written to disk"""

    def to_key_val_str(self) -> str:
        return f'type=file;env={self.stats_environment.name};tag={self.tag};path={self.file_path}'


@dataclass
class StatsS3StorageConfig(StatsStorageConfig):
    """Concrete dataclass inheriting from StatsStorageConfig distinguishing a config for storing
    statistics files to an S3 Bucket"""
    bucket: str
    """The AWS S3 Bucket that statistics will be written to"""

    def to_key_val_str(self) -> str:
        return f'type=s3;env={self.stats_environment.name};tag={self.tag};bucket={self.bucket}'
