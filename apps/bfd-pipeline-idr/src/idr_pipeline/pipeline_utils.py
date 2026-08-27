import time
from datetime import UTC, datetime, timedelta

import psycopg
from loguru import logger
from snowflake.connector import ProgrammingError
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from .batch_worker import LoadingBatchWorkerClient
from .constants import (
    CLAIM_INSTITUTIONAL_ITEM_NCH_TABLE,
    CLAIM_INSTITUTIONAL_ITEM_SS_TABLE,
    CLAIM_INSTITUTIONAL_NCH_TABLE,
    CLAIM_INSTITUTIONAL_SS_TABLE,
    CLAIM_PROFESSIONAL_ITEM_NCH_TABLE,
    CLAIM_PROFESSIONAL_ITEM_SS_TABLE,
    CLAIM_PROFESSIONAL_NCH_TABLE,
    CLAIM_PROFESSIONAL_SS_TABLE,
    DEFAULT_MAX_DATE,
    PHASE_1_CUTOFF,
)
from .extractor import PostgresExtractor, SnowflakeExtractor, Source
from .load_partition import DEFAULT_PARTITION, LoadPartition
from .loader import LoadType, PostgresLoader, get_connection_string, should_track_load_progress
from .model.base_model import (
    LoadMode,
    T,
    stale_non_part_d_claims_query,
    stale_phase_1_claims_query,
)
from .model.idr_beneficiary_low_income_subsidy_cmbnd import IdrBeneficiaryLowIncomeSubsidyCmbnd
from .model.idr_beneficiary_ma_part_d_enrollment import IdrBeneficiaryMaPartDEnrollment
from .model.idr_beneficiary_ma_part_d_enrollment_rx import IdrBeneficiaryMaPartDEnrollmentRx
from .model.load_progress import LoadProgress
from .settings import SETTINGS

_SHARED_SYSTEM_CLAIM_ITEM_TABLES = {
    CLAIM_INSTITUTIONAL_SS_TABLE: CLAIM_INSTITUTIONAL_ITEM_SS_TABLE,
    CLAIM_PROFESSIONAL_SS_TABLE: CLAIM_PROFESSIONAL_ITEM_SS_TABLE,
}


# Keep this mapping explicit so stale non-Part-D pruning remains independent of Phase 1 SS pruning.
_NON_PART_D_CLAIM_ITEM_TABLES = {
    CLAIM_INSTITUTIONAL_NCH_TABLE: CLAIM_INSTITUTIONAL_ITEM_NCH_TABLE,
    CLAIM_INSTITUTIONAL_SS_TABLE: CLAIM_INSTITUTIONAL_ITEM_SS_TABLE,
    CLAIM_PROFESSIONAL_NCH_TABLE: CLAIM_PROFESSIONAL_ITEM_NCH_TABLE,
    CLAIM_PROFESSIONAL_SS_TABLE: CLAIM_PROFESSIONAL_ITEM_SS_TABLE,
}


class ModelExtractError(Exception):
    pass


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

            data_iter = (
                data_extractor.extract_full_idr_data(source)
                if cls.should_delete_missing()
                else data_extractor.extract_idr_data(progress, job_start, source)
            )
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
            raise ModelExtractError(f"error loading {cls.table()}-{partition.name}") from ex


def prune_phase_1_ss_claims(
    cls: type[T],
    load_mode: LoadMode,
    job_start: datetime,
) -> bool:
    claim_table = cls.table()
    item_table = _SHARED_SYSTEM_CLAIM_ITEM_TABLES.get(claim_table)
    if item_table is None:
        return True

    prune_cutoff_date = job_start - timedelta(days=PHASE_1_CUTOFF)
    logger.info("pruning phase 1 ss claims older than {}", prune_cutoff_date)

    prune_query, params = stale_phase_1_claims_query(claim_table, item_table, prune_cutoff_date)

    with psycopg.connect(get_connection_string(load_mode)) as conn:
        for target_table in [item_table, claim_table]:
            total_row_count = 0
            while True:
                with conn.transaction():
                    res = conn.execute(
                        f"""DELETE FROM {target_table} WHERE clm_uniq_id IN ({prune_query})""",  # type: ignore
                        params,
                    )

                    total_row_count += res.rowcount
                    logger.info("pruned {} rows from {}", res.rowcount, item_table)

                    if res.rowcount == 0:
                        logger.info("Total rows pruned from {}: {}", item_table, total_row_count)
                        break
    return True


