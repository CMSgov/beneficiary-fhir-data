'''Regression test suite for V1 BFD Server endpoints.

The tests within this Locust test suite hit various endpoints that were
determined to be representative of typical V1 endpoint loads. When running
this test suite, all tests in this suite will be run in parallel, with
equal weighting being applied to each.
'''

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_V1_BASELINE
from locust import task

class BFDUser(BFDUserBase):
    '''Regression test suite for V1 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were
    determined to be representative of typical V1 endpoint loads. When running
    this test suite, all tests in this suite will be run in parallel, with
    equal weighting being applied to each.
    '''

    DATA_REQUIRED = [ 'BENE_IDS', 'MBIS', 'CONTRACT_IDS' ]
    SLA_BASELINE = SLA_V1_BASELINE


    @task
    def coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/Coverage', params={
                'beneficiary': self.bene_ids,
                '_count': '10'
            }, name='/v1/fhir/Coverage search by id / count=10')


    @task
    def coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v1/fhir/Coverage', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')


    @task
    def eob_test_id(self):
        '''Explanation of Benefit search by ID'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'application/fhir+json'
        }, name='/v1/fhir/ExplanationOfBenefit search by id')


    @task
    def eob_test_id_count_type_pde(self):
        '''Explanation of Benefit search by ID, type PDE, paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_count': '50',
                '_types': 'PDE'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50')


    @task
    def eob_test_id_last_updated_count(self):
        '''Explanation of Benefit search by ID, last updated, paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_count': '100',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / count = 100')


    @task
    def eob_test_id_include_tax_number(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_lastUpdated': f'gt{self.last_updated}',
                '_IncludeTaxNumbers': 'true'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers')


    @task
    def eob_test_id_last_updated(self):
        '''Explanation of Benefit search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated')


    @task
    def patient_test_id(self):
        '''Patient search by ID'''
        def make_url():
            return create_url_path(f'/v1/fhir/Patient/{self.bene_ids.pop()}', {})

        self.run_task(name='/v1/fhir/Patient/id', url_callback=make_url)


    @task
    def patient_test_id_last_updated_include_mbi_include_address(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        self.run_task_by_parameters(base_path='/v1/fhir/Patient', params={
                '_id': self.bene_ids.pop(),
                '_lastUpdated': f'gt{self.last_updated}',
                '_IncludeIdentifiers': 'mbi',
                '_IncludeTaxNumbers': 'true'
        }, name='/v1/fhir/Patient/id search by id / (2 weeks) / includeTaxNumbers / mbi')


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


    @task
    def patient_test_hashed_mbi(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        def make_url():
            return create_url_path('/v1/fhir/Patient/', {
                'identifier': 'https://bluebutton.cms.gov/resources/identifier/mbi-hash|'
                    f'{self.mbis.pop()}',
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(name='/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi',
            url_callback=make_url)
