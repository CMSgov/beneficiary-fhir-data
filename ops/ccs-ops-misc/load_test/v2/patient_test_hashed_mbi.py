'''Single Locust test for BFD endpoint'''

from common.mbi_tests import MBITestUser
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(MBITestUser):
    '''Single Locust test for BFD endpoint'''

    # The goals against which to measure these results. Note that they also include the Failsafe
    # cutoff, which will default to the V2 cutoff time if not set.
    VALIDATION_GOALS = SLA_PATIENT


    @task
    def patient_test_hashed_mbi(self):
        '''Patient search by hashed MBI, include identifiers'''
        self._test_v2_patient_test_hashed_mbi()
