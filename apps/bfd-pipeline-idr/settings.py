from os import getenv


def _parse_bool_default_false(var_name: str) -> bool:
    return getenv(var_name, "").lower() in ("1", "true")


def _parse_bool_default_true(var_name: str) -> bool:
    return getenv(var_name, "1").lower() not in ("0", "false")


ENABLE_PARTITIONS = _parse_bool_default_true("IDR_ENABLE_PARTITIONS")
MIN_TRANSACTION_DATE = getenv("IDR_MIN_TRANSACTION_DATE")
PARTITION_TYPE = getenv("IDR_PARTITION_TYPE", "year").lower()
LATEST_CLAIMS = _parse_bool_default_false("IDR_LATEST_CLAIMS")
LOAD_TYPE = getenv("IDR_LOAD_TYPE", "incremental")
BATCH_MULTIPLIER = int(getenv("IDR_BATCH_MULTIPLIER", "2_000_000"))
MIN_BATCH_COMPLETION_DATE = getenv("IDR_MIN_COMPLETION_DATE")
MAX_TASKS = int(getenv("IDR_MAX_TASKS", "32"))
TABLES_TO_LOAD = [t for t in getenv("IDR_TABLES", "").split(",") if t]

IDR_PRIVATE_KEY = getenv("IDR_PRIVATE_KEY", "")
IDR_USERNAME = getenv("IDR_USERNAME", "")
IDR_ACCOUNT = getenv("IDR_ACCOUNT", "")
IDR_WAREHOUSE = getenv("IDR_WAREHOUSE", "")
IDR_DATABASE = getenv("IDR_DATABASE", "")
IDR_SCHEMA = getenv("IDR_SCHEMA", "")

# These need to be lazy-loaded since we override them in the tests


def force_load_progress() -> bool:
    # We don't normally want to store the load progress info for synthetic data since the dates
    # won't be in order like in prod. However, we need a way to override this for the tests.
    return _parse_bool_default_false("IDR_FORCE_LOAD_PROGRESS")


def bfd_db_port() -> str:
    return getenv("BFD_DB_PORT", "5432")


def bfd_db_name() -> str:
    return getenv("BFD_DB_NAME", "fhirdb")


def bfd_db_endpoint() -> str:
    return getenv("BFD_DB_ENDPOINT", "")


def bfd_db_username() -> str:
    return getenv("BFD_DB_USERNAME", "")


def bfd_db_password() -> str:
    return getenv("BFD_DB_PASSWORD", "")
