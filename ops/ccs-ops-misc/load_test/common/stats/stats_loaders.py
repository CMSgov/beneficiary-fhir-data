from abc import ABC, abstractmethod
import json
import os
from typing import List

from common.stats.aggregated_stats import AggregatedStats, StatsMetadata
from common.stats.stats_config import StatsComparisonType, StatsConfiguration, StatsStorageType


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
        filtered_stats = [stats for stats in stats_list if self.__verify_metadata(stats.metadata)]

        # Sort them based upon timestamp, greater to lower
        filtered_stats.sort(key=lambda x: x.metadata.timestamp, reverse=True)

        # Take the first item, if it exists -- this is the most recent, previous run
        return (filtered_stats[0:1] or [None])[0]

    def load_average(self) -> AggregatedStats:
        raise NotImplementedError('Average stats is not implemented for files at this time.')

    def __load_stats_from_files(self, suffix: str = '.stats.json') -> List[AggregatedStats]:
        path = self.stats_config.path
        stats_files = [os.path.join(path, file)
                       for file in os.listdir(path) if file.endswith(suffix)]
        
        aggregated_stats_list = []
        for stats_file in stats_files:
            with open(stats_file) as json_file:
                aggregated_stats_list.append(AggregatedStats(**json.load(json_file)))

        return aggregated_stats_list

    def __verify_metadata(self, loaded_metadata: StatsMetadata):
        return all([
            loaded_metadata.environment == self.stats_config.env,
            loaded_metadata.tag == self.stats_config.comp_tag,
            loaded_metadata.num_total_users == self.metadata.num_total_users,
            loaded_metadata.num_users_per_second == self.metadata.num_users_per_second,
            loaded_metadata.stats_reset_after_spawn == self.metadata.stats_reset_after_spawn,
            # Pick some delta that the runtimes should be under -- in this case, we're using 0.2
            loaded_metadata.total_runtime - self.metadata.total_runtime < 0.2
        ])

class StatsAthenaLoader(StatsLoader):
    def load_previous(self) -> AggregatedStats:
        return super().load_previous()

    def load_average(self) -> AggregatedStats:
        return super().load_average()
