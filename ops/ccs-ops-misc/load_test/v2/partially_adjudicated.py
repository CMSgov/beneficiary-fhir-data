from locust import HttpUser, task, tag
from common.pac_tests import PACTestUser


class PACUser(PACTestUser):

    """Tests for Partially Adjudicated Claims endpoints"""

    @tag('claim')
    @task
    def get_claim(self):
        """Get single Claim"""
        self._get_claim()

    @tag('claim', 'service-date')
    @task
    def get_claim_with_service_date(self):
        """Get single Claim with service date"""
        self._get_claim_with_service_date()

    @tag('claim', 'last-updated')
    @task
    def get_claim_with_last_updated(self):
        """Get single Claim with last updated"""
        self._get_claim_with_last_updated()

    @tag('claim', 'service-date', 'last-updated')
    @task
    def get_claim_with_service_date_and_last_updated(self):
        """Get single Claim with last updated and service date"""
        self._get_claim_with_service_date_and_last_updated()

    @tag("claim-response")
    @task
    def get_claim_response(self):
        """Get single ClaimResponse"""
        self._get_claim_response()

    @tag('claim-response', 'service-date')
    @task
    def get_claim_response_with_service_date(self):
        """Get single ClaimResponse with service date"""
        self._get_claim_response_with_service_date()

    @tag('claim-response', 'last-updated')
    @task
    def get_claim_response_with_last_updated(self):
        """Get single ClaimResponse with last updated"""
        self._get_claim_response_with_last_updated()

    @tag('claim-response', 'service-date', 'last-updated')
    @task
    def get_claim_response_with_service_date_and_last_updated(self):
        """Get single ClaimResponse with last updated and service date"""
        self._get_claim_response_with_service_date_and_last_updated()
