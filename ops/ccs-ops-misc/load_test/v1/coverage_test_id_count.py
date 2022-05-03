'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_COVERAGE
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    SLA_BASELINE = SLA_COVERAGE

    @task
    def coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self._test_v1_coverage_test_id_count()
