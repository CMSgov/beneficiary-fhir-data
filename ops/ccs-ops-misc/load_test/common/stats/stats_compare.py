"""Members of this file/module are related to the comparison and subsequent validation 
of performance statistics against a previous set of statistics or an average of all
previous statistics"""
from dataclasses import asdict, dataclass
from typing import Dict, List, Set, Type, Union
from common.stats.aggregated_stats import AggregatedStats, TaskStats

TaskStatsOrPercentiles = type(Union[TaskStats, Dict[str, int]])
"""Type indicating a value that is either a TaskStats instance or a response time
percentiles dictionary"""

DEFAULT_DEVIANCE_FAILURE_THRESHOLD = 500.0
"""The default percent threshold that stats should not exceed or be worse than compared to a previous or average run"""


@dataclass
class StatCompareResult:
    """Dataclass representing the result of a comparison between the baseline/previous value of a
    TaskStats stat and the current value of said stat"""
    stat: str
    """The name of the stat"""
    percent: float
    """The percent value of this stat's value versus a previous run's, i.e. this stat is percent% times worse than
    the previous run or average of all runs. A value of 100 means no change (i.e. current stat is 100% 
    of a previous snapshot of the same stat), whereas values lower than 100 generally indicate
    positive change and values greater than 100 indicate negative change"""
    baseline: Union[float, int]
    """The value of the stat that the stat's current value is being compared against"""
    current: Union[float, int]
    """The value of the stat from the current test run"""


def get_stats_compare_results(previous: TaskStats, current: TaskStats) -> List[StatCompareResult]:
    """Get the comparison results between two TaskStats instances by comparing each of their stat fields

    Args:
        previous (TaskStats): The TaskStats to compare against
        current (TaskStats): The TaskStats representing the statistics of the current run for a given Locust Task

    Returns:
        List[StatCompareResult]: A list of comparison results for each TasksStats stats
    """
    # Here we define sets of TaskStats fields that will be compared depending on
    # whether a larger value is better (i.e. more requests per second) or a smaller
    # value is better (i.e. less failures). This determines which of the two TaskStats
    # (previous or current) is the dividend and divisor, as order matters.
    fields_higher_better = {'num_requests', 'total_reqs_per_second'}
    fields_lower_better = {'num_failures', 'median_response_time', 'average_response_time',
                           'min_response_time', 'max_response_time', 'total_fails_per_sec'}

    # Compute the list of StatCompareResults for each type of field
    results_higher = _compute_results_list(previous, current, fields_higher_better, 
                                           is_higher_val_better=True)
    results_lower = _compute_results_list(previous, current, fields_lower_better,
                                          is_higher_val_better=False)

    # Percentiles are stored as a dict within TaskStats, so we need to get the keys common to
    # both response percentiles dicts
    prev_percentiles = previous.response_time_percentiles
    cur_percentiles = current.response_time_percentiles
    percentiles = prev_percentiles.keys() & cur_percentiles.keys()
    results_percentiles = _compute_results_list(prev_percentiles, cur_percentiles, percentiles,
                                                is_higher_val_better=False)

    # Combine all of the lists created above into a single list with all of the important
    # stats and their comparison results
    return results_higher + results_lower + results_percentiles


def get_stats_above_threshold(compare_results: List[StatCompareResult], threshold: float) -> List[StatCompareResult]:
    """Computes the list of  whose relative percentages exceed a given threshold

    Args:
        stat_percents (List[StatCompareResult]): A list of comparison results between the baseline and current run
        threshold (float, optional): Percentage threshold that the delta should not exceed. Defaults to 500.0.

    Returns:
        List[StatCompareResult]: A filtered list of comparison results that exceed the threshold percentage given
    """
    return [stat_delta for stat_delta in compare_results if stat_delta.percent > threshold]


def validate_aggregated_stats(previous: AggregatedStats, current: AggregatedStats, threshold: float = DEFAULT_DEVIANCE_FAILURE_THRESHOLD) -> Dict[str, List[StatCompareResult]]:
    """Validates and compares the given AggregatedStats instances against each other, checking each of their common
    TaskStats and returning a dictionary of the name of those tasks that exceed the given threshold to the actual stats
    that failed

    Args:
        previous (AggregatedStats): A previous run or average of all previous runs that will be compared against for the curren run
        current (AggregatedStats): The current run's stats
        threshold (float, optional): A percent threshold that the current run's task's stats must not exceed/be worse than. Defaults to DEFAULT_PERCENT_THRESHOLD

    Returns:
        Dict[str, List[StatCompareResult]]: A dictionary of failing task names to the stats that failed along with their comparison results
    """
    prev_tasks = {task.task_name: task for task in previous.tasks}
    cur_tasks = {task.task_name: task for task in current.tasks}
    common_tasks = prev_tasks.keys() & cur_tasks.keys()

    failed_tasks_with_percents = {}
    for task in common_tasks:
        prev_task = prev_tasks[task]
        cur_task = cur_tasks[task]

        deltas = get_stats_compare_results(prev_task, cur_task)
        failed_deltas = get_stats_above_threshold(deltas, threshold)
        if failed_deltas:
            failed_tasks_with_percents[task] = failed_deltas

    return failed_tasks_with_percents


def _compute_results_list(previous: TaskStatsOrPercentiles, current: TaskStatsOrPercentiles,
                          keys: Set[str], is_higher_val_better: bool) -> List[StatCompareResult]:
    # Convert TaskStats instances to dicts so that we can use the same logic for both
    prev_dict = asdict(previous) if isinstance(previous, TaskStats) else previous
    cur_dict = asdict(current) if isinstance(current, TaskStats) else current
    
    # Uses list comprehension to compute the relative percent "worseness" of a given stat in the 
    # dict when comparing a previous run of the given stats versus the current run.
    dividend = prev_dict if is_higher_val_better else cur_dict
    divisor = cur_dict if is_higher_val_better else prev_dict
    return [StatCompareResult(key,
                              _safe_division(dividend[key], divisor[key]) * 100.0,
                              baseline=prev_dict[key], current=cur_dict[key])
            for key in keys]

def _safe_division(dividend: Union[float, int], divisor: Union[float, int]) -> float:
    # Often some fields will be 0 (such as the failure stats), and dividing by 0 results
    # in an error. So, we can check first if the divisor is 0 before dividing to ensure we don't
    # raise an error unexpectedly when comparing stats
    if divisor == 0:
        return 0

    return dividend / divisor