def prune_stale_non_part_d_claims(
    cls: type[T],
    load_mode: LoadMode,
) -> bool:
    claim_table = cls.table()
    item_table = _NON_PART_D_CLAIM_ITEM_TABLES.get(claim_table)
    if item_table is None:
        return True

    logger.info("pruning stale non-Part-D claims")

    prune_query = stale_non_part_d_claims_query(claim_table)

    total_row_counts: dict[str, int] = {
        item_table: 0,
        claim_table: 0,
    }

    with psycopg.connect(get_connection_string(load_mode)) as conn:
        while True:
            claim_row_count = 0
            with conn.transaction():
                for target_table in [item_table, claim_table]:
                    res = conn.execute(
                        f"""DELETE FROM {target_table} WHERE clm_uniq_id IN ({prune_query})""",  # type: ignore
                        (),
                    )

                    total_row_counts[target_table] += res.rowcount
                    logger.info("pruned {} rows from {}", res.rowcount, target_table)

                    if target_table == claim_table:
                        claim_row_count = res.rowcount

            if claim_row_count == 0:
                for target_table in [item_table, claim_table]:
                    logger.info(
                        "Total rows pruned from {}: {}",
                        target_table,
                        total_row_counts[target_table],
                    )
                break

    return True


def prune_bene_lis_cmbnd(
    load_mode: LoadMode,
) -> bool:
    bene_table = IdrBeneficiaryLowIncomeSubsidyCmbnd.table()

    logger.info("pruning obsolete lis beneficiaries")

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
                (DEFAULT_MAX_DATE, SETTINGS.prune_batch_limit),
            )
            logger.info("pruned {} rows from {}", res.rowcount, bene_table)
            if res.rowcount < SETTINGS.prune_batch_limit:
                break

    return True


def prune_bene_ma_part_d(
    load_mode: LoadMode,
) -> bool:
    bene_table = IdrBeneficiaryMaPartDEnrollment.table()

    logger.info("pruning obsolete part d beneficiaries", DEFAULT_MAX_DATE)

    with psycopg.connect(get_connection_string(load_mode)) as conn, conn.transaction():
        while True:
            res = conn.execute(
                f"""
                DELETE FROM {bene_table}
                WHERE (bene_sk, bene_enrlmt_bgn_dt, bene_enrlmt_pgm_type_cd) IN (
                    SELECT bene_sk, bene_enrlmt_bgn_dt, bene_enrlmt_pgm_type_cd
                    FROM {bene_table}
                    WHERE idr_trans_obslt_ts < %s
                    LIMIT %s
                )
                """,  # type: ignore
                (DEFAULT_MAX_DATE, SETTINGS.prune_batch_limit),
            )
            logger.info("pruned {} rows from {}", res.rowcount, bene_table)
            if res.rowcount < SETTINGS.prune_batch_limit:
                break

    return True


def prune_bene_ma_part_d_rx(
    load_mode: LoadMode,
) -> bool:
    bene_table = IdrBeneficiaryMaPartDEnrollmentRx.table()

    logger.info("pruning obsolete part d rx beneficiaries", DEFAULT_MAX_DATE)

    with psycopg.connect(get_connection_string(load_mode)) as conn, conn.transaction():
        while True:
            res = conn.execute(
                f"""
                DELETE FROM {bene_table}
                WHERE (bene_sk, 
                       bene_cntrct_num, 
                       bene_pbp_num, 
                       bene_enrlmt_bgn_dt, 
                       bene_enrlmt_pdp_rx_info_bgn_dt
                    ) IN (
                    SELECT bene_sk, 
                           bene_cntrct_num, 
                           bene_pbp_num, 
                           bene_enrlmt_bgn_dt, 
                           bene_enrlmt_pdp_rx_info_bgn_dt
                    FROM {bene_table}
                    WHERE idr_trans_obslt_ts < %s
                    LIMIT %s
                )
                """,  # type: ignore
                (DEFAULT_MAX_DATE, SETTINGS.prune_batch_limit),
            )
            logger.info("pruned {} rows from {}", res.rowcount, bene_table)
            if res.rowcount < SETTINGS.prune_batch_limit:
                break

    return True
