'''Set up data for use in the tests.'''

from typing import Callable, List
import datetime
import os

from . import config, test_setup as setup


def load_all(load_function: Callable, *args, use_table_sample: bool = False) -> List:
    '''Loads all of the data from the database, using the database connection from the
    configuration file.'''

    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return []

    config_file = config.load()
    print('Collecting test data...')
    if use_table_sample:
        table_sample_pct = config_file.get('tableSamplePct', 0.25)
        print(f"Table Sampling at: {table_sample_pct}")
        results = load_function(uri=config_file['dbUri'], table_sample_pct=table_sample_pct, *args)
    else:
        results = load_function(uri=config_file['dbUri'], *args)

    print(f'Loaded {len(results)} results from the database')
    return results


def load_data_segment(load_function: Callable, *args) -> List:
    '''Loads a segment of data and either returns all the data in a list if not a distributed test,
    or takes a percentage of the data to distribute to the current worker thread.

    The percentage of the data in distributed mode depends on the total number of workers and the
    index of the data is dependant on which worker index calls this method.
    '''

    if setup.is_master_thread():
        ## Don't bother loading data for the master thread, it doenst run a test
        return []

    if setup.is_worker_thread():
        worker_number = str(os.environ['LOCUST_WORKER_NUM'])
        num_workers = os.environ['LOCUST_NUM_WORKERS']
        print(f"Worker {worker_number} loading segmented data...")
        full_data_list = load_all(load_function, *args)
        data_per_user = len(full_data_list) // int(num_workers)
        start_index = int(worker_number) * data_per_user
        end_index = start_index + data_per_user - 1
        print(f"Worker {worker_number} using data from indexes {start_index} to {end_index}")
        return full_data_list[start_index:end_index]

    # This is neither master nor worker, so we must not be using multi-threading.
    return load_all(load_function, *args)


def get_last_updated() -> str:
    '''Gets a sample last_updated field for testing. Uses a date two weeks before when the script
    is run.'''

    today = datetime.datetime.now()
    delta = datetime.timedelta(weeks = 2)
    prior_date = today - delta
    return prior_date.strftime('%Y-%m-%d')
