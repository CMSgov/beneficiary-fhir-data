from typing import Callable, List
import datetime
import os
from . import config, test_setup as setup
import logging


def load_data_segment(load_function: Callable, *args) -> List:
    """
    Loads a segment of data and either returns all the data in a list if not a distributed test,
    or takes a percentage of the data to distribute to the current worker thread. The percentage
    of the data in distributed mode depends on the total number of workers and the index of the
    data is dependant on which worker index calls this method.
    """

    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return
    elif setup.is_worker_thread():
        logger = logging.getLogger()
        worker_number = str(os.environ['LOCUST_WORKER_NUM'])
        num_workers = os.environ['LOCUST_NUM_WORKERS']
        logger.info(f"Worker {worker_number} starting...")
        configFile = config.load()
        full_data_list = load_function(configFile['dbUri'], *args)
        data_per_user = len(full_data_list) // int(num_workers)
        start_index = int(worker_number) * data_per_user
        end_index = start_index + data_per_user - 1
        logger.info(f"Worker {worker_number} using data from indexes {start_index} to {end_index}")
        return full_data_list[start_index:end_index]
    else:
        configFile = config.load()
        return load_function(configFile['dbUri'], *args)


def get_last_updated() -> str:
    """
    Gets a sample last_updated field for testing. Uses a date two weeks before when the script is run.
    """
    today = datetime.datetime.now()
    delta = datetime.timedelta(weeks = 2)
    prior_date = today - delta
    return prior_date.strftime('%Y-%m-%d')
