"""High Volume Load test suite for BFD Server endpoints."""

import inspect
import random
import sys
from collections.abc import Callable, Collection
from typing import (
    Any,
    Protocol,
    TypeVar,
)

from common import data, db
from common.bfd_user_base import BFDUserBase
from common.locust_utils import is_distributed, is_locust_master
from common.url_path import create_url_path
from common.user_init_aware_load_shape import UserInitAwareLoadShape
from locust import TaskSet, User, events, tag, task
from locust.env import Environment

TaskT = TypeVar("TaskT", Callable[..., None], type["TaskSet"])
MASTER_BENE_IDS: Collection[str] = []
MASTER_CONTRACT_DATA: Collection[dict[str, str]] = []
MASTER_HASHED_MBIS: Collection[str] = []
TAGS: set[str] = set()
EXCLUDE_TAGS: set[str] = set()


@events.test_start.add_listener
def _(environment: Environment, **kwargs: dict[str, Any]) -> None:
    if (
        is_distributed(environment) and is_locust_master(environment)
    ) or not environment.parsed_options:
        return

    # See https://docs.locust.io/en/stable/extending-locust.html#test-data-management
    # for Locust's documentation on the test data management pattern used here
    global MASTER_BENE_IDS  # noqa: PLW0603
    MASTER_BENE_IDS = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_bene_ids,
        use_table_sample=True,
        data_type_name="bene_ids",
    )

    global TAGS  # noqa: PLW0603
    TAGS = (
        set(environment.parsed_options.locust_tags.split())
        if hasattr(environment.parsed_options, "locust_tags")
        else set()
    )

    global EXCLUDE_TAGS  # noqa: PLW0603
    EXCLUDE_TAGS = (
        set(environment.parsed_options.locust_exclude_tags.split())
        if hasattr(environment.parsed_options, "locust_exclude_tags")
        else set()
    )

    global MASTER_CONTRACT_DATA  # noqa: PLW0603
    MASTER_CONTRACT_DATA = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_contract_ids,
        use_table_sample=True,
        data_type_name="contract_data",
    )

    global MASTER_HASHED_MBIS  # noqa: PLW0603
    MASTER_HASHED_MBIS = data.load_from_parsed_opts(
        environment.parsed_options,
        db.get_hashed_mbis,
        use_table_sample=True,
        data_type_name="hashed_mbis",
    )


class TestLoadShape(UserInitAwareLoadShape):
    pass


class TaskHolder(Protocol[TaskT]):
    tasks: list[TaskT]


class HighVolumeTaskSet(TaskSet):
    @property
    def user(self) -> "HighVolumeUser":
        # This forces the type of self.user for all derived TaskSets to narrow to HighVolumeUser,
        # thus giving correct type hinting for all of HighVolumeUser's properties.
        return self._high_volume_user

    def __init__(self, parent: User) -> None:
        if not isinstance(parent, HighVolumeUser):
            raise ValueError(
                f"User is {type(self.user).__name__}; expected {type(HighVolumeUser).__name__}"
            )
        self._high_volume_user: HighVolumeUser = parent
        super().__init__(parent)


EOB_TAG = "eob"


