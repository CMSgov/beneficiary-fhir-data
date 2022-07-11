"""Set up data for use in the tests."""

import datetime
import logging
from argparse import Namespace
from typing import Callable, List


def load_from_parsed_opts(
    parsed_opts: Namespace, load_function: Callable, *args, use_table_sample: bool = False
) -> List:
    """Loads data from the database given the database load function provided. Gets the database URI and the
    table sampling percent from the given parsed options. Returns an empty list if database URI and/or table sample percent are not
    set in the given parsed options

    Args:
        parsed_opts (Namespace): A collection of parsed options that includes the database URI and table sampling percentage
        load_function (Callable): A database load function that will query the database to get the desired data
        use_table_sample (bool, optional): Whether or not to use Postgres's table sampling to randomly sample a given table's data. Defaults to False.

    Returns:
        List: A list of data returned by the load function
    """
    if not parsed_opts.database_uri:
        logging.getLogger().error('"database_uri" was not defined in parsed options')
        return []

    database_uri = str(parsed_opts.database_uri)
    table_sample_percent = float(parsed_opts.table_sample_percent)
    return load_from_uri(
        database_uri, load_function, *args, use_table_sample=use_table_sample, table_sample_percent=table_sample_percent
    )


def load_from_uri(
    database_uri: str,
    load_function: Callable,
    *args,
    use_table_sample: bool = False,
    table_sample_percent: float = 0.25,
) -> List:
    """Loads all of the data from the database, using the database connection provided."""
    print("Collecting test data...")
    if use_table_sample:
        print(f"Table Sampling at: {table_sample_percent}")
        results = load_function(uri=database_uri, table_sample_pct=table_sample_percent, *args)
    else:
        results = load_function(uri=database_uri, *args)

    print(f"Loaded {len(results)} results from the database")
    return results


def get_last_updated() -> str:
    """Gets a sample last_updated field for testing. Uses a date two weeks before when the script
    is run."""

    today = datetime.datetime.now()
    delta = datetime.timedelta(weeks=2)
    prior_date = today - delta
    return prior_date.strftime("%Y-%m-%d")
