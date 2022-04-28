from common.BFDUserBase import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_EOB_WITHOUT_SINCE
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_EOB_WITHOUT_SINCE


    @task
    def eob_test_id_count_typePDE(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_format': 'json',
                '_count': '50',
                '_types': 'PDE'
            })

        self.run_task(name='/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50', url_callback=make_url)
