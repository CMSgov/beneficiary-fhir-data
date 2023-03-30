import json
import os
import re
import sys
from dataclasses import asdict, dataclass
from enum import Enum
from typing import Any, Optional
from urllib.parse import quote_plus, unquote

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
DEPLOYED_GIT_BRANCH = os.environ.get("DEPLOYED_GIT_BRANCH", "")
JENKINS_TARGET_JOB_NAME = os.environ.get("JENKINS_TARGET_JOB_NAME", "")
JENKINS_JOB_RUNNER_QUEUE = os.environ.get("JENKINS_JOB_RUNNER_QUEUE", "")
ONGOING_LOAD_QUEUE = os.environ.get("ONGOING_LOAD_QUEUE", "")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
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
    etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
    sqs_resource = boto3.resource("sqs", config=BOTO_CONFIG)
    jenkins_job_runner_queue = sqs_resource.get_queue_by_name(QueueName=JENKINS_JOB_RUNNER_QUEUE)
    ongoing_load_queue = sqs_resource.get_queue_by_name(QueueName=ONGOING_LOAD_QUEUE)
except Exception as exc:
    print(
        f"Unrecoverable exception occurred when attempting to create boto3 clients/resources: {exc}"
    )
    sys.exit(0)


class PipelineLoadType(str, Enum):
    NON_SYNTHETIC = ""
    SYNTHETIC = "Synthetic"


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


@dataclass
class OngoingLoadQueueMessage:
    """Represents a message in the ongoing load queue that acts as a sentinel indicating a
    particular group is currently being loaded. This Lambda checks for the existence of these
    messages in the queue, and will only start/stop the CCW pipeline instance depending on the
    existence of such messages"""

    load_type: PipelineLoadType
    load_group: str
    message_id: Optional[str] = None
    receipt_handle: Optional[str] = None


@dataclass
class JenkinsJobRunnerQueueMessage:
    """Represents the message that should be posted to the Jenkins job runner queue to invoke a
    Jenkins job"""

    job: str
    parameters: "JenkinsTerraserviceJobParameters"


@dataclass
class JenkinsTerraserviceJobParameters:
    """Represents the parameters for the BFD Pipeline Deploy Terraservice Jenkins Pipeline/job"""

    env: str
    create_ccw_pipeline_instance: bool


def _check_ongoing_load_queue(timeout: int = 1) -> list[OngoingLoadQueueMessage]:
    responses = ongoing_load_queue.receive_messages(WaitTimeSeconds=timeout)

    def load_json_safe(json_str: str) -> Optional[dict[str, str]]:
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            return None
        except TypeError:
            return None

    raw_messages = [
        (response.message_id, response.receipt_handle, load_json_safe(response.body))
        for response in responses
    ]
    filtered_messages = [
        OngoingLoadQueueMessage(
            load_type=PipelineLoadType(message["load_type"]),
            load_group=message["load_group"],
            message_id=message_id,
            receipt_handle=message_receipt,
        )
        for message_id, message_receipt, message in raw_messages
        if message is not None and "load_type" in message and "load_group" in message
    ]

    return filtered_messages


def _post_ongoing_load_message(load_type: PipelineLoadType, group_timestamp: str):
    ongoing_load_queue.send_message(
        MessageBody=json.dumps(
            asdict(OngoingLoadQueueMessage(load_type=load_type, load_group=group_timestamp))
        )
    )


def _remove_ongoing_load_message(message_id: str, message_receipt: str):
    ongoing_load_queue.delete_messages(
        Entries=[{"Id": message_id, "ReceiptHandle": message_receipt}]
    )


def _post_jenkins_job_message(create_ccw_instance: bool):
    encoded_branch_name = quote_plus(DEPLOYED_GIT_BRANCH)
    jenkins_job_runner_queue.send_message(
        MessageBody=json.dumps(
            asdict(
                JenkinsJobRunnerQueueMessage(
                    job=f"{JENKINS_TARGET_JOB_NAME}/{encoded_branch_name}",
                    parameters=JenkinsTerraserviceJobParameters(
                        env=BFD_ENVIRONMENT, create_ccw_pipeline_instance=create_ccw_instance
                    ),
                )
            )
        )
    )


