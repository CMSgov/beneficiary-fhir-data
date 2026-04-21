import itertools
import logging
from collections.abc import Collection
from typing import Any, ClassVar

from common import data, db_idr
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from locust import events, tag, task
from locust.env import Environment

master_bene_sks: Collection[str] = []
master_bene_sks_part_a: Collection[str] = []
master_bene_sks_part_b: Collection[str] = []
master_bene_mbis: Collection[str] = []
master_claim_ids: Collection[str] = []


@events.test_start.add_listener
def _(environment: Environment, **kwargs: dict[str, Any]) -> None:
    if (
        is_distributed(environment) and is_locust_master(environment)
    ) or not environment.parsed_options:
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global master_bene_sks  # noqa: PLW0603
    master_bene_sks = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_bene_sks,
        data_type_name="bene_ids",
    )

    global master_bene_sks_part_a  # noqa: PLW0603
    master_bene_sks_part_a = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_current_part_a_bene_sks,
        data_type_name="bene_ids_part_a",
    )

    global master_bene_sks_part_b  # noqa: PLW0603
    master_bene_sks_part_b = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_current_part_b_bene_sks,
        data_type_name="bene_ids_part_b",
    )

    global master_bene_mbis  # noqa: PLW0603
    master_bene_mbis = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_bene_mbis,
        data_type_name="bene_mbis",
    )

    global master_claim_ids  # noqa: PLW0603
    master_claim_ids = data.load_from_parsed_opts(
        environment.parsed_options,
        db_idr.get_regression_claim_ids,
        data_type_name="claim_ids",
    )


logger = logging.getLogger(__name__)


