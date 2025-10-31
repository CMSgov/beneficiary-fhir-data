"""Regression test suite for V1 BFD Server endpoints.

The tests within this Locust test suite hit various endpoints that were
determined to be representative of typical V1 endpoint loads. When running
this test suite, all tests in this suite will be run in parallel, with
equal weighting being applied to each.
"""

import itertools
from collections.abc import Collection
from typing import Any

from common import data, db
from common.bfd_user_base import BFDUserBase, set_comparisons_metadata_path
from common.locust_utils import is_distributed, is_locust_master
from common.url_path import create_url_path
from common.user_init_aware_load_shape import UserInitAwareLoadShape
from locust import events, tag, task
from locust.env import Environment

master_bene_ids: Collection[str] = []
master_contract_data: Collection[dict[str, str]] = []
master_hashed_mbis: Collection[str] = []


@events.test_start.add_listener
def _(environment: Environment, **kwargs: dict[str, Any]) -> None:
    if (
        is_distributed(environment) and is_locust_master(environment)
    ) or not environment.parsed_options:
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_bene_ids  # noqa: PLW0603
    master_bene_ids = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_bene_ids,
        data_type_name="bene_ids",
    )

    global master_contract_data  # noqa: PLW0603
    master_contract_data = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_contract_ids,
        data_type_name="contract_data",
    )

    global master_hashed_mbis  # noqa: PLW0603
    master_hashed_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_hashed_mbis,
        data_type_name="hashed_mbis",
    )


set_comparisons_metadata_path("./config/regression_suites_compare_meta.json")


class TestLoadShape(UserInitAwareLoadShape):
    pass


class RegressionV1User(BFDUserBase):
    """Regression test suite for V1 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were
    determined to be representative of typical V1 endpoint loads. When running
    this test suite, all tests in this suite will be run in parallel, with
    equal weighting being applied to each.
    """

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False

    def __init__(self, *args: tuple, **kwargs: dict[str, Any]) -> None:
        super().__init__(*args, **kwargs)
        self.bene_ids = itertools.cycle(list(master_bene_ids))
        self.contract_data = itertools.cycle(list(master_contract_data))
        self.hashed_mbis = itertools.cycle(list(master_hashed_mbis))

    @tag("coverage", "coverage_test_id_count")
    @task
    def coverage_test_id_count(self) -> None:
        """Coverage search by ID, Paginated."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/Coverage",
            params={"beneficiary": next(self.bene_ids), "_count": "10"},
            name="/v1/fhir/Coverage search by id / count=10",
        )

    @tag("eob", "eob_test_id_count_type_pde")
    @task
    def eob_test_id_count_type_pde(self) -> None:
        """Explanation of Benefit search by ID, type PDE, paginated."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_ids),
                "_format": "json",
                "_count": "50",
                "_types": "PDE",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50",
        )

    @tag("eob", "eob_test_id_count")
    @task
    def eob_test_id_count(self) -> None:
        """Explanation of Benefit search by ID, paginated."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_ids),
                "_format": "json",
                "_count": "100",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / count = 100",
        )

    @tag("eob", "eob_test_id_include_tax_number")
    @task
    def eob_test_id_include_tax_number(self) -> None:
        """Explanation of Benefit search by ID, Include Tax Numbers."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_ids),
                "_format": "json",
                "_IncludeTaxNumbers": "true",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / includeTaxNumbers",
        )

    @tag("eob", "eob_test_id")
    @task
    def eob_test_id(self) -> None:
        """Explanation of Benefit search by ID."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={"patient": next(self.bene_ids), "_format": "application/fhir+json"},
            name="/v1/fhir/ExplanationOfBenefit search by id",
        )

    @tag("patient", "patient_test_coverage_contract")
    @task
    def patient_test_coverage_contract(self) -> None:
        """Patient search by coverage contract (all pages)."""

        def make_url() -> str:
            contract = next(self.contract_data)
            return create_url_path(
                "/v1/fhir/Patient",
                {
                    "_has:Coverage.extension": f"https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract['id']}",
                    "_has:Coverage.rfrncyr": f"https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract['year']}",
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.run_task(
            name="/v1/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient", "patient_test_hashed_mbi")
    @task
    def patient_test_hashed_mbi(self) -> None:
        """Patient search by ID, include MBI, include Address."""

        def make_url() -> str:
            return create_url_path(
                "/v1/fhir/Patient/",
                {
                    "identifier": f"https://bluebutton.cms.gov/resources/identifier/mbi-hash|{next(self.hashed_mbis)}",
                    "_IncludeIdentifiers": "mbi",
                },
            )

        self.run_task(
            name="/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi",
            url_callback=make_url,
        )

    @tag("patient", "patient_test_id_include_mbi_include_address")
    @task
    def patient_test_id_include_mbi_include_address(self) -> None:
        """Patient search by ID, include MBI, include Address."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/Patient",
            params={
                "_id": next(self.bene_ids),
                "_IncludeIdentifiers": "mbi",
                "_IncludeTaxNumbers": "true",
            },
            name="/v1/fhir/Patient/id search by id / includeTaxNumbers / mbi",
        )

    @tag("patient", "patient_test_id")
    @task
    def patient_test_id(self) -> None:
        """Patient search by ID."""

        def make_url() -> str:
            return create_url_path(f"/v1/fhir/Patient/{next(self.bene_ids)}", {})

        self.run_task(name="/v1/fhir/Patient/id", url_callback=make_url)
