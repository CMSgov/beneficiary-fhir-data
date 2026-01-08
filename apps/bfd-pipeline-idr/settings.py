from os import getenv

ENABLE_PARTITIONS = getenv("IDR_ENABLE_PARTITIONS", "1").lower() not in ("0", "false")
MIN_TRANSACTION_DATE = getenv("IDR_MIN_TRANSACTION_DATE")
PARTITION_TYPE = getenv("IDR_PARTITION_TYPE", "year").lower()
LATEST_CLAIMS = getenv("IDR_LATEST_CLAIMS", "").lower() in ("1", "true")
LOAD_TYPE = getenv("IDR_LOAD_TYPE", "incremental")
BATCH_SIZE = int(getenv("IDR_BATCH_SIZE", "100_000"))
MIN_BATCH_COMPLETION_DATE = getenv("IDR_MIN_COMPLETION_DATE")
MAX_TASKS = int(getenv("IDR_MAX_TASKS", "32"))

IDR_PRIVATE_KEY = getenv("IDR_PRIVATE_KEY", "")
IDR_USERNAME = getenv("IDR_USERNAME", "")
IDR_ACCOUNT = getenv("IDR_ACCOUNT", "")
IDR_WAREHOUSE = getenv("IDR_WAREHOUSE", "")
IDR_DATABASE = getenv("IDR_DATABASE", "")
IDR_SCHEMA = getenv("IDR_SCHEMA", "")

# These need to be lazy-loaded since we override them in the tests


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
