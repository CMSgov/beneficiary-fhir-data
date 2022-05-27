import time
import os
import json
from common.stats.aggregated_stats import AggregatedStats

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
from gevent import monkey
monkey.patch_all()
import boto3


class StatsJsonFileWriter(object):
    """Writes an AggegratedStats instance to a specified directory path in JSON format"""

    def __init__(self, stats: AggregatedStats) -> None:
        """Creates a new instance of StatsJsonFileWriter given an AggregatedStats object

        Args:
            stats (AggregatedStats): An AggregatedStats object that encodes the aggregated performance statistics of all Locust tasks in the current environment
        """
        super().__init__()

        self.stats = stats

    def write(self, path: str = '') -> None:
        """Writes the JSON-formatted statistics to the given path

        Args:
            path (str, optional): The _parent_ path of the file to write to disk. Defaults to ''.
        """
        with open(os.path.join(path, f'{self.stats.running_env.name}-{self.stats.stats_tag}-{int(time.time())}.json'), 'x') as json_file:
            json_file.write(json.dumps(self.stats.all_stats, indent=4))


class StatsJsonS3Writer(object):
    """Writes an AggegratedStats instance to a specified S3 bucket in JSON format"""

    def __init__(self, stats: AggregatedStats) -> None:
        """Creates a new instance of StatsJsonS3Writer given an AggregatedStats object

        Args:
            stats (AggregatedStats): An AggregatedStats object that encodes the aggregated performance statistics of all Locust tasks in the current environment
        """
        super().__init__()

        self.stats = stats
        self.s3 = boto3.client('s3')

    def write(self, bucket: str) -> None:
        """Writes the JSON-formatted statistics to the given S3 bucket to a pre-determined path
        following BFD Insights data organization standards

        Args:
            bucket (str): The S3 bucket in AWS to write the JSON to
        """
        self.s3.put_object(
            Bucket=bucket, Key=f'databases/bfd/test_performance_stats/env={self.stats.running_env.name}/tag={self.stats.stats_tag}/{int(time.time())}.json', Body=json.dumps(self.stats.all_stats))
