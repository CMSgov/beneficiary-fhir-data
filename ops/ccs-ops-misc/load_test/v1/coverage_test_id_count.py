'''Single Locust test for BFD endpoint'''

from locust import task
from common.bene_tests import BeneTestUser
from common import validation

validation.set_validation_goal(validation.ValidationGoal.SLA_COVERAGE)

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    @task
    def coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self._test_v1_coverage_test_id_count()
