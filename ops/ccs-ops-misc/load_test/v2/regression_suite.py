'''Regression test suite for V2 BFD Server endpoints.'''

from common.bene_tests import BeneTestUser
from common.contract_tests import ContractTestUser
from common.mbi_tests import MBITestUser
from common.validation import SLA_V2_BASELINE
from locust import task

class BFDUser(BeneTestUser, MBITestUser, ContractTestUser):
    '''Regression test suite for V2 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were determined to be
    representative of typical V2 endpoint loads. When running this test suite, all tests in this
    suite will be run in parallel, with equal weighting being applied to each.
    '''

    # The goals against which to measure these results. Note that they also include the Failsafe
    # cutoff, which will default to the V2 cutoff time if not set.
    VALIDATION_GOALS = SLA_V2_BASELINE

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False

    # No Table Sample for the Regression Suite, because we want to keep the tests more consistent.
    USE_TABLE_SANPLE = False


    @task
    def coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self._test_v2_coverage_test_id_count()


    @task
    def coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self._test_v2_coverage_test_id_last_updated()


    @task
    def coverage_test_id(self):
        '''Coverage search by ID, Last Updated'''
        self._test_v2_coverage_test_id()


    @task
    def eob_test_id_count(self):
        '''Explanation of Benefit search by ID, Paginated'''
        self._test_v2_eob_test_id_count()


    @task
    def eob_test_id_include_tax_number(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self._test_v2_eob_test_id_include_tax_number()


    @task
    def eob_test_id(self):
        '''Explanation of Benefit search by ID'''
        self._test_v2_eob_test_id()


    @task
    def patient_test_coverage_contract(self):
        '''Patient search by Coverage Contract, paginated'''
        self._test_v2_patient_test_coverage_contract()


    @task
    def patient_test_hashed_mbi(self):
        '''Patient search by hashed MBI, include identifiers'''
        self._test_v2_patient_test_hashed_mbi()


    @task
    def patient_test_id_last_updated(self):
        '''Patient search by ID with last updated, include MBI'''
        self._test_v2_patient_test_id_last_updated()


    @task
    def patient_test_id(self):
        '''Patient search by ID'''
        self._test_v2_patient_test_id()
