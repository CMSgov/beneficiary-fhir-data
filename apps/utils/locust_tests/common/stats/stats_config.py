import dataclasses
import logging
import re
from argparse import Namespace
from dataclasses import dataclass
from enum import Enum
from typing import Optional, Type, TypeVar

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
    """Indicates that the running environment is in testing, using testing resources"""
    PROD = "prod"
    """Indicates that the running environment is in production, using production resources"""


class StatsComparisonType(str, Enum):
    """Enumeration for each possible type of stats comparison"""

    PREVIOUS = "previous"
    """Indicates that the comparison will be against the most recent, previous run under a given tag"""
    AVERAGE = "average"
    """Indicates that the comparison will be against the average of all runs under a given tag"""


@dataclass
class StatsConfiguration:
    """Dataclass that holds data about where and how aggregated performance statistics are stored and compared
    """

    store: StatsStorageType
    """The storage type that the stats will be written to"""
    env: StatsEnvironment
    """The test running environment from which the statistics will be collected"""
    store_tag: str
    """A simple string tag that is used to partition collected statistics when stored"""
    path: Optional[str]
    """The local parent directory where JSON files will be written to. Used only if type is file, ignored if type is s3"""
    bucket: Optional[str]
    """The AWS S3 Bucket that the JSON will be written to. Used only if type is s3, ignored if type is file"""
    compare: Optional[StatsComparisonType]
    """Indicates the type of performance stats comparison that will be done"""
    comp_tag: Optional[str]
    """Indicates the tag from which comparison statistics will be loaded"""
    athena_tbl: Optional[str]
    """Name of the table to query using Athena if store is s3 and compare is set"""

    def to_key_val_str(self) -> str:
        """Returns a key-value string representation of this StatsConfiguration instance.
        Used to serialize this object to config.

        Returns:
            str: The key-value string representation of this object.
        """
        as_dict = dataclasses.asdict(self)
        dict_non_empty = {k: v for k, v in as_dict.items() if v is not None and v != ""}
        return ";".join(
            [
                f"{k}={str(v) if not isinstance(v, Enum) else v.name}"
                for k, v in dict_non_empty.items()
            ]
        )

    @classmethod
    def from_key_val_str(cls, key_val_str: str) -> "StatsConfiguration":
        """Constructs a concrete instance of StatsConfiguration from a given string in key-value format seperated
        by semi-colons ("key1=value1;key2=value2").

        Args:
            key_val_str (str): The key-value representation of this object.

        Raises:
            ValueError: Raised if the passed string does not follow the proper format.

        Returns:
            StatsConfiguration: Returns a concrete instance of StatsConfiguration with the values specified in the key-value string.
        """
        key_vals_list = key_val_str.split(";")
        # Create a dictionary from the list of split key-value pairs by parsing each
        # "key=value" string in the list into {'key': 'value'}.
        # Empty values are simply considered to be empty strings.
        config_dict = {
            k: str(v or "") for k, v in (key_val.split("=") for key_val in key_vals_list)
        }

        # Check for required parameters, like type, tag, environment
        if not set(["store", "store_tag", "env"]).issubset(set(config_dict.keys())):
            raise ValueError('"store", "store_tag", and "env" must be specified') from None

        # Handle all of the enum-backed fields
        storage_type = cls.__enum_from_val(config_dict["store"], StatsStorageType, "store")
        stats_environment = cls.__enum_from_val(config_dict["env"], StatsEnvironment, "env")
        compare_type = (
            cls.__enum_from_val(config_dict["compare"], StatsComparisonType, "compare")
            if "compare" in config_dict
            else None
        )

        # Validate all of the tags passed in
        storage_tag = cls.__validate_tag(config_dict["store_tag"], "store_tag")
        comparison_tag = (
            cls.__validate_tag(config_dict["comp_tag"], "comp_tag")
            if "comp_tag" in config_dict
            else storage_tag
        )

        # Validate necessary parameters if S3 is specified
        if storage_type == StatsStorageType.S3:
            # Validate that bucket is always specified if S3 is specified
            if not "bucket" in config_dict:
                raise ValueError('"bucket" must be specified if "store" is "s3"') from None
            # Validate that the Athena table is set if compare is set
            if compare_type and not "athena_tbl" in config_dict:
                raise ValueError(
                    '"athena_tbl" must be specified if "store" is "s3" and "compare" is set'
                ) from None

        return cls(
            store=storage_type,
            env=stats_environment,
            store_tag=storage_tag,
            path=config_dict.get("path") or "./",
            bucket=config_dict.get("bucket"),
            compare=compare_type,
            comp_tag=comparison_tag,
            athena_tbl=config_dict.get("athena_tbl"),
        )

    @classmethod
    def from_parsed_opts(cls, parsed_opts: Namespace) -> Optional["StatsConfiguration"]:
        """Constructs an instance of StatsConfiguration from a parsed options Namespace, specifically
        from a "stats_config" option. This will typically be the Locust Environment.parsed_options Namespace.

        Returns:
            Optional[StatsConfiguration]: A StatsConfiguration instance if "stats_config" is valid, None otherwise
        """
        # Check to make sure that stats_config was passed-in -- if not, return
        if not parsed_opts.stats_config:
            return None

        stats_config_str = str(parsed_opts.stats_config)
        try:
            stats_config = StatsConfiguration.from_key_val_str(stats_config_str)
        except ValueError as e:
            logger = logging.getLogger()
            logger.warn('--stats-config was invalid: "%s"', e)
            return None

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
