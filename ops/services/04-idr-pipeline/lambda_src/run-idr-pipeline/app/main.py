import calendar
import json
import os
import re
from collections.abc import Sequence
from dataclasses import asdict, dataclass
from datetime import UTC, datetime, timedelta
from typing import TYPE_CHECKING, Annotated, Any, cast
from zoneinfo import ZoneInfo

import boto3
from annotated_types import Len
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parser import parse
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from pydantic.fields import Field
from pydantic.main import BaseModel
from pydantic_settings import BaseSettings

if TYPE_CHECKING:
    from mypy_boto3_ecs.type_defs import CapacityProviderStrategyItemTypeDef
else:
    CapacityProviderStrategyItemTypeDef = object

REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")

BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
IDR_TASK_SCHEDULE_NAME_TEMPLATE = "bfd-{env!s}-run-idr-pipeline-at-{unix_timestamp!s}"

logger = Logger()


# BaseSettings, from pydantic-settings, uses Pydantic to validate and define what would otherwise be
# typical environment variable settings for this Lambda. Pydantic auto-validates each field from the
# environment, case-insensitively, using each field's validation alias. For example,
# idr_task_subnet_ids is set by the environment variable IDR_TASK_SUBNET_IDS_JSON, and so on.
class Settings(BaseSettings):
    bfd_environment: str
    ecs_cluster_arn: str
    idr_task_definition_arn: str
    idr_container_name: str
    idr_task_group: str
    idr_task_subnet_ids: Annotated[
        list[str], Field(validation_alias="idr_task_subnet_ids_json"), Len(min_length=1)
    ]
    idr_task_security_group_id: str
    idr_task_tags: Annotated[dict[str, str], Field(validation_alias="idr_task_tags_json")] = {}
    idr_task_environment: Annotated[
        dict[str, str], Field(validation_alias="idr_task_environment_json")
    ] = {}
    idr_task_capacity_provider_strategies: Annotated[
        list[CapacityProviderStrategyModel],
        Field(validation_alias="idr_task_capacity_provider_strategies_json"),
        Len(min_length=1),
    ]
    idr_task_schedules_group: str
    idr_task_scheduler_role_arn: str
    lambda_reschedule_in: timedelta = timedelta(minutes=5)


class CapacityProviderStrategyModel(BaseModel):
    capacity_provider: str = Field(serialization_alias="capacityProvider")
    weight: int
    base: int


class InvokeModel(BaseModel):
    env: dict[str, str] | None = None
    command: str | None = None
    ecs_exec: bool | None = False
    reschedule: bool | None = False


@dataclass(frozen=True, eq=True)
class EnvKeyValue:
    name: str
    value: str


@dataclass(frozen=True, eq=True)
class IdrContainerOverrides:
    command: Sequence[str] | None
    environment: list[EnvKeyValue]

    @classmethod
    def from_invoke(cls, invoke_model: InvokeModel) -> IdrContainerOverrides:
        env_kvs = (
            [EnvKeyValue(name=k, value=v) for k, v in invoke_model.env] if invoke_model.env else []
        )
        command_seq = invoke_model.command.split() if invoke_model.command else None

        return IdrContainerOverrides(command=command_seq, environment=env_kvs)


type LambdaResult = (
    AlreadyRunningResult | AlreadyScheduledResult | RescheduledResult | PipelineStartedResult
)


@dataclass(frozen=True, eq=True)
class AlreadyRunningResult:
    running_task_arns: list[str]


@dataclass(frozen=True, eq=True)
class AlreadyScheduledResult:
    running_task_arns: list[str]
    scheduled_at: datetime
    existing_schedule_name: str


@dataclass(frozen=True, eq=True)
class RescheduledResult:
    running_task_arns: list[str]
    rescheduled_for: datetime
    schedule_name: str


@dataclass(frozen=True, eq=True)
class PipelineStartedResult:
    task_arn: str
    exec_command: str | None = None


