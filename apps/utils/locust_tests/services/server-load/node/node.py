"""
A lambda function that starts a worker node which coordinates tests between a swarm of worker nodes.
This is a modified version of the `server-regression` lambda.
"""

import asyncio
import os
import sys
import time
import urllib.parse
from dataclasses import dataclass

import boto3
from botocore.config import Config

sys.path.append("..")  # Allows for module imports from sibling directories
from common.boto_utils import (
    check_queue,
    get_rds_db_uri,
    get_ssm_parameter,
    get_warm_pool_count,
)
from common.convert_utils import to_bool
from common.message_filters import (
    QUEUE_STOP_SIGNAL_FILTER,
    WARM_POOL_INSTANCE_LAUNCH_FILTER,
    filter_message_by_keys,
)

environment = os.environ.get("BFD_ENVIRONMENT", "test")
region = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
sqs_queue_name = os.environ.get("SQS_QUEUE_NAME", "bfd-test-server-load")
asg_name = os.environ.get("ASG_NAME", "")
# Default dangerous variables to values that will not cause any issues
coasting_time = int(os.environ.get("COASTING_TIME", 0))
warm_instance_target = int(os.environ.get("WARM_INSTANCE_TARGET", 0))
stop_on_scaling = to_bool(os.environ.get("STOP_ON_SCALING", True))

boto_config = Config(region_name=region)
ssm_client = boto3.client("ssm", config=boto_config)
rds_client = boto3.client("rds", config=boto_config)
autoscaling_client = boto3.client("autoscaling", config=boto_config)
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

    try:
        # Assuming we get this far, invoke_event should have the information
        # required to run the lambda:
        cluster_id = get_ssm_parameter(
            ssm_client=ssm_client,
            name=f"/bfd/{environment}/common/nonsensitive/rds_cluster_identifier",
        )
        username = get_ssm_parameter(
            ssm_client=ssm_client,
            name=f"/bfd/{environment}/server/sensitive/vault_data_server_db_username",
            with_decrypt=True,
        )
        raw_password = get_ssm_parameter(
            ssm_client=ssm_client,
            name=f"/bfd/{environment}/server/sensitive/vault_data_server_db_password",
            with_decrypt=True,
        )
        cert_key = get_ssm_parameter(
            ssm_client=ssm_client,
            name=f"/bfd/{environment}/server/sensitive/test_client_key",
            with_decrypt=True,
        )
        cert = get_ssm_parameter(
            ssm_client=ssm_client,
            name=f"/bfd/{environment}/server/sensitive/test_client_cert",
            with_decrypt=True,
        )
    except ValueError as exc:
        print(exc)
        return

    cert_path = "/tmp/bfd_test_cert.pem"
    with open(cert_path, "w", encoding="utf-8") as file:
        file.write(cert_key + cert)

    password = urllib.parse.quote(raw_password)
    try:
        db_uri = get_rds_db_uri(rds_client=rds_client, clusterd_id=cluster_id)
    except ValueError as exc:
        print(exc)
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
        cwd="/var/task",
    )

    print(f"Started locust worker with pid {process.pid}")

    has_received_stop = False
    while process.returncode is None:
        scale_or_stop_events = check_queue(
            queue=queue,
            timeout=1,
        )

        if any(
            filter_message_by_keys(msg, [QUEUE_STOP_SIGNAL_FILTER]) for msg in scale_or_stop_events
        ):
            has_received_stop = True
            print("Stop signal encountered, stopping")
            break

        if (
            stop_on_scaling
            and any(
                filter_message_by_keys(msg, [WARM_POOL_INSTANCE_LAUNCH_FILTER])
                for msg in scale_or_stop_events
            )
            and get_warm_pool_count(autoscaling_client=autoscaling_client, asg_name=asg_name)
            >= warm_instance_target
        ):
            print(
                f"Scaling target of {warm_instance_target} instances in {asg_name} has"
                " been hit, stopping"
            )
            break

    if process.returncode:
        # If returncode is not None, then the locust process has finished on its own and we do not
        # need to end it manually
        print("Locust worker process ended without intervention, stopping...")
        return

    if not has_received_stop and coasting_time > 0:
        print(f"Coasting for {coasting_time} seconds before stopping...")
        time.sleep(int(coasting_time))
        print("Coasting time complete")

    print("Terminating worker node...")
    try:
        process.terminate()
    except ProcessLookupError as e:
        print("Could not terminate Locust worker subprocess")
        print(f"Received exception {e}")

    await process.wait()

    # Accessing a protected member on purpose to work around known problem with
    # orphaned processes in asyncio.
    # If the process is already closed, this is a noop.
    # pylint: disable=protected-access
    process._transport.close()

    print("Locust worker node process has been stopped")
