'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id_last_updated(self):
        '''Patient search by ID with last updated, include MBI'''
        self._test_v2_patient_test_id_last_updated()
