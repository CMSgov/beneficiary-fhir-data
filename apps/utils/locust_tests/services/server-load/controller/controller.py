"""
A lambda function that starts the load test controller and then periodically launches worker nodes
until a scaling event occurs.
"""
import functools
import json
import os
import socket
import subprocess
import sys
import time
import urllib.parse
from datetime import datetime, timedelta

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

# Default all prints to flush immediately so that print statements are immediately logged to STDOUT
# instead of waiting to flush the buffer once the Locust process is finished.
# See https://stackoverflow.com/a/35467658
print = functools.partial(print, flush=True)  # pylint: disable=redefined-builtin

environment = os.environ.get("BFD_ENVIRONMENT", "test")
sqs_queue_name = os.environ.get("SQS_QUEUE_NAME", "bfd-test-server-load")
node_lambda_name = os.environ.get("NODE_LAMBDA_NAME", "bfd-test-server-load-node")
asg_name = os.environ.get("ASG_NAME", "")
test_host = os.environ.get("TEST_HOST", "https://test.bfd.cms.gov")
region = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
# Default dangerous variables to values that will not cause any issues
initial_worker_nodes = int(os.environ.get("INITIAL_WORKER_NODES", 0))
node_spawn_time = int(os.environ.get("NODE_SPAWN_TIME", 10))
max_spawned_nodes = int(os.environ.get("MAX_SPAWNED_NODES", 0))
max_users = int(os.environ.get("MAX_SPAWNED_USERS", 0))
user_spawn_rate = int(os.environ.get("USER_SPAWN_RATE", 1))
runtime_limit = int(os.environ.get("TEST_RUNTIME_LIMIT", 0))
coasting_time = int(os.environ.get("COASTING_TIME", 0))
warm_instance_target = int(os.environ.get("WARM_INSTANCE_TARGET", 0))
stop_on_scaling = to_bool(os.environ.get("STOP_ON_SCALING", True))
stop_on_node_limit = to_bool(os.environ.get("STOP_ON_NODE_LIMIT", True))

boto_config = Config(region_name=region)

ssm_client = boto3.client("ssm", config=boto_config)
rds_client = boto3.client("rds", config=boto_config)
autoscaling_client = boto3.client("autoscaling", config=boto_config)
sqs = boto3.resource("sqs", config=boto_config)
lambda_client = boto3.client("lambda", config=boto_config)


def _start_node(controller_ip: str, host: str):
    """
    Invokes the lambda function that runs a Locust worker node process.
    """
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


def _main():
    try:
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
    except ValueError as exc:
        print(exc)
        return

    password = urllib.parse.quote(raw_password)
    try:
        db_uri = get_rds_db_uri(rds_client=rds_client, cluster_id=cluster_id)
    except ValueError as exc:
        print(exc)
        return

    db_dsn = f"postgres://{username}:{password}@{db_uri}:5432/fhirdb"
    locust_process = subprocess.Popen(
        [
            "locust",
            "--locustfile=high_volume_suite.py",
            f"--host={test_host}",
            f"--users={max_users}",
            f"--spawn-rate={user_spawn_rate}",
            f"--database-uri={db_dsn}",
            "--master",
            "--master-bind-port=5557",
            f"--expect-workers={initial_worker_nodes}",
            "--client-cert-path=/tmp/bfd_test_cert.pem",
            "--enable-rebalancing",
            "--loglevel=DEBUG",
            "--csv=load",
            "--headless",
        ],
        cwd="../../../",
        stderr=subprocess.STDOUT,
    )

    # Get the SQS queue and purge it of any possible stale messages.
    queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)
    queue.purge()

    ip_address = socket.gethostbyname(socket.gethostname())

    spawn_count = 0
    for _ in range(0, initial_worker_nodes):
        print(f"Spawning initial worker node #{spawn_count + 1} of {max_spawned_nodes}...")
        _start_node(controller_ip=ip_address, host=test_host)
        spawn_count += 1
        print(f"Spawned initial worker node #{spawn_count} successfully")

    runtime_limit_end = datetime.now() + timedelta(seconds=runtime_limit)
    next_node_spawn = (
        datetime.now()
        if spawn_count == 0
        else datetime.now() + timedelta(seconds=node_spawn_time + 1)
    )
    while locust_process.returncode is None:
        if datetime.now() >= runtime_limit_end:
            print(f"User provided runtime of {runtime_limit} seconds has been exceeded, stopping")
            break

        scale_or_stop_events = check_queue(
            queue=queue,
            timeout=1,
        )

        if any(
            filter_message_by_keys(msg, [QUEUE_STOP_SIGNAL_FILTER]) for msg in scale_or_stop_events
        ):
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

        if spawn_count < max_spawned_nodes:
            if datetime.now() >= next_node_spawn:
                print(f"Spawning worker node #{spawn_count + 1} of {max_spawned_nodes}...")
                _start_node(controller_ip=ip_address, host=test_host)
                spawn_count += 1
                print(f"Worker node #{spawn_count} spawned successfully")

                next_node_spawn = datetime.now() + timedelta(seconds=node_spawn_time)
        elif stop_on_node_limit:
            print(f"Worker node spawn limit of {max_spawned_nodes} encountered, stopping...")
            break

    # Unconditionally send a stop signal to the queue to force all nodes to stop
    print("Sending stop signal to remaining nodes...")
    queue.send_message(MessageBody=json.dumps(QUEUE_STOP_SIGNAL_FILTER))
    print("Stop signal sent successfully")

    if locust_process.returncode:
        # If returncode is not None, then the locust process has finished on its own and we do not
        # need to end it manually
        print("Locust master process ended without intervention, stopping...")
        return

    # Sleep for the coasting time plus an additional 10 seconds before forcing the master
    # process to end.
    print(f"Coasting for {coasting_time + 10} seconds before stopping...")
    time.sleep(int(coasting_time) + 10)
    print("Coasting time complete")

    print("Stopping Locust master process...")
    locust_process.wait()
    try:
        locust_process.terminate()
    except ProcessLookupError as e:
        print("Could not terminate Locust master subprocess")
        print(f"Received exception {e}")

    print("Locust master process has been stopped")


if __name__ == "__main__":
    _main()
