import os
import subprocess
import urllib.parse
from enum import StrEnum, auto
from pathlib import Path
from typing import Annotated, Any

from annotated_types import Len
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parameters import get_parameter
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from pydantic import BaseModel

REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", default="").lower()
STATS_BUCKET_ID = os.environ.get("STATS_BUCKET_ID", default="")
STATS_ATHENA_WORKGROUP = os.environ.get("STATS_ATHENA_WORKGROUP", default="")
STATS_ATHENA_DATABASE = os.environ.get("STATS_ATHENA_DATABASE", default="")
STATS_ATHENA_TABLE = os.environ.get("STATS_ATHENA_TABLE", default="")
READER_ENDPOINT = os.environ.get("READER_ENDPOINT", default="")
LAMBDA_TASK_ROOT = os.environ.get("LAMBDA_TASK_ROOT", default="/var/task")

BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logger = Logger()


class CompareType(StrEnum):
    PREVIOUS = auto()
    AVERAGE = auto()


class CompareConfigModel(BaseModel):
    type: CompareType
    tag: str
    load_limit: int | None = None


class StoreConfigModel(BaseModel):
    tags: Annotated[list[str], Len(min_length=1)]


class InvokeEventModel(BaseModel):
    suite: str
    host: str
    spawn_rate: int
    users: int
    spawned_runtime: str
    only_summary: bool = True
    compare: CompareConfigModel | None = None
    store: StoreConfigModel | None = None
    extra_args: list[str] | None = []


class TestResult(StrEnum):
    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"


class ResultModel(BaseModel):
    result: TestResult
    message: str
    log_stream_name: str
    log_group_name: str


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> dict[str, Any]:
    if not all([
        REGION,
        BFD_ENVIRONMENT,
        STATS_BUCKET_ID,
        STATS_ATHENA_WORKGROUP,
        STATS_ATHENA_DATABASE,
        STATS_ATHENA_TABLE,
        READER_ENDPOINT,
        LAMBDA_TASK_ROOT,
    ]):
        raise RuntimeError("Not all necessary environment variables were defined")

    try:
        invoke_event = InvokeEventModel.model_validate(event)

        # Assuming we get this far, invoke_event should have the information
        # required to run the lambda:
        username = get_parameter(
            name=f"/bfd/{BFD_ENVIRONMENT}/locust/sensitive/db/username", decrypt=True
        )
        raw_password = get_parameter(
            name=f"/bfd/{BFD_ENVIRONMENT}/locust/sensitive/db/password", decrypt=True
        )
        cert_key = get_parameter(
            name=f"/bfd/{BFD_ENVIRONMENT}/locust/sensitive/cert/key", decrypt=True
        )
        cert = get_parameter(
            name=f"/bfd/{BFD_ENVIRONMENT}/locust/sensitive/cert/pem_data", decrypt=True
        )

        cert_path = "/tmp/bfd_test_cert.pem"
        with Path(cert_path).open(mode="w", encoding="utf-8") as file:
            file.write(cert_key + cert)

        password = urllib.parse.quote(raw_password)
        db_dsn = f"postgres://{username}:{password}@{READER_ENDPOINT}:5432/fhirdb"

        locust_process_args = (
            [
                "locust",
                f"--locustfile={LAMBDA_TASK_ROOT}/app/{invoke_event.suite}",
                f"--host={invoke_event.host}",
                f"--users={invoke_event.users}",
                f"--spawn-rate={invoke_event.spawn_rate}",
                f"--spawned-runtime={invoke_event.spawned_runtime}",
                f"--database-connection-string={db_dsn}",
                f"--client-cert-path={cert_path}",
                "--headless",
            ]
            + (["--only-summary"] if invoke_event.only_summary else [])
            + (invoke_event.extra_args or [])
        )

        if invoke_event.compare or invoke_event.store:
            locust_process_args += [
                f"--stats-env={BFD_ENVIRONMENT}",
            ]

        if invoke_event.compare:
            compare_type_flag = (
                "--stats-compare-average"
                if invoke_event.compare.type == CompareType.AVERAGE
                else "--stats-compare-previous"
            )
            locust_process_args += [
                compare_type_flag,
                f"--stats-compare-meta-file={LAMBDA_TASK_ROOT}/app/config/regression_suites_compare_meta.json",
                f"--stats-compare-tag={invoke_event.compare.tag}",
            ] + (
                [f"--stats-compare-load-limit={invoke_event.compare.load_limit}"]
                if invoke_event.compare.load_limit
                else []
            )

        if invoke_event.store:
            store_tag_args = [f"--stats-store-tag={tag}" for tag in invoke_event.store.tags]
            locust_process_args += [
                "--stats-store-s3",
                f"--stats-store-s3-workgroup={STATS_ATHENA_WORKGROUP}",
                f"--stats-store-s3-bucket={STATS_BUCKET_ID}",
                f"--stats-store-s3-database={STATS_ATHENA_DATABASE}",
                f"--stats-store-s3-table={STATS_ATHENA_TABLE}",
                *store_tag_args,
            ]

        # The command to run Locust with the current test file
        locust_process = subprocess.run(
            args=locust_process_args,
            text=True,
            check=False,
        )

        locust_succeeded = locust_process.returncode == 0

        return ResultModel(
            result=TestResult.SUCCESS if locust_succeeded else TestResult.FAILURE,
            message=(
                "Locust finished running. See 'log_stream_name' and 'log_group_name' properties "
                "for CloudWatch Log output location"
            ),
            log_stream_name=context.log_stream_name,
            log_group_name=context.log_group_name,
        ).model_dump()
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
