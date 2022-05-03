'''Locust tests that require a pool of Contract data.'''

import random

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common import data, db


# If we try to load things from the database within the BFDUserBase class, we'll end up loading
# them once for every Worker, whereas loading it here will load once and each Worker will inherit
# a copy.
contract_data = data.load_data_segment(db.get_contract_ids).copy()
random.shuffle(contract_data)


class ContractTestUser(BFDUserBase):
    '''Locust tests that require a pool of contract data.'''

    # Helpers

    def __test_patient_test_coverage_contract(self, version: str):
        '''Patient search by coverage contract (all pages)'''

        if version not in ['v1', 'v2']:
            raise ValueError("Invalid version number")

        def make_url():
            contract = contract_data.pop()
            return create_url_path(f'/{version}/fhir/Patient', {
                '_has:Coverage.extension':
                    f'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract["id"]}',
                '_has:Coverage.rfrncyr':
                    f'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract["year"]}',
                '_count': 25,
                '_format': 'json'
            })

        self.run_task(name=f'/{version}/fhir/Patient search by coverage contract (all pages)',
            headers={"IncludeIdentifiers": "mbi"}, url_callback=make_url)


    # Tests

    def _test_v1_patient_test_coverage_contract(self):
        '''Patient search by coverage contract (all pages)'''
        self.__test_patient_test_coverage_contract('v1')


    def _test_v2_patient_test_coverage_contract(self):
        '''Patient search by Coverage Contract, paginated'''
        self.__test_patient_test_coverage_contract('v2')
