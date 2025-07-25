"""Members of this file/module are related to the comparison and subsequent validation
of performance statistics against a previous set of statistics or an average of all
previous statistics.
"""

import functools
import itertools
import json
import logging
from dataclasses import asdict, dataclass
from enum import StrEnum
from pathlib import Path
from typing import Any

from common.stats.aggregated_stats import AggregatedStats, FinalCompareResult, TaskStats
from common.stats.stats_config import StatsComparisonType, StatsConfiguration
from common.stats.stats_loaders import StatsLoader


class StatBetterIf(StrEnum):
    """Enum representing whether a given stat is better if its value is smaller or larger."""

    SMALLER = "SMALLER"
    LARGER = "LARGER"


class StatCompareResult(StrEnum):
    """Enum representing the result of a particular stat comparison."""

    PASS = "PASS"
    WARNING = "WARNING"
    FAILURE = "FAILURE"


@dataclass
class StatsComparisonMetadata:
    """Dataclass representing how a given stat should be compared against a baseline value of the
    same stat.
    """

    name: str
    """Name of the stat"""
    better_if: StatBetterIf
    """Enum indicating whether the stat is "better" if its value is smaller or larger in comparison
    to a baseline. I.e. number of requests is better if larger, but response time is better if
    smaller"""
    warn_percent: float
    """A percent threshold that if the percent ratio between the current stat and a baseline exceeds
    will be reported as a warning. Does not cause a failure for the run -- only used to indicate
    anomalously high stats"""
    fail_percent: float
    """A percent threshold that if the percent ratio between the current stat and a baseline exceeds
    will be reported as a failure. Causes a failure for the run"""


@dataclass
class AllComparisonMetadata:
    """Dataclass representing comparison metadata defining how stats are compared for both
    aggregated totals stats and individual tasks' stats.
    """

    totals: list[StatsComparisonMetadata]
    """List of metadata about each stat for aggregated totals"""
    tasks: list[StatsComparisonMetadata]
    """List of metadata about each stat for each invidividual task"""


@dataclass
class StatComparison:
    """Dataclass representing the result of a comparison between the baseline/previous value of a
    TaskStats stat and the current value of said stat.
    """

    stat: str
    """The name of the stat"""
    percent: float
    """The percent value of this stat's value versus a previous run's, i.e. this stat is percent%
	times worse than the previous run or average of all runs. A value of 100 means no change (i.e.
	current stat is 100% of a previous snapshot of the same stat), whereas values lower than 100
	generally indicate positive change and values greater than 100 indicate negative change"""
    baseline: float | int
    """The value of the stat that the stat's current value is being compared against"""
    current: float | int
    """The value of the stat from the current test run"""
    threshold: float | None
    """The percentage threshold that the comparison exceeded. Either will be the warning percentage
    threshold or the failure percentage threshold"""
    result: StatCompareResult
    """The result of the comparison depending on whether the stat exceeded a threshold or not"""