def _is_pipeline_load_complete(load_type: PipelineLoadType, group_timestamp: str) -> bool:
    done_prefix = (
        "/".join(
            # Filters out any falsey elements, which will remove any empty strings from the joined
            # string
            filter(
                None,
                [load_type.capitalize(), PipelineDataStatus.DONE.capitalize(), group_timestamp],
            )
        )
        + "/"
    )
    # Returns the file names of all text files within the "done" folder for the current bucket
    finished_rifs = [
        str(object.key).removeprefix(done_prefix)
        for object in etl_bucket.objects.filter(Prefix=done_prefix)
        if str(object.key).endswith(".txt") or str(object.key).endswith(".csv")
    ]

    # We check for all RIFs _except_ beneficiary history as beneficiary history is a RIF type not
    # expected to exist in CCW-provided loads -- it only appears in synthetic loads
    rif_types_to_check = [e for e in RifFileType if e != RifFileType.BENEFICIARY_HISTORY]

    # Check, for each rif file type, if any finished rif has the corresponding rif type prefix.
    # Essentially, this ensures that all non-optional (excluding beneficiary history) RIF types have
    # been loaded and exist in the Done/ folder
    return all(
        any(rif_type.value in rif_file_name.lower() for rif_file_name in finished_rifs)
        for rif_type in rif_types_to_check
    )


def _is_incoming_folder_empty(load_type: PipelineLoadType, group_timestamp: str) -> bool:
    incoming_key_prefix = (
        "/".join(
            filter(
                None,
                [load_type.capitalize(), PipelineDataStatus.INCOMING.capitalize(), group_timestamp],
            )
        )
        + "/"
    )
    incoming_objects = list(etl_bucket.objects.filter(Prefix=incoming_key_prefix))

    return len(incoming_objects) == 0


