"""Regression test suite for V2 BFD Server endpoints."""

from typing import Dict, List

from locust import events, tag, task
from locust.env import Environment

from common import data, db
from common.bfd_user_base import BFDUserBase, set_comparisons_metadata_path
from common.locust_utils import is_distributed, is_locust_master
from common.url_path import create_url_path
from common.user_init_aware_load_shape import UserInitAwareLoadShape

master_bene_ids: List[str] = []
master_contract_data: List[Dict[str, str]] = []
master_hashed_mbis: List[str] = []


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
    global master_bene_ids
    master_bene_ids = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_bene_ids,
        data_type_name="bene_ids",
    )

    global master_contract_data
    master_contract_data = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_contract_ids,
        data_type_name="contract_data",
    )

    global master_hashed_mbis
    master_hashed_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_hashed_mbis,
        data_type_name="hashed_mbis",
    )


set_comparisons_metadata_path("./config/regression_suites_compare_meta.json")


class TestLoadShape(UserInitAwareLoadShape):
    pass


class RegressionV2User(BFDUserBase):
    """Regression test suite for V2 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were determined to be
    representative of typical V2 endpoint loads. When running this test suite, all tests in this
    suite will be run in parallel, with equal weighting being applied to each.
    """

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.bene_ids = master_bene_ids.copy()
        self.contract_data = master_contract_data.copy()
        self.hashed_mbis = master_hashed_mbis.copy()

    @tag("coverage", "coverage_test_id_count")
    @task
    def coverage_test_id_count(self):
        """Coverage search by ID, Paginated"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={"beneficiary": self.bene_ids.pop(), "_count": "10"},
            name="/v2/fhir/Coverage search by id / count=10",
        )

    @tag("coverage", "coverage_test_id")
    @task
    def coverage_test_id(self):
        """Coverage search by ID"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={
                "beneficiary": self.bene_ids.pop(),
            },
            name="/v2/fhir/Coverage search by id",
        )

    @tag("eob", "eob_test_id_count")
    @task
    def eob_test_id_count(self):
        """Explanation of Benefit search by ID, Paginated"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": self.bene_ids.pop(),
                "_count": "10",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / count=10",
        )

    @tag("eob", "eob_test_id_include_tax_number")
    @task
    def eob_test_id_include_tax_number(self):
        """Explanation of Benefit search by ID, Include Tax Numbers"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": self.bene_ids.pop(),
                "_IncludeTaxNumbers": "true",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / includeTaxNumbers",
        )

    @tag("eob", "eob_test_id")
    @task
    def eob_test_id(self):
        """Explanation of Benefit search by ID"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={"patient": self.bene_ids.pop(), "_format": "application/fhir+json"},
            name="/v2/fhir/ExplanationOfBenefit search by id",
        )

    @tag("patient", "patient_test_coverage_contract")
    @task
    def patient_test_coverage_contract(self):
        """Patient search by Coverage Contract, paginated"""

        def make_url():
            contract = self.contract_data.pop()
            return create_url_path(
                f"/v2/fhir/Patient",
                {
                    "_has:Coverage.extension": f'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract["id"]}',
                    "_has:Coverage.rfrncyr": f'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract["year"]}',
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.run_task(
            name=f"/v2/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient", "patient_test_hashed_mbi")
    @task
    def patient_test_hashed_mbi(self):
        """Patient search by hashed MBI, include identifiers"""

        def make_url():
            return create_url_path(
                f"/v2/fhir/Patient/",
                {
                    "identifier": f"https://bluebutton.cms.gov/resources/identifier/mbi-hash|{self.hashed_mbis.pop()}",
                    "_IncludeIdentifiers": "mbi",
                },
            )

        self.run_task(
            name=f"/v2/fhir/Patient search by hashed mbi / includeIdentifiers = mbi",
            url_callback=make_url,
        )

    @tag("patient", "patient_test_id_include_mbi")
    @task
    def patient_test_id_include_mbi(self):
        """Patient search by ID, include MBI"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": self.bene_ids.pop(),
                "_format": "application/fhir+json",
                "_IncludeIdentifiers": "mbi",
            },
            name="/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi",
        )

    @tag("patient", "patient_test_id")
    @task
    def patient_test_id(self):
        """Patient search by ID"""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": self.bene_ids.pop(),
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/Patient search by id",
        )
