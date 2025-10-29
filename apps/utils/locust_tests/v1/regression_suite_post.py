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
from common.user_init_aware_load_shape import UserInitAwareLoadShape
from locust import events, tag, task
from locust.env import Environment

master_bene_ids: Collection[str] = []
master_contract_data: Collection[dict[str, str]] = []
master_mbis: Collection[str] = []


@events.test_start.add_listener
def _(environment: Environment, **kwargs: dict[str, Any]) -> None:  # noqa: ARG001
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

    global master_mbis  # noqa: PLW0603
    master_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_mbis,
        data_type_name="mbis",
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
        self.mbis = itertools.cycle(list(master_mbis))

    @tag("eob", "eob_test_id_count_type_pde")
    @task
    def eob_test_id_count_type_pde(self) -> None:
        """Explanation of Benefit search by ID, type PDE, paginated."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit/_search",
            body={
                "patient": next(self.bene_ids),
                "_format": "json",
                "_count": "50",
                "_types": "PDE",
            },
            name="/v1/fhir/ExplanationOfBenefit/_search search by id / type = PDE / count = 50",
        )

    @tag("eob", "eob_test_id_count")
    @task
    def eob_test_id_count(self) -> None:
        """Explanation of Benefit search by ID, paginated."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit/_search",
            body={
                "patient": next(self.bene_ids),
                "_format": "json",
                "_count": "100",
            },
            name="/v1/fhir/ExplanationOfBenefit/_search search by id / count = 100",
        )

    @tag("eob", "eob_test_id_include_tax_number")
    @task
    def eob_test_id_include_tax_number(self) -> None:
        """Explanation of Benefit search by ID, Include Tax Numbers."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit/_search",
            body={
                "patient": next(self.bene_ids),
                "_format": "json",
                "_IncludeTaxNumbers": "true",
            },
            name="/v1/fhir/ExplanationOfBenefit/_search search by id / includeTaxNumbers",
        )

    @tag("eob", "eob_test_id")
    @task
    def eob_test_id(self) -> None:
        """Explanation of Benefit search by ID."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit/_search",
            body={"patient": next(self.bene_ids), "_format": "application/fhir+json"},
            name="/v1/fhir/ExplanationOfBenefit/_search search by id",
        )

    @tag("patient", "patient_test_coverage_contract")
    @task
    def patient_test_coverage_contract(self) -> None:
        """Patient search by coverage contract (all pages)."""
        contract = next(self.contract_data)
        self.run_task_by_parameters(
            base_path="/v1/fhir/Patient/_search",
            body={
                "_id": next(self.bene_ids),
                "_has:Coverage.extension": f"https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract['id']}",
                "_has:Coverage.rfrncyr": f"https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract['year']}",
                "_count": "25",
            },
            name="/v1/fhir/Patient search by coverage contract (all pages)",
        )

    @tag("patient", "patient_test_mbi")
    @task
    def patient_test_mbi(self) -> None:
        """Patient search by ID, include MBI, include Address."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/Patient/_search",
            body={
                "_id": next(self.bene_ids),
                "_IncludeIdentifiers": "mbi",
            },
            name="/v1/fhir/Patient/_search search by id / mbi",
        )

    @tag("patient", "patient_test_id_include_mbi_include_address")
    @task
    def patient_test_id_include_mbi_include_address(self) -> None:
        """Patient search by ID, include MBI, include Address."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/Patient/_search",
            body={
                "_id": next(self.bene_ids),
                "_IncludeIdentifiers": "mbi",
                "_IncludeTaxNumbers": "true",
            },
            name="/v1/fhir/Patient/_search search by id / includeTaxNumbers / mbi",
        )

    @tag("patient", "patient_test_id")
    @task
    def patient_test_id(self) -> None:
        """Patient search by ID."""
        self.run_task_by_parameters(
            base_path="/v1/fhir/Patient/_search",
            body={
                "_id": next(self.bene_ids),
            },
            name="/v1/fhir/Patient/_search search by id",
        )
