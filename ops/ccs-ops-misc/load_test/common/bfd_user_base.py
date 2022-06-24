'''Base class for Locust tests run against the FHIR endpoints.
'''

import json
import logging
import os

from typing import Callable, Dict, List, Union
from common import config, data, test_setup as setup, validation
from common.stats.aggregated_stats import StatsCollector
from common.stats.stats_compare import DEFAULT_DEVIANCE_FAILURE_THRESHOLD, validate_aggregated_stats
from common.stats.stats_config import StatsConfiguration, StatsStorageType
from common.stats.stats_loaders import StatsLoader
from common.stats.stats_writers import StatsJsonFileWriter, StatsJsonS3Writer
from common.url_path import create_url_path
from locust import HttpUser, events
from locust.env import Environment
from locust.argument_parser import LocustArgumentParser

import urllib3

setup.set_locust_env(config.load())

@events.init_command_line_parser.add_listener
def custom_args(parser: LocustArgumentParser):
    parser.add_argument(
        '--client-cert-path',
        type=str,
        required=True,
        help='Specifies path to client cert, ex: "<path/to/client/pem/file>" (Required)',
        dest='client_cert_path',
        env_var='LOCUST_BFD_CLIENT_CERT_PATH'
    )
    parser.add_argument(
        '--database-uri',
        type=str,
        required=True,
        help='Specfies database URI path, ex: "https://<nodeIp>:7443 or https://<environment>.bfd.cms.gov" (Required)',
        dest='database_uri',
        env_var='LOCUST_BFD_DATABASE_URI'
    )
    parser.add_argument(
        '--server-public-key',
        type=str,
        help='"<server public key>" (Optional, Default: "")',
        dest='server_public_key',
        env_var='LOCUST_BFD_SERVER_PUBLIC_KEY'
    )
    parser.add_argument(
        '--table-sample-percent',
        type=float,
        help='<% of table to sample> (Optional, Default: 0.25)',
        dest='table_sample_percent',
        env_var='LOCUST_DATA_TABLE_SAMPLE_PERCENT'
    )
    parser.add_argument(
        '--stats-config',
        type=StatsConfiguration.from_key_val_str,
        help='"<If set, stores stats in JSON to S3 or local file. Key-value list seperated by semi-colons. See README.>" (Optional)',
        dest='stats_config',
        env_var='LOCUST_STATS_CONFIG'
    )

