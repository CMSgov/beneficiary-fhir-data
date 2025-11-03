"""Members of this file/module are related to writing performance statistics to a user-specified
data "store" (such as to file or AWS S3).
"""

import json
import logging
from dataclasses import asdict
from pathlib import Path

from common.stats.aggregated_stats import AggregatedStats
from common.stats.stats_config import StatsConfiguration, StatsStorageType
from gevent import monkey

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
monkey.patch_all()
import boto3  # noqa: E402

__s3_client = boto3.client("s3")


def write_stats(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    """Write aggregated performance stats to user-specified storage.

    Args:
        stats_config (StatsConfiguration): The user-specified configuration for stats-related
        operations from the command-line or from configuration files
        stats (AggregatedStats): The current run's aggregated stats
    """
    logger = logging.getLogger()
    if stats_config.stats_store == StatsStorageType.FILE:
        logger.info("Writing aggregated performance statistics to file.")
        _write_file(stats_config, stats)
    elif stats_config.stats_store == StatsStorageType.S3:
        logger.info("Writing aggregated performance statistics to S3.")
        _write_s3(stats_config, stats)


def _write_file(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    """Write the JSON-formatted statistics to the given path.

    Args:
        path (str, optional): The _parent_ path of the file to write to disk. Defaults to ''.

    Raises:
        ValueError: Raised if this object's AggregatedStats instance does not have any StatsMetadata
    """
    if not stats.metadata:
        raise ValueError("AggregatedStats instance must have metadata to write to file")

    stats_hash = stats.metadata.hash
    stats_timestamp = stats.metadata.timestamp
    parent_path = stats_config.stats_store_file_path or "./"
    full_path = Path(parent_path) / f"{stats_timestamp}-{stats_hash}.stats.json"
    with full_path.open(
        mode="x",
        encoding="utf-8",
    ) as json_file:
        json_file.write(json.dumps(asdict(stats), indent=4))

    logging.getLogger().info("Wrote aggregated performance statistics to file path: %s", full_path)


def _write_s3(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    """Write the JSON-formatted statistics to the given S3 bucket to a pre-determined path
    following BFD Insights data organization standards.

    Args:
        bucket (str): The S3 bucket in AWS to write the JSON to

    Raises:
        ValueError: Raised if this object's AggregatedStats instance does not have any StatsMetadata
    """
    if not stats.metadata:
        raise ValueError("AggregatedStats instance must have metadata to write to S3")

    if not stats_config.stats_store_s3_bucket:
        raise ValueError("--stats-store-s3-bucket must be specified")

    if not stats_config.stats_store_s3_database:
        raise ValueError("--stats-store-s3-database must be specified")

    if not stats_config.stats_store_s3_table:
        raise ValueError("--stats-store-s3-table must be specified")

    stats_hash = stats.metadata.hash
    stats_timestamp = stats.metadata.timestamp
    s3_path = "/".join(
        [
            "databases",
            stats_config.stats_store_s3_database,
            stats_config.stats_store_s3_table,
            f"hash={stats_hash}",
            f"{stats_timestamp}.stats.json",
        ]
    )
    try:
        put_response = __s3_client.put_object(
            Bucket=stats_config.stats_store_s3_bucket, Key=s3_path, Body=json.dumps(asdict(stats))
        )

        logging.getLogger().info(
            'Wrote aggregated performance statistics to s3 bucket "%s" at path: %s',
            stats_config.stats_store_s3_bucket,
            s3_path,
        )
        if not put_response:
            raise RuntimeError(
                f"Storing stats to {s3_path} failed as an invalid response from AWS was returned"
            )
    except __s3_client.exceptions.NoSuchBucket as exc:
        raise ValueError(f"S3 bucket {stats_config.stats_store_s3_bucket} does not exist") from exc
    except __s3_client.exceptions.ClientError as exc:
        raise RuntimeError(f"Unable to upload to {s3_path}") from exc
