from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_COVERAGE
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS' ]
    SLA_BASELINE = SLA_COVERAGE

    @task
    def coverage_test_id_lastUpdated(self):
        def make_url(instance):
            return create_url_path('/v2/fhir/Coverage', {
                '_lastUpdated': f'gt{instance.last_updated}',
                'beneficiary': instance.bene_ids.pop(),
            })

        self.run_task(name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)', url_callback=make_url)