def do_stats_comparison(
    stats_config: StatsConfiguration,
    stats_metadata_path: str,
    stats: AggregatedStats,
) -> FinalCompareResult:
    """Compare the current run's stats (totals and all tasks) against a user-configured baseline,
    logging the result.

    Args:
        environment (Environment): The Locust environment
        stats_config (StatsConfiguration): The configuration read from the command line or .conf
        stats_metadata_path (str): The path to the JSON file describing the comparison metadata for
        totals and tasks
        stats (AggregatedStats): The current run's stats

    Returns:
        FinalCompareResult: The overall result of the comparison between the baseline and current
        run
    """
    if not stats_config.stats_compare:
        return FinalCompareResult.NOT_APPLICABLE

    logger = logging.getLogger()

    stats_loader = StatsLoader.create(stats_config, stats.metadata)  # type: ignore
    try:
        previous_stats = stats_loader.load()
    except RuntimeError as ex:
        logger.error(
            "%s stats were unable to be loaded from %s due to: %s",
            str(stats_config.stats_compare.value),
            str(stats_config.stats_store.value if stats_config.stats_store else ""),
            str(ex),
        )
        return FinalCompareResult.FAILED

    if not previous_stats:
        logger.warning(
            'No applicable performance statistics under tag "%s" to compare against',
            stats_config.stats_compare_tag,
        )
        return FinalCompareResult.NOT_APPLICABLE

    logger.info(
        'Comparing current run\'s aggregated stats against %s stats from tag "%s" from %s'
        " storage...",
        "previous"
        if stats_config.stats_compare == StatsComparisonType.PREVIOUS
        else f"average (last {stats_config.stats_compare_load_limit})",
        stats_config.stats_compare_tag,
        stats_config.stats_store.value if stats_config.stats_store else None,
    )
    all_comparisons_meta = _load_stats_comparison_metadata(stats_metadata_path)
    totals_exceeded_results, tasks_exceeded_results = validate_aggregated_stats(
        previous_stats, stats, all_comparisons_meta
    )

    # We don't want null values (for example, thresholds if the stat comparison passed)
    # to appear in the logged JSON. The default JSON dumper does not have any built-in logic
    # for simply ignoring nulls, so we must remove them from the dictionary when converting
    # a StatComparison to a dict
    def ignore_nulls_factory(data_class: list[tuple[str, Any]]) -> dict[str, Any]:
        return {k: v for (k, v) in data_class if v is not None}

    logger.info(
        "Totals comparison results: %s",
        json.dumps([asdict(x, dict_factory=ignore_nulls_factory) for x in totals_exceeded_results]),
    )
    logger.info(
        "Tasks comparison results: %s",
        json.dumps(
            {
                k: [asdict(meta_dict, dict_factory=ignore_nulls_factory) for meta_dict in v]
                for k, v in tasks_exceeded_results.items()
            }
        ),
    )

    # Look at _all_ of the stat comparisons and check for failures and warnings results;
    # the generator in this reduction simply flattens the totals and individual tasks
    # comparisons so that we do not have to repeat ourselves. The reduce will take the resulting
    # list of boolean tuples and collapse them into a single tuple of _all_ of them OR'd
    # together.
    any_failures, any_warnings = functools.reduce(
        lambda a, b: (a[0] or b[0], a[1] or b[1]),
        (
            (x.result == StatCompareResult.FAILURE, x.result == StatCompareResult.WARNING)
            for x in [
                *totals_exceeded_results,
                *itertools.chain.from_iterable(tasks_exceeded_results.values()),
            ]
        ),
    )
    if any_failures or any_warnings:
        # If we get here, that means some tasks or some totals stats have stats exceeding the
        # threshold percent between the previous/average run and the current.
        if any_warnings:
            logger.warning(
                'Some comparisons against %s stats under "%s" tag exceeded their warn '
                "thresholds, see result JSON in log above",
                str(stats_config.stats_compare.value),
                stats_config.stats_compare_tag,
            )

        if any_failures:
            logger.error(
                'Some comparisons against %s stats under "%s" tag exceeded their failure '
                "thresholds, see result JSON in log above. Returning exit code 1 to indicate "
                "failure",
                str(stats_config.stats_compare.value),
                stats_config.stats_compare_tag,
            )

            return FinalCompareResult.FAILED
    else:
        logger.info(
            'Comparison against %s stats under "%s" tag passed, see JSON in log above',
            stats_config.stats_compare.value,
            stats_config.stats_compare_tag,
        )

    return FinalCompareResult.PASSED


def get_stats_compare_results(
    previous: TaskStats,
    current: TaskStats,
    stats_compare_metadata: list[StatsComparisonMetadata],
) -> list[StatComparison]:
    """Get the comparison results between two TaskStats instances by comparing each of their stat
    fields.

    Args:
        previous (TaskStats): The TaskStats to compare against
        current (TaskStats): The TaskStats representing the statistics of the current run for a
        given Locust Task

    Returns:
            List[StatComparison]: A list of comparison results for each TasksStats stats
    """
    prev_stats = {**asdict(previous), **previous.response_time_percentiles}
    cur_stats = {**asdict(current), **current.response_time_percentiles}
    # Compute the list of StatCompareResults for each type of field
    return _compute_results_list(prev_stats, cur_stats, stats_compare_metadata)


