'''Set up data for use in the tests.'''

import logging
from typing import Callable, List
import datetime
from locust.env import Environment
from common.locust_utils import is_distributed, is_locust_master

def load_from_env(locust_env: Environment, load_function: Callable, *args, use_table_sample: bool = False) -> List:
    """Loads data from the database given the database load function provided. Gets the database URI and the
    table sampling percent from Locust's parsed options. Returns an empty dataset if the current runner is the master
    runner, or if database URI and/or table sample percent are undefined

    Args:
        locust_env (Environment): The current Locust environment
        load_function (Callable): A database load function that will query the database to get the desired data
        use_table_sample (bool, optional): Whether or not to use Postgres's table sampling to randomly sample a given table's data. Defaults to False.

    Returns:
        List: A list of data returned by the load function
    """
    if is_distributed(locust_env) and is_locust_master(locust_env):
        # Don't bother loading data for the master runner, it doesn't run a test
        return []
    
    config = vars(locust_env.parsed_options)
    try:
        database_uri = config['database_uri']
        table_sample_percent = config['table_sample_percent']
    except KeyError as e:
        logging.getLogger().error('"database_uri" and/or "table_sample_percent" was not defined: %s', str(e))
        return []
    
    return load_from_uri(database_uri, load_function, *args, use_table_sample=use_table_sample, table_sample_percent=table_sample_percent)

def load_from_uri(database_uri: str, load_function: Callable, *args, use_table_sample: bool = False, table_sample_percent: float = 0.25) -> List:
    '''Loads all of the data from the database, using the database connection provided.'''
    print('Collecting test data...')
    if use_table_sample:
        print(f"Table Sampling at: {table_sample_percent}")
        results = load_function(uri=database_uri, table_sample_pct=table_sample_percent, *args)
    else:
        results = load_function(uri=database_uri, *args)

    print(f'Loaded {len(results)} results from the database')
    return results

def get_last_updated() -> str:
    '''Gets a sample last_updated field for testing. Uses a date two weeks before when the script
    is run.'''

    today = datetime.datetime.now()
    delta = datetime.timedelta(weeks = 2)
    prior_date = today - delta
    return prior_date.strftime('%Y-%m-%d')
