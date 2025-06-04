"""Downloads certstores and healthcheck PEM and saves them locally to specified paths.

Used as the entrypoint for the "bfd-mgmt-mount-certstores" container image which is ran as part of
the "server" ECS Service. The certstores and healthcheck PEM are downloaded to a shared bind mount
from which the BFD Server reads them.
"""

from pathlib import Path

import boto3
import click


@click.command()
@click.option(
    "-e",
    "--bfd-env",
    envvar="BFD_ENV",
    type=str,
    default="test",
    show_default=True,
    help="The BFD SDLC Environment that the Healthcheck Certificate should be pulled from",
)
@click.option(
    "-b",
    "--bucket",
    envvar="BUCKET",
    type=str,
    required=True,
    help="The name of the bucket containing the certstores",
)
@click.option(
    "-t",
    "--truststore-key",
    envvar="TRUSTSTORE_KEY",
    type=str,
    default="truststore.pfx",
    show_default=True,
    help="The S3 key of the truststore file",
)
@click.option(
    "-k",
    "--keystore-key",
    envvar="KEYSTORE_KEY",
    type=str,
    default="keystore.pfx",
    show_default=True,
    help="The S3 key of the keystore file",
)
@click.option(
    "-T",
    "--truststore-out",
    envvar="TRUSTSTORE_OUTPUT_PATH",
    type=str,
    default="./truststore.pfx",
    show_default=True,
    help="The local output path for the truststore file",
)
@click.option(
    "-K",
    "--keystore-out",
    envvar="KEYSTORE_OUTPUT_PATH",
    type=str,
    default="./keystore.pfx",
    show_default=True,
    help="The local output path for the keystore file",
)
@click.option(
    "-C",
    "--healthcheck-cert-out",
    envvar="HEALTHCHECK_CERT_OUTPUT_PATH",
    type=str,
    default="./healthcheck.pem",
    show_default=True,
    help="The local output path for the healthcheck certificate file",
)
@click.option(
    "-r",
    "--region",
    envvar="REGION",
    type=str,
    default="us-east-1",
    show_default=True,
    help="AWS Region",
)
def main(
    bfd_env: str,
    bucket: str,
    truststore_key: str,
    keystore_key: str,
    truststore_out: str,
    keystore_out: str,
    healthcheck_cert_out: str,
    region: str,
) -> None:
    """Download the certstores from the specified S3 bucket and the healthcheck PEM certificate."""
    print(f"Environment: {bfd_env}")
    print(f"Bucket: {bucket}")
    print(f"Region: {region}")
    print(f"Truststore Key: {truststore_key}")
    print(f"Keystore Key: {keystore_key}")
    print(f"Truststore Output Path: {truststore_out}")
    print(f"Keystore Output Path: {keystore_out}")
    print(f"Healthcheck PEM Output Path: {healthcheck_cert_out}")

    print("Downloading certstores from S3...")
    s3 = boto3.client("s3")  # type: ignore

    s3.download_file(Bucket=bucket, Key=truststore_key, Filename=truststore_out)
    s3.download_file(Bucket=bucket, Key=keystore_key, Filename=keystore_out)

    print("Certstores downloaded successfully.")

    print("Downloading healthcheck certificate from SSM...")
    ssm = boto3.client("ssm")  # type: ignore

    # TODO: Remove /ng/ prefix
    cert = ssm.get_parameter(
        Name=f"/ng/bfd/{bfd_env}/server/sensitive/test_client_cert", WithDecryption=True
    )["Parameter"].get("Value", None)
    key = ssm.get_parameter(
        Name=f"/ng/bfd/{bfd_env}/server/sensitive/test_client_key",  # gitleaks:allow
        WithDecryption=True,
    )["Parameter"].get("Value", None)
    if not cert or not key:
        raise ValueError("Failed to download healthcheck certificate and/or key from SSM.")

    with Path.open(Path(healthcheck_cert_out), "w") as f:
        f.write(cert)
        f.write(key)

    print("Healthcheck certificate downloaded successfully.")


if __name__ == "__main__":
    main()
