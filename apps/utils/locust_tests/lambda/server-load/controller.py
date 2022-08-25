"""
A lambda function that starts a controller node which coordinates tests between worker nodes.
This is a modified version of the `server-regression` lambda.
"""

import json
import os
import socket
import subprocess
from dataclasses import dataclass

import boto3
from botocore.config import Config

ip_address = socket.gethostbyname(socket.gethostname())

environment = os.environ.get("BFD_ENVIRONMENT", "test")
locust_port = os.environ.get("LOCUST_PORT", "5557")
sqs_queue_name = os.environ.get("SQS_QUEUE_NAME")

boto_config = Config(region_name="us-east-1")
sqs = boto3.resource("sqs", config=boto_config)

queue = sqs.get_queue_by_name(QueueName=sqs_queue_name)


@dataclass
class InvokeEvent:
    """
    Values contained in the event object passed to the handler function on invocation.
    """

    host: str
    users: int


def handler(event, context):
    """
    Handles execution of a controller node.
    """
    try:
        invoke_event = InvokeEvent(**event)
    except TypeError as ex:
        print(f"Message body missing required keys: {str(ex)}")
        return None

    message_body = json.dumps({"ip_address": ip_address})

    queue.send_message(MessageBody=message_body, DelaySeconds=1)

    process = subprocess.run(
        [
            "locust",
            "--locustfile=/var/task/high_volume_suite.py",
            f"--host={invoke_event.host}",
            f"--users={invoke_event.users}",
            "--master",
            f"--master-bind-port={locust_port}",
            "--headless",
            "--only-summary",
        ],
        text=True,
        check=False,
    )

    return process.stdout
