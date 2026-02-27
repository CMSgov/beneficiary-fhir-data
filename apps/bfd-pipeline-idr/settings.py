from os import getenv


def _parse_bool_default_false(var_name: str) -> bool:
    return getenv(var_name, "").lower() in ("1", "true")


def _parse_bool_default_true(var_name: str) -> bool:
    return getenv(var_name, "1").lower() not in ("0", "false")


# Enables partitioning claims data based on dates.
# It's useful to disable this for synthetic loads since
# the smaller volume of data means this will probably be much slower
ENABLE_DATE_PARTITIONS = _parse_bool_default_true("IDR_ENABLE_DATE_PARTITIONS")

# Minimum claim date to load.
# Any claims created before this date will be skipped.
# Useful for partial loads with large amounts of data.
MIN_TRANSACTION_DATE = getenv("IDR_MIN_TRANSACTION_DATE")

# Partition type - year/month/day
# This should be set to "day" in prod to reduce the batch sizes
PARTITION_TYPE = getenv("IDR_PARTITION_TYPE", "year").lower()

# Only pull in latest claims
# Useful for the initial data pull since we only want to pull in
# the latest version of each claim.
LATEST_CLAIMS = _parse_bool_default_false("IDR_LATEST_CLAIMS")

# Load type - initial (first load) or incremental (adding on to an existing load)
# The load type affects the shape of the DAG.
# Only useful for prod data and testing.
LOAD_TYPE = getenv("IDR_LOAD_TYPE", "incremental")

# Batch sizes are calculated based on the number of columns in the table
# in order to keep memory usage stable relative to the number of concurrent tasks.
# Change this to increase or decrease the number of rows loaded per batch.
# Increasing this means the memory per task will also increase and
# you will likely need to decrease the number of concurrent tasks
# to prevent the server from running out of memory.
BATCH_MULTIPLIER = int(getenv("IDR_BATCH_MULTIPLIER", "2_000_000"))

# Minimum batch completion date to process
# This is useful if you've already loaded some data and you do not want to reprocess
# any batches that have already completed before this date.
MIN_BATCH_COMPLETION_DATE = getenv("IDR_MIN_BATCH_COMPLETION_DATE")

# Maximum concurrent tasks to run.
# Changing this has a drastic effect on the runtime.
# In prod, we want to run as many tasks as possible without running out of memory.
MAX_TASKS = int(getenv("IDR_MAX_TASKS", "32"))

# List of tables to include - any table not included will be skipped.
# Useful if you only want to load a subset of data and don't want to wait
# for the other tables to load.
TABLES_TO_LOAD = [t for t in getenv("IDR_TABLES", "").split(",") if t]

# IDR credentials, these are pulled from SSM in prod.
# You likely don't want to touch these otherwise.
IDR_PRIVATE_KEY = getenv("IDR_PRIVATE_KEY", "")
IDR_USERNAME = getenv("IDR_USERNAME", "")
IDR_ACCOUNT = getenv("IDR_ACCOUNT", "")
IDR_WAREHOUSE = getenv("IDR_WAREHOUSE", "")
IDR_DATABASE = getenv("IDR_DATABASE", "")
IDR_SCHEMA = getenv("IDR_SCHEMA", "")

# These need to be lazy-loaded since we override them in the tests


# Tracking load progress is disabled for synthetic data loads.
# Use this to force enabling load progress for testing.
def force_load_progress() -> bool:
    # We don't normally want to store the load progress info for synthetic data since the dates
    # won't be in order like in prod. However, we need a way to override this for the tests.
    return _parse_bool_default_false("IDR_FORCE_LOAD_PROGRESS")


# Database credentials/settings


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