def validate_aggregated_stats(
    previous: AggregatedStats, current: AggregatedStats, all_comparisons_meta: AllComparisonMetadata
) -> tuple[list[StatComparison], dict[str, list[StatComparison]]]:
    """Validate and compare the given AggregatedStats instances against each other, checking each
    of their common TaskStats and returning a dictionary of the name of those tasks that exceed the
    given threshold to the actual stats that failed.

    Args:
            previous (AggregatedStats): A previous run or average of all previous runs that will
            be compared against for the current run
            current (AggregatedStats): The current run's stats
            all_comparisons_meta (AllComparisonMetadata): All of the stat comparison metadata needed
            to compare stats

    Returns:
            Tuple[List[StatComparison], Dict[str, List[StatComparison]]]: A Tuple where the first
            entry is the list of comparison results for the aggregated totls and the second entry
            is a dictionary of tasks to their list of comparison results
    """
    prev_tasks = {task.task_name: task for task in previous.tasks}
    cur_tasks = {task.task_name: task for task in current.tasks}
    common_tasks = prev_tasks.keys() & cur_tasks.keys()

    # all_tasks_results will be a dictionary of all of the StatCompareResults for _each individual
    # task_ keyed by the name of said Task
    all_tasks_results = {}
    for task in common_tasks:
        prev_task = prev_tasks[task]
        cur_task = cur_tasks[task]

        compare_results = get_stats_compare_results(prev_task, cur_task, all_comparisons_meta.tasks)
        all_tasks_results[task] = compare_results

    # Compare totals
    prev_totals = previous.totals
    cur_totals = current.totals
    totals_results = get_stats_compare_results(prev_totals, cur_totals, all_comparisons_meta.totals)

    return (totals_results, all_tasks_results)


def _load_stats_comparison_metadata(path: str) -> AllComparisonMetadata:
    with Path(path).open(encoding="utf-8") as json_file:
        as_json = json.load(json_file)
        return AllComparisonMetadata(
            totals=[StatsComparisonMetadata(**meta_dict) for meta_dict in as_json["totals"]],
            tasks=[StatsComparisonMetadata(**meta_dict) for meta_dict in as_json["tasks"]],
        )


def _compare_stat(
    name: str,
    prev_value: int | float,
    cur_value: int | float,
    metadata: StatsComparisonMetadata,
) -> StatComparison:
    dividend = prev_value if metadata.better_if == StatBetterIf.LARGER else cur_value
    divisor = cur_value if metadata.better_if == StatBetterIf.LARGER else prev_value
    percent_ratio = _safe_division(dividend, divisor) * 100.0

    threshold_exceeded = None
    compare_result = StatCompareResult.PASS
    if percent_ratio >= metadata.fail_percent:
        threshold_exceeded = metadata.fail_percent
        compare_result = StatCompareResult.FAILURE
    elif percent_ratio >= metadata.warn_percent:
        threshold_exceeded = metadata.warn_percent
        compare_result = StatCompareResult.WARNING

    return StatComparison(
        stat=name,
        percent=percent_ratio,
        baseline=prev_value,
        current=cur_value,
        threshold=threshold_exceeded,
        result=compare_result,
    )


def _compute_results_list(
    prev_stats: dict[str, Any],
    cur_stats: dict[str, Any],
    comparisons_metadata: list[StatsComparisonMetadata],
) -> list[StatComparison]:
    # Uses list comprehension to compute the relative percent "worseness" of a given stat in the
    # dict when comparing a previous run of the given stats versus the current run.
    comparison_meta_by_name = {
        compare_meta.name: compare_meta for compare_meta in comparisons_metadata
    }
    return [
        _compare_stat(stat, prev_stats[stat], cur_stats[stat], comparison_meta_by_name[stat])
        for stat in comparison_meta_by_name
    ]


def _safe_division(dividend: float | int, divisor: float | int) -> float:
    # Often some fields will be 0 (such as the failure stats), and dividing by 0 results
    # in an error. So, we can check first if the divisor is 0 before dividing to ensure we don't
    # raise an error unexpectedly when comparing stats
    if divisor == 0:
        return 0

    return dividend / divisor
