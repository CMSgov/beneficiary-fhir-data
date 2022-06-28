'''Set up Locust tests from the configuration.
'''

import os
from locust.env import Environment


def is_locust_worker(env: Environment) -> bool:
    """Returns whether or not the current Locust Runner (or thread) is a worker.
    A worker runs Tasks provided by the "master" Locust Runner/thread.

    Args:
        env (Environment): Locust environment

    Returns:
        bool: True if the Runner/thread is a worker, False otherwise
    """
    return ('LOCUST_MODE_WORKER' in os.environ and os.environ['LOCUST_MODE_WORKER'] == "True") or env.parsed_options.worker


def is_locust_master(env: Environment) -> bool:
    """Returns whether or not the current Locust Runner (or thread) is the "master".
    The "master" does not run any Tasks, however it does orchestrate test running for workers.

    Args:
        env (Environment): Locust environment

    Returns:
        bool: True if the Runner/thread is the "master", False otherwise
    """
    return ('LOCUST_MODE_MASTER' in os.environ and os.environ['LOCUST_MODE_MASTER'] == "True") or env.parsed_options.master


def is_distributed(env: Environment) -> bool:
    """Returns whether or not Locust is running in distributed mode.

    Args:
        env (Environment): Locust environment

    Returns:
        bool: True if the Locust is running in distributed mode, False otherwise
    """
    return 'LOCUST_MODE_MASTER' in os.environ or 'LOCUST_MODE_WORKER' in os.environ or env.parsed_options.master or env.parsed_options.worker
