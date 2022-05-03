'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.validation import SLA_COVERAGE
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_COVERAGE

    @task
    def coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')
