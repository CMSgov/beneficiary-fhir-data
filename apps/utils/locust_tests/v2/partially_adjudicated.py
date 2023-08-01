import random

from locust import events, tag, task
from locust.env import Environment

from common import data, db
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from common.types import CopyableEnumerable
from common.user_init_aware_load_shape import UserInitAwareLoadShape

master_pac_mbis: CopyableEnumerable[str] = []


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
        db.get_pac_hashed_mbis,
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
        self.hashed_mbis = master_pac_mbis.copy()

    def __get(self, resource, name, parameters=None):
        params = {} if parameters is None else parameters
        params["mbi"] = random.choice(self.hashed_mbis)

        self.run_task_by_parameters(base_path=f"/v2/fhir/{resource}", params=params, name=name)

    @tag("claim")
    @task
    def get_claim(self):
        """Get single Claim"""
        self.__get("Claim", "claim")

    @tag("claim", "service-date")
    @task
    def get_claim_with_service_date(self):
        """Get single Claim with service date"""
        self.__get("Claim", "claimServiceDate", self.SERVICE_DATE)

    @tag("claim", "last-updated")
    @task
    def get_claim_with_last_updated(self):
        """Get single Claim with last updated"""
        self.__get("Claim", "claimLastUpdated", self.LAST_UPDATED)

    @tag("claim", "service-date", "last-updated")
    @task
    def get_claim_with_service_date_and_last_updated(self):
        """Get single Claim with last updated and service date"""
        self.__get("Claim", "claimServiceDateLastUpdated", self.SERVICE_DATE_LAST_UPDATED)

    @tag("claim", "exclude-samsa")
    @task
    def get_claim_with_exclude_samsa(self):
        self.__get("Claim", "excludeSAMSA", True)

    @tag("claim-response")
    @task
    def get_claim_response(self):
        """Get single ClaimResponse"""
        self.__get("ClaimResponse", "claimResponse")

    @tag("claim-response", "service-date")
    @task
    def get_claim_response_with_service_date(self):
        """Get single ClaimResponse with service date"""
        self.__get("ClaimResponse", "claimResponseServiceDate", self.SERVICE_DATE)

    @tag("claim-response", "last-updated")
    @task
    def get_claim_response_with_last_updated(self):
        """Get single ClaimResponse with last updated"""
        self.__get("ClaimResponse", "claimResponseLastUpdated", self.LAST_UPDATED)

    @tag("claim-response", "service-date", "last-updated")
    @task
    def get_claim_response_with_service_date_and_last_updated(self):
        """Get single ClaimResponse with last updated and service date"""
        self.__get(
            "ClaimResponse",
            "claimResponseServiceDateLastUpdated",
            self.SERVICE_DATE_LAST_UPDATED,
        )

    @tag("claim", "exclude-samsa")
    @task
    def get_claim_response_with_exclude_samsa(self):
        self.__get("ClaimResponse", "excludeSAMSA", True)
