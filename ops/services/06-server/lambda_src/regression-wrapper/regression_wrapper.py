"""Source code for regression-wrapper Lambda.

This Lambda acts as a proxy between the existing Regression Suite Lambda, which utilizes SQS Queues,
and ECS CodeDeploy Hook invocations. Simply, this Lambda receives the CodeDeploy event, starts the
Regression Suite Lambda by posting a message on its invocation queue, and then waits for a response
on the result queue.
"""

import os
import time
from datetime import UTC, datetime, timedelta
from enum import StrEnum
from typing import Any

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import CodeDeployLifecycleHookEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from pydantic import BaseModel

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
LOCUST_HOST = os.environ.get("LOCUST_HOST", "")
INVOKE_SQS_QUEUE = os.environ.get("INVOKE_SQS_QUEUE", "")
RESULT_SQS_QUEUE = os.environ.get("RESULT_SQS_QUEUE", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logger = Logger()


class ServerRegressionInvokeModel(BaseModel):
    host: str
    suite_version: str
    spawn_rate: int
    users: int
    spawned_runtime: str
    compare_tag: str
    store_tags: list[str]


class ServerRegressionResultState(StrEnum):
    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"


class ServerRegressionResultModel(BaseModel):
    function_name: str
    result: "ServerRegressionResultState"
    message: str
    request_id: str
    log_stream_name: str
    log_group_name: str


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext) -> None:  # noqa: ARG001
    codedeploy_event = CodeDeployLifecycleHookEvent(event)

    sqs_client = boto3.client("sqs", config=BOTO_CONFIG)
    codedeploy_client = boto3.client("codedeploy", config=BOTO_CONFIG)
    try:
        if not all([REGION, LOCUST_HOST, BFD_ENVIRONMENT, INVOKE_SQS_QUEUE, RESULT_SQS_QUEUE]):
            raise RuntimeError("Not all necessary environment variables were defined")

        # This is unfortunately necessary as there appears to be a delay between NLBs indicating
        # that a listener has switched to a new Target Group and it actually happening. 3 minutes
        # seems to be the maxiumm amount of time it takes an NLB to properly register a new TG See
        # https://github.com/aws/containers-roadmap/issues/470
        logger.info(
            "Started for deployment ID '%s'. Waiting 3 minutes to allow test listener time to be "
            "available...",
            codedeploy_event.deployment_id,
        )
        time.sleep(3 * 60)

        # Post message to invoke queue to start the Regression Suite test
        logger.info("Starting Regression Suite test by sending to %s...", INVOKE_SQS_QUEUE)
        compare_tag = (
            "ecs_release" if BFD_ENVIRONMENT in ["test", "prod", "prod-sbx"] else BFD_ENVIRONMENT
        )
        sqs_client.send_message(
            QueueUrl=INVOKE_SQS_QUEUE,
            MessageBody=ServerRegressionInvokeModel(
                host=LOCUST_HOST,
                suite_version="v2",
                spawn_rate=10,
                users=10,
                spawned_runtime="30s",
                compare_tag=compare_tag,
                store_tags=[
                    compare_tag,
                    f"deploy_id__{codedeploy_event.deployment_id.lower().replace('-', '_')}",
                ],
            ).model_dump_json(),
        )

        logger.info("Invocation message sent to %s, waiting 90s for response...", INVOKE_SQS_QUEUE)
        sqs_client.purge_queue(QueueUrl=RESULT_SQS_QUEUE)
        end_time = datetime.now(UTC) + timedelta(seconds=90)
        result_message: str | None = None
        while datetime.now(UTC) <= end_time:
            result_message = next(
                (
                    x["Body"]
                    for x in sqs_client.receive_message(
                        QueueUrl=RESULT_SQS_QUEUE, WaitTimeSeconds=1
                    ).get("Messages", [])
                    if "Body" in x
                ),
                None,
            )
            if result_message:
                break

        if result_message is None:
            raise RuntimeError("No regression suite result returned within wait time")

        result = ServerRegressionResultModel.model_validate_json(result_message)
        logger.info("Received %s from %s", result.model_dump_json(), RESULT_SQS_QUEUE)

        codedeploy_result_status = (
            "Succeeded" if result.result == ServerRegressionResultState.SUCCESS else "Failed"
        )
        logger.info(
            "Putting event hook with status '%s' to CodeDeploy...", codedeploy_result_status
        )
        codedeploy_client.put_lifecycle_event_hook_execution_status(
            deploymentId=codedeploy_event.deployment_id,
            lifecycleEventHookExecutionId=codedeploy_event.lifecycle_event_hook_execution_id,
            status=codedeploy_result_status,
        )
        logger.info("Status '%s' sent to CodeDeploy", codedeploy_result_status)
    except Exception:
        logger.error("Exception encountered, signaling failure to CodeDeploy...")
        codedeploy_client.put_lifecycle_event_hook_execution_status(
            deploymentId=codedeploy_event.deployment_id,
            lifecycleEventHookExecutionId=codedeploy_event.lifecycle_event_hook_execution_id,
            status="Failed",
        )
        logger.exception("Unrecoverable exception raised")
        raise
