'''Regression test suite for V2 BFD Server endpoints.'''

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_V2_BASELINE
from locust import task

class BFDUser(BFDUserBase):
    '''Regression test suite for V2 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were determined to be
    representative of typical V2 endpoint loads. When running this test suite, all tests in this
    suite will be run in parallel, with equal weighting being applied to each.
    '''

    DATA_REQUIRED = [ 'BENE_IDS', 'MBIS', 'CONTRACT_IDS' ]
    SLA_BASELINE = SLA_V2_BASELINE

    @task
    def coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                'beneficiary': self.bene_ids,
                '_count': '10'
            }, name='/v2/fhir/Coverage search by id / count=10')


    @task
    def coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')


    @task
    def coverage_test_id(self):
        '''Coverage search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id')


    @task
    def eob_test_id_count(self):
        '''Explanation of Benefit search by ID, Paginated'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_count': '10',
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id / count=10')


    @task
    def eob_test_id_include_tax_number(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'patient': self.bene_ids,
                '_IncludeTaxNumbers': 'true',
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers')


    @task
    def eob_test_id(self):
        '''Explanation of Benefit search by ID'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id')


    @task
    def patient_test_coverage_contract(self):
        '''Patient search by Coverage Contract, paginated'''
        def make_url():
            contract_data = self.contract_ids.pop()
            return create_url_path('/v2/fhir/Patient', {
                '_has:Coverage.extension':
                    'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|'
                    f'{contract_data["id"]}',
                '_has:Coverage.rfrncyr':
                    'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|'
                    f'{contract_data["year"]}',
                '_count': 25,
                '_format': 'json'
            })

        self.run_task(name='/v2/fhir/Patient search by coverage contract (all pages)',
            headers={"IncludeIdentifiers": "mbi"}, url_callback=make_url)


    @task
    def patient_test_hashed_mbi(self):
        '''Patient search by hashed MBI, include identifiers'''
        def make_url():
            return create_url_path('/v2/fhir/Patient', {
                'identifier': 'https://bluebutton.cms.gov/resources/identifier/mbi-hash|' +
                    self.mbis.pop(),
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(name='/v2/fhir/Patient search by hashed mbi / _IncludeIdentifiers=mbi',
            url_callback=make_url)


    @task
    def patient_test_id_last_updated(self):
        '''Patient search by ID with last updated, include MBI'''
        self.run_task_by_parameters(base_path='/v2/fhir/Patient', params={
                '_id': self.bene_ids,
                '_format': 'application/fhir+json',
                '_IncludeIdentifiers': 'mbi',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / (2 weeks)')


    @task
    def patient_test_id(self):
        '''Patient search by ID'''
        self.run_task_by_parameters(base_path='/v2/fhir/Patient', params={
                '_id': self.bene_ids,
                '_format': 'application/fhir+json',
        }, name='/v2/fhir/Patient search by id')
