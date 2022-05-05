'''Locust tests that require a pool of MBIs.'''

import random

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common import data, db


# If we try to load things from the database within the BFDUserBase class, we'll end up loading
# them once for every Worker, whereas loading it here will load once and each Worker will inherit
# a copy.
hashed_mbis = data.load_all(db.get_hashed_mbis, use_table_sample=True).copy()
random.shuffle(hashed_mbis)


class MBITestUser(BFDUserBase):
    '''Locust tests that require a pool of hashed MBIs.'''

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    # Helpers

    def __test_patient_hashed_mbi(self, version: str):
        '''Patient search by ID, Last Updated, include MBI, include Address'''

        if version not in ['v1', 'v2']:
            raise ValueError("Invalid version number")

        def make_url():
            return create_url_path(f'/{version}/fhir/Patient/', {
                'identifier': 'https://bluebutton.cms.gov/resources/identifier/mbi-hash|'
                    f'{hashed_mbis.pop()}',
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(
            name=f'/{version}/fhir/Patient search by hashed mbi / includeIdentifiers = mbi',
            url_callback=make_url)



    # Tests

    def _test_v1_patient_test_hashed_mbi(self):
        '''Patient search by ID, Last Updated, include MBI, include Address'''
        self.__test_patient_hashed_mbi('v1')


    def _test_v2_patient_test_hashed_mbi(self):
        '''Patient search by hashed MBI, include identifiers'''
        self.__test_patient_hashed_mbi('v2')