@tag(EOB_TAG)
@task
class EobTaskSet(HighVolumeTaskSet):
    @tag("eob_test_id_count_type_pde_v1", "v1")
    @task
    def eob_test_id_count_type_pde_v1(self) -> None:
        """Explanation of Benefit search by ID, type PDE, paginated."""
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

    @tag("eob_test_id_last_updated_count_v1", "v1")
    @task
    def eob_test_id_last_updated_count_v1(self) -> None:
        """Explanation of Benefit search by ID, last updated, paginated."""
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

    @tag("eob_test_id_include_tax_number_last_updated_v1", "v1")
    @task
    def eob_test_id_include_tax_number_last_updated_v1(self) -> None:
        """Explanation of Benefit search by ID, Last Updated, Include Tax Numbers."""
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

    @tag("eob_test_id_last_updated_v1", "v1")
    @task
    def eob_test_id_last_updated_v1(self) -> None:
        """Explanation of Benefit search by ID, Last Updated."""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "json",
                "_lastUpdated": f"gt{self.user.last_updated}",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id / lastUpdated",
        )

    @tag("eob_test_id_v1", "v1")
    @task
    def eob_test_id_v1(self) -> None:
        """Explanation of Benefit search by ID."""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "application/fhir+json",
            },
            name="/v1/fhir/ExplanationOfBenefit search by id",
        )

    @tag("eob_test_id", "v2")
    @task
    def eob_test_id(self) -> None:
        """Explanation of Benefit search by ID."""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id",
        )

    @tag("eob_test_id_count", "v2")
    @task
    def eob_test_id_count(self) -> None:
        """Explanation of Benefit search by ID, Paginated."""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/ExplanationOfBenefit",
            params={
                "patient": self.user.bene_ids.pop(),
                "_count": "10",
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/ExplanationOfBenefit search by id / count=10",
        )

    @tag("eob_test_id_include_tax_number_last_updated", "v2")
    @task
    def eob_test_id_include_tax_number_last_updated(self) -> None:
        """Explanation of Benefit search by ID, Last Updated, Include Tax Numbers."""
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


COVERAGE_TAG = "coverage"


@tag(COVERAGE_TAG)
@task
class CoverageTaskSet(HighVolumeTaskSet):
    @tag("coverage_test_id_count_v1", "v1")
    @task
    def coverage_test_id_count_v1(self) -> None:
        """Coverage search by ID, Paginated."""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/Coverage",
            params={"beneficiary": self.user.bene_ids.pop(), "_count": "10"},
            name="/v1/fhir/Coverage search by id / count=10",
        )

    @tag("coverage_test_id_last_updated_v1", "v1")
    @task
    def coverage_test_id_last_updated_v1(self) -> None:
        """Coverage search by ID, Last Updated."""
        self.user.run_task_by_parameters(
            base_path="/v1/fhir/Coverage",
            params={
                "_lastUpdated": f"gt{self.user.last_updated}",
                "beneficiary": self.user.bene_ids.pop(),
            },
            name="/v1/fhir/Coverage search by id / lastUpdated (2 weeks)",
        )

    @tag("coverage_test_id", "v2")
    @task
    def coverage_test_id(self) -> None:
        """Coverage search by ID."""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={
                "beneficiary": self.user.bene_ids.pop(),
            },
            name="/v2/fhir/Coverage search by id",
        )

    @tag("coverage_test_id_count", "v2")
    @task
    def coverage_test_id_count(self) -> None:
        """Coverage search by ID, Paginated."""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={"beneficiary": self.user.bene_ids.pop(), "_count": "10"},
            name="/v2/fhir/Coverage search by id / count=10",
        )

    @tag("coverage_test_id_last_updated", "v2")
    @task
    def coverage_test_id_last_updated(self) -> None:
        """Coverage search by ID, Last Updated."""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Coverage",
            params={
                "_lastUpdated": f"gt{self.user.last_updated}",
                "beneficiary": self.user.bene_ids.pop(),
            },
            name="/v2/fhir/Coverage search by id / lastUpdated (2 weeks)",
        )


PATIENT_TAG = "patient"


