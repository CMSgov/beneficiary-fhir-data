"""Code in this file is related to defining and registering custom Locust arguments for testing"""

from locust.argument_parser import LocustArgumentParser
from locust.util.timespan import parse_timespan

from .stats.stats_config import StatsComparisonType, StatsEnvironment, StatsStorageType


def register_custom_args(parser: LocustArgumentParser):
    parser.add_argument(
        "--client-cert-path",
        type=str,
        required=True,
        help='Specifies path to client cert, ex: "<path/to/client/pem/file>" (Required)',
        dest="client_cert_path",
        env_var="LOCUST_BFD_CLIENT_CERT_PATH",
    )
    parser.add_argument(
        "--database-uri",
        type=str,
        required=True,
        help=(
            'Specfies database URI path, ex: "https://<nodeIp>:7443 or'
            ' https://<environment>.bfd.cms.gov" (Required)'
        ),
        dest="database_uri",
        env_var="LOCUST_BFD_DATABASE_URI",
    )
    parser.add_argument(
        "--spawned-runtime",
        type=parse_timespan,
        help=(
            "Specifies the test runtime limit that begins after all users have spawned when running"
            " tests with the custom UserInitAwareLoadShape load shape, which should be all of the"
            " tests in this repository. If unspecified, tests run indefinitely even after all users"
            ' have spawned. Specifying "0<s/h/m>" will stop the tests immediately once all users'
            " have spawned. Note that this is not the same option as --run-time, which handles the"
            " total runtime limit for the Locust run including non-test tasks and does not"
            " compensate for spawn rate."
        ),
        dest="spawned_runtime",
        env_var="LOCUST_USERS_SPAWNED_RUNTIME",
    )
    parser.add_argument(
        "--server-public-key",
        type=str,
        help='"<server public key>" (Optional, Default: "")',
        dest="server_public_key",
        env_var="LOCUST_BFD_SERVER_PUBLIC_KEY",
        default="",
    )
    parser.add_argument(
        "--table-sample-percent",
        type=float,
        help="<% of table to sample> (Optional, Default: 0.25)",
        dest="table_sample_percent",
        env_var="LOCUST_DATA_TABLE_SAMPLE_PERCENT",
        default=0.25,
    )

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
        env_var="LOCUS_STATS_STORE_TO_FILE",
        action="store_const",
        const=StatsStorageType.FILE,
    )
    storage_type_group.add_argument(
        "--stats-store-s3",
        help="Specifies that stats will be written to an S3 bucket",
        dest="stats_store",
        env_var="LOCUS_STATS_STORE_TO_S3",
        action="store_const",
        const=StatsStorageType.FILE,
    )

    stats_group.add_argument(
        "--stats-env",
        type=StatsEnvironment,
        help="Specifies the test running environment which the tests are running against",
        dest="stats_env",
        env_var="LOCUST_STATS_ENVIRONMENT",
    )
    stats_group.add_argument(
        "--stats-store-tag",
        type=str,
        help=(
            "Specifies the tag under which collected statistics will be stored. Can be specified"
            " multiple times"
        ),
        dest="stats_store_tag",
        env_var="LOCUS_STATS_STORE_TAG",
        action="append",
    )
    stats_group.add_argument(
        "--stats-store-file-path",
        type=str,
        help=(
            "Specifies the parent directory where JSON stats will be written to. Only used if"
            ' --stats-store is "FILE"'
        ),
        dest="stats_store_file_path",
        env_var="LOCUS_STATS_STORE_FILE_PATH",
    )
    stats_group.add_argument(
        "--stats-store-s3-bucket",
        type=str,
        help=(
            "Specifies the S3 bucket that JSON stats will be written to. Only used if --stats-store"
            ' is "S3"'
        ),
        dest="stats_store_s3_bucket",
        env_var="LOCUS_STATS_STORE_S3_BUCKET",
    )
    stats_group.add_argument(
        "--stats-store-s3-database",
        type=str,
        help=(
            "Specifies the Athena database that is queried upon when comparing statistics. Also"
            " used as part of the S3 key/path when storing stats to S3"
        ),
        dest="stats_store_s3_database",
        env_var="LOCUS_STATS_STORE_S3_DATABASE",
    )
    stats_group.add_argument(
        "--stats-store-s3-table",
        type=str,
        help=(
            "Specifies the Athena table that is queried upon when comparing statistics. Also used"
            " as part of the S3 key/path when storing stats to S3"
        ),
        dest="stats_store_s3_table",
        env_var="LOCUS_STATS_STORE_S3_TABLE",
    )

    # Ensure that only one type of comparison can be chosen via arguments
    compare_type_group = stats_group.add_mutually_exclusive_group()
    compare_type_group.add_argument(
        "--stats-compare-previous",
        help=(
            "Specifies that the current run's performance statistics will be compared against the"
            " previous matching run's performance statistics"
        ),
        dest="stats_compare",
        env_var="LOCUST_STATS_COMPARE_PREVIOUS",
        action="store_const",
        const=StatsComparisonType.PREVIOUS,
    )
    compare_type_group.add_argument(
        "--stats-compare-average",
        help=(
            "Specifies that the current run's performance statistics will be compared against an"
            " average of the last, by default, 5 matching runs"
        ),
        dest="stats_compare",
        env_var="LOCUST_STATS_COMPARE_AVERAGE",
        action="store_const",
        const=StatsComparisonType.AVERAGE,
    )

    stats_group.add_argument(
        "--stats-compare-tag",
        type=str,
        help="Specifies the tag that will matching runs will be found under to compare against",
        dest="stats_compare_tag",
        env_var="LOCUST_STATS_COMPARE_TAG",
    )
