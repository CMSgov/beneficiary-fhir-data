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

    @classmethod
    def _hashed_mbis(cls):
        if not hasattr(cls, '_hashed_mbis'):
            cls._hashed_mbis = data.load_all(
                    db.get_pac_hashed_mbis
                    use_table_sample=cls.USE_TABLE_SAMPLE)
        return cls._hashed_mbis

    # Helper

    def _get(self, resource, name, parameters=None):
        params = {} if parameters is None else parameters
        params["mbi"] = self.hashed_mbis()

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
