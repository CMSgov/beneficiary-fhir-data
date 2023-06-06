"""High Volume Load test suite for BFD Server endpoints."""

from random import shuffle
from typing import Dict, List

from locust import TaskSet, events, tag, task
from locust.env import Environment

from common import data, db, validation
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from common.url_path import create_url_path
from common.user_init_aware_load_shape import UserInitAwareLoadShape

MASTER_BENE_IDS: List[str] = []
MASTER_CONTRACT_DATA: List[Dict[str, str]] = []
MASTER_HASHED_MBIS: List[str] = []


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
    global MASTER_BENE_IDS
    MASTER_BENE_IDS = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_bene_ids,
        use_table_sample=True,
        data_type_name="bene_ids",
    )

    global MASTER_CONTRACT_DATA
    MASTER_CONTRACT_DATA = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_contract_ids,
        use_table_sample=True,
        data_type_name="contract_data",
    )

    global MASTER_HASHED_MBIS
    MASTER_HASHED_MBIS = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_hashed_mbis,
        use_table_sample=True,
        data_type_name="hashed_mbis",
    )

class TestLoadShape(UserInitAwareLoadShape):
    pass

@tag('MyTaskSet')
class MyTaskSet(TaskSet):
    @tag("coverage", "coverage_test_id_count", "v2")
    @task
    def coverage_test_id_count(self):
        """Coverage search by ID, Paginated"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={"beneficiary": self.user.bene_ids.pop(), "_count": "10"},
            name="/v2/fhir/Coverage search by id / count=10",
        )

    @tag("coverage", "coverage_test_id_last_updated", "v2")
    @task
    def coverage_test_id_last_updated(self):
        """Coverage search by ID, Last Updated"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={
                "_lastUpdated": f"gt{self.user.last_updated}",
                "beneficiary": self.user.bene_ids.pop(),
            },
            name="/v2/fhir/Coverage search by id / lastUpdated (2 weeks)",
        )

    @tag("coverage", "coverage_test_id", "v2")
    @task
    def coverage_test_id(self):
        """Coverage search by ID"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={
                "beneficiary": self.user.bene_ids.pop(),
            },
            name="/v2/fhir/Coverage search by id",
        )

    @tag("eob", "eob_test_id_count", "v2")
    @task
    def eob_test_id_count(self):
        """Explanation of Benefit search by ID, Paginated"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_count": "10",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / count=10",
        )

    @tag("eob", "eob_test_id_include_tax_number_last_updated", "v2")
    @task
    def eob_test_id_include_tax_number_last_updated(self):
        """Explanation of Benefit search by ID, Last Updated, Include Tax Numbers"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "_lastUpdated": f"gt{self.user.last_updated}",
                "patient": self.user.bene_ids.pop(),
                "_IncludeTaxNumbers": "true",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers",
        )

    @tag("eob", "eob_test_id", "v2")
    @task
    def eob_test_id(self):
        """Explanation of Benefit search by ID"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={"patient": self.user.bene_ids.pop(), "_format": "application/fhir+json"},
            name="/v2/fhir/ExplanationOfBenefit search by id",
        )

    @tag("patient", "patient_test_coverage_contract", "v2")
    @task
    def patient_test_coverage_contract(self):
        """Patient search by Coverage Contract, paginated"""

        def make_url():
            contract = self.user.contract_data.pop()
            return create_url_path(
                "/v2/fhir/Patient",
                {
                    "_has:Coverage.extension": f'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract["id"]}',
                    "_has:Coverage.rfrncyr": f'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract["year"]}',
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.user.run_task(
            name="/v2/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient", "patient_test_hashed_mbi", "v2")
    @task
    def patient_test_hashed_mbi(self):
        """Patient search by hashed MBI, include identifiers"""

        def make_url():
            return create_url_path(
                "/v2/fhir/Patient/",
                {
                    "identifier": f"https://bluebutton.cms.gov/resources/identifier/mbi-hash|{self.user.hashed_mbis.pop()}",
                    "_IncludeIdentifiers": "mbi",
                },
            )

        self.user.run_task(
            name="/v2/fhir/Patient search by hashed mbi / includeIdentifiers = mbi",
            url_callback=make_url,
        )

    @tag("patient", "patient_test_id_include_mbi_last_updated", "v2")
    @task
    def patient_test_id_include_mbi_last_updated(self):
        """Patient search by ID with last updated, include MBI"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": self.user.bene_ids.pop(),
                "_format": "application/fhir+json",
                "_IncludeIdentifiers": "mbi",
                "_lastUpdated": f"gt{self.user.last_updated}",
            },
            name="/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / (2 weeks)",
        )

    @tag("patient", "patient_test_id", "v2")
    @task
    def patient_test_id(self):
        """Patient search by ID"""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": self.user.bene_ids.pop(),
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/Patient search by id",
        )

    @tag("coverage", "coverage_test_id_count_v1", "v1")
    @task
    def coverage_test_id_count_v1(self):
        """Coverage search by ID, Paginated"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/Coverage",
            params={"beneficiary": self.user.bene_ids.pop(), "_count": "10"},
            name="/v1/fhir/Coverage search by id / count=10",
        )

    @tag("coverage", "coverage_test_id_last_updated_v1", "v1")
    @task
    def coverage_test_id_last_updated_v1(self):
        """Coverage search by ID, Last Updated"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/Coverage",
            params={
                "_lastUpdated": f"gt{self.user.last_updated}",
                "beneficiary": self.user.bene_ids.pop(),
            },
            name="/v2/fhir/Coverage search by id / lastUpdated (2 weeks)",
        )

    @tag("eob", "eob_test_id_count_type_pde_v1", "v1")
    @task
    def eob_test_id_count_type_pde_v1(self):
        """Explanation of Benefit search by ID, type PDE, paginated"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "json",
                "_count": "50",
                "_types": "PDE",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50",
        )

    @tag("eob", "eob_test_id_last_updated_count_v1", "v1")
    @task
    def eob_test_id_last_updated_count_v1(self):
        """Explanation of Benefit search by ID, last updated, paginated"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "json",
                "_count": "100",
                "_lastUpdated": f"gt{self.user.last_updated}",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / count = 100",
        )

    @tag("eob", "eob_test_id_include_tax_number_last_updated_v1", "v1")
    @task
    def eob_test_id_include_tax_number_last_updated_v1(self):
        """Explanation of Benefit search by ID, Last Updated, Include Tax Numbers"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "json",
                "_lastUpdated": f"gt{self.user.last_updated}",
                "_IncludeTaxNumbers": "true",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers",
        )

    @tag("eob", "eob_test_id_last_updated_v1", "v1")
    @task
    def eob_test_id_last_updated_v1(self):
        """Explanation of Benefit search by ID, Last Updated"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "json",
                "_lastUpdated": f"gt{self.user.last_updated}",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / lastUpdated",
        )

    @tag("eob", "eob_test_id_v1", "v1")
    @task
    def eob_test_id_v1(self):
        """Explanation of Benefit search by ID"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={"patient": self.user.bene_ids.pop(), "_format": "application/fhir+json"},
            name="/v1/fhir/ExplanationOfBenefit search by id",
        )

    @tag("patient", "patient_test_coverage_contract_v1", "v1")
    @task
    def patient_test_coverage_contract_v1(self):
        """Patient search by coverage contract (all pages)"""

        def make_url():
            contract = self.user.contract_data.pop()
            return create_url_path(
                "/v1/fhir/Patient",
                {
                    "_has:Coverage.extension": f'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract["id"]}',
                    "_has:Coverage.rfrncyr": f'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract["year"]}',
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.user.run_task(
            name="/v1/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient", "patient_test_hashed_mbi_v1", "v1")
    @task
    def patient_test_hashed_mbi_v1(self):
        """Patient search by ID, Last Updated, include MBI, include Address"""

        def make_url():
            return create_url_path(
                "/v1/fhir/Patient/",
                {
                    "identifier": f"https://bluebutton.cms.gov/resources/identifier/mbi-hash|{self.user.hashed_mbis.pop()}",
                    "_IncludeIdentifiers": "mbi",
                },
            )

        self.user.run_task(
            name="/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi",
            url_callback=make_url,
        )

    @tag("patient", "patient_test_id_last_updated_include_mbi_include_address_v1", "v1")
    @task
    def patient_test_id_last_updated_include_mbi_include_address_v1(self):
        """Patient search by ID, Last Updated, include MBI, include Address"""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/Patient",
            params={
                "_id": self.user.bene_ids.pop(),
                "_lastUpdated": f"gt{self.user.last_updated}",
                "_IncludeIdentifiers": "mbi",
                "_IncludeTaxNumbers": "true",
            },
            name="/v1/fhir/Patient/id search by id / (2 weeks) / includeTaxNumbers / mbi",
        )

    @tag("patient", "patient_test_id_v1", "v1")
    @task
    def patient_test_id_v1(self):
        """Patient search by ID"""

        def make_url():
            return create_url_path(f"/v1/fhir/Patient/{self.user.bene_ids.pop()}", {})

        self.user.run_task(name="/v1/fhir/Patient/id", url_callback=make_url)

class HighVolumeUser(BFDUserBase):
    """High volume load test suite for V2 BFD Server endpoints.

    The tests in this suite generate a large volume of traffic to endpoints that are hit most
    frequently during a peak load event.
    """

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False

    tasks = [MyTaskSet]

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.bene_ids = MASTER_BENE_IDS.copy()
        self.contract_data = MASTER_CONTRACT_DATA.copy()
        self.hashed_mbis = MASTER_HASHED_MBIS.copy()

        # Shuffle all the data around so that each HighVolumeUser is _probably_
        # not requesting the same data.
        shuffle(self.bene_ids)
        shuffle(self.contract_data)
        shuffle(self.hashed_mbis)

        # Override the value for last_updated with a static value
        self.last_updated = "2022-06-29"
