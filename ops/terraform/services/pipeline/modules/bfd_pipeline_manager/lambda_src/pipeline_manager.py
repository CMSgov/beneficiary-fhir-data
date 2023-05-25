import itertools
import os
import re
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import Enum
from typing import Any, Optional
from urllib.parse import unquote

import boto3
from botocore.config import Config

INCOMING_BUCKET_PREFIXES = ["Incoming/", "Synthetic/Incoming/"]

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
PIPELINE_ASG_NAME = os.environ.get("PIPELINE_ASG_NAME", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

try:
    s3_resource = boto3.resource("s3", config=BOTO_CONFIG)
    autoscaling_client = boto3.client("autoscaling", config=BOTO_CONFIG)
    etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
except Exception as exc:
    print(
        f"Unrecoverable exception occurred when attempting to create boto3 clients/resources: {exc}"
    )
    sys.exit(0)


class PipelineLoadType(Enum):
    """Represents the possible types of data loads: either the data load is non-synthetic, meaning
    that it contains production data and was placed within the root-level Incoming/Done folders of
    the ETL bucket, or it is synthetic, meaning that it contains non-production, testing data and
    was placed within the Incoming/Done folders within the Synthetic folder of the ETL bucket."""

    NON_SYNTHETIC = 0
    SYNTHETIC = 1


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    INCOMING = "Incoming"
    DONE = "Done"


class RifFileType(str, Enum):
    """Represents all of the possible RIF file types that can be loaded by the BFD ETL Pipeline. The
    value of each enum is a specific substring that is used to match on each type of file"""

    BENEFICIARY_HISTORY = "beneficiary_history"
    BENEFICIARY = "bene"
    CARRIER = "carrier"
    DME = "dme"
    HHA = "hha"
    HOSPICE = "hospice"
    INPATIENT = "inpatient"
    OUTPATIENT = "outpatient"
    PDE = "pde"
    SNF = "snf"


@dataclass(frozen=True, eq=True)
class TimestampedDataLoad:
    """Represents a "group" of RIF files (known as a "data load"). These groups are organized within
    a folder/subkey in S3 which is a timestamp in ISO format"""

    load_type: PipelineLoadType
    name: str

    @property
    def timestamp(self) -> datetime:
        return datetime.fromisoformat(self.name.removesuffix("Z"))


def _get_all_valid_incoming_loads_before_date(
    time_cutoff: Optional[datetime] = None,
) -> set[TimestampedDataLoad]:
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    incoming_objects = itertools.chain.from_iterable(
        etl_bucket.objects.filter(Prefix=prefix) for prefix in INCOMING_BUCKET_PREFIXES
    )
    incoming_rifs = [
        str(object.key)
        for object in incoming_objects
        if re.match(
            pattern=rf".*({rif_types_group_str})_.*(txt|csv)",
            string=str(object.key),
        )
        is not None
    ]
    valid_data_loads = {
        TimestampedDataLoad(
            load_type=(
                PipelineLoadType.SYNTHETIC
                if object_key.startswith("Synthetic")
                else PipelineLoadType.NON_SYNTHETIC
            ),
            name=group_name_match.group(1),
        )
        for object_key in incoming_rifs
        if (group_name_match := re.search(pattern=r"([\d\-:TZ]+)/", string=object_key)) is not None
    }

    # If no time cutoff was specified we return all data loads, including future data loads
    if not time_cutoff:
        return valid_data_loads

    # Else, we return only valid data loads that should be loaded prior to the given time cutoff
    return {d for d in valid_data_loads if d.timestamp < time_cutoff}


def _try_schedule_pipeline_asg_action(
    scheduled_action_name: str, start_time: datetime, desired_capacity: int
) -> bool:
    # If either the desired scheduled action already exists or if the desired capacity for this
    # scheduled action is already set on the ASG, we do not need to create a scheduled action;
    # return False to indicate that no action was scheduled
    if (
        autoscaling_client.describe_scheduled_actions(
            AutoScalingGroupName=PIPELINE_ASG_NAME,
            ScheduledActionNames=[scheduled_action_name],
        )["ScheduledUpdateGroupActions"]
        or autoscaling_client.describe_auto_scaling_groups(
            AutoScalingGroupNames=[PIPELINE_ASG_NAME]
        )["AutoScalingGroups"][0]["DesiredCapacity"]
        == desired_capacity
    ):
        return False

    # We know now that this scheduled action has not yet been scheduled and the desired capacity is
    # not met, so schedule the action and return True to indicate an action was scheduled
    autoscaling_client.put_scheduled_update_group_action(
        AutoScalingGroupName=PIPELINE_ASG_NAME,
        ScheduledActionName=scheduled_action_name,
        StartTime=f"{start_time.replace(microsecond=0).isoformat()}Z",
        DesiredCapacity=1,
    )

    return True


def handler(event: Any, context: Any):
    if not all([REGION, BFD_ENVIRONMENT, ETL_BUCKET_ID]):
        print("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError as ex:
        print(f"The incoming event was invalid: {ex}")
        return
    except IndexError:
        print("Invalid event notification, no records found")
        return

    try:
        file_key: str = record["s3"]["object"]["key"]
    except KeyError as ex:
        print(f"No bucket file found in event notification: {ex}")
        return

    decoded_file_key = unquote(file_key)
    status_group_str = "|".join([e.value for e in PipelineDataStatus])
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    # The incoming file's key should match an expected format, as follows:
    # "<Synthetic/>/<Incoming/Done>/<ISO date format>/<file name>".
    if match := re.search(
        pattern=(
            rf"^(Synthetic){{0,1}}/{{0,1}}"
            rf"({status_group_str})/"
            rf"([\d\-:TZ]+)/"
            rf".*({rif_types_group_str}).*$"
        ),
        string=decoded_file_key,
        flags=re.IGNORECASE,
    ):
        pipeline_data_status = PipelineDataStatus(match.group(2))

        if pipeline_data_status == PipelineDataStatus.INCOMING:
            # Retrieve _all_ incoming data loads from both synthetic and non-synthetic loads
            all_incoming_data_loads = _get_all_valid_incoming_loads_before_date()

            if not all_incoming_data_loads:
                # This should never happen as we can only get here if the Lambda was started by a
                # file being moved to Incoming and that file matches valid RIFs; still, this is a
                # good signal that something is _wrong_
                print("No incoming data loads were discovered, exiting...")
                return

            print(
                f"Discovered {len(all_incoming_data_loads)} incoming data loads waiting to be"
                " ingested or being ingested by the Pipeline. Attempting to schedule scale-out for"
                " the times specified by each data load group..."
            )

            # If there are any loads that are timestamped prior to now, immediately schedule a
            # scale-out for the next minute:
            if any(
                data_load.timestamp < datetime.utcnow() for data_load in all_incoming_data_loads
            ):
                next_minute = datetime.utcnow() + timedelta(minutes=1)
                # Only schedule a new action to scale out if we haven't already done so
                if _try_schedule_pipeline_asg_action(
                    scheduled_action_name="scale_out_immediately",
                    start_time=next_minute,
                    desired_capacity=1,
                ):
                    print(
                        "Scheduled the Pipeline to start immediately at"
                        f" {next_minute.isoformat()} UTC"
                    )
                else:
                    print(
                        "Pipeline is already scheduled to start within the next minute or its"
                        " desired capacity is already 1"
                    )

            # Then, for any _future_ loads, we schedule an ASG scale-out for the time specified by
            # the data load
            for future_load in (
                data_load
                for data_load in all_incoming_data_loads
                if data_load.timestamp > datetime.utcnow()
            ):
                if _try_schedule_pipeline_asg_action(
                    scheduled_action_name=f"scale_out_at_{future_load.name}",
                    start_time=future_load.timestamp,
                    desired_capacity=1,
                ):
                    print(
                        "Scheduled the Pipeline to start in the future at"
                        f" {future_load.timestamp.isoformat()} UTC for future data load"
                        f" {future_load.name}"
                    )
                else:
                    print(
                        "Pipeline is already scheduled to scale-out for future data load"
                        f" {future_load.name}"
                    )
        elif (
            pipeline_data_status == PipelineDataStatus.DONE
            and not _get_all_valid_incoming_loads_before_date(
                time_cutoff=datetime.utcnow() + timedelta(minutes=10)
            )
        ):
            # If there are no valid incoming data loads/groups that should be loaded within the next
            # ten minutes in any of the Incoming folders, then we know that the Pipeline has
            # successfully loaded everything it needs to (since this Lambda was invoked by the
            # Pipeline moving the last file to the Done folder). We can now tell the Pipeline to
            # scale-in within the next ten minutes:
            print(
                "No valid, non-future data loads were discovered to be ingested by the Pipeline"
                " within the next ten minutes. Trying to schedule an action to scale-in the"
                " Pipeline to stop it..."
            )

            # This is a temporary workaround for a lack of good signaling between the Pipeline and
            # its ASG as to when the Pipeline instance can gracefully be stopped. Until such a time
            # that the Pipeline can signal that it has finished, we give the Pipeline 10 minutes to
            # run any final tasks it needs to after finishing data load before scaling it in
            # TODO: Replace this when the Pipeline is able to signal it can be stopped
            ten_minutes_from_now = datetime.utcnow() + timedelta(minutes=10)
            if _try_schedule_pipeline_asg_action(
                scheduled_action_name="scale_in_in_ten_minutes",
                start_time=ten_minutes_from_now,
                desired_capacity=0,
            ):
                print(
                    f"Scheduled the Pipeline to scale-in at {ten_minutes_from_now.isoformat()} UTC"
                    " as all valid, non-future Incoming data loads have been completed"
                )
            else:
                print(
                    "The Pipeline is already scheduled to scale-in within the next ten minutes or"
                    " its ASG's desired capacity is already set to 0"
                )
        else:
            print("The Pipeline is still loading data from Incoming, exiting...")
