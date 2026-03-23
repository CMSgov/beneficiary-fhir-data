import os
from calendar import EPOCH
from datetime import UTC, datetime
from io import BytesIO
from pathlib import Path
from typing import Any

import boto3
import snowflake.connector
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parameters import SSMProvider
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from botocore.exceptions import ClientError
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from jinja2 import Environment, PackageLoader, StrictUndefined
from snowflake.connector import DictCursor

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENV = os.environ.get("BFD_ENV", "prod")
PARTNERS = os.environ.get("PARTNERS", "")

if os.environ.get("POWERTOOLS_SERVICE_NAME") is None:
    os.environ["POWERTOOLS_SERVICE_NAME"] = "bene-prefs"

YYYYMMDD = datetime.now(UTC).strftime("%Y%m%d")
YYMMDD = datetime.now(UTC).strftime("%y%m%d")
TABLE_NAME = f"bfd-{BFD_ENV}-bene-preferences"
TEMPLATES = Environment(loader=PackageLoader("app", "templates"), undefined=StrictUndefined)

BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
S3 = boto3.client("s3", config=BOTO_CONFIG)
SSM = SSMProvider(config=BOTO_CONFIG)
TABLE = boto3.resource("dynamodb", config=BOTO_CONFIG).Table(TABLE_NAME)

logger = Logger()


