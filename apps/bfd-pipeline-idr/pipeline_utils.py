import logging
import time
from datetime import UTC, datetime, timedelta

from snowflake.connector import ProgrammingError
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor
from load_partition import LoadPartition
from loader import LoadType, PostgresLoader, should_track_load_progress
from model.base_model import (
    IdrBaseModel,
    LoadMode,
    T,
)
from model.load_progress import LoadProgress

logger = logging.getLogger(__name__)


def get_progress(
    load_mode: LoadMode,
    table_name: str,
    start_time: datetime,
    partition: LoadPartition,
) -> LoadProgress | None:
    if not should_track_load_progress(load_mode):
        return None

    return PostgresExtractor(
        load_mode=load_mode, cls=LoadProgress, partition=partition
    ).extract_single(
        LoadProgress.fetch_query(partition, start_time, load_mode),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load(
    cls: type[T],
    load_mode: LoadMode,
    job_start: datetime,
    load_type: LoadType,
    partition: LoadPartition | None = None,
) -> bool:
    partition = partition or DEFAULT_PARTITION
    if load_mode == LoadMode.LOCAL or load_mode == LoadMode.SYNTHETIC:
        data_extractor = PostgresExtractor(load_mode=load_mode, cls=cls, partition=partition)
    else:
        data_extractor = SnowflakeExtractor(cls=cls, partition=partition)

    logger.info("loading %s", cls.table())
    last_error = datetime.min.replace(tzinfo=UTC)
    loader = PostgresLoader(load_mode)
    error_count = 0
    max_errors = 3

    while True:
        try:
            progress = get_progress(load_mode, cls.table(), job_start, partition)

            if progress:
                logger.info(
                    "progress for %s %s - last_ts: %s job_start_ts: %s batch_complete_ts: %s",
                    cls.table(),
                    progress.batch_partition,
                    progress.last_ts,
                    progress.job_start_ts,
                    progress.batch_complete_ts,
                )
            else:
                logger.info("no previous progress for %s - %s", cls.table(), partition.name)

            data_iter = data_extractor.extract_idr_data(progress, job_start, load_mode)
            res = loader.load(data_iter, cls, job_start, partition, progress, load_type, load_mode)
            data_extractor.close()
            loader.close()
            return res
        # Snowflake will throw a reauth error if the pipeline has been running for several hours
        # but it seems to be wrapped in a ProgrammingError.
        # Unclear the best way to handle this, it will require a bit more trial and error
        except (
            ReauthenticationRequest,
            RetryRequest,
            ForbiddenError,
            ProgrammingError,
        ) as ex:
            time_expired = datetime.now(UTC) - last_error > timedelta(seconds=10)
            if time_expired:
                error_count = 0
            error_count += 1
            if error_count < max_errors:
                last_error = datetime.now(UTC)
                logger.warning("received transient error, retrying...", exc_info=ex)
                data_extractor.reconnect()
            else:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
        except Exception as ex:
            logger.error("error loading %s", cls.table(), exc_info=ex)
            raise ex


def purge_non_latest_claims(
    cls: type[IdrBaseModel],
    load_mode: LoadMode,
    parent_child_tables: dict[type[IdrBaseModel], type[IdrBaseModel] | None],
    partition: LoadPartition | None = None,
) -> bool:
    partition = partition or DEFAULT_PARTITION
    logger.info("purging %s", cls.table())
    parent_table: type[IdrBaseModel] = cls
    child_table: type[IdrBaseModel] | None = parent_child_tables.get(cls)
    loader = PostgresLoader(load_mode)

    claim_range_filter = (
        "1 = 1"
        if partition.start_date is None or partition.end_date is None
        else f"""
    p.clm_from_dt BETWEEN '{partition.start_date.strftime("%Y-%m-%d")}'
    AND '{partition.end_date.strftime("%Y-%m-%d")}'
    """
    )
    claim_type_codes = ",".join([str(c) for c in partition.claim_type_codes])
    claim_type_code_filter = f"p.clm_type_cd IN ( {claim_type_codes} )"

    parent_table_name = parent_table.table()

    if child_table:
        child_table_name = child_table.table()
        logger.info("Child: %s", child_table_name)
        childSQL = f"""
            DELETE FROM {child_table_name} AS c
            WHERE EXISTS
                (   SELECT NULL
                    FROM {parent_table_name} AS p 
                    WHERE p.clm_uniq_id = c.clm_uniq_id
                        AND p.clm_ltst_clm_ind  = 'N' 
                        AND p.clm_type_cd NOT IN ( 1, 2, 3, 4 )
                        AND {claim_range_filter}
                        AND {claim_type_code_filter}
                )
        """

        loader.run_sql(childSQL)

    logger.info("Parent : %s", parent_table_name)
    parentSQL = f"""
        DELETE FROM {parent_table_name} AS p
        WHERE p.clm_ltst_clm_ind  = 'N' 
            AND p.clm_type_cd NOT IN ( 1, 2, 3, 4 )
            AND {claim_range_filter}
            AND {claim_type_code_filter}
    """

    loader.run_sql(parentSQL)
    return True