class BFDUserBase(HttpUser):
    '''Base Class for Locust tests against BFD.

    This class should automatically handle most of the common tasks that our load tests require.
    '''

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    # The goals against which to measure these results. Note that they also include the Failsafe
    # cutoff, which will default to the V2 cutoff time if not set.
    VALIDATION_GOALS = None

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = True

    def __init__(self, *args, **kwargs):
        HttpUser.__init__(self, *args, **kwargs)

        # Load configuration needed for making requests to the FHIR server
        self.client_cert = config.get_client_cert()
        self.server_public_key = config.load_server_public_key()
        setup.disable_no_cert_warnings(self.server_public_key, urllib3)
        self.last_updated = data.get_last_updated()

        # Initialize data / URL pools
        self.bene_ids = []
        self.mbis = []
        self.contract_ids = []
        self.url_pools = {}

        self.logger = logging.getLogger()
        self.has_reported_no_data = []


    def on_start(self):
        '''Run once when a BFDUser is initialized by Locust.
        '''

        # Is this either the first worker or the only worker?
        worker_number = self.__get_worker_number()
        if worker_number is None or str(worker_number) == '0':
            # Adds a global failsafe check to ensure that if this test overwhelms
            # the database, we bail out and stop hitting the server
            if hasattr(self, 'VALIDATION_GOALS') and self.VALIDATION_GOALS:
                validation.setup_failsafe_event(self.environment, self.VALIDATION_GOALS)
            else:
                validation.setup_failsafe_event(self.environment, validation.SLA_V2_BASELINE)

    def on_stop(self):
        '''Run tear-down tasks after the tests have completed.'''

        # Report the various response time percentiles against the SLA
        if hasattr(self, 'VALIDATION_GOALS') and self.VALIDATION_GOALS:
            validation.check_sla_validation(self.environment, self.VALIDATION_GOALS)

    def get_by_url(self, url: str, headers: Dict[str, str] = None,
            name: str = ''):
        '''Send one GET request and parse the response for pagination.

        This method extends Locust's HttpUser::client.get() method to make creating the requests
        nicer. Specifically, the query string parameters are specified as a separate dictionary
        opposed to part of the path, the cert and verify arguments (which will never change) are
        already set, and Cache-Control headers are automatically set to ensure caching is disabled.
        '''

        safe_headers = {} if headers is None else headers

        with self.client.get(url,
                cert=self.client_cert,
                verify=self.server_public_key,
                headers={**safe_headers, 'Cache-Control': 'no-store, no-cache'},
                name=name,
                catch_response=True
        ) as response:
            if response.status_code != 200:
                response.failure(f'Status Code: {response.status_code}')
            else:
                # Check for valid "next" URLs that we can add to a URL pool.
                next_url = BFDUserBase.__get_next_url(response.text)
                if next_url is not None:
                    if name not in self.url_pools:
                        self.url_pools[name] = []
                    self.url_pools[name].append(next_url)


    def run_task(self, url_callback: Callable, headers: Dict[str, str] = None,
            name: str = ''):
        '''Figure out which URL we should query next and query the server.

        Note that the Data Pools (Bene ID, MBI, etc.) are limited and we don't want to grab a value
        outside of the URL Callback. Doing so runs the risk of consuming an ID without using it,
        especially if some future implementation does not always consume IDs before consuming the
        paginated URL Pool.
        '''

        # First, see if we can generate a URL using the callback
        try:
            url = url_callback()
        except IndexError:
            url = None

        if url is None and self.url_pools.get(name):
            # We can't generate a URL from the callback, but there are still
            # URLs in the pool of paginated data. Use that.
            url = self.url_pools[name].pop()

        if url is not None:
            # Run the test using the URL we found
            self.get_by_url(url=url, headers=headers, name=name)
        else:
            # If no URL is found, then this test isn't counted in statistics

            # Should we also terminate future tests?
            worker_num = self.__get_worker_number()

            if self.END_ON_NO_DATA:
                if worker_num is None:
                    self.logger.error("Ran out of data, stopping test...")
                else:
                    self.logger.error('Worker %s ran out of data and will terminate.', worker_num)

                self.environment.runner.quit()
            elif name not in self.has_reported_no_data:
                self.has_reported_no_data.append(name)
                if worker_num is None:
                    self.logger.warning('Test "%s" has run out of data', name)
                else:
                    self.logger.warning('Test "%s" for worker %s has run out of data', name,
                        worker_num)


    def run_task_by_parameters(self, base_path: str, params: Dict[str, Union[str, List]] = None,
            headers: Dict[str, str] = None, name: str = ''):
        '''Run a task using a base path and parameters'''

        safe_params = {} if params is None else params
        safe_headers = {} if headers is None else headers

        def make_url():
            return create_url_path(base_path, safe_params)

        self.run_task(name=name, url_callback=make_url, headers=safe_headers)


    # Helper Functions

    @staticmethod
    def __get_next_url(payload: str) -> str:
        '''Parse the JSON response and return the "next" URL if it exists'''

        parsed_payload = json.loads(payload)
        for link in parsed_payload.get("link", {}):
            if "relation" in link and link["relation"] == "next":
                return link.get("url", None)
        return None


    def __get_worker_number(self):
        '''Find the number of the Locust worker for this instance.'''

        if 'LOCUST_WORKER_NUM' in os.environ:
            return str(os.environ['LOCUST_WORKER_NUM'])
        return None

@events.test_stop.add_listener
def one_time_teardown(environment: Environment, **kwargs) -> None:
    """Run one-time teardown tasks after the tests have completed

    Args:
        environment (Environment): The current Locust environment
    """

    logger = logging.getLogger()
    stats_config = config.load_stats_config()
    if not stats_config:
        return

    # If --stats was set and it is valid, get the aggregated stats of the stopping test run
    stats_collector = StatsCollector(environment, stats_config.store_tag, stats_config.env)
    stats = stats_collector.collect_stats()

    if stats_config.compare:
        stats_loader = StatsLoader.create(stats_config, stats.metadata)  # type: ignore
        previous_stats = stats_loader.load()
        if previous_stats:
            failed_stats_results = validate_aggregated_stats(previous_stats, stats, DEFAULT_DEVIANCE_FAILURE_THRESHOLD)
            if not failed_stats_results:
                logger.info(
                    'Comparison against %s stats under "%s" tag passed', stats_config.compare.value, stats_config.comp_tag)
            else:
                # If we get here, that means some tasks have stats exceeding the threshold percent
                # between the previous/average run and the current. Fail the test run, and log the
                # failing tasks along with their relative stat percents   
                environment.process_exit_code = 1
                logger.error('Comparison against %s stats under "%s" tag failed; following tasks had stats that exceeded %.2f%% of the baseline: %s', 
                            stats_config.compare.value, stats_config.comp_tag, DEFAULT_DEVIANCE_FAILURE_THRESHOLD, failed_stats_results)
        else:
            logger.warn(
                'No applicable performance statistics under tag "%s" to compare against', stats_config.comp_tag)

    if stats_config.store == StatsStorageType.FILE:
        logger.info("Writing aggregated performance statistics to file.")

        stats_json_writer = StatsJsonFileWriter(stats)
        stats_json_writer.write(stats_config.path or '')
    elif stats_config.store == StatsStorageType.S3:
        logger.info("Writing aggregated performance statistics to S3.")

        stats_s3_writer = StatsJsonS3Writer(stats)
        if not stats_config.bucket:
            raise ValueError('S3 bucket must be provided when writing stats to S3')
        stats_s3_writer.write(stats_config.bucket)
