'''Single Locust test for BFD endpoint'''

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(BFDUserBase):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'CONTRACT_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_coverage_contract(self):
        '''Patient search by coverage contract (all pages)'''
        def make_url():
            contract_data = self.contract_ids.pop()
            return create_url_path('/v1/fhir/Patient', {
                '_has:Coverage.extension':
                    'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|'
                    f'{contract_data["id"]}',
                '_has:Coverage.rfrncyr':
                    'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|'
                    f'{contract_data["year"]}',
                '_count': 25,
                '_format': 'json'
            })

        self.run_task(name='/v1/fhir/Patient search by coverage contract (all pages)',
            headers={"IncludeIdentifiers": "mbi"}, url_callback=make_url)
