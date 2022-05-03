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
    def patient_test_id(self):
        '''Patient search by ID'''
        def make_url():
            return create_url_path(f'/v1/fhir/Patient/{self.bene_ids.pop()}', {})

        self.run_task(name='/v1/fhir/Patient/id', url_callback=make_url)
