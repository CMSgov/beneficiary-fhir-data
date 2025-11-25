"""Base class for Locust tests run against the FHIR endpoints."""

import json
import logging
import re
import ssl
from collections.abc import Callable, Mapping
from typing import Any

from common import custom_args, data, validation
from common.locust_utils import is_distributed, is_locust_worker
from common.stats import stats_compare, stats_writers
from common.stats.aggregated_stats import FinalCompareResult, StatsCollector
from common.stats.stats_config import StatsConfiguration
from common.url_path import create_url_path
from common.validation import ValidationResult
from locust import FastHttpUser, events
from locust.argument_parser import LocustArgumentParser
from locust.contrib.fasthttp import ResponseContextManager
from locust.env import Environment

_COMPARISONS_METADATA_PATH = None
"""The path to a given stats comparison metadata JSON file for a particular test suite. Should be
overriden in modules (Locustfiles) that import bfd_user_base using set_comparisons_metadata_path().
Also overriden by the value of --stats-compare-meta-file"""


@events.init_command_line_parser.add_listener
def _(parser: LocustArgumentParser, **kwargs: dict[str, Any]) -> None:
    custom_args.register_custom_args(parser)


@events.init.add_listener
def _(environment: Environment, **kwargs: dict[str, Any]) -> None:
    if is_distributed(environment) and is_locust_worker(environment):
        return

    validation.setup_failsafe_event(environment)

    # Remove trailing slashes as Locust does not do so itself
    host_no_trailing_slash = re.sub("[\\/]*$", "", environment.host or "")
    environment.host = host_no_trailing_slash


@events.quitting.add_listener
def _(environment: Environment, **kwargs: dict[str, Any]) -> None:
    """Run one-time teardown tasks after the tests have completed.

    Args:
        environment (Environment): The current Locust environment
    """
    if is_distributed(environment) and is_locust_worker(environment):
        return

    validation_result = validation.check_validation_goals(environment)

    logger = logging.getLogger()

    if not environment.parsed_options:
        logger.warning("No parsed options found -- is Locust running as a Library?")
        return

    try:
        stats_config = StatsConfiguration.from_parsed_opts(environment.parsed_options)
    except ValueError as exc:
        logger.warning("Unable to get stats configuration: %s", str(exc))
        environment.process_exit_code = 1
        return

    compare_result = FinalCompareResult.NOT_APPLICABLE
    if stats_config.stats_store and stats_config.stats_env:
        # If stats_config is valid, get the aggregated stats of the stopping test run
        stats_collector = StatsCollector(
            environment, stats_config.stats_store_tags, stats_config.stats_env
        )
        stats = stats_collector.collect_stats()
        assert stats.metadata is not None

        try:
            stat_comparsion_meta_path = (
                stats_config.stats_compare_meta_file or _COMPARISONS_METADATA_PATH
            )
            if stat_comparsion_meta_path:
                compare_result = stats_compare.do_stats_comparison(
                    stats_config,
                    stat_comparsion_meta_path,
                    stats,
                )
        except Exception as exc:
            compare_result = FinalCompareResult.FAILED
            logger.error("Stat comparison was not able to complete; err: %s", repr(exc))

        stats.metadata.compare_result = compare_result
        stats.metadata.validation_result = validation_result

        if stats_config.stats_store:
            stats_writers.write_stats(stats_config, stats)

    logger.info("Final comparison result was: %s", compare_result.value)
    logger.info("Final validation result was: %s", validation_result.value)

    if compare_result == FinalCompareResult.FAILED or validation_result == ValidationResult.FAILED:
        environment.process_exit_code = 1
        logger.error(
            "Test run failed overall as comparison or validation result failed; locust exiting with"
            " return code 1"
        )


def set_comparisons_metadata_path(path: str) -> None:
    """Set the file path used to define metadata about stat comparisons (i.e. failure and warning
    percent ratio thresholds, etc.) for a given Locustfile/test suite. Should be called in the
    Locustfile's module scope.

    Args:
        path (str): Path to a JSON file describing stat comparison metadata
    """
    global _COMPARISONS_METADATA_PATH  # noqa: PLW0603
    _COMPARISONS_METADATA_PATH = path


