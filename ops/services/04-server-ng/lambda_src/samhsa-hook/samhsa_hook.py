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
HOSTNAME = os.environ.get("HOSTNAME", default="")
SAMHSA_REGRESSION_LAMBDA_NAME = os.environ.get("SAMHSA_REGRESSION_LAMBDA_NAME", default="")
TABLESAMPLE = float(os.environ.get("TABLESAMPLE", default="10.0"))
LIMIT = int(os.environ.get("LIMIT", default="300"))
CONCURRENCY = int(os.environ.get("CONCURRENCY", default="10"))
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
    # The samhsa-regression Lambda has a 300 second timeout; give the synchronous invocation a
    # little extra room so the read does not time out before the work Lambda finishes
    read_timeout=360,
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


class SamhsaRegressionInvokeModel(BaseModel):
    host: str
    tablesample: float
    limit: int
    concurrency: int


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
    # Validate the shape of the ECS deployment event before doing any work
    parse(event=event, model=EcsServiceDeploymentEventModel)

    lambda_client = boto3.client("lambda", config=BOTO_CONFIG)  # pyright: ignore[reportUnknownMemberType]
    hook_status: str
    try:
        if not all([REGION, HOSTNAME, BFD_ENVIRONMENT, SAMHSA_REGRESSION_LAMBDA_NAME]):
            raise RuntimeError("Not all necessary environment variables were defined")

        # Synchronously invoke the samhsa-regression Lambda to run the SAMHSA regression suite
        # against the replacement tasks
        logger.info(
            "Starting SAMHSA regression test by invoking %s Lambda...",
            SAMHSA_REGRESSION_LAMBDA_NAME,
        )
        response = lambda_client.invoke(
            FunctionName=SAMHSA_REGRESSION_LAMBDA_NAME,
            InvocationType="RequestResponse",
            Payload=SamhsaRegressionInvokeModel(
                host=HOSTNAME,
                tablesample=TABLESAMPLE,
                limit=LIMIT,
                concurrency=CONCURRENCY,
            ).model_dump_json(),
        )

        result = ResultModel.model_validate_json(response["Payload"].read())
        logger.info("Received %s from %s", result.model_dump_json(), SAMHSA_REGRESSION_LAMBDA_NAME)

        hook_status = SUCCEEDED if result.result == TestResult.SUCCESS else FAILED
    except Exception:
        logger.exception("Unrecoverable exception raised")
        hook_status = FAILED

    logger.info("Returning status '%s'...", hook_status)
    return {"hookStatus": hook_status}
