'''Single Locust test for BFD endpoint'''

from locust import task
from common import validation
from common.mbi_tests import MBITestUser

validation.set_validation_goal(validation.ValidationGoal.SLA_PATIENT)

class BFDUser(MBITestUser):
    '''Single Locust test for BFD endpoint'''

    @task
    def patient_test_hashed_mbi(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        self._test_v1_patient_test_hashed_mbi()
