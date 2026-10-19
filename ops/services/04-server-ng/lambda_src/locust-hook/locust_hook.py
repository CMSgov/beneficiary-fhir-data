import os
from enum import StrEnum, auto
from typing import Any

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parser import parse
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from pydantic import BaseModel, Field

SUCCEEDED = "SUCCEEDED"
FAILED = "FAILED"

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
    # Double the read timeout for some extra safety when synchronously invoking the run-locust
    # Lambda
    read_timeout=120,
)

logger = Logger()


class EcsDeploymentEventExecutionDetailsModel(BaseModel):
    service_arn: str = Field(validation_alias="serviceArn")
    target_service_revision_arn: str = Field(validation_alias="targetServiceRevisionArn")
    test_traffic_weights: dict[str, int] = Field(validation_alias="testTrafficWeights")
    production_traffic_weights: dict[str, int] = Field(validation_alias="productionTrafficWeights")


class EcsServiceDeploymentEventModel(BaseModel):
    execution_id: str = Field(validation_alias="executionId")
    lifecycle_stage: str = Field(validation_alias="lifecycleStage")
    resource_arn: str = Field(validation_alias="resourceArn")
    execution_details: EcsDeploymentEventExecutionDetailsModel = Field(
        validation_alias="executionDetails"
    )
    hook_details: dict[str, object] | None = Field(validation_alias="hookDetails", default=None)


class CompareType(StrEnum):
    PREVOUS = auto()
    AVERAGE = auto()


class CompareConfigModel(BaseModel):
    type: CompareType
    tag: str
    load_limit: int


class StoreConfigModel(BaseModel):
    tags: list[str]


class ServerNgRegressionInvokeModel(BaseModel):
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
def handler(event: dict[Any, Any], context: LambdaContext) -> dict[str, str]:  # noqa: ARG001
    deployment_event = parse(event=event, model=EcsServiceDeploymentEventModel)

    lambda_client = boto3.client("lambda", config=BOTO_CONFIG)  # pyright: ignore[reportUnknownMemberType]
    hook_status: str
    try:
        if not all([REGION, LOCUST_HOST, BFD_ENVIRONMENT, RUN_LOCUST_LAMBDA_NAME]):
            raise RuntimeError("Not all necessary environment variables were defined")

        # Post message to invoke queue to start the Regression Suite test
        logger.info(
            "Starting Regression Suite test by invoking %s Lambda...", RUN_LOCUST_LAMBDA_NAME
        )
        compare_tag = (
            f"{BFD_ENVIRONMENT}_release"
            if BFD_ENVIRONMENT in ["test", "prod", "sandbox"]
            else BFD_ENVIRONMENT
        )
        deployment_id = deployment_event.resource_arn.split("/")[-1].lower().replace("-", "_")
        response = lambda_client.invoke(
            FunctionName=RUN_LOCUST_LAMBDA_NAME,
            InvocationType="RequestResponse",
            Payload=ServerNgRegressionInvokeModel(
                suite="v3/regression_suite.py",
                host=LOCUST_HOST,
                spawn_rate=10,
                users=10,
                spawned_runtime="30s",
                compare=CompareConfigModel(type=CompareType.AVERAGE, tag=compare_tag, load_limit=5),
                store=StoreConfigModel(
                    tags=[
                        compare_tag,
                        f"deploy_id__{deployment_id}",
                    ]
                ),
            ).model_dump_json(),
        )

        result = ResultModel.model_validate_json(response["Payload"].read())
        logger.info("Received %s from %s", result.model_dump_json(), RUN_LOCUST_LAMBDA_NAME)

        hook_status = SUCCEEDED if result.result == TestResult.SUCCESS else FAILED
    except Exception:
        logger.exception("Unrecoverable exception raised")
        hook_status = FAILED

    logger.info("Returning status '%s'...", hook_status)
    return {"hookStatus": hook_status}
