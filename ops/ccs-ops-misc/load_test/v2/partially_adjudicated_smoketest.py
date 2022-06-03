from locust import HttpUser, task, tag
from common.pac_tests import PACTestUser


class PACUser(PACTestUser):

    """
    Tests for Partially Adjudicated Claims endpoints to error check the transformers
    
    Copies are made of the MBI list so that both the claim and claimResponse object is checked
    for every MBI.
    """

    @classmethod
    def _hashed_mbis(cls):
        return super()._hashed_mbis().copy()

    @tag('claim')
    @task
    def get_claim(self):
        """Get single Claim"""
        self._get_claim()

    @tag("claim-response")
    @task
    def get_claim_response(self):
        """Get single ClaimResponse"""
        self._get_claim_response()
