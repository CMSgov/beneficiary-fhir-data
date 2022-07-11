from typing import List
from locust import events
from locust.env import Environment
from common import data, db
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master

table_sample_pac_mbis = False
master_pac_mbis: List[str] = []

@events.init.add_listener
def _(environment: Environment, **kwargs):
    if is_distributed(environment) and is_locust_master(environment) or not environment.parsed_options:
        # Don't bother loading data for the master runner, it doesn't run a test
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_pac_mbis
    master_pac_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_pac_hashed_mbis,
        use_table_sample=table_sample_pac_mbis
    )

class PACTestUser(BFDUserBase):
    SERVICE_DATE = {'service-date': 'gt2020-01-05'}
    LAST_UPDATED = {'_lastUpdated': 'gt2020-05-05'}
    SERVICE_DATE_LAST_UPDATED = dict(SERVICE_DATE, **LAST_UPDATED)

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    def _hashed_mbis(self):
        return master_pac_mbis

    # Helper

    def _get(self, resource, name, parameters=None):
        params = {} if parameters is None else parameters
        params["mbi"] = self._hashed_mbis()

        self.run_task_by_parameters(
                base_path=f'/v2/fhir/{resource}',
                params=params,
                name=name)

    # Tests

    def _get_claim(self):
        self._get('Claim', 'claim')

    def _get_claim_with_service_date(self):
        self._get('Claim', 'claimServiceDate', self.SERVICE_DATE)

    def _get_claim_with_last_updated(self):
        self._get('Claim', 'claimLastUpdated', self.LAST_UPDATED)

    def _get_claim_with_service_date_and_last_updated(self):
        self._get('Claim', 'claimServiceDateLastUpdated', self.SERVICE_DATE_LAST_UPDATED)

    def _get_claim_response(self):
        self._get('ClaimResponse', 'claimResponse')

    def _get_claim_response_with_service_date(self):
        self._get('ClaimResponse', 'claimResponseServiceDate', self.SERVICE_DATE)

    def _get_claim_response_with_last_updated(self):
        self._get('ClaimResponse', 'claimResponseLastUpdated', self.LAST_UPDATED)

    def _get_claim_response_with_service_date_and_last_updated(self):
        self._get('ClaimResponse', 'claimResponseServiceDateLastUpdated', self.SERVICE_DATE_LAST_UPDATED)
