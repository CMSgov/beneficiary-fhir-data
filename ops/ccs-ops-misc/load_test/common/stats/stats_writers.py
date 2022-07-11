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


def write_stats(stats_config: StatsConfiguration, stats: AggregatedStats) -> None:
    logger = logging.getLogger()
    if stats_config.store == StatsStorageType.FILE:
        logger.info("Writing aggregated performance statistics to file.")

        stats_json_writer = StatsJsonFileWriter(stats)
        stats_json_writer.write(stats_config.path or "")
    elif stats_config.store == StatsStorageType.S3:
        logger.info("Writing aggregated performance statistics to S3.")

        stats_s3_writer = StatsJsonS3Writer(stats)
        if not stats_config.bucket:
            raise ValueError("S3 bucket must be provided when writing stats to S3")
        stats_s3_writer.write(stats_config.bucket)


class StatsJsonFileWriter(object):
    """Writes an AggegratedStats instance to a specified directory path in JSON format"""

    def __init__(self, stats: AggregatedStats) -> None:
        """Creates a new instance of StatsJsonFileWriter given an StatsCollector object

        Args:
            stats (AggregatedStats): An AggregatedStats object that represents the stats of a test suite run
        """
        super().__init__()

        self.stats = stats

    def write(self, path: str = "") -> None:
        """Writes the JSON-formatted statistics to the given path

        Args:
            path (str, optional): The _parent_ path of the file to write to disk. Defaults to ''.

        Raises:
            ValueError: Raised if this object's AggregatedStats instance does not have any StatsMetadata
        """
        if not self.stats.metadata:
            raise ValueError("AggregatedStats instance must have metadata to write to file")

        env_name = self.stats.metadata.environment.name
        store_tag = self.stats.metadata.tag
        with open(os.path.join(path, f"{env_name}-{store_tag}-{int(time.time())}.stats.json"), "x") as json_file:
            json_file.write(json.dumps(asdict(self.stats), indent=4))


class StatsJsonS3Writer(object):
    """Writes an AggegratedStats instance to a specified S3 bucket in JSON format"""

    def __init__(self, stats: AggregatedStats) -> None:
        """Creates a new instance of StatsJsonS3Writer given an AggregatedStats object

        Args:
            stats (AggregatedStats): An AggregatedStats object that represents the stats of a test suite run
        """
        super().__init__()

        self.stats = stats
        self.s3 = boto3.client("s3")

    def write(self, bucket: str) -> None:
        """Writes the JSON-formatted statistics to the given S3 bucket to a pre-determined path
        following BFD Insights data organization standards

        Args:
            bucket (str): The S3 bucket in AWS to write the JSON to

        Raises:
            ValueError: Raised if this object's AggregatedStats instance does not have any StatsMetadata
        """
        if not self.stats.metadata:
            raise ValueError("AggregatedStats instance must have metadata to write to S3")

        env_name = self.stats.metadata.environment.name
        store_tag = self.stats.metadata.tag
        s3_path = f"databases/bfd/test_performance_stats/env={env_name}/tag={store_tag}/{int(time.time())}.json"
        self.s3.put_object(Bucket=bucket, Key=s3_path, Body=json.dumps(asdict(self.stats)))
