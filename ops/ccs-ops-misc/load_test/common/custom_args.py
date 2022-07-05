"""Code in this file is related to defining and registering custom Locust arguments for testing"""
from datetime import timedelta
from math import ceil
import logging
from locust.env import Environment
from locust.argument_parser import LocustArgumentParser
from common.locust_utils import is_distributed, is_locust_worker


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
        help='Specfies database URI path, ex: "https://<nodeIp>:7443 or https://<environment>.bfd.cms.gov" (Required)',
        dest="database_uri",
        env_var="LOCUST_BFD_DATABASE_URI",
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
    parser.add_argument(
        "--stats-config",
        type=str,
        help='"<If set, stores stats in JSON to S3 or local file. Key-value list seperated by semi-colons. See README.>" (Optional)',
        dest="stats_config",
        env_var="LOCUST_STATS_CONFIG",
    )


def adjust_parsed_run_time(environment: Environment):
    logger = logging.getLogger()
    if not environment.parsed_options:
        logger.warn('Cannot adjust runtime when running Locust as library')
        return

    # Adjust the runtime to account for spawn rate
    num_users = int(environment.parsed_options.num_users)
    spawn_rate = int(environment.parsed_options.spawn_rate)
    init_run_time = environment.parsed_options.run_time

    adjusted_run_time = _adjusted_run_time(init_run_time, num_users, spawn_rate)
    if adjusted_run_time != init_run_time:
        environment.parsed_options.run_time = adjusted_run_time
        logger.info(
            "Run time adjusted to account for ramp-up time. New run time: "
            f"{timedelta(seconds=environment.parsed_options.run_time)}"
        )


def _adjusted_run_time(run_time: int, max_clients: int, clients_per_second: int) -> int:
    """Get the adjusted run time of the test to account for the time it takes to instantiate and connect
    all the clients.

    If a user specifies a one-minute test, but it's going to take thirty seconds to ramp up to full
    clients, then we actually run for one minute and thirty seconds, so that we can have the
    specified time with full client capacity. You can optionally reset the statistics to zero at
    the end of this ramp-up period using the --resetStats command line flag.
    """

    return run_time + ceil(int(max_clients) // int(clients_per_second))
