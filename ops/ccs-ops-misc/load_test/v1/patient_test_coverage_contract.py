'''Single Locust test for BFD endpoint'''

from locust import task
from common import validation
from common.contract_tests import ContractTestUser

validation.set_validation_goal(validation.ValidationGoal.SLA_PATIENT)

class BFDUser(ContractTestUser):
    '''Single Locust test for BFD endpoint'''

    @task
    def patient_test_coverage_contract(self):
        '''Patient search by coverage contract (all pages)'''
        self._test_v1_patient_test_coverage_contract()
