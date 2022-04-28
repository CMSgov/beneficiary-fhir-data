import common.config as config
import common.data as data
import common.db as db
import common.test_setup as setup
import common.validation as validation
from locust import HttpUser
import json
import random
from typing import Callable, Dict, List
import urllib3

setup.set_locust_env(config.load())

'''
Base Class for Locust tests against BFD. This class should automatically handle
most of the common tasks that our load tests require.
'''
class BFDUserBase(HttpUser):

    '''
    If a child class is going to use a set of data during the test, such as 
    Beneficiary IDs, MBI numbers, etc. then it has to be defined in this class
    field so that we can pre-load that data from the database.
    '''
    DATA_REQUIRED = [
        # 'BENE_IDS',
        # 'MBIS',
        # 'CONTRACT_IDS'
    ]

    '''
    If a child class needs to set up a Failsafe event (highly recommended!) to
    stop the test if SLAs degrade past a certain point, then these are the SLAs
    to use for that. Also, these are the SLAs to compare against upon test
    completion.
    '''
    SLA_BASELINE = None

    '''
    Run setup tasks prior to the tests being counted.
    '''
    def on_start(self):
        # Load configuration needed for making requests to the FHIR server
        self.client_cert = setup.getClientCert()
        self.server_public_key = setup.loadServerPublicKey()
        setup.disable_no_cert_warnings(self.server_public_key, urllib3)
        self.last_updated = data.get_last_updated()

        # Pre-load data needed for creating URLs
        self.eob_ids = self.load_data('BENE_IDS', db.get_bene_ids)
        self.mbis = self.load_data('MBIS', db.get_hashed_mbis)
        self.contract_ids = self.load_data('CONTRACT_IDS', db.get_contract_ids)

        # Create the pools for storing paginated URLs
        self.url_pools = {}

        # Adds a global failsafe check to ensure that if this test overwhelms 
        # the database, we bail out and stop hitting the server
        if hasattr(self, 'SLA_BASELINE') and self.SLA_BASELINE:
            validation.setup_failsafe_event(self.environment, self.SLA_BASELINE)


    '''
    Run tear-down tasks after the tests have completed.
    '''
    def on_stop(self):
        # Report the various response time percentiles against the SLA
        if hasattr(self, 'SLA_BASELINE') and self.SLA_BASELINE:
            validation.check_sla_validation(self.environment, self.SLA_BASELINE)


    '''
    Send one request to the FHIR server and parse the response for pagination
    '''
    def get_by_url(self, url: str, headers: Dict[str, str] = {}, name: str = ''):
        with self.client.get(url,
                cert=self.client_cert,
                verify=self.server_public_key,
                headers={**headers, 'Cache-Control': 'no-store, no-cache'},
                name=name,
                catch_response=True
        ) as response:
            # This is normally where you would validate a response, but we
            # don't need to do that. Instead, we're checking it for valid
            # "next" URLs that we can add to a URL pool.
            next_url = self.get_next_url(response.text)
            if next_url is not None:
                if name not in self.url_pools:
                    self.url_pools[name] = []
                self.url_pools[name].append(next_url)
    

    '''
    Figure out which URL we should query next and then query the server using 
    get_by_url()
    '''
    def run_task(self, url_callback: Callable, headers: Dict[str, str] = {}, name: str = ''):
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


    # Helper Functions

    '''
    Check if the given DATA_REQUIRED flag exists on this class, and if it does,
    pre-load the data from the database.
    '''
    def load_data(self, flag_name: str, load_function: Callable, *args) -> List:
        if hasattr(self, 'DATA_REQUIRED') and flag_name in self.DATA_REQUIRED:
            data_list = data.load_data_segment(load_function, *args).copy()
            random.shuffle(data_list)
            return data_list
        else:
            return []

    '''
    Parse the JSON response and return the "next" URL if it exists
    '''
    def get_next_url(self, payload: str) -> str:
        parsed_payload = json.loads(payload)
        for link in parsed_payload.get("link", {}):
            if "relation" in link and link["relation"] == "next":
                return link.get("url", None)
        return None
