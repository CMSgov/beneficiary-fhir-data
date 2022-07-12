"""Validate tests against target SLIs"""
import logging
import time
from enum import Enum
from typing import Optional

import gevent
from locust.env import Environment
from locust.runners import STATE_CLEANUP, STATE_STOPPED, STATE_STOPPING

_DEFAULT_SLA_FAILSAFE = 10000
"""Default failsafe, in ms, that the test's response time should not exceed.
Used only if _VALIDATION_GOAL is unset."""
_validation_goal: Optional["ValidationGoal"] = None
"""The goals against which to measure these results. Note that if unset the failsafe
validation will default to 10000ms"""

# TODO: Pull these values from production metrics (i.e. New Relic)
class ValidationGoal(Enum):
    SLA_COVERAGE = (10, 100, 250)
    SLA_PATIENT = (1000, 3000, 5000)
    SLA_EOB_WITH_SINCE = (100, 250, 1000)
    SLA_EOB_WITHOUT_SINCE = (500, 1000, 3000)
    SLA_V1_BASELINE = (10, 325, 550)
    SLA_V2_BASELINE = (10, 325, 550)

    def __init__(self, sla_50: int, sla_95: int, sla_99: int) -> None:
        self.sla_50 = sla_50
        self.sla_95 = sla_95
        self.sla_99 = sla_99


def set_validation_goal(validation_goal: ValidationGoal) -> None:
    """Sets the validation goal that will be validated against once the test run is
    complete. Should be called prior to any work being done in a Locustfile

    Args:
        validation_goal (ValidationGoal): The validation goal to validate against
    """
    global _validation_goal
    _validation_goal = validation_goal


def setup_failsafe_event(environment: Environment) -> None:
    """Adds a listener that will add a repeating check for the global failsafe response time in
    order to stop the test if the event the environment/box under test is overwhelmed and at risk
    of crashing. Should be called during Locust's "init" event

    Args:
        environment (Environment): The current Locust environment
    """
    logging.getLogger().info("Setting up failsafe event")
    gevent.spawn(_check_global_fail, environment, _DEFAULT_SLA_FAILSAFE)


def check_sla_validation(environment: Environment) -> None:
    """Checks the SLA numbers for various percentiles based on the given sla category name. This
    function is ignored unless it is the main test thread or a non-distributed test.
    """
    if not _validation_goal:
        return

    logger = logging.getLogger()

    logger.info("Checking SLAs...")
    sla_50 = _validation_goal.sla_50
    sla_95 = _validation_goal.sla_95
    sla_99 = _validation_goal.sla_99

    if environment.stats.total.fail_ratio > 0:
        logger.error("Test failed due to request failure ratio > 0%")
        environment.process_exit_code = 1
    elif environment.stats.total.get_response_time_percentile(0.50) > sla_50:
        logger.error("Test failed due to 50th percentile response time > %d ms", sla_50)
        environment.process_exit_code = 1
    elif environment.stats.total.get_response_time_percentile(0.95) > sla_95:
        logger.error("Test failed due to 95th percentile response time > %d ms", sla_95)
        environment.process_exit_code = 1
    elif environment.stats.total.get_response_time_percentile(0.99) > sla_99:
        logger.error("Test failed due to 99th percentile response time > %d ms", sla_99)
        environment.process_exit_code = 1
    else:
        logger.info("SLAs within acceptable bounds")


def _check_global_fail(environment: Environment, fail_time_ms: int) -> None:
    """Checks if the test response time is too long (in the event the database is being
    overwhelmed) and if so, we stop the test.
    """
    if not environment.runner:
        return

    while not environment.runner.state in [
        STATE_STOPPING,
        STATE_STOPPED,
        STATE_CLEANUP,
    ]:
        time.sleep(1)
        if environment.stats.total.avg_response_time > fail_time_ms:
            logging.getLogger().warning(
                "WARNING: Test aborted due to triggering test failsafe "
                "(average response time ratio > %d ms)",
                fail_time_ms,
            )
            environment.runner.quit()
            return
