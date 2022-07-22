from dataclasses import dataclass
import json
import os
import subprocess
import urllib.parse
from typing import Optional

import boto3
from botocore.config import Config

environment = os.environ.get("BFD_ENVIRONMENT", "test")

boto_config = Config(region_name="us-east-1")
ssm_client = boto3.client("ssm", config=boto_config)
rds_client = boto3.client("rds", config=boto_config)


@dataclass
class InvokeEvent:
    host: str
    suite_version: str
    spawn_rate: int
    users: int
    spawned_runtime: str


def get_ssm_parameter(name: str, with_decrypt: bool = False) -> Optional[str]:
    response = ssm_client.get_parameter(Name=name, WithDecryption=with_decrypt)

    try:
        return response["Parameter"]["Value"]
    except KeyError:
        return None


def get_rds_db_uri(cluster_id: str) -> Optional[str]:
    response = rds_client.describe_db_clusters(DBClusterIdentifier=cluster_id)

    try:
        return response["DBClusters"][0]["ReaderEndpoint"]
    except KeyError:
        return None


def handler(event, context):
    # We take only the first record, if it exists
    try:
        record = event["Records"][1]
    except IndexError:
        return "Invalid queue message, no records found"

    # We extract the body, and attempt to convert from JSON
    try:
        body = json.loads(record["body"])
    except json.JSONDecodeError:
        return "Record body was not valid JSON"

    # We then attempt to extract an InvokeEvent instance from
    # the JSON body
    try:
        invoke_event = InvokeEvent(**body)
    except TypeError as ex:
        return f"Message body missing required keys: {str(ex)}"

    # Assuming we get this far, invoke_event should have the information
    # required to run the lambda:
    cluster_id = get_ssm_parameter(f"/bfd/{environment}/common/nonsensitive/rds_cluster_identifier")
    username = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/vault_data_server_db_username", with_decrypt=True
    )
    raw_password = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/vault_data_server_db_password", with_decrypt=True
    )
    cert_key = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/test_client_key", with_decrypt=True
    )
    cert = get_ssm_parameter(
        f"/bfd/{environment}/server/sensitive/test_client_cert", with_decrypt=True
    )

    if not cluster_id:
        return (
            f'SSM parameter "/bfd/{environment}/common/nonsensitive/rds_cluster_identifier" not'
            " found or empty"
        )

    if not username:
        return (
            f'SSM parameter "/bfd/{environment}/common/nonsensitive/vault_data_server_db_username"'
            " not found or empty"
        )

    if not raw_password:
        return (
            f'SSM parameter "/bfd/{environment}/common/nonsensitive/vault_data_server_db_password"'
            " not found or empty"
        )

    if not cert_key:
        return (
            f'SSM parameter "/bfd/{environment}/server/sensitive/test_client_key" not found or'
            " empty"
        )

    if not cert:
        return (
            f'SSM parameter "/bfd/{environment}/server/sensitive/test_client_cert" not found or'
            " empty"
        )

    cert_path = "/tmp/bfd_test_cert.pem"
    with open(cert_path, "w", encoding="utf-8") as file:
        file.write(cert_key + cert)

    password = urllib.parse.quote(raw_password)
    db_uri = get_rds_db_uri(cluster_id)

    db_dsn = f"postgres://{username}:{password}@{db_uri}:5432/fhirdb"

    process = subprocess.run(
        [
            "locust",
            f"--locustfile=/var/task/{invoke_event.suite_version}/regression_suite.py",
            f"--host={invoke_event.host}",
            f"--users={invoke_event.users}",
            f"--spawn-rate={invoke_event.spawn_rate}",
            f"--spawned-runtime={invoke_event.spawned_runtime}",
            f"--database-uri={db_dsn}",
            f"--client-cert-path={cert_path}",
            "--headless",
            "--only-summary",
        ],
        text=True,
        check=False,
    )

    return process.stdout
