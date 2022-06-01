'''Single Locust test for BFD endpoint'''

from common.bene_tests import BeneTestUser
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BeneTestUser):
    '''Single Locust test for BFD endpoint'''
    
    # The goals against which to measure these results. Note that they also include the Failsafe
    # cutoff, which will default to the V2 cutoff time if not set.
    VALIDATION_GOALS = SLA_EOB_WITHOUT_SINCE

    @task
    def eob_test_id_count(self):
        '''Explanation of Benefit search by ID, Paginated'''
        self._test_v2_eob_test_id_count()
