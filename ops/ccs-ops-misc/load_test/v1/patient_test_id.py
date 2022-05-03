'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id(self):
        '''Patient search by ID'''
        self._test_v1_patient_test_id()
