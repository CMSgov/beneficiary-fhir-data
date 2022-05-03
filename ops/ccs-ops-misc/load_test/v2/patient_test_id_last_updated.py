'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id_last_updated(self):
        '''Patient search by ID with last updated, include MBI'''
        self.run_task_by_parameters(base_path='/v2/fhir/Patient', params={
                '_id': self.bene_ids,
                '_format': 'application/fhir+json',
                '_IncludeIdentifiers': 'mbi',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / (2 weeks)')
