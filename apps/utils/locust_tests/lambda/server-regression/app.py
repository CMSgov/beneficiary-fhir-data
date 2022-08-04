import json
import os
import subprocess
import urllib.parse
from dataclasses import asdict, dataclass
from typing import Optional

import boto3
from botocore.config import Config

environment = os.environ.get("BFD_ENVIRONMENT", "test")
s3_bucket = os.environ.get("INSIGHTS_BUCKET_NAME")
sqs_pipeline_signal = os.environ.get("SQS_PIPELINE_SIGNAL_NAME")

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
    store_tag: str


@dataclass
class PipelineSignalMessage:
    function_name: str
    result: str
    request_id: str
    log_stream_name: str
    log_group_name: str


def get_ssm_parameter(name: str, with_decrypt: bool = False) -> Optional[str]:
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]
    except KeyError:
        print(f'SSM parameter "{name}" not found or empty')
        return None


def get_rds_db_uri(cluster_id: str) -> Optional[str]:
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]
    except KeyError:
        print(f'DB URI not found for cluster ID "{cluster_id}"')
        return None


def get_sqs_queue_url(sqs_queue_name: str) -> Optional[str]:
    response = sqs_client.get_queue_url(QueueName=sqs_queue_name)

    try:
        return response["QueueUrl"]
    except KeyError:
        print(f'SQS Queue URL not found for queue "{sqs_queue_name}"')
        return None


def send_sqs_message(sqs_queue_url: str, msg_body: PipelineSignalMessage) -> bool:
    try:
        sqs_client.send_message(QueueUrl=sqs_queue_url, MessageBody=json.dumps(asdict(msg_body)))
        return True
    except sqs_client.exceptions.UnsupportedOperation:
        print(f'Unable to post message to queue at URL "{sqs_queue_url}"')
        return False


def handler(event, context):
    if not s3_bucket or not sqs_pipeline_signal:
        print(
            '"INSIGHTS_BUCKET_NAME" and "SQS_PIPELINE_SIGNAL_NAME" environment variables must be specified'
        )
        return

    signal_queue_url = get_sqs_queue_url(sqs_pipeline_signal)
    if not signal_queue_url:
        return

    # We take only the first record, if it exists
    try:
        record = event["Records"][0]
    except IndexError:
        print("Invalid queue message, no records found")
        return

    # We extract the body, and attempt to convert from JSON
    try:
        body = json.loads(record["body"])
    except json.JSONDecodeError:
        print("Record body was not valid JSON")
        return

    # We then attempt to extract an InvokeEvent instance from
    # the JSON body
    try:
        invoke_event = InvokeEvent(**body)
    except TypeError as ex:
        print(f"Message body missing required keys: {str(ex)}")
        return

    # Assuming we get this far, invoke_event should have the information
    # required to run the lambda:
    cluster_id = get_ssm_parameter(f"/bfd/{environment}/common/nonsensitive/rds_cluster_identifier")
    username = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/vault_data_server_db_username", with_decrypt=True
    )
    raw_password = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/vault_data_server_db_password", with_decrypt=True
    )
    cert_key = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/test_client_key", with_decrypt=True
    )
    cert = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/test_client_cert", with_decrypt=True
    )

    if not cluster_id or not username or not raw_password or not cert_key or not cert:
        return

    cert_path = "/tmp/bfd_test_cert.pem"
    with open(cert_path, "w", encoding="utf-8") as file:
        file.write(cert_key + cert)

    password = urllib.parse.quote(raw_password)
    db_uri = get_rds_db_uri(cluster_id)

    if not db_uri:
        return

    db_dsn = f"postgres://{username}:{password}@{db_uri}:5432/fhirdb"

    stats_config = {
        "store": "s3",
        "env": environment,
        "compare": "previous",
        "store_tag": invoke_event.store_tag,
        "comp_tag": invoke_event.compare_tag,
        "bucket": s3_bucket,
        "database": f"bfd-insights-bfd-{environment}",
        "table": f"bfd_insights_bfd_{environment.replace('-', '_')}_server_regression",
    }
    stats_config_str = ";".join([f"{k}={str(v)}" for k, v in stats_config.items()])
    process = subprocess.run(
        [
            "locust",
            f"--locustfile=/var/task/{invoke_event.suite_version}/regression_suite.py",
            f"--host={invoke_event.host}",
            f"--users={invoke_event.users}",
            f"--spawn-rate={invoke_event.spawn_rate}",
            f"--spawned-runtime={invoke_event.spawned_runtime}",
            f"--database-uri={db_dsn}",
            f"--client-cert-path={cert_path}",
            f"--stats-config={stats_config_str}",
            "--headless",
            "--only-summary",
        ],
        text=True,
        check=False,
    )

    # Signal the outcome of the locust test run
    pipeline_signal_msg = PipelineSignalMessage(
        function_name=context.function_name,
        result="SUCCESS" if process.returncode == 0 else "FAILURE",
        request_id=context.aws_request_id,
        log_stream_name=context.log_stream_name,
        log_group_name=context.log_group_name,
    )
    send_sqs_message(signal_queue_url, pipeline_signal_msg)

    return process.stdout
