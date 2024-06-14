import calendar
import itertools
import json
import os
import re
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import StrEnum
from typing import Any, Optional
from urllib.parse import unquote

import boto3
from botocore.config import Config

SCALE_IN_FIVE_MINUTES_ACTION_NAME = "scale_in_in_five_minutes"
SCALE_OUT_IMMEDIATELY_ACTION_NAME = "scale_out_immediately"
SCALE_OUT_FUTURE_LOAD_ACTION_NAME_PREFIX = "scale_out_at_"

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


class S3EventType(StrEnum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"
    OBJECT_REMOVED = "ObjectRemoved"


class PipelineLoadType(StrEnum):
    """Represents the possible types of data loads: either the data load is non-synthetic, meaning
    that it contains production data and was placed within the root-level Incoming/Done folders of
    the ETL bucket, or it is synthetic, meaning that it contains non-production, testing data and
    was placed within the Incoming/Done folders within the Synthetic folder of the ETL bucket. The
    value of each enum represents the name of the Incoming/Done folders' parent directory, with
    empty string indicating that those paths have no parent"""

    NON_SYNTHETIC = ""
    SYNTHETIC = "Synthetic"


class PipelineDataStatus(StrEnum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    INCOMING = "Incoming"
    DONE = "Done"

    @classmethod
    def match_str(cls) -> str:
        return "|".join([e.value for e in cls])


class RifFileType(StrEnum):
    """Represents all of the possible RIF file types that can be loaded by the BFD ETL Pipeline. The
    value of each enum is a specific substring that is used to match on each type of file
    """

    BENEFICIARY = "bene"
    CARRIER = "carrier"
    DME = "dme"
    HHA = "hha"
    HOSPICE = "hospice"
    INPATIENT = "inpatient"
    OUTPATIENT = "outpatient"
    PDE = "pde"
    SNF = "snf"

    @classmethod
    def match_str(cls) -> str:
        return "|".join([e.value for e in cls])


@dataclass(frozen=True, eq=True)
class TimestampedDataLoad:
    """Represents a "group" of RIF files (known as a "data load"). These groups are organized within
    a folder/subkey in S3 which is a timestamp in ISO format"""

    load_type: PipelineLoadType
    name: str

    @classmethod
    def match_str(cls) -> str:
        return r"[\d\-:TZ]+"

    @property
    def timestamp(self) -> datetime:
        return datetime.fromisoformat(self.name.removesuffix("Z"))


def _get_all_valid_incoming_loads_before_date(
    time_cutoff: Optional[datetime] = None,
) -> set[TimestampedDataLoad]:
    incoming_bucket_prefixes = [
        "/".join(filter(None, [load_type.value, PipelineDataStatus.INCOMING])) + "/"
        for load_type in PipelineLoadType
    ]
    # We get all objects in both the non-synthetic and synthetic incoming folders within the S3
    # Bucket; chain() will flatten the resulting iterable so that it's a single iterable of bucket
    # objects
    incoming_objects = itertools.chain.from_iterable(
        etl_bucket.objects.filter(Prefix=prefix) for prefix in incoming_bucket_prefixes
    )
    # We then filter out any objects that are not valid RIF files
    incoming_rifs = [
        str(object.key)
        for object in incoming_objects
        if re.match(
            pattern=rf".*({RifFileType.match_str()}).*(txt|csv)",
            string=str(object.key),
        )
        is not None
    ]
    # Finally, valid data loads/groups are extracted by regex matching the data load timestamp
    # folder within each object's key and taking the match. Using a set automatically ensures the
    # resulting loads are unique
    valid_data_loads = {
        TimestampedDataLoad(
            load_type=(
                PipelineLoadType.SYNTHETIC
                if object_key.startswith(PipelineLoadType.SYNTHETIC)
                else PipelineLoadType.NON_SYNTHETIC
            ),
            name=group_name_match.group(1),
        )
        for object_key in incoming_rifs
        if (
            group_name_match := re.search(
                pattern=rf"({TimestampedDataLoad.match_str()})/", string=object_key
            )
        )
        is not None
    }

    # If no time cutoff was specified we return all data loads, including future data loads
    if not time_cutoff:
        return valid_data_loads

    # Else, we return only valid data loads that should be loaded prior to the given time cutoff
    return {d for d in valid_data_loads if d.timestamp < time_cutoff}


def _is_incoming_folder_empty(data_load: TimestampedDataLoad) -> bool:
    incoming_key_prefix = (
        "/".join(
            filter(
                None,
                [data_load.load_type, PipelineDataStatus.INCOMING, data_load.name],
            )
        )
        + "/"
    )

    # We check each object with a matching prefix to see if they match the expected names of RIF
    # files. If any match, that means there is a valid RIF still within Incoming/. We negate the
    # result of any() as we're returning if Incoming/ is empty, not if it's non-empty
    return not any(
        re.search(pattern=rf".*({RifFileType.match_str()}).*(txt|csv)", string=str(object.key))
        is not None
        for object in etl_bucket.objects.filter(Prefix=incoming_key_prefix)
    )


def _try_schedule_pipeline_asg_action(
    scheduled_action_name: str, start_time: datetime, desired_capacity: int
) -> bool:
    # We try to schedule the requested action, and if the AWS API returns an error indicating that
    # the scheduled action exists we return false indicating so
    try:
        autoscaling_client.put_scheduled_update_group_action(
            AutoScalingGroupName=PIPELINE_ASG_NAME,
            ScheduledActionName=scheduled_action_name,
            StartTime=f"{start_time.replace(microsecond=0).isoformat()}Z",
            DesiredCapacity=desired_capacity,
        )
    except autoscaling_client.exceptions.AlreadyExistsFault:
        return False

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
        sns_message_json: str = record["Sns"]["Message"]
        sns_message = json.loads(sns_message_json)
    except KeyError:
        print("No message found in SNS notification")
        return
    except json.JSONDecodeError:
        print("SNS message body was not valid JSON")
        return

    try:
        s3_event = sns_message["Records"][0]
    except KeyError:
        print("Invalid S3 event, no records found")
        return
    except IndexError:
        print("Invalid event notification, no records found")
        return

    try:
        event_type_str = s3_event["eventName"]
        event_type: S3EventType
        if S3EventType.OBJECT_CREATED in event_type_str:
            event_type = S3EventType.OBJECT_CREATED
        elif S3EventType.OBJECT_REMOVED in event_type_str:
            event_type = S3EventType.OBJECT_REMOVED
        else:
            print(f"Event type {event_type_str} is unsupported. Exiting...")
            return
    except KeyError as ex:
        print(f"The incoming event record did not contain the type of S3 event: {ex}")
        return

    try:
        file_key: str = s3_event["s3"]["object"]["key"]
        decoded_file_key = unquote(file_key)
    except KeyError as ex:
        print(f"No bucket file found in event notification: {ex}")
        return

    # Log the various bits of data extracted from the invoking event to aid debugging:
    print(f"Invoked at: {datetime.utcnow().isoformat()} UTC")
    print(f"S3 Object Key: {decoded_file_key}")
    print(f"S3 Event Type: {event_type.name}, Specific Event Name: {event_type_str}")

    # The incoming file's key should match an expected format, as follows:
    # "<Synthetic/>/<Incoming/Done>/<ISO date format>/<file name>".
    match = re.search(
        pattern=(
            rf"^({PipelineLoadType.SYNTHETIC}){{0,1}}/{{0,1}}"
            rf"({PipelineDataStatus.match_str()})/"
            rf"({TimestampedDataLoad.match_str()})/"
            rf".*({RifFileType.match_str()}).*(txt|csv)$"
        ),
        string=decoded_file_key,
        flags=re.IGNORECASE,
    )
    if match is None:
        print(
            "Invocation event unsupported, no matching RIF file discovered. See log for additional"
            " detail. Exiting..."
        )
        return

    pipeline_load_type = PipelineLoadType(match.group(1) or "")
    pipeline_data_status = PipelineDataStatus(match.group(2))
    data_load = TimestampedDataLoad(load_type=pipeline_load_type, name=match.group(3))
    is_future_load = data_load.timestamp >= datetime.utcnow()

    # Log data extracted from S3 object key now that we know this is a valid RIF file within a valid
    # data load
    print(f"RIF type: {RifFileType(match.group(4)).name}")
    print(f"Load Status: {pipeline_data_status.name}")
    print(f"Load Type: {pipeline_load_type.name}")
    print(f"Data Load: {data_load.name}")

    if (
        pipeline_data_status == PipelineDataStatus.INCOMING
        and event_type == S3EventType.OBJECT_CREATED
    ):
        # A load was added to Incoming (or files were added to a new load, the distinction
        # doesn't really matter); add a scheduled action for scale-out in the future if it's
        # a future load or immediately if the load is timestamped in the past

        if not is_future_load:
            # We also need to ensure that there are no near-future scale-in scheduled
            # actions on the ASG so that we can avoid scaling-out and then prematurely
            # scaling-in
            print(
                "Attempting to delete any near-future scale-in actions on"
                f" {PIPELINE_ASG_NAME} to avoid premature scale-in..."
            )
            try:
                autoscaling_client.delete_scheduled_action(
                    AutoScalingGroupName=PIPELINE_ASG_NAME,
                    ScheduledActionName=SCALE_IN_FIVE_MINUTES_ACTION_NAME,
                )
                print(
                    f"Scheduled action {SCALE_IN_FIVE_MINUTES_ACTION_NAME} successfully"
                    " deleted. Continuing..."
                )
            except autoscaling_client.exceptions.ClientError as ex:
                print(
                    f"No near-future scale-in actions scheduled on {PIPELINE_ASG_NAME}."
                    " Continuing..."
                )

            # For immediate scale outs we do not want to set a scheduled action if the
            # desired capacity is already at 1; so, if this is not a future load we check
            # the desired capacity and exit if it's already at 1
            if (
                autoscaling_client.describe_auto_scaling_groups(
                    AutoScalingGroupNames=[PIPELINE_ASG_NAME]
                )["AutoScalingGroups"][0]["DesiredCapacity"]
                == 1
            ):
                print("Pipeline ASG desired capacity is already at 1. Exiting...")
                return

        scheduled_action_time = (
            data_load.timestamp if is_future_load else datetime.utcnow() + timedelta(minutes=1)
        )
        scheduled_action_name = (
            f"{SCALE_OUT_FUTURE_LOAD_ACTION_NAME_PREFIX}{calendar.timegm(data_load.timestamp.utctimetuple())}"
            if is_future_load
            else SCALE_OUT_IMMEDIATELY_ACTION_NAME
        )
        if _try_schedule_pipeline_asg_action(
            scheduled_action_name=scheduled_action_name,
            start_time=scheduled_action_time,
            desired_capacity=1,
        ):
            print(
                "Scheduled the Pipeline to start"
                f" {'in the future' if is_future_load else 'immediately'} at"
                f" {scheduled_action_time.isoformat()} UTC"
            )
        else:
            print(
                f"The scheduled action {scheduled_action_name} was already scheduled on the"
                f" Pipeline's ASG for data load {data_load.name}. Exiting..."
            )
    elif (
        pipeline_data_status == PipelineDataStatus.INCOMING
        and event_type == S3EventType.OBJECT_REMOVED
    ):
        # If an object is removed from Incoming, that could mean that the Pipeline loaded
        # the data load it was from _or_ an operator removed the data load manually. Either
        # case, we want to check if this load is now completely gone from Incoming and
        # remove any corresponding scheduled actions if so so the Pipeline doesn't scale
        # unnecessarily.
        if not _is_incoming_folder_empty(data_load=data_load):
            print(
                f"The data load {data_load.name} has not yet been fully removed from"
                " Incoming. Exiting..."
            )
            return

        # The invoking event was for the last file in this load being removed from Incoming;
        # remove the data load's corresponding scheduled action, if it exists:
        invalid_scheduled_action_name = (
            f"{SCALE_OUT_FUTURE_LOAD_ACTION_NAME_PREFIX}{calendar.timegm(data_load.timestamp.utctimetuple())}"
            if is_future_load
            else SCALE_OUT_IMMEDIATELY_ACTION_NAME
        )
        print(
            f"Data load {data_load} has no data in Incoming; it has either been loaded or"
            " removed by an external operator. Trying to remove corresponding scheduled"
            f" action {invalid_scheduled_action_name}..."
        )
        try:
            autoscaling_client.delete_scheduled_action(
                AutoScalingGroupName=PIPELINE_ASG_NAME,
                ScheduledActionName=invalid_scheduled_action_name,
            )
            print(f"Scheduled action {invalid_scheduled_action_name} successfully deleted")
        except autoscaling_client.exceptions.ClientError as ex:
            print(
                "An error occurred when attempting to delete the scheduled action"
                f" {invalid_scheduled_action_name}. It is likely that this scheduled action"
                f" had already been invoked and the data load {data_load.name} has already"
                " been loaded successfully."
            )
    elif (
        pipeline_data_status == PipelineDataStatus.DONE and event_type == S3EventType.OBJECT_CREATED
    ):
        # If the invoking event was from a file being created in Done, we can assume that a
        # Pipeline Data Load is either in-progress or being completed (as the Pipeline will move
        # Incoming files to Done when they are finished loading); we only want to schedule the
        # Pipeline to scale-in when there is no ongoing data load and there are no upcoming
        # future loads in the _immediate_ future

        if not _is_incoming_folder_empty(data_load=data_load):
            # If the invoking file's data load has corresponding data yet to still be loaded
            # waiting in Incoming/, that means that this particular data load is not yet
            # finished and there's no further work to do. This avoids the relatively expensive
            # operation of scanning all data loads that's done below since we _know_ this data
            # load is not done yet
            print(f"The data load {data_load.name} has not yet been fully loaded. Exiting...")
            return

        five_minutes_from_now = datetime.utcnow() + timedelta(minutes=5)
        if _get_all_valid_incoming_loads_before_date(time_cutoff=five_minutes_from_now):
            # If there are any valid incoming data loads (or future loads within the next 5
            # minutes), then the Pipeline still has work to do and should not be scaled-in
            print("The Pipeline is still loading data from Incoming, exiting...")
            return

        # If there are no valid incoming data loads/groups that should be loaded within the next
        # five minutes in any of the Incoming folders, then we know that the Pipeline has
        # successfully loaded everything it needs to (since this Lambda was invoked by the
        # Pipeline moving the last file to the Done folder). We can now tell the Pipeline to
        # scale-in within the next five minutes:
        print(
            "No valid, non-future data loads were discovered to be ingested by the Pipeline"
            " within the next five minutes. Trying to schedule an action to scale-in the"
            " Pipeline to stop it..."
        )

        # This is a temporary workaround for a lack of good signaling between the Pipeline and
        # its ASG as to when the Pipeline instance can gracefully be stopped. Until such a time
        # that the Pipeline can signal that it has finished, we give the Pipeline 5 minutes to
        # run any final tasks it needs to after finishing data load before scaling it in
        # TODO: Replace this when the Pipeline is able to signal it can be stopped
        current_desired_capacity = autoscaling_client.describe_auto_scaling_groups(
            AutoScalingGroupNames=[PIPELINE_ASG_NAME]
        )["AutoScalingGroups"][0]["DesiredCapacity"]
        if current_desired_capacity != 0 and _try_schedule_pipeline_asg_action(
            scheduled_action_name=SCALE_IN_FIVE_MINUTES_ACTION_NAME,
            start_time=five_minutes_from_now,
            desired_capacity=0,
        ):
            print(
                f"Scheduled the Pipeline to scale-in at {five_minutes_from_now.isoformat()} UTC"
                " as all valid, non-future Incoming data loads have been completed"
            )
        else:
            print(
                "The Pipeline is already scheduled to scale-in within the next five minutes or"
                " its ASG's desired capacity is already set to 0"
            )
    else:
        print(
            f"Unsupported invocation (data status: {pipeline_data_status.name}, s3 event:"
            f" {event_type.name}). Exiting..."
        )
