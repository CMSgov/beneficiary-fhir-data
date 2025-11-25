import json
import os
import subprocess
import urllib.parse
from dataclasses import asdict, dataclass
from enum import StrEnum
from pathlib import Path

import boto3
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config

environment = os.environ.get("BFD_ENVIRONMENT", "test").lower()
s3_bucket = os.environ.get("INSIGHTS_BUCKET_NAME")
sqs_pipeline_signal = os.environ.get("SQS_PIPELINE_SIGNAL_NAME")
lambda_task_root = os.environ.get("LAMBDA_TASK_ROOT", "/var/task")

boto_config = Config(region_name="us-east-1")
ssm_client = boto3.client("ssm", config=boto_config)
rds_client = boto3.client("rds", config=boto_config)
sqs_client = boto3.client("sqs", config=boto_config)


@dataclass
class InvokeEvent:
    host: str
    suite_version: str
    spawn_rate: int
    users: int
    spawned_runtime: str
    compare_tag: str
    store_tags: list[str]


@dataclass
class PipelineSignalMessage:
    function_name: str
    result: "TestResult"
    message: str
    request_id: str
    log_stream_name: str
    log_group_name: str


class TestResult(StrEnum):
    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"


def get_ssm_parameter(name: str, with_decrypt: bool = False) -> str:
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]  # type: ignore
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{name}" not found or empty') from exc


def get_rds_db_uri(cluster_id: str) -> str:
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]  # type: ignore
    except KeyError as exc:
        raise ValueError(f'DB URI not found for cluster ID "{cluster_id}"') from exc


def get_sqs_queue_url(sqs_queue_name: str) -> str:
    response = sqs_client.get_queue_url(QueueName=sqs_queue_name)

    try:
        return response["QueueUrl"]
    except KeyError as exc:
        raise ValueError(f'SQS Queue URL not found for queue "{sqs_queue_name}"') from exc


def send_sqs_message(sqs_queue_url: str, msg_body: PipelineSignalMessage) -> bool:
    try:
        sqs_msg = asdict(msg_body)
        print(f"Sending message to queue: {sqs_msg}")

        sqs_client.send_message(QueueUrl=sqs_queue_url, MessageBody=json.dumps(sqs_msg))
        return True
    except sqs_client.exceptions.UnsupportedOperation:
        print(f'Unable to post message to queue at URL "{sqs_queue_url}"')
        return False


def send_pipeline_signal(
    signal_queue_url: str, result: TestResult, message: str, context: LambdaContext
) -> None:
    pipeline_signal_msg = PipelineSignalMessage(
        function_name=context.function_name,
        result=result,
        message=message,
        request_id=context.aws_request_id,
        log_stream_name=context.log_stream_name,
        log_group_name=context.log_group_name,
    )
    send_sqs_message(signal_queue_url, pipeline_signal_msg)


def handler(event: dict, context: LambdaContext) -> str | None:
    if not s3_bucket or not sqs_pipeline_signal:
        print(
            '"INSIGHTS_BUCKET_NAME" and "SQS_PIPELINE_SIGNAL_NAME" environment variables must be'
            " specified"
        )
        return None

    try:
        signal_queue_url = get_sqs_queue_url(sqs_pipeline_signal)
    except ValueError as exc:
        print(str(exc))
        return None

    # We take only the first record, if it exists
    try:
        record = event["Records"][0]
    except IndexError:
        message = "Invalid queue message, no records found"
        send_pipeline_signal(signal_queue_url, TestResult.FAILURE, message, context)
        return None

    # We extract the body, and attempt to convert from JSON
    try:
        body = json.loads(record["body"])
    except json.JSONDecodeError:
        message = "Record body was not valid JSON"
        send_pipeline_signal(signal_queue_url, TestResult.FAILURE, message, context)
        return None

    # We then attempt to extract an InvokeEvent instance from
    # the JSON body
    try:
        invoke_event = InvokeEvent(**body)
    except TypeError as ex:
        message = f"Message body missing required keys: {ex!s}"
        send_pipeline_signal(signal_queue_url, TestResult.FAILURE, message, context)
        return None

    # Assuming we get this far, invoke_event should have the information
    # required to run the lambda:
    try:
        cluster_id = get_ssm_parameter(
            f"/bfd/{environment}/common/nonsensitive/rds_cluster_identifier"
        )
        username = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/db/username", with_decrypt=True
        )
        raw_password = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/db/password", with_decrypt=True
        )
        cert_key = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/server_regression_key",
            with_decrypt=True,
        )
        cert = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/server_regression_cert",
            with_decrypt=True,
        )
    except ValueError as exc:
        send_pipeline_signal(
            signal_queue_url=signal_queue_url,
            result=TestResult.FAILURE,
            message=str(exc),
            context=context,
        )
        return None

    cert_path = "/tmp/bfd_test_cert.pem"
    with Path(cert_path).open("w", encoding="utf-8") as file:
        file.write(cert_key + cert)

    password = urllib.parse.quote(raw_password)
    try:
        db_uri = get_rds_db_uri(cluster_id)
    except ValueError as exc:
        send_pipeline_signal(
            signal_queue_url=signal_queue_url,
            result=TestResult.FAILURE,
            message=str(exc),
            context=context,
        )
        return None

    db_dsn = f"postgres://{username}:{password}@{db_uri}:5432/fhirdb"

    store_tag_args = [f"--stats-store-tag={tag}" for tag in invoke_event.store_tags]

    # Prepare to run the regression test suite
    locust_files = ["regression_suite.py", "regression_suite_post.py"]

    for locust_file in locust_files:
        # The command to run Locust with the current test file
        regression_process = subprocess.run(
            [
                "locust",
                f"--locustfile={lambda_task_root}/app/{invoke_event.suite_version}/{locust_file}",
                f"--host={invoke_event.host}",
                f"--users={invoke_event.users}",
                f"--spawn-rate={invoke_event.spawn_rate}",
                f"--spawned-runtime={invoke_event.spawned_runtime}",
                f"--database-connection-string={db_dsn}",
                f"--client-cert-path={cert_path}",
                "--stats-store-s3",
                f"--stats-env={environment}",
                f"--stats-store-s3-bucket={s3_bucket}",
                f"--stats-store-s3-database=bfd-insights-bfd-{environment}",
                f"--stats-store-s3-table=bfd_insights_bfd_{environment.replace('-', '_')}_server_regression",
                "--stats-compare-average",
                f"--stats-compare-tag={invoke_event.compare_tag}",
                f"--stats-compare-meta-file={lambda_task_root}/app/config/regression_suites_compare_meta.json",
                "--headless",
                "--only-summary",
                *store_tag_args,
            ],
            text=True,
            check=False,
        )

        regression_suite_succeeded = regression_process.returncode == 0

        # Signal the outcome of the locust test run
        send_pipeline_signal(
            signal_queue_url=signal_queue_url,
            result=TestResult.SUCCESS if regression_suite_succeeded else TestResult.FAILURE,
            message="Pipeline run finished, check the CloudWatch logs for more information",
            context=context,
        )

        return regression_process.stdout
    return None
