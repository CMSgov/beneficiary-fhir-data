"""Downloads certstores from an S3 bucket and saves them locally to specified paths.

Used as the entrypoint for the "bfd-mgmt-mount-certstores" container image which is ran as part of
the "server" ECS Service. The certstores are downloaded to a shared bind mount from which the BFD
Server reads them.
"""

import boto3
import click


@click.command()
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
    "-r",
    "--region",
    envvar="REGION",
    type=str,
    default="us-east-1",
    show_default=True,
    help="AWS Region",
)
def main(
    bucket: str,
    truststore_key: str,
    keystore_key: str,
    truststore_out: str,
    keystore_out: str,
    region: str,
) -> None:
    """Download certstores from an S3 bucket to a specified path."""
    print(f"Bucket: {bucket}")
    print(f"Region: {region}")
    print(f"Truststore Key: {truststore_key}")
    print(f"Keystore Key: {keystore_key}")
    print(f"Truststore Output Path: {truststore_out}")
    print(f"Keystore Output Path: {keystore_out}")

    s3 = boto3.client("s3")  # type: ignore

    # Download the files from S3
    s3.download_file(Bucket=bucket, Key=truststore_key, Filename=truststore_out)
    s3.download_file(Bucket=bucket, Key=keystore_key, Filename=keystore_out)

    print("Certstores downloaded successfully.")


if __name__ == "__main__":
    main()
