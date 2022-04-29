from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'MBIS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_hashedMbi(self):
        def make_url(instance):
            return create_url_path(f'/v1/fhir/Patient/', {
                'identifier': f'https://bluebutton.cms.gov/resources/identifier/mbi-hash|{instance.mbis.pop()}',
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(name='/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi', url_callback=make_url)
