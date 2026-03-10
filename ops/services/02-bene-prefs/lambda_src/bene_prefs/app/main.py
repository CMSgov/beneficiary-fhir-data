import os
from datetime import UTC, datetime
from io import BytesIO
from pathlib import Path

import boto3
import snowflake.connector
from botocore.config import Config
from botocore.exceptions import ClientError
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from jinja2 import Environment, PackageLoader, StrictUndefined
from snowflake.connector import DictCursor

from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.typing import LambdaContext
from aws_lambda_powertools.utilities.parameters import SSMProvider

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENV = os.environ.get("BFD_ENV", "prod")
PARTNERS = os.environ.get("PARTNERS", "")

if os.environ.get("POWERTOOLS_SERVICE_NAME") is None:
    os.environ["POWERTOOLS_SERVICE_NAME"] = "bene-prefs"

YYYYMMDD = datetime.now(UTC).strftime("%Y%m%d")
YYMMDD = datetime.now(UTC).strftime("%y%m%d")
TABLE_NAME = f"bfd-{BFD_ENV}-bene-preferences"
TEMPLATES = Environment(
    loader=PackageLoader("app", "templates"),
    undefined=StrictUndefined
)

BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
SSM = SSMProvider(config=BOTO_CONFIG)
TABLE = boto3.resource("dynamodb", config=BOTO_CONFIG).Table(TABLE_NAME)

logger = Logger()


def execute_query(query: str) -> list:
    """Execute the given query and return the resultant rows from Snwoflake.

    Args:
        query: the fully specified (un-templated) Snowflake SQL query.

    Returns:
        List: The results of the query.
    """
    try:
        account = SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_account", decrypt=True)
        database = SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_database", decrypt=True)
        private_key_raw = SSM.get(
            f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_private_key", decrypt=True)
        schema = SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_schema", decrypt=True)
        user = SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_username", decrypt=True)
        warehouse = SSM.get(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_warehouse", decrypt=True)
    except Exception as exc:
        raise ValueError(f"Missing snowflake configuration: {exc}")

    try:
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

        # Establish connection
        conn = snowflake.connector.connect(
            account=account,
            user=user,
            private_key=private_key_bytes,
            warehouse=warehouse,
            database=database,
            schema=schema,
        )

        cursor = conn.cursor(DictCursor)
        cursor.execute(query)
        results = cursor.fetchall()
        cursor.close()
        return results

    except snowflake.connector.errors.Error as e:
        logger.exception("Snowflake error: {e}")
        raise

    finally:
        if "conn" in locals():
            conn.close()


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
            self._logger.exception(f"""
            Error retrieving last execution: {e.response["Error"]["Message"]}
            """)
            raise

        return self._execution

    @property
    def environment_indicator(self) -> str:
        """Return a 'P' for production or a T for test."""
        return "P" if BFD_ENV == "prod" else "T"

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
                f"Updating last_exeuction from {self.last_execution} to {latest_timestamp}"
            )
            self._execution = latest_timestamp

        except ClientError as e:
            self._logger.exception(
                f"Error updating last execution: {e.response['Error']['Message']}"
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
            local_file = Path("/".join([file_name.split("/")[-1]]))
            with Path.open(local_file, "wb") as local:
                self._logger.info(f"Storing local file:{local_file}")
                local.write(preferences_data.encode("utf-8"))

        if store_remote:
            bs_report = preferences_data.encode("utf-8")
            buffer = BytesIO(bs_report)
            buffer.seek(0)

            bucket = SSM.get(f"/bfd/{BFD_ENV}/bene-prefs/{self.partner}/nonsensitive/bucket")

            s3 = boto3.client("s3", config=BOTO_CONFIG)
            self._logger.info(f"Storing remote file: s3://{bucket}/{file_name}")
            s3.upload_fileobj(buffer, bucket, file_name)

    def generate_preferences(
        self,
        since_timestamp: str | None = None,
        until_timestamp: str | None = None,
        set_last_execution: bool = True,
        store_local: bool = False,
        store_remote: bool = True,
    ) -> None:
        """Generate and store the preferences report in AWS S3.

        Args:
            since_timestamp: ISO 8601 timestamp string. If None, uses last execution timestamp.
            until_timestamp: ISO 8601 timestamp string. If None, uses the current time.
            set_last_execution: When `True`, update last_execution in DynamoDB . Default is `True`.
            store_local: When `True`, store preferences locally. Default is `False`.
            store_remote: When `True`, write the preferences to S3. Default is `True`.
        """
        query_since_timestamp = since_timestamp or self.last_execution
        query_until_timestamp = until_timestamp or datetime.now(UTC).isoformat()

        self._logger.info(
            f"Rendering query template {self.query_template.filename} with query_since_timestamp={query_since_timestamp}, query_until_timestamp={query_until_timestamp}"
        )

        query = self.query_template.render(
            query_since_timestamp=query_since_timestamp,
            query_until_timestamp=query_until_timestamp,
        )

        # TODO: Probably ought to change this to a debug-level log...
        self._logger.info(query)
        results = execute_query(query)

        self._logger.info(
            f"Rendering prefs template {self.prefs_template.filename} of {len(results)} records with data=<redacted>, extract_date={YYYYMMDD}"
        )
        preferences_data = self.prefs_template.render(data=results, extract_date=YYYYMMDD)

        report_time = datetime.now(UTC).strftime("%H%M%S")

        self._logger.info(
            f"Rendering file_name template {self.file_name_template.filename} with partner={self.partner}, env_indicator={self.environment_indicator}, report_date={YYYYMMDD}, report_time={report_time}"
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
            self._set_last_execution(query_until_timestamp)


def handler(event: dict, context: LambdaContext) -> None:
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
