from os import getenv

ENABLE_PARTITIONS = getenv("IDR_ENABLE_PARTITIONS", "1").lower() not in ("0", "false")
MIN_TRANSACTION_DATE = getenv("IDR_MIN_TRANSACTION_DATE")
PARTITION_TYPE = getenv("IDR_PARTITION_TYPE", "year").lower()
LATEST_CLAIMS = getenv("IDR_LATEST_CLAIMS", "").lower() in ("1", "true")
LOAD_TYPE = getenv("IDR_LOAD_TYPE", "incremental")
BATCH_SIZE = int(getenv("IDR_BATCH_SIZE", "100_000"))
MIN_BATCH_COMPLETION_DATE = getenv("IDR_MIN_COMPLETION_DATE")
