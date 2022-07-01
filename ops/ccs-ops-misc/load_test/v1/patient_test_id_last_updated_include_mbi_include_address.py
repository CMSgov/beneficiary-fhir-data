'''Single Locust test for BFD endpoint'''

from locust import task
from common import validation
from common.bene_tests import BeneTestUser

validation.set_validation_goal(validation.ValidationGoal.SLA_PATIENT)

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    @task
    def patient_test_id_last_updated_include_mbi_include_address(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        self._test_v1_patient_test_id_last_updated_include_mbi_include_address()
