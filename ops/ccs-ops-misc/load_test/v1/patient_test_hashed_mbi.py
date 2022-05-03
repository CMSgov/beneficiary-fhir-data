'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'MBIS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_hashed_mbi(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        def make_url():
            return create_url_path('/v1/fhir/Patient/', {
                'identifier': 'https://bluebutton.cms.gov/resources/identifier/mbi-hash|'
                    f'{self.mbis.pop()}',
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(name='/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi',
            url_callback=make_url)
