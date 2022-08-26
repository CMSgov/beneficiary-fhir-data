"""
A lambda function that starts the load test controller and then periodically launches worker nodes
until a scaling event occurs.
"""
import json
import os
from dataclasses import asdict, dataclass
from typing import Any, List, Optional

import boto3
from botocore.config import Config


@dataclass
class ControllerResponse:
    """
    A response from a successfully invoked controller node.
    """

    ip_address: str


@dataclass
class InvokeEvent:
    """
    Values contained in the event object passed to the handler function on invocation.
    """

    host: str
    users: int


environment = os.environ.get("BFD_ENVIRONMENT", "test")
sqs_queue_name = os.environ.get("SQS_QUEUE_NAME")
controller_lambda_name = os.environ.get("CONTROLLER_LAMBDA_NAME")
node_lambda_name = os.environ.get("NODE_LAMBDA_NAME")

boto_config = Config(region_name="us-east-1")

sqs = boto3.resource("sqs", config=boto_config)
lambda_client = boto3.client("lambda", config=boto_config)

queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)


def start_controller(payload: InvokeEvent):
    """
    Invokes the lambda function that runs the main Locust test instance.

    """
    payload_json = json.dumps(asdict(payload))

    response = lambda_client.invoke(
        FunctionName=controller_lambda_name,
        InvocationType="Event",
        Payload=payload_json,
    )
    if response["StatusCode"] != 202:
        print(
            f"An error occurred while trying to start the '{controller_lambda_name}' function:"
            f"{response.FunctionError}"
        )
        return None
    # TODO: define useful return value
    return response


def start_worker(controller_ip: str, host: str):
    """
    Invokes the lambda function that runs a Locust worker process.
    """
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
    response = queue.receive_messages(
        AttributeNames=["SenderId", "SentTimestamp"],
        WaitTimeSeconds=timeout,
    )

    return response


def handler(event, context):
    """
    Lambda function handler.
    """

    # Purge the message queue before we get started.
    queue.purge()

    # We take only the first record, if it exists
    try:
        record = event["Records"][0]
    except IndexError:
        print("Invalid queue message, no records found")
        return None

    # We extract the body, and attempt to convert from JSON
    try:
        body = json.loads(record["body"])
    except json.JSONDecodeError:
        print("Record body was not valid JSON")
        return None

    # We then attempt to extract an InvokeEvent instance from
    # the JSON body
    try:
        invoke_event = InvokeEvent(**body)
    except TypeError as ex:
        print(f"Message body missing required keys: {str(ex)}")
        return None

    start_controller(payload=invoke_event)

    messages = []
    while not messages:
        # Keep checking for messages with a five second wait time.
        messages = check_queue(timeout=5)

    message = messages[0]

    try:
        body = json.loads(message.body)
    except json.JSONDecodeError:
        print("Message body was not valid JSON")
        return

    try:
        controller_response = ControllerResponse(**body)
    except TypeError as ex:
        print(f"Message body missing required keys: {str(ex)}")
        return

    scaling_event = []
    while not scaling_event:
        start_worker(controller_ip=controller_response.ip_address, host=invoke_event.host)
        # Check for a scaling event
        # TODO: Make timeout an environment variable with sane default
        scaling_event = check_queue(timeout=10)

    return
