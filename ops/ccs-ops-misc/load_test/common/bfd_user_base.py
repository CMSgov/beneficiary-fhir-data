"""Base class for Locust tests run against the FHIR endpoints.
"""

import json
import logging
from typing import Callable, Dict, List, Optional, Union

import urllib3
import urllib3.exceptions
from locust import HttpUser, events
from locust.argument_parser import LocustArgumentParser
from locust.env import Environment

from common import custom_args, data, validation
from common.locust_utils import is_distributed, is_locust_worker
from common.stats import stats_compare, stats_writers
from common.stats.aggregated_stats import StatsCollector
from common.stats.stats_config import StatsConfiguration
from common.url_path import create_url_path


@events.init_command_line_parser.add_listener
def _(parser: LocustArgumentParser, **kwargs) -> None:
    custom_args.register_custom_args(parser)


@events.init.add_listener
def _(environment: Environment, **kwargs) -> None:
    if is_distributed(environment) and is_locust_worker(environment):
        return

    custom_args.adjust_parsed_run_time(environment)
    validation.setup_failsafe_event(environment)


@events.quitting.add_listener
def _(environment: Environment, **kwargs) -> None:
    """Run one-time teardown tasks after the tests have completed

    Args:
        environment (Environment): The current Locust environment
    """
    if is_distributed(environment) and is_locust_worker(environment):
        return

    validation.check_sla_validation(environment)

    if not environment.parsed_options:
        return

    stats_config = StatsConfiguration.from_parsed_opts(environment.parsed_options)
    if stats_config:
        # If --stats-config was set and it is valid, get the aggregated stats of the stopping test run
        stats_collector = StatsCollector(environment, stats_config.store_tag, stats_config.env)
        stats = stats_collector.collect_stats()

        stats_compare.do_stats_comparison(environment, stats_config, stats)
        stats_writers.write_stats(stats_config, stats)


class BFDUserBase(HttpUser):
    """Base Class for Locust tests against BFD.

    This class should automatically handle most of the common tasks that our load tests require.
    """

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = True

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        # Load configuration needed for making requests to the FHIR server
        self.client_cert = self.environment.parsed_options.client_cert_path
        self.server_public_key = self.environment.parsed_options.server_public_key
        if not self.server_public_key:
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        self.last_updated = data.get_last_updated()

        # Initialize URL pools
        self.url_pools = {}

        self.logger = logging.getLogger()
        self.has_reported_no_data = []

    def get_by_url(self, url: str, headers: Optional[Dict[str, str]] = None, name: str = ""):
        """Send one GET request and parse the response for pagination.

        This method extends Locust's HttpUser::client.get() method to make creating the requests
        nicer. Specifically, the query string parameters are specified as a separate dictionary
        opposed to part of the path, the cert and verify arguments (which will never change) are
        already set, and Cache-Control headers are automatically set to ensure caching is disabled.
        """

        safe_headers = {} if headers is None else headers

        with self.client.get(
            url,
            cert=self.client_cert,
            verify=self.server_public_key,
            headers={**safe_headers, "Cache-Control": "no-store, no-cache"},
            name=name,  # type: ignore -- known Locust argument
            catch_response=True,  # type: ignore -- known Locust argument
        ) as response:
            if response.status_code != 200:
                response.failure(f"Status Code: {response.status_code}")
            else:
                # Check for valid "next" URLs that we can add to a URL pool.
                next_url = BFDUserBase.__get_next_url(response.text)
                if next_url is not None:
                    if name not in self.url_pools:
                        self.url_pools[name] = []
                    self.url_pools[name].append(next_url)

    def run_task(
        self,
        url_callback: Callable,
        headers: Optional[Dict[str, str]] = None,
        name: str = "",
    ):
        """Figure out which URL we should query next and query the server.

        Note that the Data Pools (Bene ID, MBI, etc.) are limited and we don't want to grab a value
        outside of the URL Callback. Doing so runs the risk of consuming an ID without using it,
        especially if some future implementation does not always consume IDs before consuming the
        paginated URL Pool.
        """

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
            worker_num = self.environment.runner.client_id if is_locust_worker(self.environment) else None
            if self.END_ON_NO_DATA:
                if worker_num is None:
                    self.logger.error("Ran out of data, stopping test...")
                else:
                    self.logger.error("Worker %s ran out of data and will terminate.", worker_num)

                self.environment.runner.quit()
            elif name not in self.has_reported_no_data:
                self.has_reported_no_data.append(name)
                if worker_num is None:
                    self.logger.warning('Test "%s" has run out of data', name)
                else:
                    self.logger.warning('Test "%s" for worker %s has run out of data', name, worker_num)

    def run_task_by_parameters(
        self,
        base_path: str,
        params: Optional[Dict[str, Union[str, int, List]]] = None,
        headers: Optional[Dict[str, str]] = None,
        name: str = "",
    ):
        """Run a task using a base path and parameters"""

        safe_params = {} if params is None else params
        safe_headers = {} if headers is None else headers

        def make_url():
            return create_url_path(base_path, safe_params)

        self.run_task(name=name, url_callback=make_url, headers=safe_headers)

    # Helper Functions

    @staticmethod
    def __get_next_url(payload: str) -> Optional[str]:
        """Parse the JSON response and return the "next" URL if it exists"""

        parsed_payload = json.loads(payload)
        for link in parsed_payload.get("link", {}):
            if "relation" in link and link["relation"] == "next":
                return link.get("url", None)
        return None
