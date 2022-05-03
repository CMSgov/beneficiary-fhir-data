'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_EOB_WITHOUT_SINCE


    @task
    def eob_test_id_count_type_pde(self):
        '''Explanation of Benefit search by ID, type PDE, paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_count': '50',
                '_types': 'PDE'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50')
