from common.BFDUserBase import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id(self):
        def make_url(instance):
            return create_url_path('/v2/fhir/Patient', {
                '_id': instance.eob_ids.pop(),
                '_format': 'application/fhir+json'
            })

        self.run_task(name='/v2/fhir/Patient search by id', url_callback=make_url)
