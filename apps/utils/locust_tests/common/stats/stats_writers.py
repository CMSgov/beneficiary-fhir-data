"""Members of this file/module are related to writing performance statistics to a user-specified
data "store" (such as to file or AWS S3)"""
import json
import logging
import os
import time
from dataclasses import asdict

from gevent import monkey

from common.stats.aggregated_stats import AggregatedStats
from common.stats.stats_config import StatsConfiguration, StatsStorageType

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
monkey.patch_all()
import boto3

__s3_client = boto3.client("s3")


def write_stats(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    logger = logging.getLogger()
    if stats_config.store == StatsStorageType.FILE:
        logger.info("Writing aggregated performance statistics to file.")
        _write_file(stats_config, stats)
    elif stats_config.store == StatsStorageType.S3:
        logger.info("Writing aggregated performance statistics to S3.")
        _write_s3(stats_config, stats)


def _write_file(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    """Writes the JSON-formatted statistics to the given path

    Args:
        path (str, optional): The _parent_ path of the file to write to disk. Defaults to ''.

    Raises:
        ValueError: Raised if this object's AggregatedStats instance does not have any StatsMetadata
    """
    if not stats.metadata:
        raise ValueError("AggregatedStats instance must have metadata to write to file")

    env_name = stats.metadata.environment.name.replace("-", "_")
    store_tag = stats.metadata.tag
    path = stats_config.path or ""
    with open(
        os.path.join(path, f"{env_name}-{store_tag}-{int(time.time())}.stats.json"),
        mode="x",
        encoding="utf-8",
    ) as json_file:
        json_file.write(json.dumps(asdict(stats), indent=4))


def _write_s3(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    """Writes the JSON-formatted statistics to the given S3 bucket to a pre-determined path
    following BFD Insights data organization standards

    Args:
        bucket (str): The S3 bucket in AWS to write the JSON to

    Raises:
        ValueError: Raised if this object's AggregatedStats instance does not have any StatsMetadata
    """
    if not stats.metadata:
        raise ValueError("AggregatedStats instance must have metadata to write to S3")

    if not stats_config.bucket:
        raise ValueError("--stats-config must specify a S3 bucket to store to")

    if not stats_config.database:
        raise ValueError(
            "--stats-config must specify a database that stats will be stored under in S3"
        )

    if not stats_config.table:
        raise ValueError(
            "--stats-config must specify a table that stats will be stored under in S3"
        )

    env_name = stats.metadata.environment.name.replace("-", "_")
    store_tag = stats.metadata.tag

    s3_path = f"databases/{stats_config.database}/{stats_config.table}/env={env_name}/tag={store_tag}/{int(time.time())}.stats.json"
    try:
        put_response = __s3_client.put_object(
            Bucket=stats_config.bucket, Key=s3_path, Body=json.dumps(asdict(stats))
        )

        if not put_response:
            raise RuntimeError(
                f"Storing stats to {s3_path} failed as an invalid response from AWS was returned"
            )
    except __s3_client.exceptions.NoSuchBucket as exc:
        raise ValueError(f"S3 bucket {stats_config.bucket} does not exist") from exc
    except __s3_client.exceptions.ClientError as exc:
        raise RuntimeError(f"Unable to upload to {s3_path}") from exc
