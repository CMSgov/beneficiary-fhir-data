from common.BFDUserBase import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_EOB_WITHOUT_SINCE

    @task
    def eob_test_id_count(self):
        def make_url(instance):
            return create_url_path('/v2/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_count': '10',
                '_format': 'application/fhir+json'
            })

        self.run_task(name='/v2/fhir/ExplanationOfBenefit search by id / count=10', url_callback=make_url)
