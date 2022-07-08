'''Single Locust test for BFD endpoint'''

from locust import task
from common import validation
from common.bene_tests import BeneTestUser

validation.set_validation_goal(validation.ValidationGoal.SLA_EOB_WITHOUT_SINCE)

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    @task
    def eob_test_id_include_tax_number_last_updated(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self._test_v1_eob_test_id_include_tax_number_last_updated()
