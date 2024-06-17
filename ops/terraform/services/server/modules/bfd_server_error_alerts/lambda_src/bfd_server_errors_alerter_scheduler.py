import calendar
import json
import logging
import os
import sys
from abc import ABC, abstractmethod
from datetime import datetime, timedelta
from enum import StrEnum
from typing import Any, Optional

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
EVENTBRIDGE_SCHEDULES_GROUP = os.environ.get("EVENTBRIDGE_SCHEDULES_GROUP", "")
RECURRING_SCHEDULE_RATE_STR = os.environ.get("RECURRING_SCHEDULE_RATE_STR", "")
SCHEDULER_ROLE_ARN = os.environ.get("SCHEDULER_ROLE_ARN", "")
ALERTER_LAMBDA_ARN = os.environ.get("ALERTER_LAMBDA_ARN", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

ONETIME_SCHEDULE_NAME_PREFIX = f"bfd-{BFD_ENVIRONMENT}-run-error-alerter-at-"
RATE_SCHEDULE_NAME_PREFIX = f"bfd-{BFD_ENVIRONMENT}-run-error-alerter-every-"


class AlarmState(StrEnum):
    ALARMING = "ALARM"
    OK = "OK"


class Schedule(ABC):
    @property
    @abstractmethod
    def expression(self) -> str:
        pass

    @property
    @abstractmethod
    def name(self) -> str:
        pass


class OnetimeSchedule(Schedule):
    def __init__(self, time: datetime) -> None:
        super().__init__()
        self.time = time

    @property
    def expression(self) -> str:
        return f"at({self.isoformat})"

    @property
    def name(self) -> str:
        return f"{ONETIME_SCHEDULE_NAME_PREFIX}{self.utc_timestamp}"

    @property
    def isoformat(self) -> str:
        return self.time.isoformat(timespec="seconds")

    @property
    def utc_timestamp(self) -> str:
        return str(calendar.timegm(self.time.utctimetuple()))

    @staticmethod
    def from_name(name: str) -> Optional["OnetimeSchedule"]:
        try:
            utc_timestamp = int(name.removeprefix(ONETIME_SCHEDULE_NAME_PREFIX))
            return OnetimeSchedule(time=datetime.utcfromtimestamp(utc_timestamp))
        except ValueError:
            logger.error(
                "Unable to create OnetimeSchedule from schedule with name %s",
                name,
                exc_info=True,
            )
            return None


class RateSchedule(Schedule):
    def __init__(self, rate_str: str) -> None:
        super().__init__()
        self.rate_str = rate_str

    @property
    def expression(self) -> str:
        return f"rate({self.rate_str})"

    @property
    def name(self) -> str:
        return f"{RATE_SCHEDULE_NAME_PREFIX}{self.rate_str.replace(' ', '-')}"


logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()
try:
    scheduler_client = boto3.client("scheduler", config=BOTO_CONFIG)  # type: ignore
except Exception as exc:
    logger.error(
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources:",
        exc_info=True,
    )
    sys.exit(0)


def __timedelta_from_rate_str(rate_str: str) -> timedelta:
    try:
        interval_str, unit_str = tuple(rate_str.split(" "))
        interval = int(interval_str)

        return timedelta(**{unit_str: interval})
    except (ValueError, TypeError):
        logging.getLogger().error(
            'Unable to construct timedelta from rate string "%s":', rate_str, exc_info=True
        )
        return timedelta(seconds=0)


def __create_schedule(schedule: Schedule, start_date: Optional[datetime] = None) -> bool:
    logger.info("Creating schedule %s to run %s...", schedule.name, ALERTER_LAMBDA_ARN)
    try:
        scheduler_client.create_schedule(
            GroupName=EVENTBRIDGE_SCHEDULES_GROUP,
            Name=schedule.name,
            ScheduleExpression=schedule.expression,
            ScheduleExpressionTimezone="UTC",
            Target={"RoleArn": SCHEDULER_ROLE_ARN, "Arn": ALERTER_LAMBDA_ARN},
            FlexibleTimeWindow={"Mode": "OFF"},
            StartDate=start_date or datetime.utcnow(),
        )
    except scheduler_client.exceptions.ClientError:
        logger.error(
            "An unrecoverable error occurred when trying to schedule %s: ",
            schedule.name,
            exc_info=True,
        )
        return False

    logger.info("Schedule %s created successfully", schedule.name)
    return True


def handler(event: Any, context: Any):
    if not all(
        [
            REGION,
            BFD_ENVIRONMENT,
            EVENTBRIDGE_SCHEDULES_GROUP,
            RECURRING_SCHEDULE_RATE_STR,
        ]
    ):
        logger.error("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError as ex:
        logger.error("The incoming event was invalid: %s", repr(ex))
        return
    except IndexError:
        logger.error("Invalid event notification, no records found")
        return

    try:
        sns_message_json: str = record["Sns"]["Message"]
        alarm_details = json.loads(sns_message_json)
        alarm_state = AlarmState(alarm_details["NewStateValue"])
    except KeyError:
        logger.error("No message found in SNS notification")
        return
    except json.JSONDecodeError:
        logger.error("SNS message body was not valid JSON")
        return

    # Delete old OnetimeSchedules by listing all OnetimeSchedules within the group and deleting
    # those with names indicating times that have since passed
    try:
        all_onetime_schedules = [
            one_time_schedule
            for schedule in scheduler_client.list_schedules(
                GroupName=EVENTBRIDGE_SCHEDULES_GROUP,
                NamePrefix=ONETIME_SCHEDULE_NAME_PREFIX,
            )["Schedules"]
            if "Name" in schedule
            and ((one_time_schedule := OnetimeSchedule.from_name(schedule["Name"])) is not None)
        ]
        all_schedules_to_delete = [x for x in all_onetime_schedules if x.time <= datetime.utcnow()]
        for schedule in all_schedules_to_delete:
            logger.info(
                "Deleting schedule %s as its invocation time has passed...",
                schedule.name,
            )
            scheduler_client.delete_schedule(
                GroupName=EVENTBRIDGE_SCHEDULES_GROUP, Name=schedule.name
            )
            logger.info("Schedule %s deleted successfully", schedule.name)
    except scheduler_client.exceptions.ClientError:
        logger.error(
            "An error occurred when trying to delete old OnetimeSchedules; continuing. Err: ",
            exc_info=True,
        )

    # The scheduler always schedules the alerter to run within the next 10 seconds in order to
    # capture any errors that may have occurred between the alarm going into ALARM and invoking this
    # Lambda or the time between the last run of the alerter and the alarm returning to its OK state
    run_in_ten_seconds = OnetimeSchedule(time=datetime.utcnow() + timedelta(seconds=10))
    logger.info(
        "Scheduling %s to run in 10 seconds (%s UTC) from now...",
        ALERTER_LAMBDA_ARN,
        run_in_ten_seconds.isoformat,
    )
    if not __create_schedule(schedule=run_in_ten_seconds):
        return

    if alarm_state == AlarmState.ALARMING:
        # Create schedules to invoke alerter
        logger.info(
            "Alarm state is %s, indicating that some requests to server have errored."
            " Scheduling %s to run every %s until Alarm has returned to %s state...",
            alarm_state.value,
            ALERTER_LAMBDA_ARN,
            RECURRING_SCHEDULE_RATE_STR,
            AlarmState.OK.value,
        )

        rate_schedule = RateSchedule(rate_str=RECURRING_SCHEDULE_RATE_STR)
        if not __create_schedule(
            schedule=rate_schedule,
            start_date=datetime.utcnow()
            + __timedelta_from_rate_str(rate_str=rate_schedule.rate_str),
        ):
            return
    elif alarm_state == AlarmState.OK:
        # delete rate schedules
        logger.info(
            "Alarm state is %s, indicating that server operation has returned to"
            " normal. Deleting all rate schedules in group %s...",
            alarm_state.value,
            EVENTBRIDGE_SCHEDULES_GROUP,
        )
        try:
            for rate_schedule in [
                schedule
                for schedule in scheduler_client.list_schedules(
                    GroupName=EVENTBRIDGE_SCHEDULES_GROUP,
                    NamePrefix=RATE_SCHEDULE_NAME_PREFIX,
                )["Schedules"]
                if "Name" in schedule
            ]:
                logger.info("Deleting rate schedule %s...", rate_schedule["Name"])
                scheduler_client.delete_schedule(
                    GroupName=EVENTBRIDGE_SCHEDULES_GROUP, Name=rate_schedule["Name"]
                )
                logger.info("Rate schedule %s deleted successfully", rate_schedule["Name"])
        except scheduler_client.exceptions.ClientError:
            logger.error(
                "An unrecoverable error occurred when trying to delete rate schedules: ",
                exc_info=True,
            )
