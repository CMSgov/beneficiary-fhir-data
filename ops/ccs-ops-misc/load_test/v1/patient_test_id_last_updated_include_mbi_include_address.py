'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id_last_updated_include_mbi_include_address(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        self.run_task_by_parameters(base_path='/v1/fhir/Patient', params={
                '_id': self.bene_ids.pop(),
                '_lastUpdated': f'gt{self.last_updated}',
                '_IncludeIdentifiers': 'mbi',
                '_IncludeTaxNumbers': 'true'
        }, name='/v1/fhir/Patient/id search by id / (2 weeks) / includeTaxNumbers / mbi')
