'''Single Locust test for BFD endpoint'''

from locust import task
from common import validation
from common.bene_tests import BeneTestUser

validation.set_validation_goal(validation.ValidationGoal.SLA_COVERAGE)

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    @task
    def coverage_test_id(self):
        '''Coverage search by ID, Last Updated'''
        self._test_v2_coverage_test_id()
