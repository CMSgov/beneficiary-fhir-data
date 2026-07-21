import time
from datetime import UTC, datetime, timedelta

import psycopg
from loguru import logger
from snowflake.connector import ProgrammingError
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from batch_worker import LoadingBatchWorkerClient
from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor, Source
from load_partition import LoadPartition
from loader import LoadType, PostgresLoader, get_connection_string, should_track_load_progress
from model.base_model import (
    LoadMode,
    T,
)
from model.idr_beneficiary_low_income_subsidy_cmbnd import IdrBeneficiaryLowIncomeSubsidyCmbnd
from model.load_progress import LoadProgress
from settings import BENEFICIARY_PRUNE_BATCH_LIMIT


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


def prune_bene_lis_cmbnd(
    load_mode: LoadMode,
    job_start: datetime,
) -> bool:
    bene_table = IdrBeneficiaryLowIncomeSubsidyCmbnd.table()

    prune_cutoff_date = job_start - timedelta(days=60)
    logger.info("pruning obsolete lis beneficiaries", prune_cutoff_date)

    with psycopg.connect(get_connection_string(load_mode)) as conn, conn.transaction():
        while True:
            res = conn.execute(
                f"""
                DELETE FROM {bene_table}
                WHERE (bene_sk, bene_cmbnd_deemd_efctv_dt, idr_trans_obslt_ts) IN (
                    SELECT bene_sk, bene_cmbnd_deemd_efctv_dt, idr_trans_obslt_ts 
                    FROM {bene_table}
                    WHERE idr_trans_obslt_ts < %s
                    LIMIT %s
                )
                """,  # type: ignore
                (prune_cutoff_date, BENEFICIARY_PRUNE_BATCH_LIMIT),
            )
            logger.info("pruned {} rows from {}", res.rowcount, bene_table)
            if res.rowcount < BENEFICIARY_PRUNE_BATCH_LIMIT:
                break

    return True
