"""
A lambda function that starts a worker node which coordinates tests between a swarm of worker nodes.
This is a modified version of the `server-regression` lambda.
"""

import asyncio
import os
import urllib.parse
from dataclasses import dataclass
from typing import Any, List, Optional

import boto3
from botocore.config import Config

environment = os.environ.get("BFD_ENVIRONMENT", "test")
region = os.environ.get("AWS_DEFAULT_REGION", "us-east-1")
sqs_queue_name = os.environ.get("SQS_QUEUE_NAME", "bfd-test-server-load")

boto_config = Config(region_name=region)
ssm_client = boto3.client("ssm", config=boto_config)
rds_client = boto3.client("rds", config=boto_config)
sqs = boto3.resource("sqs", config=boto_config)
queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)


@dataclass
class InvokeEvent:
    """
    Values contained in the event object passed to the handler function on invocation.
    """

    host: str
    controller_ip: str
    locust_port: int = 5557


def get_ssm_parameter(name: str, with_decrypt: bool = False) -> Optional[str]:
    """
    Gets a named SSM parameter.
    """
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]
    except KeyError:
        print(f'SSM parameter "{name}" not found or empty')
        return None


def get_rds_db_uri(cluster_id: str) -> Optional[str]:
    """
    Gets the URI for the reader instance.
    """
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]
    except KeyError:
        print(f'DB URI not found for cluster ID "{cluster_id}"')
        return None


def check_queue(timeout: int = 1) -> List[Any]:
    """
    Checks SQS queue for messages.
    """
    # TODO: Make sure to not remove message from queue
    response = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    return response


def handler(event, context):
    """
    Handles execution of a worker node.
    """

    print("Preparing to run async worker...")
    asyncio.run(run_locust(event))
    print("Async worker terminated.")


async def run_locust(event):

    # We then attempt to extract an InvokeEvent instance from
    # the JSON body
    try:
        invoke_event = InvokeEvent(**event)
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

    print(
        f"Attempting to start locust with host: {invoke_event.host}, "
        f"master-host: {invoke_event.controller_ip}, "
        f"master-port: {invoke_event.locust_port}"
    )

    process = await asyncio.create_subprocess_exec(
        "locust",
        "--locustfile=/var/task/high_volume_suite.py",
        f"--host={invoke_event.host}",
        f"--database-uri={db_dsn}",
        f"--client-cert-path={cert_path}",
        "--worker",
        f"--master-host={invoke_event.controller_ip}",
        f"--master-port={invoke_event.locust_port}",
        "--headless",
        "--only-summary",
    )

    print(f"Started locust worker with pid {process.pid}")

    scaling_event = []
    while not scaling_event:
        scaling_event = check_queue(timeout=1)

    print("Scaling event detected, terminating.")
    print(f"Scaling event detected was: {scaling_event[0]}")

    try:
        process.terminate()
    except ProcessLookupError as e:
        print("Could not terminate subprocess")
        print(f"Received exception {e}")

    await process.wait()

    # Accessing a protected member on purpose to work around known problem with
    # orphaned processes in asyncio.
    # If the process is already closed, this is a noop.
    # pylint: disable=protected-access
    process._transport.close()
