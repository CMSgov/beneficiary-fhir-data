import os
import sys
from datetime import UTC, datetime
from io import BytesIO
from pathlib import Path

import boto3
import snowflake.connector
from botocore.config import Config
from botocore.exceptions import ClientError
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from jinja2 import Template
from snowflake.connector import DictCursor

from typing import Any

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENV = os.environ.get("BFD_ENV", "prod")

YYYYMMDD = datetime.now(UTC).strftime("%Y%m%d")
YYMMDD = datetime.now(UTC).strftime("%y%m%d")
TABLE_NAME = f"bfd-{BFD_ENV}-bene-preferences"
# Dynamically find the single templates directory below:
TEMPLATES = f"{[i for i in Path().rglob('templates')][0]}"

BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
SSM_CLIENT = boto3.client("ssm", config=BOTO_CONFIG)
# TODO: Consider whether the below is a sensible default
PARTNERS = os.environ.get("PARTNERS", "")


def get_ssm_parameter(name: str, with_decrypt: bool = True) -> str:
    """Retrieve SSM parameter using the global SSM_CLIENT.

    Args:
        name (str): The name of the SSM parameter to retrieve
        with_decrypt (bool, optional): Whether or not to decrypt the retrieved SSM parameter.
        Defaults to True.

    Raises:
        ValueError: Raised if the parameter was not found

    Returns:
        str: The value of the SSM parameter
    """
    response = SSM_CLIENT.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]  # type: ignore
    except KeyError as exc:
        raise ValueError(f'SSM parameter "{name}" not found or empty') from exc


def execute_query(query: str) -> list:
    """Execute the given query and return the resultant rows from Snwoflake.

    Args:
        query: the fully specified (un-templated) Snowflake SQL query.

    Returns:
        List: The results of the query.
    """
    try:
        account = get_ssm_parameter(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_account")
        database = get_ssm_parameter(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_database")
        private_key_raw = get_ssm_parameter(
            f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_private_key"
        )
        schema = get_ssm_parameter(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_schema")
        user = get_ssm_parameter(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_username")
        warehouse = get_ssm_parameter(f"/bfd/{BFD_ENV}/idr-pipeline/sensitive/idr_warehouse")
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
        # TODO: Consider implementing a logger instead of print statement
        print(f"Snowflake error: {e}", file=sys.stderr)
        raise

    finally:
        if "conn" in locals():
            conn.close()


class PartnerPreferences:
    def __init__(self, partner: str, region_name: str = "us-east-1") -> None:
        self.__dynamodb = boto3.resource("dynamodb", region_name=region_name)
        self.__execution = None
        self.partner = partner
        self.query_template = Path(f"{TEMPLATES}/{partner}.sql.j2")
        self.prefs_template = Path(f"{TEMPLATES}/{partner}.prefs.j2")
        self.file_name_template = Path(f"{TEMPLATES}/{partner}.file-name.j2")
        self.table = self.__dynamodb.Table(TABLE_NAME)

    @property
    def last_execution(self) -> str | None:
        try:
            response = self.table.get_item(Key={"partner": self.partner})

            if "Item" in response:
                self.__execution = response["Item"].get("last_execution")
                return self.__execution

        except ClientError as e:
            # TODO: Consider implementing a logger instead of print statement
            print(f"""
            Error retrieving last execution for {self.partner}: {e.response["Error"]["Message"]}
            """)
            raise

        return self.__execution

    @property
    def environment_indicator(self) -> str:
        """Return a 'P' for production or a T for test."""
        return "P" if BFD_ENV == "prod" else "T"

    def __set_last_execution(self, timestamp: str | None = None) -> None:
        latest_timestamp = timestamp or datetime.now(UTC).isoformat()

        try:
            self.table.update_item(
                Key={"partner": self.partner},
                UpdateExpression="SET last_execution = :timestamp, updated_at = :updated",
                ExpressionAttributeValues={
                    ":timestamp": latest_timestamp,
                    ":updated": datetime.now(UTC).isoformat(),
                },
                ReturnValues="NONE",
            )

            # TODO: Consider implementing a logger instead of print statement
            print(f"Updated last_execution for {self.partner}: {latest_timestamp}")
            self.__execution = timestamp

        except ClientError as e:
            # TODO: Consider implementing a logger instead of print statement
            print(f"Error updating last execution {self.partner}: {e.response['Error']['Message']}")
            raise

    def __store_preferences(
        self, preferences_data: str, file_name: str, store_local: bool = False
    ) -> None:
        if store_local:
            local_file = Path("/".join([file_name.split("/")[-1]]))
            with Path.open(local_file, "wb") as local:
                # TODO: Consider implementing a logger instead of print statement
                print(f"storing local... {local_file}")
                local.write(preferences_data.encode("utf-8"))

        bs_report = preferences_data.encode("utf-8")
        buffer = BytesIO(bs_report)
        buffer.seek(0)

        bucket = get_ssm_parameter(f"/bfd/{BFD_ENV}/bene-prefs/{self.partner}/nonsensitive/bucket")

        s3 = boto3.client("s3", config=BOTO_CONFIG)
        s3.upload_fileobj(buffer, bucket, file_name)

    def generate_preferences(
        self,
        since_timestamp: str | None = None,
        until_timestamp: str | None = None,
        store_preferences: bool = True,
        set_last_execution: bool = True,
        store_local: bool = False,
    ) -> None:
        """Generate and store the preferences report in AWS S3.

        Args:
            since_timestamp: ISO 8601 timestamp string. If None, uses last execution timestamp.
            until_timestamp: ISO 8601 timestamp string. If None, uses the current time.
            store_preferences: When `True`, write the preferences to S3. Default is `True`.
            set_last_execution: When `True`, update last_execution in DynamoDB . Default is `True`.
            store_local: When `True`, store preferences locally. Default is `False`.
        """
        query_since_timestamp = since_timestamp or self.last_execution
        query_until_timestamp = until_timestamp or datetime.now(UTC).isoformat()

        with Path.open(self.query_template) as template:
            query = Template(template.read()).render(
                query_since_timestamp=query_since_timestamp,
                query_until_timestamp=query_until_timestamp,
            )

        # TODO: Implement feature for logging the rendered query template to stdout
        # or through a logfile here

        results = execute_query(query)

        with Path.open(self.prefs_template) as template:
            preferences_data = Template(template.read()).render(data=results, extract_date=YYYYMMDD)

        with Path.open(self.file_name_template) as template:
            file_name = Template(template.read()).render(
                partner=self.partner,
                env_indicator=self.environment_indicator,
                report_date=YYMMDD,
                report_time=datetime.now(UTC).strftime("%H%M%S"),
            )

        if store_preferences:
            # TODO: Consider implementing a logger instead of print statement
            print(f"storing... {file_name}")
            self.__store_preferences(preferences_data, file_name, store_local=store_local)

        if set_last_execution:
            self.__set_last_execution()


def handler(event: dict[str, Any], context: dict[str, Any]) -> None:
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
