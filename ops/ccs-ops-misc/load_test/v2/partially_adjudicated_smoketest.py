from typing import List

from locust import events, tag, task
from locust.env import Environment

from common import data, db
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master

master_pac_mbis: List[str] = []


@events.test_start.add_listener
def _(environment: Environment, **kwargs):
    if is_distributed(environment) and is_locust_master(environment) or not environment.parsed_options:
        # Don't bother loading data for the master runner, it doesn't run a test
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_pac_mbis
    master_pac_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_pac_hashed_mbis,
        use_table_sample=False,
    )


class PACSmokeUser(BFDUserBase):

    """
    Tests for Partially Adjudicated Claims endpoints to error check the transformers

    Copies are made of the MBI list so that both the claim and claimResponse object is checked
    for every MBI.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.hashed_mbis = master_pac_mbis.copy()

    def __get(self, resource, name, parameters=None):
        params = {} if parameters is None else parameters
        params["mbi"] = self.hashed_mbis.pop()

        self.run_task_by_parameters(base_path=f"/v2/fhir/{resource}", params=params, name=name)

    @tag("claim")
    @task
    def get_claim(self):
        """Get single Claim"""
        self.__get("Claim", "claim")

    @tag("claim-response")
    @task
    def get_claim_response(self):
        """Get single ClaimResponse"""
        self.__get("ClaimResponse", "claimResponse")
