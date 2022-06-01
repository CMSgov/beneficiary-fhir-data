import dataclasses
from enum import Enum
import re
from dataclasses import dataclass
from typing import Optional, Type


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


class StatsComparisonType(Enum):
    """Enumeration for each possible type of stats comparison"""
    PREVIOUS = 1
    """Indicates that the comparison will be against the most recent, previous run under a given tag"""
    AVERAGE = 2
    """Indicates that the comparison will be against the average of all runs under a given tag"""


@dataclass
class StatsConfiguration():
    """Dataclass that holds data about where and how aggregated performance statistics are stored"""
    type: StatsStorageType
    """The storage type that the stats will be written to"""
    env: StatsEnvironment
    """The test running environment from which the statistics will be collected"""
    tag: str
    """A simple string tag that is used to partition collected statistics when stored"""
    path: Optional[str]
    """The local parent directory where JSON files will be written to. Used only if type is file, ignored if type is s3"""
    bucket: Optional[str]
    """The AWS S3 Bucket that the JSON will be written to. Used only if type is s3, ignored if type is file"""

    def to_key_val_str(self) -> str:
        """Returns a key-value string representation of this StatsConfiguration instance.
        Used to serialize this object to config.

        Returns:
            str: The key-value string representation of this object.
        """
        as_dict = dataclasses.asdict(self)
        dict_non_empty = {k: v for k,
                          v in as_dict.items() if v is not None and v != ''}
        return ';'.join([f'{k}={str(v) if not isinstance(v, Enum) else v.name}' for k, v in dict_non_empty.items()])

    def from_key_val_str(key_val_str: str):
        """Constructs a concrete instance of StatsConfiguration from a given string in key-value format seperated
        by semi-colons ("key1=value1;key2=value2").

        Args:
            key_val_str (str): The key-value representation of this object.

        Raises:
            ValueError: Raised if the passed string does not follow the proper format.

        Returns:
            StatsConfiguration: Returns a concrete instance of StatsConfiguration with the values specified in the key-value string.
        """
        key_vals_list = key_val_str.split(';')
        # Create a dictionary from the list of split key-value pairs by parsing each
        # "key=value" string in the list into {'key': 'value'}.
        # Empty values are simply considered to be empty strings.
        config_dict = {k: str(v or '') for k, v in (
            key_val.split('=') for key_val in key_vals_list)}

        # Check for required parameters, like type, tag, environment
        if not set(['type', 'tag', 'env']).issubset(set(config_dict.keys())):
            raise ValueError('type, tag, and env must be specified') from None

        # Handle all of the enum-backed fields
        storage_type = _enum_from_val(
            config_dict['type'], StatsStorageType, 'type')
        stats_environment = _enum_from_val(
            config_dict['env'], StatsEnvironment, 'env')

        tag = config_dict['tag']
        # Tag must follow the BFD Insights data convention constraints for
        # partition/folders names, as it is used as a partition folder when uploading
        # to S3
        if re.fullmatch('[a-z0-9_]+', tag) == None or tag == '':
            raise ValueError(
                '"tag" must only consist of lower-case letters, numbers and the "_" character') from None

        if storage_type == StatsStorageType.S3 and not 'bucket' in config_dict:
            raise ValueError(
                '"bucket" must be specified if "type" is "s3"') from None

        return StatsConfiguration(type=storage_type, env=stats_environment, tag=tag,
                                  path=config_dict.get('path') or '', bucket=config_dict.get('bucket'))


def _enum_from_val(val: str, enum_type: Type[Enum], field_name: str):
    try:
        return enum_type[val.upper()]
    except KeyError:
        raise ValueError(
            f'"{field_name}" must be one of: {", ".join([e.name for e in enum_type])}') from None
