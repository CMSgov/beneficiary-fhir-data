'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_COVERAGE
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    # The goals against which to measure these results. Note that they also include the Failsafe
    # cutoff, which will default to the V2 cutoff time if not set.
    VALIDATION_GOALS = SLA_COVERAGE

    @task
    def coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self._test_v2_coverage_test_id_last_updated()
