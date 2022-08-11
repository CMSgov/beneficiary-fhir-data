"""
A lambda function that starts the load test controller and then periodically launches worker nodes
until a scaling event occurs.
"""
import json
import os
from dataclasses import dataclass
from typing import Any, List, Optional

import boto3
from botocore.config import Config


@dataclass
class ControllerResponse:
    """
    A response from a successfully invoked controller node.
    """

    ip_address: str


environment = os.environ.get("BFD_ENVIRONMENT", "test")
sqs_queue_name = os.environ.get("SQS_QUEUE_NAME")
# TODO: Would this be better using an ARN instead?
controller_lambda_name = os.environ.get("CONTROLLER_LAMBDA_NAME")
node_lambda_name = os.environ.get("NODE_LAMBDA_NAME")

boto_config = Config(region_name="us-east-1")

ssm = boto3.resource("ssm", config=boto_config)
sqs = boto3.resource("sqs", config=boto_config)
lambda_client = boto3.client("lambda", config=boto_config)

queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)


def start_controller():
    """
    Invokes the lambda function that runs the main Locust test instance.
    """
    response = lambda_client.invoke(FunctionName=controller_lambda_name, InvocationType="Event")
    if response.StatusCode != 202:
        print(
            f"An error occurred while trying to start the '{controller_lambda_name}' function:"
            f"{response.FunctionError}"
        )
        return None
    # TODO: define useful return value
    return response


def start_worker(controller_ip: str):
    """
    Invokes the lambda function that runs a Locust worker process.
    """
    response = lambda_client.invoke(
        FunctionName=node_lambda_name,
        InvocationType="Event",
        Payload=f'{"controller_ip": controller_ip}',
    )
    if response.StatusCode != 202:
        print(
            f"An error occurred while trying to start the '{node_lambda_name}' function:"
            f"{response.FunctionError}"
        )
        return None
    # TODO: define useful return value
    return response


def check_queue(timeout: int = 1) -> Optional[List[Any]]:
    """
    Checks SQS queue for messages.
    """
    response = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    if len(response["Messages"]) == 0:
        return None

    return response["Messages"]


def handler(event, context):
    """
    Lambda function handler.
    """

    start_controller()

    messages = None
    while not messages:
        # Keep checking for messages with a five second wait time.
        messages = check_queue(timeout=5)

    message = messages[0]

    try:
        body = json.loads(message["body"])
    except json.JSONDecodeError:
        print("Message body was not valid JSON")
        return

    try:
        controller_response = ControllerResponse(**body)
    except TypeError as ex:
        print(f"Message body missing required keys: {str(ex)}")
        return

    scaling_event = None
    while not scaling_event:
        start_worker(controller_ip=controller_response.ip_address)
        # Check for a scaling event
        scaling_event = check_queue(timeout=1)

    return
