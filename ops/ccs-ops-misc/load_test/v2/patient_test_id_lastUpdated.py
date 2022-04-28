from common.BFDUserBase import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id_lastUpdated(self):
        def make_url(instance):
            return create_url_path('/v2/fhir/Patient', {
                '_id': instance.eob_ids.pop(),
                '_format': 'application/fhir+json',
                '_IncludeIdentifiers': 'mbi',
                '_lastUpdated': f'gt{instance.last_updated}'
            })

        self.run_task(name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / (2 weeks)', url_callback=make_url)
