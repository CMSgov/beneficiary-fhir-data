import time
from abc import ABC, abstractmethod
import json
import os
from typing import Any, Dict, List, Optional

from common.stats.aggregated_stats import AggregatedStats, StatsMetadata
from common.stats.stats_config import StatsComparisonType, StatsConfiguration, StatsStorageType

# botocore/boto3 is incompatible with gevent out-of-box causing issues with SSL.
# We need to monkey patch gevent _before_ importing boto3 to ensure this doesn't happen.
# See https://stackoverflow.com/questions/40878996/does-boto3-support-greenlets
from gevent import monkey
monkey.patch_all()
import boto3


class StatsLoader(ABC):
    """Loads AggregatedStats depending on what type of comparison is requested"""

    def __init__(self, stats_config: StatsConfiguration, metadata: StatsMetadata) -> None:
        self.stats_config = stats_config
        self.metadata = metadata

    def load(self) -> AggregatedStats:
        """Loads an AggregatedStats instance constructed based on what type of comparison is required

        Returns:
            AggregatedStats: An AggregatedStats instance representing the set of stats requested to load
        """
        is_avg_compare = self.stats_config.compare == StatsComparisonType.AVERAGE
        return self.load_average() if is_avg_compare else self.load_previous()

    @abstractmethod
    def load_previous(self) -> AggregatedStats:
        """Loads an AggregatedStats instance constructed based on the most recent, previous test suite runs' 
        stats under the tag specified by the user

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of the previous test suite run
        """
        pass

    @abstractmethod
    def load_average(self) -> AggregatedStats:
        """Loads an AggregatedStats instance constructed based on the the average of all of the previous test suite
        runs' stats under the tag specified by the user

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of all specified previous test suite runs
        """
        pass

    @staticmethod
    def create(stats_config: StatsConfiguration, metadata: StatsMetadata) -> 'StatsLoader':
        """Construct a new concrete instance of StatsLoader that will load from the appropriate store as
        specified in stats_config

        Args:
            stats_config (StatsConfiguration): The configuration specified for storing and comparing statistics

        Returns:
            StatsLoader: A concrete instance of StatsLoader that will load from the store specified in configuration
        """
        return StatsFileLoader(stats_config, metadata) if stats_config.store == StatsStorageType.FILE else StatsAthenaLoader(stats_config, metadata)


class StatsFileLoader(StatsLoader):
    def load_previous(self) -> AggregatedStats:
        # Get a list of all AggregatedStats from stats.json files under path
        stats_list = self.__load_stats_from_files()

        # Filter those that don't match the config and current run's metadata
        filtered_stats = [
            stats for stats in stats_list if self.__verify_metadata(stats.metadata)]

        # Sort them based upon timestamp, greater to lower
        filtered_stats.sort(
            key=lambda stats: stats.metadata.timestamp, reverse=True)

        # Take the first item, if it exists -- this is the most recent, previous run
        return (filtered_stats[0:1] or [None])[0]

    def load_average(self) -> AggregatedStats:
        raise NotImplementedError(
            'Average stats is not implemented for files at this time.')

    def __load_stats_from_files(self, suffix: str = '.stats.json') -> List[AggregatedStats]:
        path = self.stats_config.path
        stats_files = [os.path.join(path, file)
                       for file in os.listdir(path) if file.endswith(suffix)]

        aggregated_stats_list = []
        for stats_file in stats_files:
            with open(stats_file) as json_file:
                aggregated_stats_list.append(
                    AggregatedStats(**json.load(json_file)))

        return aggregated_stats_list

    def __verify_metadata(self, loaded_metadata: StatsMetadata):
        return all([
            loaded_metadata.environment == self.stats_config.env,
            loaded_metadata.tag == self.stats_config.comp_tag,
            loaded_metadata.num_total_users == self.metadata.num_total_users,
            loaded_metadata.num_users_per_second == self.metadata.num_users_per_second,
            loaded_metadata.stats_reset_after_spawn == self.metadata.stats_reset_after_spawn,
            # Pick some delta that the runtimes should be under -- in this case, we're using 1 second
            loaded_metadata.total_runtime - self.metadata.total_runtime < 1.0
        ])


class StatsAthenaLoader(StatsLoader):
    def __init__(self, stats_config: StatsConfiguration, metadata: StatsMetadata) -> None:
        self.client = boto3.client('athena')

        super().__init__(stats_config, metadata)

    def load_previous(self) -> AggregatedStats:
        return super().load_previous()

    def load_average(self) -> AggregatedStats:
        return super().load_average()

    def __start_athena_query(self, query: str) -> Dict[str, Any]:
        return self.client.start_query_execution(
            QueryString=query,
            QueryExecutionContext={
                'Database': self.stats_config.athena_db
            },
            ResultConfiguration={
                'OutputLocation': f's3://{self.stats_config.bucket}/adhoc/query_results/test_performance_stat/'
            },
            WorkGroup='bfd'
        )

    def __get_athena_query_status(self, query_execution_id: str) -> str:
        return self.client.get_query_execution(
            QueryExecutionId=query_execution_id,
            WorkGroup='bfd'
        )['Status']['State']

    def __get_athena_query_result(self, query_execution_id: str) -> Dict[str, Any]:
        return self.client.get_query_results(
            QueryExecutionId=query_execution_id
        )['ResultSet']['Rows']

    def __run_query(self, query: str, max_retries: int = 10) -> Optional[Dict[str, Any]]:
        start_response = self.__start_athena_query(query)
        query_execution_id = start_response['QueryExecutionId']

        for try_number in range(0, max_retries - 1):
            # Exponentially back-off from hitting the API to ensure we don't hit the API limit
            # See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            time.sleep((2**try_number * 100.0) / 1000.0)

            status = self.__get_athena_query_status(query_execution_id)

            if status == 'SUCCEEDED':
                break
            elif status == 'FAILED' or status == 'CANCELLED':
                raise RuntimeError(
                    f'Query failed to complete -- status returned was {status}')

        return self.__get_athena_query_result(query_execution_id)
