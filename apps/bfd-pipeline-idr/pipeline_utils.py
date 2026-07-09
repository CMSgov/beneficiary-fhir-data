import time
from datetime import UTC, datetime, timedelta

import psycopg
from loguru import logger
from snowflake.connector import ProgrammingError
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from batch_worker import LoadingBatchWorkerClient
from constants import (
    CLAIM_INSTITUTIONAL_ITEM_SS_TABLE,
    CLAIM_INSTITUTIONAL_SS_TABLE,
    CLAIM_PROFESSIONAL_ITEM_SS_TABLE,
    CLAIM_PROFESSIONAL_SS_TABLE,
    DEFAULT_PARTITION,
    FISS_CLM_SOURCE,
    IDR_CLAIM_TABLE,
    MCS_CLM_SOURCE,
    PART_D_CLAIM_TYPE_CODES,
    PHASE_1_CUTOFF,
    PHASE_1_SS_MAX,
    PHASE_1_SS_MIN,
    VMS_CLM_SOURCE,
)
from extractor import PostgresExtractor, SnowflakeExtractor, Source
from load_partition import LoadPartition
from loader import LoadType, PostgresLoader, get_connection_string, should_track_load_progress
from model.base_model import (
    LoadMode,
    T,
)
from model.load_progress import LoadProgress
from settings import PRUNE_BATCH_MAX_SIZE


def get_progress(
    load_mode: LoadMode,
    source: Source,
    table_name: str,
    start_time: datetime,
    partition: LoadPartition,
) -> LoadProgress | None:
    if not should_track_load_progress(load_mode):
        return None

    return PostgresExtractor(
        load_mode=load_mode, cls=LoadProgress, partition=partition
    ).extract_single(
        LoadProgress.fetch_query(partition, start_time, source),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load(
    cls: type[T],
    source: Source,
    load_mode: LoadMode,
    job_start: datetime,
    load_type: LoadType,
    worker_client: LoadingBatchWorkerClient,
    partition: LoadPartition | None = None,
) -> bool:
    partition = partition or DEFAULT_PARTITION
    if source == Source.SNOWFLAKE:
        data_extractor = SnowflakeExtractor(cls=cls, partition=partition)
    else:
        data_extractor = PostgresExtractor(load_mode=load_mode, cls=cls, partition=partition)

    logger.info("loading {}", cls.table())
    last_error = datetime.min.replace(tzinfo=UTC)
    loader = PostgresLoader()
    error_count = 0
    max_errors = 3

    while True:
        try:
            progress = get_progress(load_mode, source, cls.table(), job_start, partition)

            if progress:
                logger.info(
                    "progress for {} {} - last_ts: {} job_start_ts: {} batch_complete_ts: {}",
                    cls.table(),
                    progress.batch_partition,
                    progress.last_ts,
                    progress.job_start_ts,
                    progress.batch_complete_ts,
                )
            else:
                logger.info("no previous progress for {} - {}", cls.table(), partition.name)

            data_iter = data_extractor.extract_idr_data(progress, job_start, source)
            res = loader.load(
                data_iter,
                cls,
                job_start,
                partition,
                progress,
                load_type,
                load_mode,
                worker_client,
            )
            data_extractor.close()
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
                logger.opt(exception=True).warning("received transient error, retrying...")
                data_extractor.reconnect()
            else:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
        except Exception as ex:
            logger.opt(exception=True).error("error loading {}", cls.table())
            raise ex


def _prune_table_in_batches(
    conn,
    table_name: str,
    delete_query: str,
    params: tuple[object, ...] | None = None,
) -> None:
    total_row_count = 0

    while True:
        with conn.transaction():
            if params is None:
                res = conn.execute(delete_query)
            else:
                res = conn.execute(delete_query, params)

            total_row_count += res.rowcount
            logger.info("pruned {} rows from {}", res.rowcount, table_name)

            if res.rowcount == 0:
                logger.info("Total rows pruned from {}: {}", table_name, total_row_count)
                break


def prune_phase_1_ss_claims(
    cls: type[T],
    load_mode: LoadMode,
    job_start: datetime,
) -> bool:
    shared_claim_tables = {
        CLAIM_INSTITUTIONAL_SS_TABLE: CLAIM_INSTITUTIONAL_ITEM_SS_TABLE,
        CLAIM_PROFESSIONAL_SS_TABLE: CLAIM_PROFESSIONAL_ITEM_SS_TABLE,
    }

    claim_table = cls.table()
    item_table = shared_claim_tables.get(claim_table)
    if item_table is None:
        return True

    prune_cutoff_date = job_start - timedelta(days=PHASE_1_CUTOFF)
    part_d_codes = ",".join(str(code) for code in PART_D_CLAIM_TYPE_CODES)

    phase_1_claim_filter = f"""
        clm.clm_type_cd BETWEEN {PHASE_1_SS_MIN} AND {PHASE_1_SS_MAX}
        AND clm.clm_src_id IN
            ('{FISS_CLM_SOURCE}', '{MCS_CLM_SOURCE}', '{VMS_CLM_SOURCE}')
        AND clm.clm_idr_ld_dt < %s
    """
    non_latest_non_part_d_claim_filter = f"""
        clm.clm_ltst_clm_ind = 'N'
        AND clm.clm_type_cd NOT IN ({part_d_codes})
    """

    logger.info("pruning phase 1 ss claims older than {}", prune_cutoff_date)

    with psycopg.connect(get_connection_string(load_mode)) as conn:
        _prune_table_in_batches(
            conn,
            item_table,
            f"""
                DELETE FROM {item_table}
                WHERE (clm_uniq_id, bfd_row_id) IN (
                    SELECT item.clm_uniq_id, item.bfd_row_id
                    FROM {item_table} item
                    JOIN {claim_table} clm ON clm.clm_uniq_id = item.clm_uniq_id
                    WHERE {phase_1_claim_filter}
                    LIMIT {PRUNE_BATCH_MAX_SIZE}
                )
            """,
            (prune_cutoff_date,),
        )

        _prune_table_in_batches(
            conn,
            claim_table,
            f"""
                DELETE FROM {claim_table}
                WHERE clm_uniq_id IN (
                    SELECT clm.clm_uniq_id FROM {claim_table} clm
                    WHERE {phase_1_claim_filter}
                    LIMIT {PRUNE_BATCH_MAX_SIZE}
                )
            """,
            (prune_cutoff_date,),
        )

        logger.info("pruning non-latest non-Part-D ss claims")

        # Claim items can exist even when the non-latest parent claim was filtered
        # before final claim-table load, so use the source claim table.
        _prune_table_in_batches(
            conn,
            item_table,
            f"""
                DELETE FROM {item_table}
                WHERE (clm_uniq_id, bfd_row_id) IN (
                    SELECT item.clm_uniq_id, item.bfd_row_id
                    FROM {item_table} item
                    JOIN {IDR_CLAIM_TABLE} clm ON clm.clm_uniq_id = item.clm_uniq_id
                    WHERE {non_latest_non_part_d_claim_filter}
                    LIMIT {PRUNE_BATCH_MAX_SIZE}
                )
            """,
        )

        _prune_table_in_batches(
            conn,
            claim_table,
            f"""
                DELETE FROM {claim_table}
                WHERE clm_uniq_id IN (
                    SELECT clm.clm_uniq_id FROM {claim_table} clm
                    WHERE {non_latest_non_part_d_claim_filter}
                    LIMIT {PRUNE_BATCH_MAX_SIZE}
                )
            """,
        )

    return True
