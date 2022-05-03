'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_EOB_WITHOUT_SINCE


    @task
    def eob_test_id_include_tax_number(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'patient': self.bene_ids,
                '_IncludeTaxNumbers': 'true',
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers')
