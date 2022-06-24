'''Locust tests that require a pool of MBIs.'''

import random

from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common import data, db


class MBITestUser(BFDUserBase):
    '''Locust tests that require a pool of hashed MBIs.'''

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    # Should we use the Table Sample feature of Postgres to query only against a portion of the
    # table?
    USE_TABLE_SAMPLE = True


    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.hashed_mbis = data.load_all(
            self.database_uri,
            db.get_hashed_mbis,
            use_table_sample=self.USE_TABLE_SAMPLE,
            table_sample_percent=self.table_sample_percent
        ).copy()
        random.shuffle(self.hashed_mbis)


    # Helpers

    def __test_patient_hashed_mbi(self, version: str):
        '''Patient search by ID, Last Updated, include MBI, include Address'''

        if version not in ['v1', 'v2']:
            raise ValueError("Invalid version number")

        def make_url():
            return create_url_path(f'/{version}/fhir/Patient/', {
                'identifier': 'https://bluebutton.cms.gov/resources/identifier/mbi-hash|'
                    f'{self.hashed_mbis.pop()}',
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