def handler(event: Any, context: Any):
    if not all(
        [
            REGION,
            BFD_ENVIRONMENT,
            DEPLOYED_GIT_BRANCH,
            JENKINS_TARGET_JOB_NAME,
            JENKINS_JOB_RUNNER_QUEUE,
            ONGOING_LOAD_QUEUE,
            ETL_BUCKET_ID,
        ]
    ):
        print("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError as exc:
        print(f"The incoming event was invalid: {exc}")
        return
    except IndexError:
        print("Invalid event notification, no records found")
        return

    try:
        file_key: str = record["s3"]["object"]["key"]
    except KeyError as exc:
        print(f"No bucket file found in event notification: {exc}")
        return

    decoded_file_key = unquote(file_key)
    status_group_str = "|".join([e.value for e in PipelineDataStatus])
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    # The incoming file's key should match an expected format, as follows:
    # "<Synthetic/>/<Incoming/Done>/<ISO date format>/<file name>".
    if match := re.search(
        pattern=(
            rf"^({PipelineLoadType.SYNTHETIC.capitalize()}){{0,1}}/{{0,1}}"
            rf"({status_group_str})/"
            rf"([\d\-:TZ]+)/"
            rf".*({rif_types_group_str}).*$"
        ),
        string=decoded_file_key,
        flags=re.IGNORECASE,
    ):
        pipeline_load_type = PipelineLoadType(match.group(1) or "")
        pipeline_data_status = PipelineDataStatus(match.group(2))
        group_timestamp = match.group(3)

        if pipeline_data_status == PipelineDataStatus.INCOMING:
            # check queue for any ongoing load corresponding to the current load
            ongoing_loads = _check_ongoing_load_queue(timeout=5)
            if any(
                msg.load_group == group_timestamp and msg.load_type == pipeline_load_type
                for msg in ongoing_loads
            ):
                print(
                    f"The group {group_timestamp} has already been handled, and the CCW pipeline"
                    " instance should be running. Stopping..."
                )
                return

            # no messages in queue correspond to the current load, this must be a new load. Post a
            # message to the Jenkins job queue to start the deploy job indicating that the CCW
            # instance should be started/created
            print(
                f"No ongoing load messages were found in {ONGOING_LOAD_QUEUE} queue, the BFD CCW"
                " Pipeline instance must not be running. Posting a message to the"
                f" {JENKINS_JOB_RUNNER_QUEUE} queue to start the {JENKINS_TARGET_JOB_NAME} Jenkins"
                " job indicating the BFD CCW Pipeline instance should be started for environment"
                f" {BFD_ENVIRONMENT} and branch {DEPLOYED_GIT_BRANCH}..."
            )
            _post_jenkins_job_message(create_ccw_instance=True)
            print(f"Message posted successfully")

            # post a message to the ongoing load queue to stop further, unnecessary, deployments
            print(
                f"Posting message to {ONGOING_LOAD_QUEUE} queue indicating there is an ongoing data"
                f" load and that the {JENKINS_TARGET_JOB_NAME} Jenkins job has been started to"
                " create a CCW Pipeline instance..."
            )
            _post_ongoing_load_message(
                load_type=pipeline_load_type, group_timestamp=group_timestamp
            )
            print(f"Message posted successfully")
        elif (
            pipeline_data_status == PipelineDataStatus.DONE
            and _is_pipeline_load_complete(
                load_type=pipeline_load_type, group_timestamp=group_timestamp
            )
            and _is_incoming_folder_empty(
                load_type=pipeline_load_type, group_timestamp=group_timestamp
            )
        ):
            # remove the message(s) in the ongoing load queue corresponding to the current group
            print(
                f"S3 Event and location of group {group_timestamp}'s data in S3 indicates data load"
                f" for group {group_timestamp} has completed. Cleaning up messages in"
                f" {ONGOING_LOAD_QUEUE} queue corresponding to group {group_timestamp}..."
            )
            # this might seem odd -- assuming the "locking" logic for the INCOMING case above is
            # sound, there should only be one single message per-group; unfortunately, AWS SQS isn't
            # _quite_ realtime enough for this to be the case, and it is possible that if this
            # Lambda is invoked concurrently quickly enough (such as in the case where multiple
            # files are uploaded simultaneously) that multiple SQS messages get posted for a single
            # group. there's not much we can do about this other than ensure we delete _all_
            # possible messages
            group_load_msgs = [
                msg
                for msg in _check_ongoing_load_queue(timeout=5)
                if msg.load_type == pipeline_load_type and msg.load_group == group_timestamp
            ]
            for msg in group_load_msgs:
                assert msg.message_id and msg.receipt_handle
                _remove_ongoing_load_message(
                    message_id=msg.message_id, message_receipt=msg.receipt_handle
                )
            print(f"Cleanup successful")

            # now, check if the ongoing load queue is empty. we only want to stop the CCW pipeline
            # instance if there are no more data loads for it to handle.
            if list(_check_ongoing_load_queue(timeout=5)):
                print(
                    f"There are still ongoing loads queued up for the BFD CCW Pipeline to process."
                    f" Stopping..."
                )
                return

            # if, and only if, there are no messages remaining in the ongoing load queue we post a
            # message to the Jenkins job queue to start the deploy job, but this time specifying
            # that the CCW instance should be destroyed
            print(
                f" Posting a message to {JENKINS_JOB_RUNNER_QUEUE} queue to start the"
                f" {JENKINS_TARGET_JOB_NAME} Jenkins job indicating the BFD CCW Pipeline instance"
                f" should be stopped for environment {BFD_ENVIRONMENT} and branch"
                f" {DEPLOYED_GIT_BRANCH}..."
            )
            _post_jenkins_job_message(create_ccw_instance=False)
            print(f"Message posted successfully")
        else:
            print(
                f"The location of data in S3 bucket {ETL_BUCKET_ID} for the current group"
                f" ({group_timestamp}) does not indicate that the data load has finished."
                " Stopping..."
            )
