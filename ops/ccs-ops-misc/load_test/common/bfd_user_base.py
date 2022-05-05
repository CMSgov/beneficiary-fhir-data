'''Base class for Locust tests run against the FHIR endpoints.
'''

import json
import logging
import os

from typing import Callable, Dict, List, Union
from common import config, data, test_setup as setup, validation
from common.url_path import create_url_path
from locust import HttpUser

import urllib3

setup.set_locust_env(config.load())


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
        self.client_cert = setup.get_client_cert()
        self.server_public_key = setup.load_server_public_key()
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

        This method copies the necessary test data (lists of MBIs, beneficiary IDs, and contract
        cursor URLs) as members of this particular BFDUser instance. We then shuffle these copied
        lists such that concurrent BFDUsers are not querying the same data at the same time.
        '''

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

        if url is None and name in self.url_pools and self.url_pools[name]:
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