@tag(PATIENT_TAG)
@task
class PatientTaskSet(HighVolumeTaskSet):
    @tag("patient_test_coverage_contract_v1", "v1")
    @task
    def patient_test_coverage_contract_v1(self) -> None:
        """Patient search by coverage contract (all pages)."""

        def make_url():
            contract = self.user.contract_data.pop()
            return create_url_path(
                "/v1/fhir/Patient",
                {
                    "_has:Coverage.extension": f"https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract['id']}",
                    "_has:Coverage.rfrncyr": f"https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract['year']}",
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.user.run_task(
            name="/v1/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient_test_hashed_mbi_v1", "v1")
    @task
    def patient_test_hashed_mbi_v1(self) -> None:
        """Patient search by ID, Last Updated, include MBI, include Address."""

        def make_url() -> str:
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

    @tag("patient_test_id_last_updated_include_mbi_include_address_v1", "v1")
    @task
    def patient_test_id_last_updated_include_mbi_include_address_v1(self) -> None:
        """Patient search by ID, Last Updated, include MBI, include Address."""
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

    @tag("patient_test_id_v1", "v1")
    @task
    def patient_test_id_v1(self) -> None:
        """Patient search by ID."""

        def make_url() -> str:
            return create_url_path(f"/v1/fhir/Patient/{self.user.bene_ids.pop()}", {})

        self.user.run_task(name="/v1/fhir/Patient/id", url_callback=make_url)

    @tag("patient_test_coverage_contract", "v2")
    @task
    def patient_test_coverage_contract(self) -> None:
        """Patient search by Coverage Contract, paginated."""

        def make_url() -> str:
            contract = self.user.contract_data.pop()
            return create_url_path(
                "/v2/fhir/Patient",
                {
                    "_has:Coverage.extension": f"https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract['id']}",
                    "_has:Coverage.rfrncyr": f"https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract['year']}",
                    "_count": 25,
                    "_format": "json",
                },
            )

        self.user.run_task(
            name="/v2/fhir/Patient search by coverage contract (all pages)",
            headers={"IncludeIdentifiers": "mbi"},
            url_callback=make_url,
        )

    @tag("patient_test_hashed_mbi", "v2")
    @task
    def patient_test_hashed_mbi(self) -> None:
        """Patient search by hashed MBI, include identifiers."""

        def make_url() -> str:
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

    @tag("patient_test_id_include_mbi_last_updated", "v2")
    @task
    def patient_test_id_include_mbi_last_updated(self) -> None:
        """Patient search by ID with last updated, include MBI."""
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

    @tag("patient_test_id", "v2")
    @task
    def patient_test_id(self) -> None:
        """Patient search by ID."""
        self.user.run_task_by_parameters(
            base_path="/v2/fhir/Patient",
            params={
                "_id": self.user.bene_ids.pop(),
                "_format": "application/fhir+json",
            },
            name="/v2/fhir/Patient search by id",
        )


class HighVolumeUser(BFDUserBase):
    """High volume load test suite for V2 BFD Server endpoints.

    The tests in this suite generate a large volume of traffic to endpoints that are hit most
    frequently during a peak load event.
    """

    # Do we terminate the tests when a test runs out of data and paginated URLs?
    END_ON_NO_DATA = False

    @staticmethod
    def filter_tasks_by_tags(
        task_holder: type[TaskHolder],
        tags: set[str],
        exclude_tags: set[str],
        checked: dict[TaskT, bool] | None = None,
    ) -> list[Any]:
        """
        Recursively remove any tasks/TaskSets from a TaskSet/User that
        shouldn't be executed according to the tag options
        :param task_holder: the TaskSet or User with tasks
        :param tags: The set of tasks by @tag to include in the final list
        :param exclude_tags: The set of tasks by @tag to exclude from the final list
        :param checked: The running score of tasks which have or have not been processed
        :return: A list of filtered tasks to execute.
        """
        filtered_tasks = []
        if checked is None:
            checked = {}
        for cur_task in task_holder.tasks:  # type: ignore
            if cur_task in checked:
                if checked[cur_task]:
                    filtered_tasks.append(cur_task)
                continue
            passing = True
            if hasattr(cur_task, "tasks"):
                HighVolumeUser.filter_tasks_by_tags(cur_task, tags, exclude_tags, checked)
                passing = len(cur_task.tasks) > 0
            else:
                if len(tags) > 0:
                    passing &= (
                        "locust_tag_set" in dir(cur_task)
                        and len(cur_task.locust_tag_set.intersection(tags)) > 0
                    )
                if len(exclude_tags) > 0:
                    passing &= (
                        "locust_tag_set" not in dir(cur_task)
                        or len(cur_task.locust_tag_set.intersection(exclude_tags)) == 0
                    )

            if passing:
                filtered_tasks.append(cur_task)
            checked[cur_task] = passing

        return filtered_tasks

    @staticmethod
    def get_tasks(tags: set[str], exclude_tags: set[str]) -> list[Any]:
        """
        Return the list of runnable tasks for the given user, filterable by a list of tags
        or exclude_tags.
        Returns all runnable tasks if neither tags or exclude_tags contain items.

        :param tags: The list of tags to filter tasks by
        :param exclude_tags: This list of tags to exclude tasks by
        :return: A list of tasks to run
        """
        # Filter out the class members without a tasks attribute
        class_members = inspect.getmembers(sys.modules[__name__], inspect.isclass)
        potential_tasks = list(
            filter(
                lambda potential_task: hasattr(potential_task[1], "tasks")
                and issubclass(potential_task[1], HighVolumeTaskSet),
                class_members,
            )
        )

        # Filter each task holder's tasks by the given tags and exclude_tags
        tasks = []
        for task_holder in list(map(lambda task_set: task_set[1], potential_tasks)):
            tasks.extend(HighVolumeUser.filter_tasks_by_tags(task_holder, tags, exclude_tags))
        return tasks

    def get_runnable_tasks(self, tags: set[str], exclude_tags: set[str]) -> list[Any]:
        """
        Return the list of runnable tasks.

        Helper method to be called via the HighVolumerUser constructor.
        Required due to python <= 3.9 not allowing direct calls to static methods.

        :param tags: The list of tags to filter tasks by
        :param exclude_tags: This list of tags to exclude tasks by
        :return: A list of tasks to run
        """
        return HighVolumeUser.get_tasks(tags, exclude_tags)

    def __init__(self, *args: tuple, **kwargs: dict[str, Any]) -> None:
        super().__init__(*args, **kwargs)
        self.bene_ids = list(MASTER_BENE_IDS)
        self.contract_data = list(MASTER_CONTRACT_DATA)
        self.hashed_mbis = list(MASTER_HASHED_MBIS)

        # Shuffle the data to ensure each User isn't making requests with the same data in the same
        # order
        random.shuffle(self.bene_ids)
        random.shuffle(self.contract_data)
        random.shuffle(self.hashed_mbis)

        # As of 01/20/2023 there is an unresolved locust issue [1] with the --tags/--exclude-tags
        # command line options.
        # Therefore, we have implemented custom arguments (--locust-tags/--locust-exclude-tags) to
        # programmatically filter tasks by the given @tag(s) at runtime.
        # [1] https://github.com/locustio/locust/issues/1689

        # Looking at this, it's not obvious why we're dynamically generating a _class_ (not an
        # instance) with its "tasks" _attribute_ (not field) set to the filtered list of tasks we
        # want to run. Why not just pass the list of tasks directly, as Locust supports setting
        # "tasks" to a List of Callables? A few reasons:
        # 1. Locust does not support passing an _instance_ of a TaskSet as "tasks", as Locust
        #    expects to instantiate the TaskSet itself when the User is ran (passing the User to the
        #    TaskSet's __init__, as "parent"). If we want to pass a TaskSet where its "tasks" are a
        #    _subset_ of all the tasks on the TaskSet, we need to generate a class with a "tasks"
        #    attribute explicitly set. Otherwise, Locust will take all Callables tagged with "task"
        #    from the TaskSet
        # 2. If "tasks" is a list of function refs/Callables, Locust will pass the _User_ class as
        #    the first arg to each task Callable. Each Callable, in this context, is a method of a
        #    HighVolumeTaskSet, and so each Callable expects "self" to be an instance of said
        #    HighVolumeTaskSet. However, in this case, "self" is actually an instance of
        #    HighVolumeUser, and so this contract is broken and the benefit provided by static type
        #    analysis is invalidated. Passing a dynamically generated Class deriving from
        #    HighVolumeTaskSet with its "tasks" attribute set to the list of task function
        #    references solves this "self" invalidation by ensuring "self" is _always_ an instance
        #    of HighVolumeTaskSet in this context when Locust executes a given task Callable
        self.tasks = [
            type(
                "HVUFilteredTaskSet",
                (HighVolumeTaskSet,),
                {"tasks": self.get_runnable_tasks(TAGS, EXCLUDE_TAGS)},
            )
        ]

        # Override the value for last_updated with a static value
        self.last_updated = "2022-06-29"
