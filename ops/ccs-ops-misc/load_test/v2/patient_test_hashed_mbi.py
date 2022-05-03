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
        '''Patient search by hashed MBI, include identifiers'''
        def make_url():
            return create_url_path('/v2/fhir/Patient', {
                'identifier': 'https://bluebutton.cms.gov/resources/identifier/mbi-hash|' +
                    self.mbis.pop(),
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(name='/v2/fhir/Patient search by hashed mbi / _IncludeIdentifiers=mbi',
            url_callback=make_url)
