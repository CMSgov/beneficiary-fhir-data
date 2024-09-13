import itertools
from typing import Collection

from locust import events, tag, task
from locust.env import Environment

from common import data, db
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from common.user_init_aware_load_shape import UserInitAwareLoadShape

master_pac_mbis: Collection[str] = []


@events.test_start.add_listener
def _(environment: Environment, **kwargs):
    if (
        is_distributed(environment)
        and is_locust_master(environment)
        or not environment.parsed_options
    ):
        # Don't bother loading data for the master runner, it doesn't run a test
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_pac_mbis
    master_pac_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_pac_mbis_smoketest,
        use_table_sample=False,
        data_type_name="pac_mbis",
    )


class TestLoadShape(UserInitAwareLoadShape):
    pass


class PACSmokeUser(BFDUserBase):
    """
    Tests for Partially Adjudicated Claims endpoints to error check the transformers

    Copies are made of the MBI list so that both the claim and claimResponse object is checked
    for every MBI.
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.mbis = list(master_pac_mbis)

    def __post(self, resource, name, body=None):
        params = {}
        body = {} if body is None else body
        body["mbi"] = itertools.cycle(self.mbis)

        self.run_task_by_parameters(
            base_path=f"/v2/fhir/{resource}", params=params, body=body, name=name
        )

    @tag("claim")
    @task
    def get_claim(self):
        """Get single Claim"""
        self.__post("Claim/_search", "claim")

    @tag("claim-response")
    @task
    def get_claim_response(self):
        """Get single ClaimResponse"""
        self.__post("ClaimResponse/_search", "claimResponse")
