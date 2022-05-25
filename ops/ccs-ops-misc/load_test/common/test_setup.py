'''Set up Locust tests from the configuration.
'''

import os
from typing import Dict
from common import config
from locust.main import main

from common.stats import StatsStorageConfig


def get_client_cert() -> str:
    '''Checks the config file for the client cert value.
    '''

    config_file = config.load()
    return config_file["clientCertPath"]


def load_server_public_key() -> str:
    '''Load the public key to verify the BFD Server's responses or else ignore the warnings from
    the self-signed cert.
    '''

    try:
        config_file = config.load()
        server_public_key = config_file["serverPublicKey"]
        return server_public_key if server_public_key else False
    except KeyError:
        return False

def load_stats_storage_config() -> StatsStorageConfig:
    """Load the storage configuration for storing aggregated statistics.

    Returns:
        StatsStorageConfig: A dataclass representing the storage configuration for aggregated statistics
    """

    # TODO: This may not be the best spot for this method, since it's used during teardown not setup.
    # Maybe add a new test_teardown.py, or move the loading of values from config to config.py?
    config_file = config.load()
    return config_file["storeStats"]


def set_locust_env(config_file: Dict[str, str]):
    '''Sets a number of locust variables needed to run the test.
    '''

    os.environ['LOCUST_HOST'] = config_file["testHost"]
    os.environ['LOCUST_HEADLESS'] = "True"
    os.environ['LOCUST_USERS'] = config_file["testNumTotalClients"]
    os.environ['LOCUST_SPAWN_RATE'] = config_file["testCreatedClientsPerSecond"]
    os.environ['LOCUST_LOGLEVEL'] = "INFO"
    os.environ['LOCUST_RESET_STATS'] = str(
        config_file["resetStatsAfterClientSpawn"])
    # Set the runtime if not running distributed or if test master
    if not is_distributed() or is_master_thread():
        os.environ['LOCUST_RUN_TIME'] = config_file["testRunTime"]


def run_master_test(workers: int):
    '''Sets some settings specific to the distributed master test and begins the main test.
    '''

    os.environ['LOCUST_MODE_WORKER'] = "False"
    os.environ['LOCUST_MODE_MASTER'] = "True"
    os.environ['LOCUST_EXPECT_WORKERS'] = workers
    main()


def run_worker_test(worker_num: int, workers: int):
    '''Sets some settings specific to the distributed worker test and begins the worker test.

    This should be called in a threaded capacity.
    '''

    os.environ['LOCUST_MODE_WORKER'] = "True"
    os.environ['LOCUST_MODE_MASTER'] = "False"
    os.environ['LOCUST_NUM_WORKERS'] = workers
    os.environ['LOCUST_WORKER_NUM'] = str(worker_num)
    main()


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


def reset_distributed_values():
    '''Resets the distributed mode environment variables, to allow for non-distributed test runs.
    '''

    if "LOCUST_MODE_WORKER" in os.environ:
        os.environ.pop("LOCUST_MODE_WORKER")
    if "LOCUST_MODE_MASTER" in os.environ:
        os.environ.pop("LOCUST_MODE_MASTER")
    if "LOCUST_EXPECT_WORKERS" in os.environ:
        os.environ.pop("LOCUST_EXPECT_WORKERS")
    if "LOCUST_WORKER_NUM" in os.environ:
        os.environ.pop("LOCUST_WORKER_NUM")
    if "LOCUST_NUM_WORKERS" in os.environ:
        os.environ.pop("LOCUST_NUM_WORKERS")


def set_locust_test_name(test_file_name):
    '''Sets the test name for the test, useful when running from the command line.
    '''

    os.environ['LOCUST_LOCUSTFILE'] = test_file_name


def disable_no_cert_warnings(server_public_key, urllib3):
    '''If there is no server cert, disable warnings because thousands will appear in the logs and
    make it difficult to see anything else.

    We need to pass in urllib3 because if it's imported in this class it causes some conflict
    with locust and produces a recursion error.
    '''

    if not server_public_key:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
