import json
import os
import re
import sys
from dataclasses import asdict, dataclass
from enum import Enum
from typing import Any, Optional
from urllib.parse import unquote

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
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources:"
        f" {exc}"
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
    load_type: PipelineLoadType
    load_group: str


@dataclass
class JenkinsJobRunnerQueueMessage:
    job: str
    parameters: "JenkinsTerraserviceJobParameters"


@dataclass
class JenkinsTerraserviceJobParameters:
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

    raw_messages = [load_json_safe(response.body) for response in responses]
    filtered_messages = [
        OngoingLoadQueueMessage(
            load_type=PipelineLoadType(message["load_type"]), load_group=message["load_group"]
        )
        for message in raw_messages
        if message is not None and "load_type" in message and "load_group" in message
    ]

    return filtered_messages


def _post_ongoing_load_message(load_type: PipelineLoadType, group_timestamp: str):
    ongoing_load_queue.send_message(
        MessageBody=json.dumps(
            asdict(OngoingLoadQueueMessage(load_type=load_type, load_group=group_timestamp))
        )
    )


def _post_jenkins_job_message(create_ccw_instance: bool):
    jenkins_job_runner_queue.send_message(
        MessageBody=json.dumps(
            asdict(
                JenkinsJobRunnerQueueMessage(
                    job=f"{JENKINS_TARGET_JOB_NAME}/{DEPLOYED_GIT_BRANCH}",
                    parameters=JenkinsTerraserviceJobParameters(
                        env=BFD_ENVIRONMENT, create_ccw_pipeline_instance=create_ccw_instance
                    ),
                )
            )
        )
    )


def _is_pipeline_load_complete(group_timestamp: str) -> bool:
    done_prefix = f"{PipelineDataStatus.DONE.capitalize()}/{group_timestamp}/"
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


def _is_incoming_folder_empty(group_timestamp: str) -> bool:
    incoming_key_prefix = f"{PipelineDataStatus.INCOMING.capitalize()}/{group_timestamp}/"
    incoming_objects = list(etl_bucket.objects.filter(Prefix=incoming_key_prefix))

    return len(incoming_objects) == 0


def handler(event: Any, context: Any):
    if not all([REGION, ETL_BUCKET_ID]):
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
    # "<Incoming/Done>/<ISO date format>/<file name>".
    if match := re.search(
        rf"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$",
        decoded_file_key,
        re.IGNORECASE,
    ):
        pipeline_data_status = PipelineDataStatus(match.group(1).lower())
        group_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3).lower())

        if pipeline_data_status == PipelineDataStatus.INCOMING:
            pass
        elif pipeline_data_status == PipelineDataStatus.DONE:
            pass
