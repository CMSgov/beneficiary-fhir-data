"""Lambda event handler for the SAMHSA regression test suite.

This handler is invoked (synchronously) by the `samhsa-hook` wrapper Lambda during an ECS
service deployment. It receives a typed invoke payload, runs the SAMHSA filtering regression
test against the replacement tasks, and returns a result model that the wrapper converts into
an ECS `hookStatus`.
"""

import asyncio
import os
from enum import StrEnum, auto
from pathlib import Path
from typing import Any

import yaml
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parameters import (
    get_parameter,  # pyright: ignore[reportUnknownVariableType]
)
from aws_lambda_powertools.utilities.typing import LambdaContext
from pydantic import BaseModel, Field, TypeAdapter

from app.main import DatabaseDetailsModel, SecurityLabelModel, run_samhsa_regression

BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", default="").lower()
READER_ENDPOINT = os.environ.get("READER_ENDPOINT", default="")
# The security labels YAML is bundled into the Lambda image alongside this module
SECURITY_LABELS_PATH = Path(__file__).with_name("security_labels.yml")

logger = Logger()


class SamhsaRegressionInvokeModel(BaseModel):
    host: str
    tablesample: float = Field(default=10.0, ge=0.0, le=100.0)
    limit: int = Field(default=300, ge=1)
    concurrency: int = Field(default=10, ge=1)


class TestResult(StrEnum):
    SUCCESS = auto()
    FAILURE = auto()


class ResultModel(BaseModel):
    result: TestResult
    message: str
    log_stream_name: str
    log_group_name: str


def _get_db_details() -> DatabaseDetailsModel:
    username = get_parameter(
        f"/bfd/{BFD_ENVIRONMENT}/server-ng/sensitive/db/username", decrypt=True
    )
    password = get_parameter(
        f"/bfd/{BFD_ENVIRONMENT}/server-ng/sensitive/db/password", decrypt=True
    )

    return DatabaseDetailsModel(host=READER_ENDPOINT, user=username, password=password)


def _get_certificate_paths() -> tuple[Path, Path, Path, Path]:
    tmpdir = Path(os.environ.get("TMPDIR", "/tmp"))

    samhsa_cert_path = tmpdir / "samhsa_cert.pem"
    samhsa_key_path = tmpdir / "samhsa_key.pem"
    non_samhsa_cert_path = tmpdir / "non_samhsa_cert.pem"
    non_samhsa_key_path = tmpdir / "non_samhsa_key.pem"

    samhsa_cert_path.write_text(
        get_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/server-ng/sensitive/test_client_samhsa_cert", decrypt=True
        )
    )
    samhsa_key_path.write_text(
        get_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/server-ng/sensitive/test_client_samhsa_key", decrypt=True
        )
    )
    non_samhsa_cert_path.write_text(
        get_parameter(f"/bfd/{BFD_ENVIRONMENT}/server-ng/sensitive/test_client_cert", decrypt=True)
    )
    non_samhsa_key_path.write_text(
        get_parameter(f"/bfd/{BFD_ENVIRONMENT}/server-ng/sensitive/test_client_key", decrypt=True)
    )

    return samhsa_cert_path, samhsa_key_path, non_samhsa_cert_path, non_samhsa_key_path


@logger.inject_lambda_context(log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> dict[str, Any]:
    if not all([BFD_ENVIRONMENT, READER_ENDPOINT]):
        raise RuntimeError("Not all necessary environment variables were defined")

    invoke_event = SamhsaRegressionInvokeModel.model_validate(event)

    logger.info(
        "Starting SAMHSA regression test against %s (tablesample=%.1f%%, limit=%d)...",
        invoke_event.host,
        invoke_event.tablesample,
        invoke_event.limit,
    )

    samhsa_cert, samhsa_key, non_samhsa_cert, non_samhsa_key = _get_certificate_paths()

    security_labels = TypeAdapter(list[SecurityLabelModel]).validate_python(
        yaml.safe_load(SECURITY_LABELS_PATH.read_text()),
        by_alias=True,
    )

    passed = asyncio.run(
        run_samhsa_regression(
            hostname=invoke_event.host,
            security_labels=security_labels,
            db_details=_get_db_details(),
            samhsa_cert=samhsa_cert,
            samhsa_cert_key=samhsa_key,
            no_samhsa_cert=non_samhsa_cert,
            no_samhsa_cert_key=non_samhsa_key,
            host_cert=None,
            tablesample=invoke_event.tablesample,
            limit=invoke_event.limit,
            concurrency=invoke_event.concurrency,
        )
    )

    logger.info("SAMHSA regression test %s", "PASSED" if passed else "FAILED")

    return ResultModel(
        result=TestResult.SUCCESS if passed else TestResult.FAILURE,
        message=(
            "SAMHSA regression test finished. See 'log_stream_name' and 'log_group_name' "
            "properties for CloudWatch Log output location"
        ),
        log_stream_name=context.log_stream_name,
        log_group_name=context.log_group_name,
    ).model_dump()
