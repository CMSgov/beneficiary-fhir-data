'''Set up Locust tests from the configuration.
'''

import os


def is_worker_thread() -> bool:
    '''Checks if the currently running test thread is a worker thread.

    Returns false if the test is not running in distributed mode.
    '''

    return 'LOCUST_MODE_WORKER' in os.environ and os.environ['LOCUST_MODE_WORKER'] == "True"


def is_master_thread() -> bool:
    '''Checks if the currently running test thread is the singular master test thread.

    Returns false if the test is not running in distributed mode.
    '''

    return 'LOCUST_MODE_MASTER' in os.environ and os.environ['LOCUST_MODE_MASTER'] == "True"


def is_distributed() -> bool:
    '''Checks if the currently running test is running in distributed mode.
    '''

    return 'LOCUST_MODE_MASTER' in os.environ or 'LOCUST_MODE_WORKER' in os.environ
