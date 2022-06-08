from abc import ABC, abstractmethod

from common.stats.aggregated_stats import AggregatedStats
from common.stats.stats_config import StatsComparisonType, StatsConfiguration


class StatsLoader(ABC):
    """Loads AggregatedStats depending on what type of comparison is requested"""

    def __init__(self, stats_config: StatsConfiguration) -> None:
        self.stats_config = stats_config

    def load(self) -> AggregatedStats:
        """Loads an AggregatedStats instance constructed based on what type of comparison is required

        Returns:
            AggregatedStats: An AggregatedStats instance representing the set of stats requested to load
        """
        is_avg_compare = self.stats_config.compare == StatsComparisonType.AVERAGE
        return self._load_average() if is_avg_compare else self._load_previous()

    @abstractmethod
    def _load_previous(self) -> AggregatedStats:
        """Loads an AggregatedStats instance constructed based on the most recent, previous test suite runs' 
        stats under the tag specified by the user

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of the previous test suite run
        """
        pass

    @abstractmethod
    def _load_average(self) -> AggregatedStats:
        """Loads an AggregatedStats instance constructed based on the the average of all of the previous test suite
        runs' stats under the tag specified by the user

        Returns:
            AggregatedStats: An AggregatedStats instance representing the stats of all specified previous test suite runs
        """
        pass
