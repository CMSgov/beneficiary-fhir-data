from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'CONTRACT_IDS' ]
    SLA_BASELINE = SLA_PATIENT

    @task
    def patient_test_coverageContract(self):
        def make_url(instance):
            contract_data = instance.contract_ids.pop()
            return create_url_path('/v2/fhir/Patient', {
                '_has:Coverage.extension': f'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract_data["id"]}',
                '_has:Coverage.rfrncyr': f'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract_data["year"]}',
                '_count': 25,
                '_format': 'json'
            })

        self.run_task(name='/v2/fhir/Patient search by coverage contract (all pages)', headers={"IncludeIdentifiers": "mbi"},
                url_callback=make_url)
