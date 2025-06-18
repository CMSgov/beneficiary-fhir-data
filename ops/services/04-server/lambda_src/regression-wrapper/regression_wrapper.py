"""Source code for regression-wrapper Lambda.

This Lambda acts as a proxy between the existing Regression Suite Lambda, which utilizes SQS Queues,
and ECS CodeDeploy Hook invocations. Simply, this Lambda receives the CodeDeploy event, starts the
Regression Suite Lambda by posting a message on its invocation queue, and then waits for a response
on the result queue.
"""

import os
import time
from enum import StrEnum, auto
from typing import Any

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import CodeDeployLifecycleHookEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from pydantic import BaseModel

REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", default="")
LOCUST_HOST = os.environ.get("LOCUST_HOST", default="")
RUN_LOCUST_LAMBDA_NAME = os.environ.get("RUN_LOCUST_LAMBDA_NAME", default="")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
    # Double the read timeout for some extra safety when synchrnously invoking the run-locust Lambda
    read_timeout=120,
)

logger = Logger()


class CompareType(StrEnum):
    PREVIOUS = auto()
    AVERAGE = auto()


class CompareConfigModel(BaseModel):
    type: CompareType
    tag: str
    load_limit: int


class StoreConfigModel(BaseModel):
    tags: list[str]


class ServerRegressionInvokeModel(BaseModel):
    suite: str
    host: str
    spawn_rate: int
    users: int
    spawned_runtime: str
    compare: CompareConfigModel
    store: StoreConfigModel


class TestResult(StrEnum):
    SUCCESS = auto()
    FAILURE = auto()


class ResultModel(BaseModel):
    result: TestResult
    message: str
    log_stream_name: str
    log_group_name: str


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext) -> None:  # noqa: ARG001
    codedeploy_event = CodeDeployLifecycleHookEvent(event)

    lambda_client = boto3.client("lambda", config=BOTO_CONFIG)
    codedeploy_client = boto3.client("codedeploy", config=BOTO_CONFIG)
    try:
        if not all([REGION, LOCUST_HOST, BFD_ENVIRONMENT, RUN_LOCUST_LAMBDA_NAME]):
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
        logger.info(
            "Starting Regression Suite test by invoking %s Lambda...", RUN_LOCUST_LAMBDA_NAME
        )
        compare_tag = (
            "ecs_release"
            if BFD_ENVIRONMENT in ["test", "prod", "prod-sbx", "sandbox"]
            else BFD_ENVIRONMENT
        )
        response = lambda_client.invoke(
            FunctionName=RUN_LOCUST_LAMBDA_NAME,
            InvocationType="RequestResponse",
            Payload=ServerRegressionInvokeModel(
                suite="v2/regression_suite_post.py",
                host=LOCUST_HOST,
                spawn_rate=10,
                users=10,
                spawned_runtime="30s",
                compare=CompareConfigModel(type=CompareType.AVERAGE, tag=compare_tag, load_limit=5),
                store=StoreConfigModel(
                    tags=[
                        compare_tag,
                        f"deploy_id__{codedeploy_event.deployment_id.lower().replace('-', '_')}",
                    ]
                ),
            ).model_dump_json(),
        )

        result = ResultModel.model_validate_json(response["Payload"].read())
        logger.info("Received %s from %s", result.model_dump_json(), RUN_LOCUST_LAMBDA_NAME)

        codedeploy_result_status = "Succeeded" if result.result == TestResult.SUCCESS else "Failed"
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
