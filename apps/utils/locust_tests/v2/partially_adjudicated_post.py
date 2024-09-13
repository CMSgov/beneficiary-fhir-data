import random
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
        db.get_pac_mbis,
        use_table_sample=False,
        data_type_name="pac_mbis",
    )


class TestLoadShape(UserInitAwareLoadShape):
    pass


class PACUser(BFDUserBase):
    """
    Tests for Partially Adjudicated Claims endpoints to test their performance

    The MBI list is randomly shuffled to get better sampling in sequential testing.
    """

    SERVICE_DATE = {"service-date": "gt2020-01-05"}
    LAST_UPDATED = {"_lastUpdated": "gt2020-05-05"}
    SERVICE_DATE_LAST_UPDATED = dict(SERVICE_DATE, **LAST_UPDATED)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.mbis = list(master_pac_mbis)

    def __post(self, resource, name, body=None):
        params = {}
        body = {} if body is None else body
        body["mbi"] = random.choice(self.mbis)

        self.run_task_by_parameters(
            base_path=f"/v2/fhir/{resource}", params=params, body=body, name=name
        )

    @tag("claim")
    @task
    def get_claim(self):
        """Get single Claim"""
        self.__post("Claim/_search", "claim")

    @tag("claim", "service-date")
    @task
    def get_claim_with_service_date(self):
        """Get single Claim with service date"""
        self.__post("Claim/_search", "claimServiceDate", self.SERVICE_DATE)

    @tag("claim", "last-updated")
    @task
    def get_claim_with_last_updated(self):
        """Get single Claim with last updated"""
        self.__post("Claim/_search", "claimLastUpdated", self.LAST_UPDATED)

    @tag("claim", "service-date", "last-updated")
    @task
    def get_claim_with_service_date_and_last_updated(self):
        """Get single Claim with last updated and service date"""
        self.__post("Claim/_search", "claimServiceDateLastUpdated", self.SERVICE_DATE_LAST_UPDATED)

    @tag("claim", "exclude-samsa")
    @task
    def get_claim_with_exclude_samsa(self):
        self.__post("Claim/_search", "excludeSAMSA", True)

    @tag("claim-response")
    @task
    def get_claim_response(self):
        """Get single ClaimResponse"""
        self.__post("ClaimResponse/_search", "claimResponse")

    @tag("claim-response", "service-date")
    @task
    def get_claim_response_with_service_date(self):
        """Get single ClaimResponse with service date"""
        self.__post("ClaimResponse/_search", "claimResponseServiceDate", self.SERVICE_DATE)

    @tag("claim-response", "last-updated")
    @task
    def get_claim_response_with_last_updated(self):
        """Get single ClaimResponse with last updated"""
        self.__post("ClaimResponse/_search", "claimResponseLastUpdated", self.LAST_UPDATED)

    @tag("claim-response", "service-date", "last-updated")
    @task
    def get_claim_response_with_service_date_and_last_updated(self):
        """Get single ClaimResponse with last updated and service date"""
        self.__post(
            "ClaimResponse/_search",
            "claimResponseServiceDateLastUpdated",
            self.SERVICE_DATE_LAST_UPDATED,
        )

    @tag("claim", "exclude-samsa")
    @task
    def get_claim_response_with_exclude_samsa(self):
        self.__post("ClaimRespons/_search", "excludeSAMSA", True)
