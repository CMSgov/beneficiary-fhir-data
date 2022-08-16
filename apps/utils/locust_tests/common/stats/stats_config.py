import dataclasses
import logging
import re
from argparse import Namespace
from dataclasses import dataclass
from enum import Enum
from typing import Any, Dict, Optional, Type, TypeVar

E = TypeVar("E", bound=Enum)


class StatsStorageType(str, Enum):
    """Enumeration for each available type of storage for JSON stats"""

    FILE = "file"
    """Indicates that aggregated statistics will be stored to a local file"""
    S3 = "s3"
    """Indicates that aggregated statistics will be stored to an S3 bucket"""


class StatsEnvironment(str, Enum):
    """Enumeration for each possible test running environment"""

    TEST = "test"
    """Indicates that the running environment is in the TEST environment"""
    # TODO: PROD_SBX may be "prod-sbx" or "prod_sbx" depending on context (specifically, Glue Table partition columns)
    # so a better way of handling its string representation should be considered. For now, "prod-sbx" is the only
    # string representation expected to be encountered by this code and other contexts
    PROD_SBX = "prod-sbx"
    """Indicates that the running environment is in the PROD-SBX environment"""
    PROD = "prod"
    """Indicates that the running environment is in the PROD environment"""


class StatsComparisonType(str, Enum):
    """Enumeration for each possible type of stats comparison"""

    PREVIOUS = "previous"
    """Indicates that the comparison will be against the most recent, previous run under a given tag"""
    AVERAGE = "average"
    """Indicates that the comparison will be against the average of all runs under a given tag"""


@dataclass
class StatsConfiguration:
    """Dataclass that holds data about where and how aggregated performance statistics are stored
    and compared"""

    stats_store: StatsStorageType
    """The storage type that the stats will be written to"""
    stats_env: StatsEnvironment
    """The test running environment from which the statistics will be collected"""
    stats_store_tag: str
    """A simple string tag that is used to partition collected statistics when stored"""
    stats_store_file_path: Optional[str]
    """The local parent directory where JSON files will be written to.
    Used only if type is file, ignored if type is s3"""
    stats_store_s3_bucket: Optional[str]
    """The AWS S3 Bucket that the JSON will be written to.
    Used only if type is s3, ignored if type is file"""
    stats_store_s3_database: Optional[str]
    """Name of the Athena database that is queried upon when comparing statistics.
    Also used as part of the file path when storing stats in S3"""
    stats_store_s3_table: Optional[str]
    """Name of the table to query using Athena if store is s3 and compare is set.
    Also used as part of the file path when storing stats in S3"""
    stats_compare: Optional[StatsComparisonType]
    """Indicates the type of performance stats comparison that will be done"""
    stats_compare_tag: Optional[str]
    """Indicates the tag from which comparison statistics will be loaded"""

    @classmethod
    def from_parsed_opts(cls, parsed_opts: Namespace) -> "StatsConfiguration":
        """Constructs an instance of StatsConfiguration from a parsed options Namespace. This will
        typically be the Locust Environment.parsed_options Namespace.

        Returns:
            Optional[StatsConfiguration]: A StatsConfiguration instance if "stats_config" is valid,
            None otherwise
        """
        opts_as_dict = vars(parsed_opts)
        common_keys = opts_as_dict.keys() & dataclasses.fields(StatsConfiguration)
        stats_args: Dict[str, Any] = {k:v for k,v in opts_as_dict if k in common_keys}

        try:
            stats_config = StatsConfiguration(**stats_args)
        except ValueError as exc:
            raise ValueError(f'Unable to create instance of StatsConfiguration from given arguments: {str(exc)}') from exc

        return stats_config

    @staticmethod
    def __enum_from_val(val: str, enum_type: Type[E], field_name: str) -> E:
        try:
            return enum_type[val.upper()]
        except KeyError:
            raise ValueError(
                f'"{field_name}" must be one of: {", ".join([e.name for e in enum_type])}'
            ) from None

    @staticmethod
    def __validate_tag(tag: str, field_name: str) -> str:
        # Tags must follow the BFD Insights data convention constraints for
        # partition/folders names, as it is used as a partition folder when uploading
        # to S3
        if not re.fullmatch("[a-z0-9_]+", tag) or not tag:
            raise ValueError(
                f'"{field_name}" must only consist of lower-case letters, numbers and the "_"'
                " character"
            ) from None

        return tag
