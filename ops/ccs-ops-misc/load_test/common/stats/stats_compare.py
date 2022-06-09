from dataclasses import dataclass
from typing import List, Union
from common.stats.aggregated_stats import TaskStats


@dataclass
class StatDelta:
    stat: str
    """The name of the stat"""
    delta: float
    """The perent delta difference; larger values indicate worse performance"""


def get_stats_delta(previous: TaskStats, current: TaskStats) -> List[StatDelta]:
    """Compute the percentage difference between two TaskStats instances by comparing
    each of their stat fields and return the corresponding percentage deltas

    Args:
        previous (TaskStats): The TaskStats to compare against
        current (TaskStats): The TaskStats representing the statistics of the current run for a given Locust Task

    Returns:
        List[StatDelta]: A list of deltas for each stat field in the current TaskStats
    """
    # Here we define lists of TaskStats fields that will be compared depending on
    # whether a larger value is better (i.e. more requests per second) or a smaller
    # value is better (i.e. less failures). This determines which of the two TaskStats
    # (previous or current) is the dividend and divisor, as order matters.
    fields_higher_better = ['num_requests', 'total_reqs_per_second']
    fields_lower_better = ['num_failures', 'median_response_time', 'average_response_time',
                           'min_response_time', 'max_response_time', 'total_fails_per_sec']

    # Uses list comprehension to compute how much greater/lesser in percent (percent delta)
    # each field is when comparing a previous run of the given task versus the current run.
    deltas_higher = [StatDelta(field, _safe_division(getattr(previous, field),
                                                     getattr(current, field))) for field in fields_higher_better]
    deltas_lower = [StatDelta(field, _safe_division(getattr(current, field),
                                                    getattr(previous, field))) for field in fields_lower_better]

    # Percentiles are stored as a dict within TaskStats, so we need to get the keys common to
    # both TaskStats and use those as the field names (hence why getattr() isn't being used)
    prev_percents = previous.response_time_percentiles
    cur_percents = current.response_time_percentiles
    percents = prev_percents.keys() & cur_percents.keys()
    deltas_percents = [StatDelta(_get_readable_percentile(p), _safe_division(cur_percents[p],
                                                                             prev_percents[p])) for p in percents]

    # Combine all of the lists created above into a single list with all of the important
    # stats and their percentage deltas
    return deltas_higher + deltas_lower + deltas_percents


def get_stats_above_threshold(stat_deltas: List[StatDelta], threshold: float = 500.0) -> List[StatDelta]:
    """Computes the list of StatDeltas whose percentage deltas exceed a given threshold, defaulting to
    500% (or 5x)

    Args:
        stat_deltas (List[StatDelta]): A list of deltas between the current run of a Task and a previous run of a Task
        threshold (float, optional): Percentage threshold that the delta should not exceed. Defaults to 500.0.

    Returns:
        List[StatDelta]: A filtered list of stats with deltas that exceed the given threshold
    """
    return [stat_delta for stat_delta in stat_deltas if stat_delta.delta > threshold]


def _safe_division(dividend: Union[float, int], divisor: Union[float, int]) -> float:
    # Often some fields will be 0 (such as the failure stats), and dividing by 0 results
    # in an error. So, we can check first if the divisor is 0 before dividing to ensure we don't
    # raise an error unexpectedly when comparing stats
    if divisor == 0:
        return 0

    return dividend / divisor


def _get_readable_percentile(percentile: float) -> str:
    return f"{int(percentile * 100) if (percentile * 100).is_integer() else round(100 * percentile, 6)}%"
