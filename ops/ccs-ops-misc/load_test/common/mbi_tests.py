'''Locust tests that require a pool of MBIs.'''

import random
from typing import List
from locust import events
from locust.env import Environment
from common.bfd_user_base import BFDUserBase
from common.url_path import create_url_path
from common import data, db

table_sample_hashed_mbis = True
master_hashed_mbis: List[str] = []

@events.init.add_listener
def _(environment: Environment, **kwargs):
    global master_hashed_mbis
    master_hashed_mbis = data.load_from_env(
        environment,
        db.get_hashed_mbis,
        use_table_sample=table_sample_hashed_mbis
    )

class MBITestUser(BFDUserBase):
    '''Locust tests that require a pool of hashed MBIs.'''

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.hashed_mbis = master_hashed_mbis.copy()
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
