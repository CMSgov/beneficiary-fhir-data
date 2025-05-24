"""Set up data for use in the tests."""

import datetime
import logging
from argparse import Namespace
from typing import Any, Collection, Optional, Protocol


class LoadFunction(Protocol):
    def __call__(self, uri: str, table_sample_pct: Optional[float] = None) -> Collection[Any]: ...


def load_from_parsed_opts(
    parsed_opts: Namespace,
    load_function: LoadFunction,
    use_table_sample: bool = False,
    data_type_name: str = "<unknown>",
) -> Collection[Any]:
    """Loads data from the database given the database load function provided. Gets the database URI
    and the table sampling percent from the given parsed options. Returns an empty list if database
    URI and/or table sample percent are not set in the given parsed options

    Args:
        parsed_opts (Namespace): A collection of parsed options that includes the database URI and
        table sampling percentage
        load_function (Callable): A database load function that will query the database to get the
        desired data
        use_table_sample (bool, optional): Whether or not to use Postgres's table sampling to
        randomly sample a given table's data. Defaults to False.
        data_type_name (str, optional): Name of the type of data being loaded, strictly used for
        logging. Defaults to "<unknown>".

    Returns:
        Collection[Any]: A Collection of data returned by the load function
    """

    database_constr = str(parsed_opts.database_constr)
    table_sample_percent = float(parsed_opts.table_sample_percent)
    return load_from_uri(
        database_constr,
        load_function,
        use_table_sample=use_table_sample,
        table_sample_percent=table_sample_percent,
        data_type_name=data_type_name,
    )


def load_from_uri(
    database_constr: str,
    load_function: LoadFunction,
    use_table_sample: bool = False,
    table_sample_percent: float = 0.25,
    data_type_name: str = "<unknown>",
) -> Collection[Any]:
    """Loads all of the data from the database, using the database connection provided."""
    logger = logging.getLogger()

    logger.info("Collecting %s test data...", data_type_name)
    if use_table_sample:
        logger.info(f"Table Sampling at: {table_sample_percent}")
        results = load_function(uri=database_constr, table_sample_pct=table_sample_percent)
    else:
        results = load_function(uri=database_constr)

    logger.info(f"Loaded {len(results)} results from the database")
    return results


def get_last_updated() -> str:
    """Gets a sample last_updated field for testing. Uses a date two weeks before when the script
    is run."""

    today = datetime.datetime.now()
    delta = datetime.timedelta(weeks=2)
    prior_date = today - delta
    return prior_date.strftime("%Y-%m-%d")