def execute_query(query: str) -> list[dict[str, Any]]:
    """Execute the given query and return the resultant rows from Snowflake.

    Args:
        query: the fully specified (un-templated) Snowflake SQL query.

    Returns:
        List: The results of the query.
    """
    try:
        account = str(SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_account", decrypt=True))
        database = str(SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_database", decrypt=True))
        private_key_raw = str(
            SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_private_key", decrypt=True)
        )
        schema = str(SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_schema", decrypt=True))
        user = str(SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_username", decrypt=True))
        warehouse = str(
            SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_warehouse", decrypt=True)
        )
    except Exception as exc:
        raise ValueError(f"Missing snowflake configuration: {exc}") from exc

    # Load and prepare private key for authentication
    private_key = serialization.load_pem_private_key(
        private_key_raw.encode(),
        password=None,
        backend=default_backend(),
    )
    private_key_bytes = private_key.private_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )

    try:
        # Establish connection
        with snowflake.connector.connect(
            account=account,
            user=user,
            private_key=private_key_bytes,
            warehouse=warehouse,
            database=database,
            schema=schema,
        ) as conn:
            cursor = conn.cursor(DictCursor)
            cursor.execute(query)
            results = cursor.fetchall()
            cursor.close()

            return results
    except snowflake.connector.errors.Error:
        logger.exception("Snowflake error: {e}")
        raise


class PartnerPreferences:
    def __init__(self, partner: str) -> None:
        self._execution = None
        self.partner = partner
        self.query_template = TEMPLATES.get_template(f"{partner}.sql.j2")
        self.prefs_template = TEMPLATES.get_template(f"{partner}.prefs.j2")
        self.file_name_template = TEMPLATES.get_template(f"{partner}.file-name.j2")
        self._logger = Logger()
        self._logger.append_keys(partner=partner)

    @property
    def last_execution(self) -> str | None:
        try:
            response = TABLE.get_item(Key={"partner": self.partner})

            if "Item" in response:
                self._execution = response["Item"].get("last_execution")
                return self._execution

        except ClientError as e:
            self._logger.exception(
                "Error retrieving last execution: %s",
                e.response["Error"]["Message"],  # pyright: ignore[reportAttributeAccessIssue]
            )
            raise

        return self._execution

    @property
    def environment_indicator(self) -> str:
        """Return a 'P' for production or a T for test."""
        return "P" if BFD_ENV == "prod" else "T"

    @property
    def partner_code(self) -> str:
        """Return partner-specific preferences code."""
        codes = {"ab2d": "MED_AB2D", "bcda": "MED", "dpc": "MED_DPC"}
        try:
            code = codes[self.partner]
        except KeyError:
            self._logger.exception("Invalid partner: %s", self.partner)
            raise

        return code

    def _set_last_execution(self, timestamp: str | None = None) -> None:
        latest_timestamp = timestamp or datetime.now(UTC).isoformat()

        try:
            TABLE.update_item(
                Key={"partner": self.partner},
                UpdateExpression="SET last_execution = :timestamp, updated_at = :updated",
                ExpressionAttributeValues={
                    ":timestamp": latest_timestamp,
                    ":updated": datetime.now(UTC).isoformat(),
                },
                ReturnValues="NONE",
            )

            self._logger.info(
                "Updating last_execution from %s to %s", self.last_execution, latest_timestamp
            )
            self._execution = latest_timestamp

        except ClientError as e:
            self._logger.exception(
                "Error updating last execution: %s",
                e.response["Error"]["Message"],  # pyright: ignore[reportAttributeAccessIssue]
            )
            raise

    def _store_preferences(
        self,
        preferences_data: str,
        file_name: str,
        store_remote: bool = True,
        store_local: bool = False,
    ) -> None:
        if store_local:
            local_file = Path(file_name).name
            with Path.open(Path(local_file), "wb") as local:
                self._logger.info("Storing local file: %s", local_file)
                local.write(preferences_data.encode("utf-8"))

        if store_remote:
            bs_report = preferences_data.encode("utf-8")
            buffer = BytesIO(bs_report)
            buffer.seek(0)

            bucket = SSM.get(f"/bfd/{BFD_ENV}/bene-prefs/{self.partner}/nonsensitive/bucket")

            self._logger.info("Storing remote file: s3://%s/%s", bucket, file_name)
            S3.upload_fileobj(buffer, bucket, file_name)

    def generate_preferences(
        self,
        timestamp_range: tuple[str, str] = ("", ""),
        set_last_execution: bool = True,
        store_local: bool = False,
        store_remote: bool = True,
    ) -> None:
        """Generate and store the preferences report in AWS S3.

        Args:
            timestamp_range (tuple): Tuple bounding (lower, upper) ISO 8601 timestamp strings.
                Default range between last execution and utc-now.
            set_last_execution (bool): When `True`, update last_execution in DynamoDB . Default is
                `True`.
            store_local (bool): When `True`, store preferences locally. Default is `False`.
            store_remote (bool): When `True`, write the preferences to S3. Default is `True`.
        """
        query_since_timestamp = timestamp_range[0] or self.last_execution
        query_until_timestamp = timestamp_range[1] or datetime.now(UTC).isoformat()

        self._logger.info(
            "Rendering query template %s with query_since_timestamp=%s, query_until_timestamp=%s",
            self.query_template.filename,
            query_since_timestamp,
            query_until_timestamp,
        )

        query = self.query_template.render(
            partner_code=self.partner_code,
            query_since_timestamp=query_since_timestamp,
            query_until_timestamp=query_until_timestamp,
        )

        # TODO: Probably ought to change this to a debug-level log...
        self._logger.info(query)
        results = execute_query(query)

        self._logger.info(
            "Rendering prefs template %s of %d records with data=<redacted>, extract_date=%s",
            self.prefs_template.filename,
            len(results),
            YYYYMMDD,
        )
        preferences_data = self.prefs_template.render(data=results, extract_date=YYYYMMDD)

        report_time = datetime.now(UTC).strftime("%H%M%S")

        self._logger.info(
            (
                "Rendering file_name template %s with partner=%s, env_indicator=%s, report_date=%s,"
                " report_time=%s"
            ),
            self.file_name_template.filename,
            self.partner,
            self.environment_indicator,
            YYYYMMDD,
            report_time,
        )

        file_name = self.file_name_template.render(
            partner=self.partner,
            env_indicator=self.environment_indicator,
            report_date=YYMMDD,
            report_time=report_time,
        )

        self._store_preferences(
            preferences_data, file_name, store_remote=store_remote, store_local=store_local
        )

        if set_last_execution:
            if results:
                max_insert_datetime: datetime | None = max(
                    results, key=lambda x: x.get("IDR_INSRT_TS", datetime(EPOCH, 1, 1))
                ).get("IDR_INSRT_TS")
                max_insert_timestamp = (
                    max_insert_datetime.isoformat() if max_insert_datetime else None
                )
                self._set_last_execution(max_insert_timestamp)
            else:
                self._logger.info("Empty results. Skipping set of last execution.")


def handler(event: dict[str, Any], context: LambdaContext) -> None:  # noqa: ARG001
    """Lambda event handler function.

    Args:
        event (dict): EventBridge Scheduler/S3 Bucket Notification event details. Unused
        context (LambdaContext): Lambda execution context. Unused

    Raises:
        RuntimeError: If any required environment variables are undefined
    """
    partners = [p.strip() for p in PARTNERS.split(",")]

    if "bcda" in partners:
        bcda = PartnerPreferences("bcda")
        bcda.generate_preferences()

    if "ab2d" in partners:
        ab2d = PartnerPreferences("ab2d")
        ab2d.generate_preferences()

    if "dpc" in partners:
        dpc = PartnerPreferences("dpc")
        dpc.generate_preferences()
