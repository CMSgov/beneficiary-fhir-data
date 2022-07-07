"""Set up Locust tests from the configuration.
"""

import os
from locust.env import Environment
from locust.runners import WorkerRunner, MasterRunner


def is_locust_worker(env: Environment) -> bool:
    """Returns whether or not the current Locust Runner (or thread) is a worker.
    A worker runs Tasks provided by the "master" Locust Runner/thread.

    Args:
        env (Environment): Locust environment

    Returns:
        bool: True if the Runner/thread is a worker, False otherwise
    """
    return isinstance(env.runner, WorkerRunner)


def is_locust_master(env: Environment) -> bool:
    """Returns whether or not the current Locust Runner (or thread) is the "master".
    The "master" does not run any Tasks, however it does orchestrate test running for workers.

    Args:
        env (Environment): Locust environment

    Returns:
        bool: True if the Runner/thread is the "master", False otherwise
    """
    return isinstance(env.runner, MasterRunner)


def is_distributed(env: Environment) -> bool:
    """Returns whether or not Locust is running in distributed mode.

    Args:
        env (Environment): Locust environment

    Returns:
        bool: True if the Locust is running in distributed mode, False otherwise
    """
    return is_locust_master(env) or is_locust_worker(env)
