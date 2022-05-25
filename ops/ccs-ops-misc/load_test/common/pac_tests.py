import random

from common import data, db
from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path


class PACTestUser(BFDUserBase):
    SERVICE_DATE = {'service-date': 'gt2020-01-05'}
    LAST_UPDATED = {'_lastUpdated': 'gt2020-05-05'}
    SERVICE_DATE_LAST_UPDATED = dict(SERVICE_DATE, **LAST_UPDATED)

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    # Should we use the Table Sample feature of Postgres to query only against a portion of the
    # table?
    USE_TABLE_SAMPLE = False

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.hashed_mbis = data.load_all(db.get_pac_hashed_mbis, use_table_sample=self.USE_TABLE_SAMPLE).copy()
        random.shuffle(self.hashed_mbis)

    # Helper

    def _mbi(self):
        return self.hashed_mbis.pop()

    def _get(self, resource, name, parameters=None):
        params = {} if parameters is None else parameters
        params["mbi"] = f'{self.hashed_mbis.pop()}'

        def make_url():
            return create_url_path(f'/v2/fhir/{resource}/', params)

        self.run_task(
            name=name,
            url_callback=make_url)

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
