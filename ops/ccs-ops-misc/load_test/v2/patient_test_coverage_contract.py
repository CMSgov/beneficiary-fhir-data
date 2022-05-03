'''Single Locust test for BFD endpoint'''

from common.contract_tests import ContractTestUser
from common.url_path import create_url_path
from common.validation import SLA_PATIENT
from locust import task

class BFDUser(ContractTestUser):
    '''Single Locust test for BFD endpoint'''

    DATA_REQUIRED = [ 'CONTRACT_IDS' ]
    SLA_BASELINE = SLA_PATIENT


    @task
    def patient_test_coverage_contract(self):
        '''Patient search by Coverage Contract, paginated'''
        self._test_v2_patient_test_coverage_contract()