def get_at_schedule_datetime(at_expression: str, timezone: str) -> datetime | None:
    matches = re.match(r"at\((.*)\)", at_expression)
    if not matches:
        return None

    return datetime.strptime(f"{matches[1]}", "%Y-%m-%dT%H:%M:%S").astimezone(ZoneInfo(timezone))


def filtered_dict(unfiltered_dict: dict[str, Any | None]) -> dict[str, Any]:
    return {k: v for k, v in unfiltered_dict.items() if v}


def result_handler(event: dict[str, Any], context: LambdaContext) -> LambdaResult:
    # Pylance does not understand that Pydantic BaseSettings classes validate themselves, so it
    # errors thinking that every model field needs to be provided. Hence, type: ignore
    settings = Settings()  # type: ignore
    invoke_model = parse(event=event, model=InvokeModel)

    ecs_client = boto3.client("ecs", config=BOTO_CONFIG)
    tasks = ecs_client.list_tasks(cluster=settings.ecs_cluster_arn)["taskArns"]
    running_idr_tasks = (
        [
            task
            for task in ecs_client.describe_tasks(cluster=settings.ecs_cluster_arn, tasks=tasks)[
                "tasks"
            ]
            if "group" in task
            and task["group"] == settings.idr_task_group
            and "desiredStatus" in task
            and task["desiredStatus"].lower()
            not in ["deactivating", "stopping", "deprovisioning", "stopped", "deleted"]
        ]
        if tasks
        else []
    )
    if len(running_idr_tasks) > 0:
        running_task_arns = [x["taskArn"] for x in running_idr_tasks if "taskArn" in x]
        if not invoke_model.reschedule:
            logger.info(
                "%d IDR Pipeline Task(s) are running; no action needed. Stopping...",
                len(running_idr_tasks),
            )
            return AlreadyRunningResult(running_task_arns=running_task_arns)

        now = datetime.now(UTC)
        reschedule_for = now + settings.lambda_reschedule_in
        logger.info(
            (
                "%d IDR Pipeline Task(s) are running; reschedule specified, will reschedule"
                " Lambda to run again at %s"
            ),
            len(running_idr_tasks),
            reschedule_for.isoformat(),
        )
        scheduler_client = boto3.client("scheduler", config=BOTO_CONFIG)
        schedules = [
            scheduler_client.get_schedule(
                GroupName=settings.idr_task_schedules_group, Name=sched["Name"]
            )
            for sched in scheduler_client.list_schedules(
                GroupName=settings.idr_task_schedules_group
            )["Schedules"]
            if "Name" in sched and "State" in sched and sched["State"] == "ENABLED"
        ]

        if name_time_tupl := next(
            (
                (x["Name"], at_time)
                for x in schedules
                if (
                    at_time := get_at_schedule_datetime(
                        x["ScheduleExpression"], x["ScheduleExpressionTimezone"]
                    )
                )
                and at_time > now
                and at_time <= reschedule_for
            ),
            None,
        ):
            existing_sched, existing_time = name_time_tupl
            logger.info(
                "Schedule %s at %s already exists; nothing to do",
                existing_sched,
                existing_time.isoformat(),
            )
            return AlreadyScheduledResult(
                running_task_arns=running_task_arns,
                scheduled_at=existing_time,
                existing_schedule_name=existing_sched,
            )

        # Delete old schedules by removing those with an "at()" expression that has since
        # passed
        try:
            all_schedules_to_delete = [
                (sched["Name"], at_time)
                for sched in schedules
                if (
                    at_time := get_at_schedule_datetime(
                        sched["ScheduleExpression"], sched["ScheduleExpressionTimezone"]
                    )
                )
                and at_time <= now
            ]
            for sched_name, sched_time in all_schedules_to_delete:
                logger.info(
                    "Deleting schedule %s as its invocation time %s has passed...",
                    sched_name,
                    sched_time,
                )
                scheduler_client.delete_schedule(
                    GroupName=settings.idr_task_schedules_group, Name=sched_name
                )
                logger.info("Schedule %s deleted successfully", sched_name)
        except scheduler_client.exceptions.ClientError:
            logger.exception("An error occurred when trying to delete old schedules; continuing")

        # Add the new schedule at reschedule_for
        schedule_name = IDR_TASK_SCHEDULE_NAME_TEMPLATE.format(
            env=settings.bfd_environment,
            unix_timestamp=str(calendar.timegm(reschedule_for.utctimetuple())),
        )
        logger.info(
            "Creating EventBridge schedule %s to run Lambda again at %s...",
            schedule_name,
            reschedule_for.isoformat(),
        )
        scheduler_client.create_schedule(
            GroupName=settings.idr_task_schedules_group,
            Name=schedule_name,
            ScheduleExpression=f"at({reschedule_for.isoformat(timespec='seconds').removesuffix('+00:00')})",
            ScheduleExpressionTimezone="UTC",
            Target={
                "RoleArn": settings.idr_task_scheduler_role_arn,
                "Arn": context.invoked_function_arn,
                "Input": json.dumps(event),
            },
            FlexibleTimeWindow={"Mode": "OFF"},
            StartDate=now,
        )
        logger.info("Created Schedule %s successfully", schedule_name)

        return RescheduledResult(
            running_task_arns=[x["taskArn"] for x in running_idr_tasks if "taskArn" in x],
            rescheduled_for=reschedule_for,
            schedule_name=schedule_name,
        )

    logger.info("No running IDR Pipeline Tasks found; proceeding to launch a new Task...")
    idr_container_overrides = IdrContainerOverrides.from_invoke(invoke_model)
    # Avoid even _setting_ arguments (specifically, "overrides") of run_task if unnecessary
    optional_args = filtered_dict(
        {
            "overrides": {
                "containerOverrides": [
                    {"name": settings.idr_container_name}
                    | filtered_dict(asdict(idr_container_overrides))
                ]
            }
            if idr_container_overrides.command or idr_container_overrides.environment
            else None
        }
    )
    resp = ecs_client.run_task(
        capacityProviderStrategy=[
            cast(CapacityProviderStrategyItemTypeDef, strategy.model_dump(by_alias=True))
            for strategy in settings.idr_task_capacity_provider_strategies
        ],
        taskDefinition=settings.idr_task_definition_arn,
        cluster=settings.ecs_cluster_arn,
        group=settings.idr_task_group,
        enableECSManagedTags=True,
        propagateTags="TASK_DEFINITION",
        enableExecuteCommand=invoke_model.ecs_exec or False,
        count=1,
        platformVersion="LATEST",
        networkConfiguration={
            "awsvpcConfiguration": {
                "subnets": settings.idr_task_subnet_ids,
                "securityGroups": [settings.idr_task_security_group_id],
                "assignPublicIp": "DISABLED",
            }
        },
        tags=[{"key": k, "value": str(v)} for k, v in settings.idr_task_tags.items()],
        **optional_args,
    )
    launched_task_arn = next((x["taskArn"] for x in resp["tasks"] if "taskArn" in x), "unknown")
    logger.info("New IDR Pipeline Task '%s' launched successfully.", launched_task_arn)

    return PipelineStartedResult(
        task_arn=launched_task_arn,
        exec_command=(
            f"aws ecs execute-command --cluster {settings.ecs_cluster_arn.split('/')[-1]}"
            f" --task {launched_task_arn.split('/')[-1]}"
            f" --container {settings.idr_container_name}"
            " --interactive"
            " --command '/bin/bash'"
        )
        if invoke_model.ecs_exec
        else None,
    )


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> dict[str, Any]:
    try:
        result = result_handler(event, context)
        json_safe_result = {
            k: v.isoformat() if isinstance(v, datetime) else v for k, v in asdict(result).items()
        }
        lambda_result = {
            "result_type": type(result).__name__,
            "details": filtered_dict(json_safe_result),
        }
        logger.info(lambda_result)
        return lambda_result
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
