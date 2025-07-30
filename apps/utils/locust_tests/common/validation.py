"""Validate tests against target SLIs."""

import logging
import time
from enum import Enum, StrEnum
from typing import Optional

import gevent
from locust.env import Environment
from locust.runners import STATE_CLEANUP, STATE_STOPPED, STATE_STOPPING

_DEFAULT_SLA_FAILSAFE = 10000
"""Default failsafe, in ms, that the test's response time should not exceed"""
_validation_goal: Optional["ValidationGoal"] = None
"""The SLA goals against which to measure the test run's results"""


class ValidationResult(StrEnum):
    """Enum representing the result of failure ratio and SLA validation."""

    NOT_APPLICABLE = "NOT_APPLICABLE"
    """Indicates that no validation ran on the given test run"""
    PASSED = "PASSED"
    """Indicates that the test run passed validation"""
    FAILED = "FAILED"
    """Indicates that the test run failed validation"""


# TODO: Pull these values from production metrics (i.e. New Relic)
class ValidationGoal(Enum):
    SLA_COVERAGE = (10, 100, 250)
    SLA_PATIENT = (1000, 3000, 5000)
    SLA_EOB_WITH_SINCE = (100, 250, 1000)
    SLA_EOB_WITHOUT_SINCE = (500, 1000, 3000)
    SLA_V1_BASELINE = (700, 1000, 3000)
    SLA_V2_BASELINE = (700, 1000, 3000)  # noqa: PIE796

    def __init__(self, sla_50: int, sla_95: int, sla_99: int) -> None:
        self.sla_50 = sla_50
        self.sla_95 = sla_95
        self.sla_99 = sla_99


def set_validation_goal(validation_goal: ValidationGoal) -> None:
    """Set the validation goal that will be validated against once the test run is
    complete. Should be called prior to any work being done in a Locustfile.

    Args:
        validation_goal (ValidationGoal): The validation goal to validate against
    """
    global _validation_goal  # noqa: PLW0603
    _validation_goal = validation_goal


def setup_failsafe_event(environment: Environment) -> None:
    """Add a listener that will add a repeating check for the global failsafe response time in
    order to stop the test if the event the environment/box under test is overwhelmed and at risk
    of crashing. Should be called during Locust's "init" event.

    Args:
        environment (Environment): The current Locust environment
    """
    logging.getLogger().info("Setting up failsafe event")
    gevent.spawn(_check_global_fail, environment, _DEFAULT_SLA_FAILSAFE)


def check_validation_goals(environment: Environment) -> ValidationResult:
    """Check if either the failure ratio exceeds 0% and if any of the percentile SLAs specified by
    the validation goal are exceeded. If exceeded, a validation result indicating failure is
    returned; else a result indicating a pass is returned. This function is ignored unless it is the
    main test thread or a non-distributed test. If _validation_goal is undefined, only failure ratio
    is checked.

    Args:
        environment (Environment): The Locust environment of the current test run

    Returns:
        ValidationResult: FAILURE if either failure ratio is greater than 0% or any SLA percentiles
        exceed their static thresholds defined by the validation goal. PASS otherwise
    """
    logger = logging.getLogger()

    logger.info("Checking overall failure ratio...")
    if environment.stats.total.fail_ratio > 0:
        logger.error("Test failed due to request failure ratio > 0%")
        return ValidationResult.FAILED

    logger.info("Failure ratio is 0%")

    if _validation_goal:
        sla_50 = _validation_goal.sla_50
        sla_95 = _validation_goal.sla_95
        sla_99 = _validation_goal.sla_99

        logger.info("Checking 50%, 95% and 99% SLAs...")
        if environment.stats.total.get_response_time_percentile(0.50) > sla_50:
            logger.error("Test failed due to 50th percentile response time > %d ms", sla_50)
            return ValidationResult.FAILED

        if environment.stats.total.get_response_time_percentile(0.95) > sla_95:
            logger.error("Test failed due to 95th percentile response time > %d ms", sla_95)
            return ValidationResult.FAILED

        if environment.stats.total.get_response_time_percentile(0.99) > sla_99:
            logger.error("Test failed due to 99th percentile response time > %d ms", sla_99)
            return ValidationResult.FAILED

        logger.info("SLAs within acceptable bounds")

    return ValidationResult.PASSED


def _check_global_fail(environment: Environment, fail_time_ms: int) -> None:
    """Check if the test response time is too long (in the event the database is being
    overwhelmed) and if so, we stop the test.
    """
    if not environment.runner:
        return

    while environment.runner.state not in [
        STATE_STOPPING,
        STATE_STOPPED,
        STATE_CLEANUP,
    ]:
        time.sleep(1)
        if environment.stats.total.avg_response_time > fail_time_ms:
            logging.getLogger().warning(
                "WARNING: Test aborted due to triggering test failsafe (average response time ratio"
                " > %d ms)",
                fail_time_ms,
            )
            environment.runner.quit()
            return
