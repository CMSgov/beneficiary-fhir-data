import dataclasses
import re
from argparse import Namespace
from dataclasses import dataclass, field
from enum import StrEnum
from typing import Any

from locust.argument_parser import LocustArgumentParser


class StatsStorageType(StrEnum):
    """Enumeration for each available type of storage for JSON stats."""

    FILE = "file"
    """Indicates that aggregated statistics will be stored to a local file"""
    S3 = "s3"
    """Indicates that aggregated statistics will be stored to an S3 bucket"""


class StatsComparisonType(StrEnum):
    """Enumeration for each possible type of stats comparison."""

    PREVIOUS = "previous"
    """Indicates that the comparison will be against the most recent, previous run under a given
    tag"""
    AVERAGE = "average"
    """Indicates that the comparison will be against the average of all runs under a given tag"""


@dataclass
class StatsConfiguration:
    """Dataclass that holds data about where and how aggregated performance statistics are stored
    and compared.
    """

    stats_store: StatsStorageType | None
    """The storage type that the stats will be written to"""
    stats_env: str | None
    """The test running environment from which the statistics will be collected"""
    stats_store_file_path: str | None
    """The local parent directory where JSON files will be written to.
    Used only if type is file, ignored if type is s3"""
    stats_store_s3_workgroup: str | None
    """The Athena workgroup to use when querying statistics.
    Used only if type is s3, ignored if type is file"""
    stats_store_s3_bucket: str | None
    """The AWS S3 Bucket that the JSON will be written to.
    Used only if type is s3, ignored if type is file"""
    stats_store_s3_database: str | None
    """Name of the Athena database that is queried upon when comparing statistics.
    Also used as part of the file path when storing stats in S3"""
    stats_store_s3_table: str | None
    """Name of the table to query using Athena if store is s3 and compare is set.
    Also used as part of the file path when storing stats in S3"""
    stats_compare: StatsComparisonType | None
    """Indicates the type of performance stats comparison that will be done"""
    stats_compare_tag: str | None
    """Indicates the tag from which comparison statistics will be loaded"""
    stats_compare_meta_file: str | None
    """Indicates the path to a JSON file containing metadata about how stats should be compared for
    the running test suite. Overrides the default specified by the test suite, if any"""
    stats_store_tags: list[str] = field(default_factory=list)
    """A simple List of string tags that are used to partition collected statistics when stored"""
    stats_compare_load_limit: int = 5
    """Indicates the limit of previous AggregatedStats loaded for comparison; used only for average
    comparisons"""

    @classmethod
    def register_custom_args(cls, parser: LocustArgumentParser) -> None:
        """Register commnad-line arguments representing the fields of this dataclass.

        Args:
            parser (LocustArgumentParser): The argument parser to register custom arguments to
        """
        stats_group = parser.add_argument_group(
            title="stats",
            description="Argparse group for stats collection and comparison related arguments",
        )

        # Ensure only file _or_ S3 storage can be selected, not both
        storage_type_group = stats_group.add_mutually_exclusive_group()
        storage_type_group.add_argument(
            "--stats-store-file",
            help="Specifies that stats will be written to a local file",
            dest="stats_store",
            env_var="LOCUST_STATS_STORE_TO_FILE",
            action="store_const",
            const=StatsStorageType.FILE,
        )
        storage_type_group.add_argument(
            "--stats-store-s3",
            help="Specifies that stats will be written to an S3 bucket",
            dest="stats_store",
            env_var="LOCUST_STATS_STORE_TO_S3",
            action="store_const",
            const=StatsStorageType.S3,
        )

        stats_group.add_argument(
            "--stats-env",
            type=str,
            help="Specifies the test running environment which the tests are running against",
            dest="stats_env",
            env_var="LOCUST_STATS_ENVIRONMENT",
        )
        stats_group.add_argument(
            "--stats-store-tag",
            type=cls.__validate_tag,
            help=(
                "Specifies the tags under which collected statistics will be stored. Can be"
                " specified multiple times"
            ),
            dest="stats_store_tags",
            env_var="LOCUST_STATS_STORE_TAG",
            action="append",
            default=[],
        )
        stats_group.add_argument(
            "--stats-store-file-path",
            type=str,
            help=(
                "Specifies the parent directory where JSON stats will be written to. Only used if"
                ' --stats-store is "FILE"'
            ),
            dest="stats_store_file_path",
            env_var="LOCUST_STATS_STORE_FILE_PATH",
            default="./",
        )
        stats_group.add_argument(
            "--stats-store-s3-workgroup",
            type=str,
            help=(
                "Specifies the Athena workgroup used during querying. Only used if --stats-store is"
                ' "S3". Defaults to "bfd" if unspecified'
            ),
            dest="stats_store_s3_workgroup",
            env_var="LOCUST_STATS_STORE_S3_WORKGROUP",
            default="bfd",
        )
        stats_group.add_argument(
            "--stats-store-s3-bucket",
            type=str,
            help=(
                "Specifies the S3 bucket that JSON stats will be written to. Only used if"
                ' --stats-store is "S3"'
            ),
            dest="stats_store_s3_bucket",
            env_var="LOCUST_STATS_STORE_S3_BUCKET",
        )
        stats_group.add_argument(
            "--stats-store-s3-database",
            type=str,
            help=(
                "Specifies the Athena database that is queried upon when comparing statistics. Also"
                " used as part of the S3 key/path when storing stats to S3"
            ),
            dest="stats_store_s3_database",
            env_var="LOCUST_STATS_STORE_S3_DATABASE",
        )
        stats_group.add_argument(
            "--stats-store-s3-table",
            type=str,
            help=(
                "Specifies the Athena table that is queried upon when comparing statistics. Also"
                " used as part of the S3 key/path when storing stats to S3"
            ),
            dest="stats_store_s3_table",
            env_var="LOCUST_STATS_STORE_S3_TABLE",
        )

        # Ensure that only one type of comparison can be chosen via arguments
        compare_type_group = stats_group.add_mutually_exclusive_group()
        compare_type_group.add_argument(
            "--stats-compare-previous",
            help=(
                "Specifies that the current run's performance statistics will be compared against"
                " the previous matching run's performance statistics"
            ),
            dest="stats_compare",
            env_var="LOCUST_STATS_COMPARE_PREVIOUS",
            action="store_const",
            const=StatsComparisonType.PREVIOUS,
        )
        compare_type_group.add_argument(
            "--stats-compare-average",
            help=(
                "Specifies that the current run's performance statistics will be compared against"
                " an average of the last, by default, 5 matching runs"
            ),
            dest="stats_compare",
            env_var="LOCUST_STATS_COMPARE_AVERAGE",
            action="store_const",
            const=StatsComparisonType.AVERAGE,
        )

        stats_group.add_argument(
            "--stats-compare-tag",
            type=cls.__validate_tag,
            help="Specifies the tag that matching runs will be found under to compare against",
            dest="stats_compare_tag",
            env_var="LOCUST_STATS_COMPARE_TAG",
        )
        stats_group.add_argument(
            "--stats-compare-load-limit",
            type=int,
            help=(
                "Specifies the limit for number of previous stats to load when when doing"
                " comparisons, defaults to 5. Used solely for limiting stats loaded during average"
                " comparisons"
            ),
            dest="stats_compare_load_limit",
            env_var="LOCUST_STATS_COMPARE_LOAD_LIMIT",
            default=5,
        )
        stats_group.add_argument(
            "--stats-compare-meta-file",
            type=str,
            help=(
                "Specifies the file path to a JSON file containing metadata about how stats should"
                " be compared for a given test suite. Overrides the default path specified by a"
                " test suite, if any"
            ),
            dest="stats_compare_meta_file",
            env_var="LOCUST_STATS_COMPARE_META_FILE",
        )

    @classmethod
    def from_parsed_opts(cls, parsed_opts: Namespace) -> "StatsConfiguration":
        """Construct an instance of StatsConfiguration from a parsed options Namespace. This will
        typically be the Locust Environment.parsed_options Namespace.

        Returns:
            Optional[StatsConfiguration]: A StatsConfiguration instance if "stats_config" is valid,
            None otherwise
        """
        opts_as_dict = vars(parsed_opts)
        common_keys = opts_as_dict.keys() & {
            field.name for field in dataclasses.fields(StatsConfiguration)
        }
        stats_args: dict[str, Any] = {k: v for k, v in opts_as_dict.items() if k in common_keys}

        try:
            stats_config = StatsConfiguration(**stats_args)
        except ValueError as exc:
            raise ValueError(
                f"Unable to create instance of StatsConfiguration from given arguments: {exc!s}"
            ) from exc

        return stats_config

    @staticmethod
    def __validate_tag(tag: str) -> str:
        # Tags must follow the BFD Insights data convention constraints for
        # partition/folders names, as it is used as a partition folder when uploading
        # to S3
        if not re.fullmatch("[a-z0-9_]+", tag) or not tag:
            raise ValueError(
                'Value must only consist of lower-case letters, numbers and the "_" character'
            ) from None

        return tag