class BFDUserBase(FastHttpUser):
    """Base Class for Locust tests against BFD.

    This class should automatically handle most of the common tasks that our load tests require.
    """

    # Disables certificate verification for FastHttpUser requests
    insecure = True

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = True

    def __init__(self, *args: tuple, **kwargs: dict[str, Any]) -> None:
        super().__init__(*args, **kwargs)

        # Load configuration needed for making requests to the FHIR server
        self.client_cert = self.environment.parsed_options.client_cert_path
        self.server_public_key = self.environment.parsed_options.server_public_key
        self.last_updated = data.get_last_updated()

        # Initialize URL pools
        self.url_pools = {}

        self.logger = logging.getLogger()
        self.has_reported_no_data = []

    def ssl_context_factory(self) -> ssl.SSLContext:
        """Configure the SSLContext for FastHttpUser requests. Specifically, the context returned
        by this method sets the PEM cert to the cert provided via configuration or the CLI.

        Returns:
            ssl.SSLContext: An SSLContext that authenticates the Locust tests against the BFD Server
        """
        context = ssl.create_default_context()
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE
        try:
            context.load_cert_chain(certfile=self.client_cert)
        except Exception as e:
            self.logger.error(
                "Error loading certificate. Ensure the certificate is formatted correctly. %s",
                e,
            )
            raise e
        if self.server_public_key:
            context.load_verify_locations(cafile=self.server_public_key)
        return context

    def get_by_url(
        self, url: str, headers: Mapping[str, str] | None = None, name: str = ""
    ) -> None:
        """Send one GET request and parse the response for pagination.

        This method extends Locust's HttpUser::client.get() method to make creating the requests
        nicer. Specifically, the query string parameters are specified as a separate dictionary
        opposed to part of the path, the cert and verify arguments (which will never change) are
        already set, and Cache-Control headers are automatically set to ensure caching is disabled.
        """
        safe_headers = {} if headers is None else headers

        with self.client.get(
            url,
            headers={**safe_headers, "Cache-Control": "no-store, no-cache"},
            name=name,
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                if isinstance(response, ResponseContextManager):
                    # pylint: disable=E1121
                    response.failure(f"Status Code: {response.status_code}")
                else:
                    response.failure()
            elif response.text:
                # Check for valid "next" URLs that we can add to a URL pool.
                next_url = BFDUserBase.__get_next_url(response.text)
                if next_url is not None:
                    if name not in self.url_pools:
                        self.url_pools[name] = []
                    self.url_pools[name].append(next_url)

    def post_by_url(
        self,
        url: str,
        headers: dict[str, str] | None = None,
        body: dict[str, str] | None = None,
        name: str = "",
    ) -> None:
        """Send one POST request and parse the response for pagination.

        This method extends Locust's HttpUser::client.post() method to make creating the requests
        nicer. Specifically, the query string parameters are specified as a separate dictionary
        opposed to part of the path, the cert and verify arguments (which will never change) are
        already set, and Cache-Control headers are automatically set to ensure caching is disabled.
        """
        safe_headers = {} if headers is None else headers
        safe_body = {} if body is None else body

        with self.client.post(
            url,
            headers={**safe_headers, "Cache-Control": "no-store, no-cache"},
            data=safe_body,
            name=name,
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                if isinstance(response, ResponseContextManager):
                    # pylint: disable=E1121
                    response.failure(f"Status Code: {response.status_code}")
                else:
                    response.failure()
            elif response.text:
                # Check for valid "next" URLs that we can add to a URL pool.
                next_url = BFDUserBase.__get_next_url(response.text)
                if next_url is not None:
                    if name not in self.url_pools:
                        self.url_pools[name] = []
                    self.url_pools[name].append(next_url)

    def run_task(
        self,
        url_callback: Callable,
        headers: dict[str, str] | None = None,
        body: dict[str, str] | None = None,
        name: str = "",
    ) -> None:
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
            if body is None:
                self.get_by_url(url=url, headers=headers, name=name)
            else:
                self.post_by_url(url=url, headers=headers, body=body, name=name)
        else:
            # If no URL is found, then this test isn't counted in statistics

            # Should we also terminate future tests?
            worker_num = (
                self.environment.runner.client_id if is_locust_worker(self.environment) else None
            )
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
                    self.logger.warning(
                        'Test "%s" for worker %s has run out of data', name, worker_num
                    )

    def run_task_by_parameters(
        self,
        base_path: str,
        params: Mapping[str, str | int | list[Any]] | None = None,
        headers: dict[str, str] | None = None,
        body: dict[str, str] | None = None,
        name: str = "",
    ) -> None:
        """Run a task using a base path and parameters."""
        safe_params = {} if params is None else params
        safe_headers = {} if headers is None else headers

        def make_url() -> str:
            return create_url_path(base_path, safe_params)

        self.run_task(name=name, url_callback=make_url, headers=safe_headers, body=body)

    # Helper Functions

    @staticmethod
    def __get_next_url(payload: str) -> str | None:
        """Parse the JSON response and return the "next" URL if it exists."""
        parsed_payload = json.loads(payload)
        for link in parsed_payload.get("link", {}):
            if "relation" in link and link["relation"] == "next":
                return link.get("url", None)
        return None