class RegressionV3User(BFDUserBase):
    """Regression test suite for V3 BFD Server endpoints.

    The tests within this Locust test suite hit various endpoints that were determined to be
    representative of typical V3 endpoint loads. When running this test suite, all tests in this
    suite will be run in parallel, with equal weighting being applied to each.
    """

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False
    LAST_UPDATED_FILTER: ClassVar = {"_lastUpdated": "gt2020-05-05"}
    SERVICE_DATE_RANGE: ClassVar = {"service-date": "ge2020-01-01"}

    def __init__(self, *args: tuple, **kwargs: dict[str, Any]) -> None:
        super().__init__(*args, **kwargs)
        logger.info("Loaded %s bene_sks.", len(master_bene_sks))
        logger.info("Loaded %s bene_sks_part_a.", len(master_bene_sks_part_a))
        logger.info("Loaded %s bene_sks_part_b.", len(master_bene_sks_part_b))
        logger.info("Loaded %s bene_mbis.", len(master_bene_mbis))
        logger.info("Loaded %s claim_ids.", len(master_claim_ids))

        self.bene_sks = itertools.cycle(list(master_bene_sks))
        self.bene_sks_part_a = itertools.cycle(list(master_bene_sks_part_a))
        self.bene_sks_part_b = itertools.cycle(list(master_bene_sks_part_b))
        self.bene_mbis = itertools.cycle(list(master_bene_mbis))
        self.claim_ids = itertools.cycle(list(master_claim_ids))

    @tag("patient", "patient_read_id")
    @task
    def patient_read_id(self) -> None:
        """Patient search by ID."""
        self.run_task_by_parameters(
            base_path=f"/v3/fhir/Patient/{next(self.bene_sks)}",
            params={
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Patient read",
        )

    @tag("patient", "patient_search_id")
    @task
    def patient_search_id(self) -> None:
        """Patient search by ID."""
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
    def patient_search_id_post(self) -> None:
        """Patient search by ID."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient/_search",
            body={
                "_id": next(self.bene_sks),
            },
            name="/v3/fhir/Patient search by id (POST)",
        )

    @tag("patient", "patient_search_mbi")
    @task
    def patient_search_mbi(self) -> None:
        """Patient search by ID."""
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
    def patient_search_mbi_last_updated(self) -> None:
        """Patient search by ID."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient",
            params={
                "identifier": f"http://hl7.org/fhir/sid/us-mbi|{next(self.bene_mbis)}",
                "_format": "application/fhir+json",
            }
            | self.LAST_UPDATED_FILTER,
            name="/v3/fhir/Patient search by mbi with last updated",
        )

    @tag("patient", "patient_search_mbi_post")
    @task
    def patient_search_mbi_post(self) -> None:
        """Patient search by ID."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Patient/_search",
            body={
                "identifier": f"http://hl7.org/fhir/sid/us-mbi|{next(self.bene_mbis)}",
            },
            name="/v3/fhir/Patient search by mbi (POST)",
        )

    # ==========================================================
    # Coverage Endpoint Tests
    # ==========================================================

    @tag("coverage", "coverage_read_id_part_a")
    @task
    def coverage_read_id_part_a(self) -> None:
        """Coverage read by composite ID for Part A."""
        coverage_id = f"part-a-{next(self.bene_sks_part_a)}"

        self.run_task_by_parameters(
            base_path=f"/v3/fhir/Coverage/{coverage_id}",
            params={
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Coverage read part A",
        )

    @tag("coverage", "coverage_read_id_part_b")
    @task
    def coverage_read_id_part_b(self) -> None:
        """Coverage read by composite ID for Part B."""
        coverage_id = f"part-b-{next(self.bene_sks_part_b)}"

        self.run_task_by_parameters(
            base_path=f"/v3/fhir/Coverage/{coverage_id}",
            params={
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Coverage read Part B",
        )

    @tag("coverage", "coverage_search_id")
    @task
    def coverage_search_id_part_b(self) -> None:
        """Coverage search by composite ID for Part B."""
        bene_sk = next(self.bene_sks)
        coverage_id = f"part-b-{bene_sk}"
        self.run_task_by_parameters(
            base_path="/v3/fhir/Coverage",
            params={
                "_id": coverage_id,
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Coverage search by id",
        )

    @tag("coverage", "coverage_search_beneficiary")
    @task
    def coverage_search_by_beneficiary(self) -> None:
        """Coverage search by beneficiary ID."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Coverage",
            params={
                "beneficiary": f"Patient/{next(self.bene_sks)}",
                "_format": "application/fhir+json",
            },
            name="/v3/fhir/Coverage search by beneficiary",
        )

    @tag("coverage", "coverage_search_beneficiary_post")
    @task
    def coverage_search_by_beneficiary_post(self) -> None:
        self.run_task_by_parameters(
            base_path="/v3/fhir/Coverage/_search",
            body={
                "beneficiary": f"Patient/{next(self.bene_sks)}",
            },
            name="/v3/fhir/Coverage search by beneficiary (POST)",
        )

    @tag("coverage", "coverage_search_last_updated")
    @task
    def coverage_search_last_updated(self) -> None:
        """Coverage search by beneficiary with _lastUpdated filter."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/Coverage",
            params={
                "beneficiary": f"Patient/{next(self.bene_sks)}",
                "_format": "application/fhir+json",
            }
            | self.LAST_UPDATED_FILTER,
            name="/v3/fhir/Coverage search by beneficiary with last updated",
        )

    #     ==========================================================
    #     ExplanationOfBenefit (EOB) Endpoint Tests
    #     ==========================================================

    @tag("eob", "eob_read")
    @task
    def eob_read(self) -> None:
        """ExplanationOfBenefit read by its unique claim ID."""
        self.run_task_by_parameters(
            base_path=f"/v3/fhir/ExplanationOfBenefit/{next(self.claim_ids)}",
            params={"_format": "application/fhir+json"},
            name="/v3/fhir/ExplanationOfBenefit read",
        )

    @tag("eob", "eob_search_id")
    @task
    def eob_search_id(self) -> None:
        """ExplanationOfBenefit search by its unique claim ID."""
        claim_id = next(self.claim_ids)

        self.run_task_by_parameters(
            base_path="/v3/fhir/ExplanationOfBenefit",
            params={"_id": claim_id, "_format": "application/fhir+json"},
            name="/v3/fhir/ExplanationOfBenefit search by id",
        )

    @tag("eob", "eob_search_patient")
    @task
    def eob_search_patient(self) -> None:
        """ExplanationOfBenefit search by patient ID (bene_sk)."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/ExplanationOfBenefit",
            params={"patient": next(self.bene_sks), "_format": "application/fhir+json"},
            name="/v3/fhir/ExplanationOfBenefit search by patient",
        )

    @tag("eob", "eob_search_service_date")
    @task
    def eob_search_service_date(self) -> None:
        """ExplanationOfBenefit search by patient and service-date."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_sks),
                "_format": "application/fhir+json",
            }
            | self.SERVICE_DATE_RANGE,
            name="/v3/fhir/ExplanationOfBenefit search by service-date",
        )

    @tag("eob", "eob_search_last_updated")
    @task
    def eob_search_last_updated(self) -> None:
        """ExplanationOfBenefit search by patient with _lastUpdated."""
        self.run_task_by_parameters(
            base_path="/v3/fhir/ExplanationOfBenefit",
            params={
                "patient": next(self.bene_sks),
                "_format": "application/fhir+json",
            }
            | self.LAST_UPDATED_FILTER,
            name="/v3/fhir/ExplanationOfBenefit search by patient with last updated",
        )
