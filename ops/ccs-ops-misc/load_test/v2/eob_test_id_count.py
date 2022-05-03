'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''

    SLA_BASELINE = SLA_EOB_WITHOUT_SINCE

    @task
    def eob_test_id_count(self):
        '''Explanation of Benefit search by ID, Paginated'''
        self._test_v2_eob_test_id_count()
