'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    SLA_BASELINE = SLA_EOB_WITHOUT_SINCE


    @task
    def eob_test_id_include_tax_number(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self._test_v1_eob_test_id_include_tax_number()
