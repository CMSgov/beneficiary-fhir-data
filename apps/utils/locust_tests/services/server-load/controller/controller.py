"""
A lambda function that starts the load test controller and then periodically launches worker nodes
until a scaling event occurs.
"""
import asyncio
import json
import os
import socket
import time
import urllib.parse

import boto3
from botocore.config import Config

from common.boto_utils import check_queue, get_rds_db_uri, get_ssm_parameter
from common.convert_utils import to_bool


def start_node(lambda_client, node_lambda_name: str, controller_ip: str, host: str):
    """
    Invokes the lambda function that runs a Locust worker node process.
    """
    # TODO: Properly type hint 'lambda_client'
    print(f"Starting node with host:{host}, controller_ip:{controller_ip}")
    payload_json = json.dumps({"controller_ip": controller_ip, "host": host})

    response = lambda_client.invoke(
        FunctionName=node_lambda_name,
        InvocationType="Event",
        Payload=payload_json,
    )
    if response["StatusCode"] != 202:
        print(
            f"An error occurred while trying to start the '{node_lambda_name}' function:"
            f"{response.FunctionError}"
        )
        return None

    return response


async def async_main():
    environment = os.environ.get("BFD_ENVIRONMENT", "test")
    sqs_queue_name = os.environ.get("SQS_QUEUE_NAME", "bfd-test-server-load")
    node_lambda_name = os.environ.get("NODE_LAMBDA_NAME", "bfd-test-server-load-node")
    test_host = os.environ.get("TEST_HOST", "https://test.bfd.cms.gov")
    region = os.environ.get("AWS_DEFAULT_REGION", "us-east-1")
    initial_worker_nodes = int(os.environ.get("INITIAL_WORKER_NODES", 0))
    node_spawn_time = int(os.environ.get("NODE_SPAWN_TIME", 10))
    # Default maximum of 80 spawned nodes _should_ be sufficient to cause scaling.
    # This may need some adjustment, but should be a fine default.
    max_spawned_nodes = int(os.environ.get("MAX_SPAWNED_NODES", 80))
    max_users = int(os.environ.get("MAX_SPAWNED_USERS", 5000))
    user_spawn_rate = int(os.environ.get("USER_SPAWN_RATE", 1))
    runtime_limit = os.environ.get("TEST_RUNTIME_LIMIT", "10m30s")
    coasting_time = int(os.environ.get("COASTING_TIME", 10))
    warm_instance_target = int(os.environ.get("WARM_INSTANCE_TARGET", 7))
    stop_on_scaling = to_bool(os.environ.get("STOP_ON_SCALING", True))
    stop_on_node_limit = to_bool(os.environ.get("STOP_ON_NODE_LIMIT", True))

    boto_config = Config(region_name=region)

    sqs = boto3.resource("sqs", config=boto_config)
    lambda_client = boto3.client("lambda", config=boto_config)

    try:
        cluster_id = get_ssm_parameter(
            f"/bfd/{environment}/common/nonsensitive/rds_cluster_identifier"
        )
        username = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/vault_data_server_db_username", with_decrypt=True
        )
        raw_password = get_ssm_parameter(
            f"/bfd/{environment}/server/sensitive/vault_data_server_db_password", with_decrypt=True
        )
    except ValueError as exc:
        print(exc)
        return

    password = urllib.parse.quote(raw_password)
    try:
        db_uri = get_rds_db_uri(cluster_id)
    except ValueError as exc:
        print(exc)
        return

    db_dsn = f"postgres://{username}:{password}@{db_uri}:5432/fhirdb"
    locust_process = await asyncio.create_subprocess_exec(
        "locust",
        "--locustfile=high_volume_suite.py",
        f"--host={test_host}",
        f"--users={max_users}",
        f"--spawn-rate={user_spawn_rate}",
        f"--database-uri={db_dsn}",
        "--master",
        "--master-bind-port=5557",
        "--client-cert-path='tmp/bfd_test_cert.pem'",
        "--enable-rebalancing",
        "--logfile=locust.log",
        "--loglevel=DEBUG",
        "--csv=load",
        "--headless",
    )

    # Get the SQS queue and purge it of any possible stale messages.
    queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)
    queue.purge()

    ip_address = socket.gethostbyname(socket.gethostname())

    scaling_event = []
    spawn_count = 0
    while not scaling_event and spawn_count < max_spawned_nodes:
        start_node(
            lambda_client=lambda_client,
            node_lambda_name=node_lambda_name,
            controller_ip=ip_address,
            host=test_host,
        )
        scaling_event = check_queue(
            timeout=node_spawn_time, message_filter={"Origin": "EC2", "Destination": "WarmPool"}
        )
        spawn_count += 1

    # Sleep for the coasting time plus an additional 10 seconds before forcing the master process
    # to end
    time.sleep(int(coasting_time) + 10)

    if locust_process.returncode:
        # If returncode is not None, then the locust process has finished on its own and we do not
        # need to end it manually
        return

    try:
        locust_process.terminate()
    except ProcessLookupError as e:
        print("Could not terminate Locust master subprocess")
        print(f"Received exception {e}")

    await locust_process.wait()

    # Accessing a protected member on purpose to work around known problem with
    # orphaned processes in asyncio.
    # If the process is already closed, this is a noop.
    # pylint: disable=protected-access
    locust_process._transport.close()


if __name__ == "__main__":
    asyncio.run(async_main())
