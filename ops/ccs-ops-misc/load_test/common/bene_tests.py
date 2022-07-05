'''Locust tests that require a pool of beneficiary IDs.'''

import random
from typing import List
from locust import events
from locust.env import Environment
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from common.url_path import create_url_path
from common import data, db

table_sample_bene_ids = True
master_bene_ids: List[str] = []

@events.init.add_listener
def _(environment: Environment, **kwargs):
    if is_distributed(environment) and is_locust_master(environment) or not environment.parsed_options:
        return
    
    global master_bene_ids
    master_bene_ids = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_bene_ids,
        use_table_sample=table_sample_bene_ids
    )

class BeneTestUser(BFDUserBase):
    '''Locust tests that require a pool of beneficiary IDs.'''

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    def __init__(self, *args, **kwargs):
        '''Initialize.
        '''
        super().__init__(*args, **kwargs)
        self.bene_ids = master_bene_ids.copy()
        random.shuffle(self.bene_ids)


    # Tests

    def _test_v1_coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/Coverage', params={
                'beneficiary': self.bene_ids,
                '_count': '10'
            }, name='/v1/fhir/Coverage search by id / count=10')


    def _test_v1_coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v1/fhir/Coverage', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')


    def _test_v1_eob_test_id(self):
        '''Explanation of Benefit search by ID'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'application/fhir+json'
        }, name='/v1/fhir/ExplanationOfBenefit search by id')


    def _test_v1_eob_test_id_count_type_pde(self):
        '''Explanation of Benefit search by ID, type PDE, paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_count': '50',
                '_types': 'PDE'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50')


    def _test_v1_eob_test_id_last_updated_count(self):
        '''Explanation of Benefit search by ID, last updated, paginated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_count': '100',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / count = 100')


    def _test_v1_eob_test_id_include_tax_number_last_updated(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_lastUpdated': f'gt{self.last_updated}',
                '_IncludeTaxNumbers': 'true'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers')


    def _test_v1_eob_test_id_last_updated(self):
        '''Explanation of Benefit search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v1/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'json',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated')


    def _test_v1_patient_test_id(self):
        '''Patient search by ID'''
        def make_url():
            return create_url_path(f'/v1/fhir/Patient/{self.bene_ids.pop()}', {})

        self.run_task(name='/v1/fhir/Patient/id', url_callback=make_url)


    def _test_v1_patient_test_id_last_updated_include_mbi_include_address(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        self.run_task_by_parameters(base_path='/v1/fhir/Patient', params={
                '_id': self.bene_ids.pop(),
                '_lastUpdated': f'gt{self.last_updated}',
                '_IncludeIdentifiers': 'mbi',
                '_IncludeTaxNumbers': 'true'
        }, name='/v1/fhir/Patient/id search by id / (2 weeks) / includeTaxNumbers / mbi')


    def _test_v2_coverage_test_id_count(self):
        '''Coverage search by ID, Paginated'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                'beneficiary': self.bene_ids,
                '_count': '10'
            }, name='/v2/fhir/Coverage search by id / count=10')


    def _test_v2_coverage_test_id_last_updated(self):
        '''Coverage search by ID, Last Updated'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')


    def _test_v2_coverage_test_id(self):
        '''Coverage search by ID'''
        self.run_task_by_parameters(base_path='/v2/fhir/Coverage', params={
                'beneficiary': self.bene_ids,
        }, name='/v2/fhir/Coverage search by id')


    def _test_v2_eob_test_id_count(self):
        '''Explanation of Benefit search by ID, Paginated'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_count': '10',
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id / count=10')


    def _test_v2_eob_test_id_include_tax_number_last_updated(self):
        '''Explanation of Benefit search by ID, Last Updated, Include Tax Numbers'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                '_lastUpdated': f'gt{self.last_updated}',
                'patient': self.bene_ids,
                '_IncludeTaxNumbers': 'true',
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers')


    def _test_v2_eob_test_id(self):
        '''Explanation of Benefit search by ID'''
        self.run_task_by_parameters(base_path='/v2/fhir/ExplanationOfBenefit', params={
                'patient': self.bene_ids,
                '_format': 'application/fhir+json'
        }, name='/v2/fhir/ExplanationOfBenefit search by id')


    def _test_v2_patient_test_id_include_mbi_last_updated(self):
        '''Patient search by ID with last updated, include MBI'''
        self.run_task_by_parameters(base_path='/v2/fhir/Patient', params={
                '_id': self.bene_ids,
                '_format': 'application/fhir+json',
                '_IncludeIdentifiers': 'mbi',
                '_lastUpdated': f'gt{self.last_updated}'
        }, name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / (2 weeks)')


    def _test_v2_patient_test_id(self):
        '''Patient search by ID'''
        self.run_task_by_parameters(base_path='/v2/fhir/Patient', params={
                '_id': self.bene_ids,
                '_format': 'application/fhir+json',
        }, name='/v2/fhir/Patient search by id')
