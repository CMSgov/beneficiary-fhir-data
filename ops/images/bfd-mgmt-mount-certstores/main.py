import boto3
import click

TRUSTSTORE_KEY = "truststore.pfx"
KEYSTORE_KEY = "keystore.pfx"


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
    "-o",
    "--out",
    envvar="OUTPUT_PATH",
    type=str,
    default="./",
    show_default=True,
    help="The output path for the certstores",
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
def main(bucket: str, region: str, out: str) -> None:
    """Download certstores from an S3 bucket to a specified path."""
    print(f"Bucket: {bucket}")
    print(f"Region: {region}")
    print(f"Mount Path: {out}")

    s3 = boto3.client("s3")  # type: ignore

    # Download the files from S3
    s3.download_file(Bucket=bucket, Key=TRUSTSTORE_KEY, Filename=f"{out}/{TRUSTSTORE_KEY}")
    s3.download_file(Bucket=bucket, Key=KEYSTORE_KEY, Filename=f"{out}/{KEYSTORE_KEY}")


if __name__ == "__main__":
    main()
