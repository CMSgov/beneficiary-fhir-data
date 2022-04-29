from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_COVERAGE
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_COVERAGE

    @task
    def coverage_test_id_count(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/Coverage', {
                'beneficiary': instance.bene_ids.pop(),
                '_count': '10'
            })

        self.run_task(name='/v1/fhir/Coverage search by id / count=10', url_callback=make_url)
