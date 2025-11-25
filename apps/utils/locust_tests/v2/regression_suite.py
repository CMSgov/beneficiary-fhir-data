"""Regression test suite for V2 BFD Server endpoints."""

import itertools
from collections.abc import Collection
from typing import Any, ClassVar

from common import data, db
from common.bfd_user_base import BFDUserBase, set_comparisons_metadata_path
from common.locust_utils import is_distributed, is_locust_master
from common.task_utils import params_to_str
from common.url_path import create_url_path
from common.user_init_aware_load_shape import UserInitAwareLoadShape
from locust import events, tag, task
from locust.env import Environment

master_bene_ids: Collection[str] = []
master_contract_data: Collection[dict[str, str]] = []
master_hashed_mbis: Collection[str] = []
master_pac_hashed_mbis: Collection[str] = []


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

    global master_pac_hashed_mbis  # noqa: PLW0603
    master_pac_hashed_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_regression_pac_hashed_mbis,
        data_type_name="pac_hashed_mbis",
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
    PAC_SERVICE_DATE: ClassVar = {"service-date": "gt2020-01-05"}
    PAC_LAST_UPDATED: ClassVar = {"_lastUpdated": "gt2020-05-05"}
    PAC_SERVICE_DATE_LAST_UPDATED = PAC_SERVICE_DATE | PAC_LAST_UPDATED
    PAC_EXCLUDE_SAMHSA: ClassVar = {"excludeSAMHSA": "true"}

    def __init__(self, *args: tuple, **kwargs: dict[str, Any]) -> None:
        super().__init__(*args, **kwargs)
        self.bene_ids = itertools.cycle(list(master_bene_ids))
        self.contract_data = itertools.cycle(list(master_contract_data))
        self.hashed_mbis = itertools.cycle(list(master_hashed_mbis))
        self.pac_hashed_mbis = itertools.cycle(list(master_pac_hashed_mbis))

    @tag("coverage", "coverage_test_id_count")
    @task
    def coverage_test_id_count(self) -> None:
        """Coverage search by ID, Paginated."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={"beneficiary": next(self.bene_ids), "_count": "10"},
            name="/v2/fhir/Coverage search by id / count=10",
        )

    @tag("coverage", "coverage_test_id")
    @task
    def coverage_test_id(self) -> None:
        """Coverage search by ID."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={
                "beneficiary": next(self.bene_ids),
            },
            name="/v2/fhir/Coverage search by id",
        )

    @tag("eob", "eob_test_id_count")
    @task
    def eob_test_id_count(self) -> None:
        """Explanation of Benefit search by ID, Paginated."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_ids),
                "_count": "10",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / count=10",
        )

    @tag("eob", "eob_test_id_include_tax_number")
    @task
    def eob_test_id_include_tax_number(self) -> None:
        """Explanation of Benefit search by ID, Include Tax Numbers."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_ids),
                "_IncludeTaxNumbers": "true",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / includeTaxNumbers",
        )

    @tag("eob", "eob_test_id")
    @task
    def eob_test_id(self) -> None:
        """Explanation of Benefit search by ID."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={"patient": next(self.bene_ids), "_format": "application/fhir+json"},
            name="/v2/fhir/ExplanationOfBenefit search by id",
        )

    @tag("patient", "patient_test_coverage_contract")
    @task
    def patient_test_coverage_contract(self) -> None:
        """Patient search by Coverage Contract, paginated."""

        def make_url() -> str:
            contract = next(self.contract_data)
            return create_url_path(
                "/v2/fhir/Patient",
                {
                    "_has:Coverage.extension": f"https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract['id']}",
                    "_has:Coverage.rfrncyr": f"https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract['year']}",
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.run_task(
            name="/v2/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient", "patient_test_hashed_mbi")
    @task
    def patient_test_hashed_mbi(self) -> None:
        """Patient search by hashed MBI, include identifiers."""

        def make_url() -> str:
            return create_url_path(
                "/v2/fhir/Patient/",
                {
                    "identifier": f"https://bluebutton.cms.gov/resources/identifier/mbi-hash|{next(self.hashed_mbis)}",
                    "_IncludeIdentifiers": "mbi",
                },
            )

        self.run_task(
            name="/v2/fhir/Patient search by hashed mbi / includeIdentifiers = mbi",
            url_callback=make_url,
        )

    @tag("patient", "patient_test_id_include_mbi")
    @task
    def patient_test_id_include_mbi(self) -> None:
        """Patient search by ID, include MBI."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": next(self.bene_ids),
                "_format": "application/fhir+json",
                "_IncludeIdentifiers": "mbi",
            },
            name="/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi",
        )

    @tag("patient", "patient_test_id")
    @task
    def patient_test_id(self) -> None:
        """Patient search by ID."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": next(self.bene_ids),
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/Patient search by id",
        )

    @tag("claim")
    @task
    def claim(self) -> None:
        """Get single Claim."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Claim",
            params={"mbi": next(self.pac_hashed_mbis)},
            name="/v2/fhir/claim search by mbi hash",
        )

    @tag("claim", "claim_with_service_date")
    @task
    def claim_with_service_date(self) -> None:
        """Get single Claim with service date."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Claim",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_SERVICE_DATE,
            name=f"/v2/fhir/claim search by mbi hash / {params_to_str(self.PAC_SERVICE_DATE)}",
        )

    @tag("claim", "claim_with_last_updated")
    @task
    def claim_with_last_updated(self) -> None:
        """Get single Claim with last updated."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Claim",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_LAST_UPDATED,
            name=f"/v2/fhir/claim search by mbi hash / {params_to_str(self.PAC_LAST_UPDATED)}",
        )

    @tag("claim", "claim_with_service_date_and_last_updated")
    @task
    def claim_with_service_date_and_last_updated(self) -> None:
        """Get single Claim with last updated and service date."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Claim",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_SERVICE_DATE_LAST_UPDATED,
            name=(
                "/v2/fhir/claim search by mbi hash /"
                f" {params_to_str(self.PAC_SERVICE_DATE_LAST_UPDATED)}"
            ),
        )

    @tag("claim", "claim_with_exclude_samhsa")
    @task
    def claim_with_exclude_samhsa(self) -> None:
        """Get a single Claim specifying to exclude SAMHSA."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/Claim",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_EXCLUDE_SAMHSA,
            name=f"/v2/fhir/claim search by mbi hash / {params_to_str(self.PAC_EXCLUDE_SAMHSA)}",
        )

    @tag("claim_response")
    @task
    def claim_response(self) -> None:
        """Get single ClaimResponse."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ClaimResponse",
            params={"mbi": next(self.pac_hashed_mbis)},
            name="/v2/fhir/claimResponse search by mbi hash",
        )

    @tag("claim_response", "claim_response_with_service_date")
    @task
    def claim_response_with_service_date(self) -> None:
        """Get single ClaimResponse with service date."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ClaimResponse",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_SERVICE_DATE,
            name=(
                "/v2/fhir/claimResponse search by mbi hash /"
                f" {params_to_str(self.PAC_SERVICE_DATE)}"
            ),
        )

    @tag("claim_response", "claim_response_with_last_updated")
    @task
    def claim_response_with_last_updated(self) -> None:
        """Get single ClaimResponse with last updated."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ClaimResponse",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_LAST_UPDATED,
            name=(
                "/v2/fhir/claimResponse search by mbi hash /"
                f" {params_to_str(self.PAC_LAST_UPDATED)}"
            ),
        )

    @tag("claim_response", "claim_response_with_service_date_and_last_updated")
    @task
    def claim_response_with_service_date_and_last_updated(self) -> None:
        """Get single ClaimResponse with last updated and service date."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ClaimResponse",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_SERVICE_DATE_LAST_UPDATED,
            name=(
                "/v2/fhir/claimResponse search by mbi hash /"
                f" {params_to_str(self.PAC_SERVICE_DATE_LAST_UPDATED)}"
            ),
        )

    @tag("claim", "claim_response_with_exclude_samhsa")
    @task
    def claim_response_with_exclude_samhsa(self) -> None:
        """Get a single ClaimResponse specifying to exclude SAMHSA."""
        self.run_task_by_parameters(
            base_path="/v2/fhir/ClaimResponse",
            params={"mbi": next(self.pac_hashed_mbis)} | self.PAC_EXCLUDE_SAMHSA,
            name=(
                "/v2/fhir/claimResponse search by mbi hash /"
                f" {params_to_str(self.PAC_EXCLUDE_SAMHSA)}"
            ),
        )
