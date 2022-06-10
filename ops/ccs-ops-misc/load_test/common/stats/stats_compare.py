from dataclasses import dataclass
from typing import Dict, List, Union
from common.stats.aggregated_stats import AggregatedStats, TaskStats

DEFAULT_PERCENT_THRESHOLD = 500.0
"""The default percent threshold that stats should not exceed or be worse than compared to a previous or average run"""


@dataclass
class StatPercent:
    """Dataclass representing a given TaskStats field/stat and the percent relativity between the current run's value
    and a previous run's value of the same stat"""
    stat: str
    """The name of the stat"""
    percent: float
    """The percent value of this stat's value versus a previous run's, i.e. this stat is percent% worse than
    the previous run or average of all runs. A value of 100 means no change, whereas values lower than 100 generally
    indicate positive change and values greater than 100 indicate negative change"""


def get_stats_relative_percent(previous: TaskStats, current: TaskStats) -> List[StatPercent]:
    """Compute the percent relativity (i.e. previous is n% of current) between two TaskStats
    instances by comparing each of their stat fields

    Args:
        previous (TaskStats): The TaskStats to compare against
        current (TaskStats): The TaskStats representing the statistics of the current run for a given Locust Task

    Returns:
        List[StatPercent]: A list of percent relativity indicating how much worse or better each stat is for each stat field in the current TaskStats
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
    percents_higher = [StatPercent(field, _safe_division(getattr(previous, field),
                                                         getattr(current, field)) * 100.0) for field in fields_higher_better]
    percents_lower = [StatPercent(field, _safe_division(getattr(current, field),
                                                        getattr(previous, field)) * 100.0) for field in fields_lower_better]

    # Percentiles are stored as a dict within TaskStats, so we need to get the keys common to
    # both TaskStats and use those as the field names (hence why getattr() isn't being used)
    prev_percentiles = previous.response_time_percentiles
    cur_percentiles = current.response_time_percentiles
    percentiles = prev_percentiles.keys() & cur_percentiles.keys()
    percents_percentiles = [StatPercent(p, _safe_division(cur_percentiles[p],
                                                          prev_percentiles[p]) * 100.0) for p in percentiles]

    # Combine all of the lists created above into a single list with all of the important
    # stats and their relative percentages
    return percents_higher + percents_lower + percents_percentiles


def get_stats_above_threshold(stat_percents: List[StatPercent], threshold: float) -> List[StatPercent]:
    """Computes the list of StatPercents whose relative percentages exceed a given threshold

    Args:
        stat_percents (List[StatPercent]): A list of relative stat percents to the current run of a Task and a previous run of a Task
        threshold (float, optional): Percentage threshold that the delta should not exceed. Defaults to 500.0.

    Returns:
        List[StatPercent]: A filtered list of stats with relative percent increase that exceed the given threshold
    """
    return [stat_delta for stat_delta in stat_percents if stat_delta.percent > threshold]


def validate_aggregated_stats(previous: AggregatedStats, current: AggregatedStats, threshold: float = DEFAULT_PERCENT_THRESHOLD) -> Dict[str, List[StatPercent]]:
    """Validates and compares the given AggregatedStats instances against each other, checking each of their common
    TaskStats and returning a dictionary of the name of those tasks that exceed the given threshold to the actual stats
    that failed

    Args:
        previous (AggregatedStats): A previous run or average of all previous runs that will be compared against for the curren run
        current (AggregatedStats): The current run's stats
        threshold (float, optional): A percent threshold that the current run's task's stats must not exceed/be worse than. Defaults to DEFAULT_PERCENT_THRESHOLD

    Returns:
        Dict[str, List[StatPercent]]: A dictionary of failing task names to the stats that failed along with their percent relativity
    """
    prev_tasks = {task.task_name: task for task in previous.tasks}
    cur_tasks = {task.task_name: task for task in current.tasks}
    common_tasks = prev_tasks.keys() & cur_tasks.keys()

    failed_tasks_with_percents = {}
    for task in common_tasks:
        prev_task = prev_tasks[task]
        cur_task = cur_tasks[task]

        deltas = get_stats_relative_percent(prev_task, cur_task)
        failed_deltas = get_stats_above_threshold(deltas, threshold)
        if failed_deltas != []:
            failed_tasks_with_percents[task] = failed_deltas

    return failed_tasks_with_percents


def _safe_division(dividend: Union[float, int], divisor: Union[float, int]) -> float:
    # Often some fields will be 0 (such as the failure stats), and dividing by 0 results
    # in an error. So, we can check first if the divisor is 0 before dividing to ensure we don't
    # raise an error unexpectedly when comparing stats
    if divisor == 0:
        return 0

    return dividend / divisor
