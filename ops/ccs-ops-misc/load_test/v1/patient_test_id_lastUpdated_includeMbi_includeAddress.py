from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_id_lastUpdated_includeMbi_includeAddress(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/Patient', {
                '_id': instance.bene_ids.pop(),
                '_lastUpdated': f'gt{instance.last_updated}',
                '_IncludeIdentifiers': 'mbi',
                '_IncludeTaxNumbers': 'true'
            })

        self.run_task(name='/v1/fhir/Patient/id search by id / (2 weeks) / includeTaxNumbers / mbi', url_callback=make_url)
