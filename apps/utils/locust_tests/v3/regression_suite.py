import itertools
from typing import Collection
from locust import events, tag, task
from locust.env import Environment

from common import data, db_idr
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master

master_bene_sks: Collection[str] = []
master_bene_mbis: Collection[str] = []


@events.test_start.add_listener
def _(environment: Environment, **kwargs):
    if (
        is_distributed(environment)
        and is_locust_master(environment)
        or not environment.parsed_options
    ):
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_bene_sks
    master_bene_sks = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_bene_sks,
        data_type_name="bene_ids",
    )

    global master_bene_mbis
    master_bene_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_bene_mbis,
        data_type_name="bene_mbis",
    )


class RegressionV3User(BFDUserBase):
    """Regression test suite for V3 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were determined to be
    representative of typical V3 endpoint loads. When running this test suite, all tests in this
    suite will be run in parallel, with equal weighting being applied to each.
    """

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False
    BENE_LAST_UPDATED = {"_lastUpdated": "gt2020-05-05"}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.bene_sks = itertools.cycle(list(master_bene_sks))
        self.bene_mbis = itertools.cycle(list(master_bene_mbis))

    @tag("patient", "patient_read_id")
    @task
    def patient_read_id(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path=f"/v3/fhir/Patient/{next(self.bene_sks)}",
            params={
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Patient read",
        )

    @tag("patient", "patient_search_id")
    @task
    def patient_search_id(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient",
            params={
                "_id": next(self.bene_sks),
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Patient search by id",
        )

    @tag("patient", "patient_search_id_post")
    @task
    def patient_search_id_post(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient/_search",
            body={
                "_id": next(self.bene_sks),
            },
            name="/v3/fhir/Patient search by id (POST)",
        )

    @tag("patient", "patient_search_mbi")
    @task
    def patient_search_mbi(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient",
            params={
                "identifier": f"http://hl7.org/fhir/sid/us-mbi|{next(self.bene_mbis)}",
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Patient search by mbi",
        )

    @tag("patient", "patient_search_mbi_last_updated")
    @task
    def patient_search_mbi_last_updated(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient",
            params={
                "identifier": f"http://hl7.org/fhir/sid/us-mbi|{next(self.bene_mbis)}",
                "_format": "application/fhir+json",
            }
            | self.BENE_LAST_UPDATED,
            name="/v3/fhir/Patient search by mbi with last updated",
        )

    @tag("patient", "patient_search_mbi_post")
    @task
    def patient_search_mbi_post(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient/_search",
            body={
                "identifier": f"http://hl7.org/fhir/sid/us-mbi|{next(self.bene_mbis)}",
            },
            name="/v3/fhir/Patient search by mbi (POST)",
        )
