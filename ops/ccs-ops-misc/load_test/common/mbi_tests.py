"""Locust tests that require a pool of MBIs."""

import random
from typing import List

from locust import events
from locust.env import Environment

from common import data, db
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from common.url_path import create_url_path

table_sample_hashed_mbis = True
master_hashed_mbis: List[str] = []


@events.init.add_listener
def _(environment: Environment, **kwargs):
    if is_distributed(environment) and is_locust_master(environment) or not environment.parsed_options:
        # Don't bother loading data for the master runner, it doesn't run a test
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_hashed_mbis
    master_hashed_mbis = data.load_from_parsed_opts(
        environment.parsed_options, db.get_hashed_mbis, use_table_sample=table_sample_hashed_mbis
    )


class MBITestUser(BFDUserBase):
    """Locust tests that require a pool of hashed MBIs."""

    # Mark this class as abstract so Locust knows it doesn't contain Tasks
    abstract = True

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.hashed_mbis = master_hashed_mbis.copy()
        random.shuffle(self.hashed_mbis)

    # Helpers

    def __test_patient_hashed_mbi(self, version: str):
        """Patient search by ID, Last Updated, include MBI, include Address"""

        if version not in ["v1", "v2"]:
            raise ValueError("Invalid version number")

        def make_url():
            return create_url_path(
                f"/{version}/fhir/Patient/",
                {
                    "identifier": "https://bluebutton.cms.gov/resources/identifier/mbi-hash|"
                    f"{self.hashed_mbis.pop()}",
                    "_IncludeIdentifiers": "mbi",
                },
            )

        self.run_task(
            name=f"/{version}/fhir/Patient search by hashed mbi / includeIdentifiers = mbi", url_callback=make_url
        )

    # Tests

    def _test_v1_patient_test_hashed_mbi(self):
        """Patient search by ID, Last Updated, include MBI, include Address"""
        self.__test_patient_hashed_mbi("v1")

    def _test_v2_patient_test_hashed_mbi(self):
        """Patient search by hashed MBI, include identifiers"""
        self.__test_patient_hashed_mbi("v2")
