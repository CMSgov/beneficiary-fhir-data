"""
A lambda function that starts the load test controller and then periodically launches worker nodes
until a scaling event occurs.
"""
import json
import os
import socket
from typing import Any, List

import boto3
from botocore.config import Config


def start_node(controller_ip: str, host: str):
    """
    Invokes the lambda function that runs a Locust worker node process.
    """
    # TODO get ip address of local node, use an environment variable for host, informed by ssm lookup in terraform or... similar.

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
    # TODO: define useful return value
    return response


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


if __name__ == "__main__":
    # TODO ensure that we're setting up the envvars correctly
    environment = os.environ.get("BFD_ENVIRONMENT", "test")
    sqs_queue_name = os.environ.get("SQS_QUEUE_NAME", "bfd-test-server-load")
    node_lambda_name = os.environ.get("NODE_LAMBDA_NAME", "bfd-test-server-load-node")
    test_host = os.environ.get("TEST_HOST", "https://test.bfd.cms.gov")
    region = os.environ.get("AWS_DEFAULT_REGION", "us-east-1")

    boto_config = Config(region_name=region)

    sqs = boto3.resource("sqs", config=boto_config)
    lambda_client = boto3.client("lambda", config=boto_config)

    # Get the SQS queue and purge it of any possible stale messages.
    queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)
    queue.purge()

    ip_address = socket.gethostbyname(socket.gethostname())

    scaling_event = []
    while not scaling_event:
        start_node(controller_ip=ip_address, host=test_host)
        # TODO: Make this timeout configurable to allow faster spawning.
        scaling_event = check_queue(timeout=10)
