from datetime import datetime, timedelta
from functools import cached_property
from os import getenv

from .constants import MIN_CLAIM_LOAD_DATE, MIN_PRIOR_AUTH_LOAD_DATE


class _Settings:
    def _parse_bool_default_false(self, var_name: str) -> bool:
        return getenv(var_name, "").lower() in ("1", "true")

    def _parse_bool_default_true(self, var_name: str) -> bool:
        return getenv(var_name, "1").lower() not in ("0", "false")

    # Tracking load progress is disabled for synthetic data loads.
    # Use this to force enabling load progress for testing.
    @cached_property
    def test_mode(self) -> bool:
        # We don't normally want to perform some operations outside of production mode.
        # However, we need a way to override this for the tests.
        return self._parse_bool_default_false("IDR_TEST_MODE")

    def bfd_test_date(self) -> datetime | None:
        test_date = getenv("BFD_TEST_DATE", "")
        return datetime.fromisoformat(test_date) if test_date else None

    @cached_property
    def enable_prior_auth_ingestion(self) -> bool:
        return self._parse_bool_default_true("IDR_ENABLE_PRIOR_AUTH")

    @cached_property
    def enable_date_partitions(self) -> bool:
        """
        Enables partitioning claims data based on dates.

        It's useful to disable this for synthetic loads since
        the smaller volume of data means this will probably be much slower
        """
        return self._parse_bool_default_true("IDR_ENABLE_DATE_PARTITIONS")

    @cached_property
    def min_claim_nch_transaction_date(self) -> str:
        """
        Minimum claim date to load for NCH (and DDPS).

        Any claims created before this date will be skipped.
        Useful for partial loads with large amounts of data.
        """
        return getenv("IDR_MIN_CLAIM_NCH_TRANSACTION_DATE", MIN_CLAIM_LOAD_DATE)

    @cached_property
    def min_claim_ss_transaction_date(self) -> str:
        """
        Minimum claim date to load for shared systems.

        Any claims created before this date will be skipped.
        Useful for partial loads with large amounts of data.
        """
        return getenv("IDR_MIN_CLAIM_SS_TRANSACTION_DATE", MIN_CLAIM_LOAD_DATE)

    @cached_property
    def min_prior_auth_transaction_date(self) -> str:
        """Minimum prior auth date to load."""
        return getenv("IDR_MIN_PRIOR_AUTH_TRANSACTION_DATE", MIN_PRIOR_AUTH_LOAD_DATE)

    @cached_property
    def partition_type(self) -> str:
        """
        Partition type (year/month/day).

        This should be set to "day" in prod to reduce the batch sizes
        """
        return getenv("IDR_PARTITION_TYPE", "year").lower()

    @cached_property
    def latest_claims(self) -> bool:
        """
        Only pull in latest claims.

        Useful for the initial data pull since we only want to pull in
        the latest version of each claim.
        """
        return self._parse_bool_default_false("IDR_LATEST_CLAIMS")

    @cached_property
    def batch_multiplier(self) -> int:
        """
        Batch sizes are calculated based on the number of columns in the table.

        This keeps memory usage stable relative to the number of concurrent tasks.
        Change this to increase or decrease the number of rows loaded per batch.
        Increasing this means the memory per task will also increase and
        you will likely need to decrease the number of concurrent tasks
        to prevent the server from running out of memory.
        """
        return int(getenv("IDR_BATCH_MULTIPLIER", "2_000_000"))

    @cached_property
    def min_batch_completion_date(self) -> str | None:
        """
        Minimum batch completion date to process.

        This is useful if you've already loaded some data and you do not want to reprocess
        any batches that have already completed before this date.
        """
        return getenv("IDR_MIN_BATCH_COMPLETION_DATE")

    @cached_property
    def max_tasks(self) -> int:
        """
        Maximum concurrent tasks to run.

        Changing this has a drastic effect on the runtime.
        In prod, we want to run as many tasks as possible without running out of memory.
        """
        return int(getenv("IDR_MAX_TASKS", "32"))

    @cached_property
    def tables_to_load(self) -> set[str] | None:
        """
        List of tables to include - any table not included will be skipped.

        Useful if you only want to load a subset of data and don't want to wait
        for the other tables to load. Takes precedence over source_load_events table in incremental
        mode.
        """
        idr_tables = getenv("IDR_TABLES", None)
        return {t.strip().lower() for t in idr_tables.split(",")} if idr_tables else None

    @cached_property
    def incremental_job_grace_period_hrs(self) -> timedelta:
        """
        Amount of time to tolerate no new incoming IDR Job Events for a given IDR Job type.

        When this limit passes, it loads the relevant tables. Defaults to 24 hours.
        """
        return timedelta(hours=int(getenv("IDR_INCREMENTAL_JOB_GRACE_PERIOD_HRS", default="24")))

    @cached_property
    def per_batch_concurrent_rows(self) -> int:
        """
        Number of rows per batch to concurrently load into the database at once.

        Defaults to 1000.
        """
        return int(getenv("IDR_PER_BATCH_CONCURRENT_ROWS", "1000"))

    @cached_property
    def per_batch_min_connections(self) -> int:
        """
        Number of minimum connections to hold in the pool per-batch for non-local loads.

        Defaults to 20.
        """
        return int(getenv("IDR_PER_BATCH_MIN_CONNECTIONS", "20"))

    @cached_property
    def per_batch_max_connections(self) -> int:
        """
        Number of minimum connections to hold in the pool per-batch for non-local loads.

        Defaults to 20.
        """
        return int(getenv("IDR_PER_BATCH_MAX_CONNECTIONS", "20"))

    @cached_property
    def prune_batch_limit(self) -> int:
        """
        The maximum batch size for pruning rows on INCREMENTAL loads.

        Defaults to 10000.
        """
        return int(getenv("IDR_PRUNE_BATCH_LIMIT", "10000"))

    @cached_property
    def log_level(self) -> str:
        return getenv("IDR_LOG_LEVEL", "INFO").upper()

    @cached_property
    def structured_logs(self) -> bool:
        return self._parse_bool_default_false("IDR_STRUCTURED_LOGS")

    @cached_property
    def sql_log(self) -> bool:
        return self._parse_bool_default_false("IDR_SQL_LOG")

    # IDR credentials, these are pulled from SSM in prod.
    # You likely don't want to touch these otherwise.

    @cached_property
    def idr_private_key(self) -> str:
        return getenv("IDR_PRIVATE_KEY", "")

    @cached_property
    def idr_username(self) -> str:
        return getenv("IDR_USERNAME", "")

    @cached_property
    def idr_account(self) -> str:
        return getenv("IDR_ACCOUNT", "")

    @cached_property
    def idr_warehouse(self) -> str:
        return getenv("IDR_WAREHOUSE", "")

    @cached_property
    def idr_database(self) -> str:
        return getenv("IDR_DATABASE", "")

    @cached_property
    def idr_prior_auth_database(self) -> str:
        return getenv("IDR_EDP_DATABASE", "")

    @cached_property
    def idr_schema(self) -> str:
        return getenv("IDR_SCHEMA", "")

    # Database credentials/settings

    @cached_property
    def bfd_db_port(self) -> str:
        return getenv("BFD_DB_PORT", "5432")

    @cached_property
    def bfd_db_name(self) -> str:
        return getenv("BFD_DB_NAME", "fhirdb")

    @cached_property
    def bfd_db_endpoint(self) -> str:
        return getenv("BFD_DB_ENDPOINT", "")

    @cached_property
    def bfd_db_username(self) -> str:
        return getenv("BFD_DB_USERNAME", "")

    @cached_property
    def bfd_db_password(self) -> str:
        return getenv("BFD_DB_PASSWORD", "")


SETTINGS = _Settings()
