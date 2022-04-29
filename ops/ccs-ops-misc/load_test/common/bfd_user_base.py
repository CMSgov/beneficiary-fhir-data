'''Base class for Locust tests run against the FHIR endpoints.
'''

import json
import random
from typing import Callable, Dict, List

from common import config, data, db, test_setup as setup, validation
from locust import HttpUser
import urllib3

setup.set_locust_env(config.load())


class BFDUserBase(HttpUser):
    '''Base Class for Locust tests against BFD.

    This class should automatically handle most of the common tasks that our
    load tests require.
    '''


    # If a child class is going to use a set of data during the test, such as 
    # Beneficiary IDs, MBI numbers, etc. then it has to be defined in this class
    # field so that we can pre-load that data from the database.
    DATA_REQUIRED = [
        # 'BENE_IDS',
        # 'MBIS',
        # 'CONTRACT_IDS'
    ]

    # If a child class needs to set up a Failsafe event (highly recommended!) to
    # stop the test if SLAs degrade past a certain point, then these are the SLAs
    # to use for that. Also, these are the SLAs to compare against upon test
    # completion.
    SLA_BASELINE = None

    def __init__(self, *args, **kwargs):
        HttpUser.__init__(self, *args, **kwargs)

        # Load configuration needed for making requests to the FHIR server
        self.client_cert = setup.getClientCert()
        self.server_public_key = setup.loadServerPublicKey()
        setup.disable_no_cert_warnings(self.server_public_key, urllib3)
        self.last_updated = data.get_last_updated()

        # Initialize data / URL pools
        self.bene_ids = []
        self.mbis = []
        self.contract_ids = []
        self.url_pools = {}


    def on_start(self):
        '''Run once when a BFDUser is initialized by Locust.

        This method copies the necessary test data (lists of MBIs, beneficiary
        IDs, and contract cursor URLs) as members of this particular BFDUser
        instance. We then shuffle these copied lists such that concurrent
        BFDUsers are not querying the same data at the same time.
        '''

        # Pre-load data needed for creating URLs
        self.bene_ids = self.load_data('BENE_IDS', db.get_bene_ids)
        self.mbis = self.load_data('MBIS', db.get_hashed_mbis)
        self.contract_ids = self.load_data('CONTRACT_IDS', db.get_contract_ids)

        # Adds a global failsafe check to ensure that if this test overwhelms
        # the database, we bail out and stop hitting the server
        if hasattr(self, 'SLA_BASELINE') and self.SLA_BASELINE:
            validation.setup_failsafe_event(self.environment, self.SLA_BASELINE)


    def on_stop(self):
        '''Run tear-down tasks after the tests have completed.'''

        # Report the various response time percentiles against the SLA
        if hasattr(self, 'SLA_BASELINE') and self.SLA_BASELINE:
            validation.check_sla_validation(self.environment, self.SLA_BASELINE)


    def get_by_url(self, url: str, headers: Dict[str, str] = None,
            name: str = ''):
        '''Send one GET request and parse the response for pagination.

        This method extends Locust's HttpUser::client.get() method to make
        creating the requests nicer. Specifically, the query string parameters
        are specified as a separate dictionary opposed to part of the path, the
        cert and verify arguments (which will never change) are already set,
        and Cache-Control headers are automatically set to ensure caching is
        disabled.
        '''

        safe_headers = {} if headers is None else headers

        with self.client.get(url,
                cert=self.client_cert,
                verify=self.server_public_key,
                headers={**safe_headers, 'Cache-Control': 'no-store, no-cache'},
                name=name,
                catch_response=True
        ) as response:
            # This is normally where you would validate a response, but we
            # don't need to do that. Instead, we're checking it for valid
            # "next" URLs that we can add to a URL pool.
            next_url = BFDUserBase.get_next_url(response.text)
            if next_url is not None:
                if name not in self.url_pools:
                    self.url_pools[name] = []
                self.url_pools[name].append(next_url)


    def run_task(self, url_callback: Callable, headers: Dict[str, str] = None,
            name: str = ''):
        '''Figure out which URL we should query next and query the server'''

        # First, see if we can generate a URL using the callback
        try:
            url = url_callback(self)
        except IndexError:
            url = None

        if url is None and name in self.url_pools and self.url_pools[name]:
            # We can't generate a URL from the callback, but there are still
            # URLs in the pool of paginated data. Use that.
            url = self.url_pools[name].pop()

        if url is not None:
            # Run the test using the URL we found
            self.get_by_url(url=url, headers=headers, name=name)
        # If no URL is found, then this test isn't counted in statistics


    # Helper Functions


    def load_data(self, flag_name: str, load_function: Callable, *args) -> List:
        '''If the given DATA_REQUIRED flag exists on this class, pre-load the
        data from the database.'''
        if hasattr(self, 'DATA_REQUIRED') and flag_name in self.DATA_REQUIRED:
            data_list = data.load_data_segment(load_function, *args).copy()
            random.shuffle(data_list)
            return data_list
        else:
            return []

    @staticmethod
    def get_next_url(payload: str) -> str:
        '''Parse the JSON response and return the "next" URL if it exists'''
        parsed_payload = json.loads(payload)
        for link in parsed_payload.get("link", {}):
            if "relation" in link and link["relation"] == "next":
                return link.get("url", None)
        return None
