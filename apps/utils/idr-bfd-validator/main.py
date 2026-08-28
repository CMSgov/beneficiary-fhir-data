import functools
import itertools
import json
import os
import random
import string
import sys
import tempfile
from collections.abc import Iterable
from dataclasses import asdict, dataclass
from datetime import UTC, date, datetime, timedelta
from enum import StrEnum, auto
from pathlib import Path
from typing import TYPE_CHECKING, Any, cast

import anyio
import boto3
import click
from botocore.config import Config
from idr_pipeline.constants import DEFAULT_MAX_DATE, PHASE_1_CUTOFF
from idr_pipeline.extractor import PostgresExtractor, SnowflakeExtractor
from idr_pipeline.load_partition import DEFAULT_PARTITION, LoadPartition, LoadType
from idr_pipeline.logger_config import configure_logger
from idr_pipeline.model.base_model import (
    ALIAS_CLM,
    DbType,
    IdrBaseModel,
    LoadMode,
    Source,
    T,
    transform_default_date_to_null,
)
from idr_pipeline.model.idr_beneficiary_low_income_subsidy_cmbnd import (
    IdrBeneficiaryLowIncomeSubsidyCmbnd,
)
from idr_pipeline.model.idr_beneficiary_ma_part_d_enrollment import IdrBeneficiaryMaPartDEnrollment
from idr_pipeline.model.idr_beneficiary_ma_part_d_enrollment_rx import (
    IdrBeneficiaryMaPartDEnrollmentRx,
)
from idr_pipeline.model.idr_claim_institutional_nch import IdrClaimInstitutionalNch
from idr_pipeline.model.idr_claim_institutional_ss import IdrClaimInstitutionalSs
from idr_pipeline.model.idr_claim_item_institutional_nch import IdrClaimItemInstitutionalNch
from idr_pipeline.model.idr_claim_item_institutional_ss import IdrClaimItemInstitutionalSs
from idr_pipeline.model.idr_claim_item_professional_nch import IdrClaimItemProfessionalNch
from idr_pipeline.model.idr_claim_item_professional_ss import IdrClaimItemProfessionalSs
from idr_pipeline.model.idr_claim_professional_nch import IdrClaimProfessionalNch
from idr_pipeline.model.idr_claim_professional_ss import IdrClaimProfessionalSs
from idr_pipeline.model.load_progress import LoadProgress
from idr_pipeline.parallel_executor import MultiprocessingExecutor, Stage
from idr_pipeline.pipeline_stages import (
    BENE_AUX_TABLES,
    BENE_TABLES,
    CLAIM_AUX_TABLES,
    CLAIM_TABLES,
    PRIOR_AUTH_TABLES,
)
from loguru import logger
from pydantic_partial import create_partial_model

if TYPE_CHECKING:
    from loguru import Record
else:
    Record = object


_ALL_MODELS = [
    *CLAIM_AUX_TABLES,
    *CLAIM_TABLES,
    *BENE_TABLES,
    *BENE_AUX_TABLES,
    *PRIOR_AUTH_TABLES,
]
_IGNORED_COLS_PER_MODEL = {
    k: v
    for keys, v in {
        # Some columns in the claims tables are updated too frequently for us to track updates and
        # so they may differ. This is an accepted compromise, so we must avoid checking them
        (*CLAIM_TABLES, *CLAIM_AUX_TABLES): {
            "bfd_prvdr_prscrbng_careteam_name",
            "bfd_prvdr_rndrng_careteam_name",
            "bfd_prvdr_blg_last_or_lgl_name",
            "bfd_prvdr_rfrg_careteam_name",
            "bfd_prvdr_srvc_careteam_name",
            "prvdr_srvc_last_or_lgl_name",
        }
    }.items()
    for k in keys
}
_REDACTED_PKEYS_PER_MODEL = {
    k: v
    for keys, v in {(*BENE_TABLES, *BENE_AUX_TABLES): {"bene_mbi_id", "bene_ssm_num"}}.items()
    for k in keys
}
_ADDITIONAL_WHERE_CLAUSES_PER_MODEL = {
    k: v
    for keys, v in cast(
        dict[tuple[type[IdrBaseModel]], tuple[str, list[DbType]]],
        {
            (
                IdrBeneficiaryMaPartDEnrollment,
                IdrBeneficiaryMaPartDEnrollmentRx,
                IdrBeneficiaryLowIncomeSubsidyCmbnd,
            ): ("(idr_trans_obslt_ts = {} OR idr_trans_obslt_ts = null)", [DEFAULT_MAX_DATE]),
        },
    ).items()
    for k in keys
}
_ADDITIONAL_BASE_CLAIM_WHERE_CLAUSES_PER_MODEL = {
    k: v
    for keys, v in cast(
        dict[tuple[type[IdrBaseModel]], tuple[str, list[DbType]]],
        {
            (
                IdrClaimProfessionalSs,
                IdrClaimItemProfessionalSs,
                IdrClaimInstitutionalSs,
                IdrClaimItemInstitutionalSs,
            ): (
                f"""
                {ALIAS_CLM}.idr_updt_ts > {{}}
                AND {ALIAS_CLM}.clm_ltst_clm_ind = 'Y'
                """,
                [(datetime.now(UTC) - timedelta(days=PHASE_1_CUTOFF)).date().isoformat()],
            ),
            (
                IdrClaimProfessionalNch,
                IdrClaimItemProfessionalNch,
                IdrClaimInstitutionalNch,
                IdrClaimItemInstitutionalNch,
            ): (
                f"{ALIAS_CLM}.clm_ltst_clm_ind = 'Y'",
                [],
            ),
        },
    ).items()
    for k in keys
}
_BFD_ENV = os.environ.get("BFD_ENV")
_ALERT_SNS_TOPIC_ARN = os.environ.get("ALERT_SNS_TOPIC_ARN")
_REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
_BOTO_CONFIG = Config(
    region_name=_REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
    # Double the read timeout for some extra safety
    read_timeout=120,
)


class RowValidationResult(StrEnum):
    SUCCESS = auto()
    BFD_ROW_DOES_NOT_EXIST = auto()
    MISMATCHED_COLUMNS = auto()


@dataclass(eq=True, frozen=True)
class RowResult:
    pkey: dict[str, Any]
    result: RowValidationResult
    result_metadata: dict[str, Any] | None = None
    idr_row: dict[str, Any] | None = None
    bfd_row: dict[str, Any] | None = None


@dataclass(eq=True, frozen=True)
class OverallResult:
    model: str
    partition: str
    num_idr_rows: int
    num_bfd_rows: int
    num_success: int
    num_failed: int
    per_row_results: list[RowResult]


def _compare_table(
    model: type[T],
    partition: LoadPartition,
    num_rows: int,
    job_id: int,
    enable_reports: bool,
    reports_dir: Path,
    allow_sensitive_logs: bool,
    additional_where_clauses: list[str],
    additional_clm_where_clauses: list[str],
) -> bool:
    idr_extractor = SnowflakeExtractor(model, partition)
    # We must use pydantic-partial to create a partial model for some model types because not all
    # required fields of a given model are inserted into the BFD database. So, if we don't use
    # partial models then Pydantic will fail to validate rows that come back from the BFD DB.
    bfd_extractor: PostgresExtractor[T] = PostgresExtractor(
        create_partial_model(model), partition, LoadMode.PROD
    )
    load_progress_extractor = PostgresExtractor(LoadProgress, DEFAULT_PARTITION, LoadMode.PROD)

    with logger.contextualize(table=model.table(), part=partition.name):
        progress = load_progress_extractor.extract_single(
            f"""
            SELECT DISTINCT ON (last_ts) *
            FROM {LoadProgress.table()}
            WHERE batch_partition = {_escape_sql_val(partition.name)}
            AND table_name = %(table)s
            AND job_id = %(job_id)s
            ORDER BY last_ts
            """,
            {"table": model.table(), "job_id": job_id},
        )

        if progress:
            logger.info(
                "Last load progress time: {}",
                progress.last_ts.astimezone(UTC).isoformat(),
            )

        batch_timestamp_clause = idr_extractor.build_filter_columns(progress)
        model_pkeys = model.ordered_pkeys()
        columns = _comma_list(model.column_aliases())
        columns_raw = _comma_list(model.columns_raw())

        idr_pkeys_str = _comma_list(model.format_aliases(model.ordered_pkeys()))
        base_where_clause = (
            "WHERE TRUE"
            if progress is None
            else f"WHERE ({batch_timestamp_clause} < {_escape_sql_val(progress.last_ts)})"
        )
        where_clauses = [base_where_clause]
        if model in _ADDITIONAL_WHERE_CLAUSES_PER_MODEL:
            additional_where_template, additional_where_params = (
                _ADDITIONAL_WHERE_CLAUSES_PER_MODEL[model]
            )
            where_clauses.append(
                additional_where_template.format(
                    *[_escape_sql_val(param) for param in additional_where_params]
                )
            )
        where_clause = " AND ".join([*where_clauses, *additional_where_clauses])

        base_claims_where_filters = ""
        if progress:
            default_base_claim_where = (
                f"{ALIAS_CLM}.idr_updt_ts < {_escape_sql_val(progress.last_ts)}"
            )
            base_claim_where_clauses = [default_base_claim_where]
            if model in _ADDITIONAL_BASE_CLAIM_WHERE_CLAUSES_PER_MODEL:
                addl_base_where_templ, addl_base_where_params = (
                    _ADDITIONAL_BASE_CLAIM_WHERE_CLAUSES_PER_MODEL[model]
                )
                base_claim_where_clauses.append(
                    addl_base_where_templ.format(
                        *[_escape_sql_val(param) for param in addl_base_where_params]
                    )
                )

            if progress.max_run_ts is not None:
                base_claim_where_clauses.append(
                    f"{batch_timestamp_clause} <= '{progress.max_run_ts}'"
                )

            joined_base_claim_wheres = " AND ".join(
                [*base_claim_where_clauses, *additional_clm_where_clauses]
            )
            base_claims_where_filters = f"""
            AND (
                {joined_base_claim_wheres}
            )
            """

        idr_query = (
            model.fetch_query(partition, datetime.now(UTC), Source.SNOWFLAKE)
            .replace("{COLUMNS}", columns)
            .replace("{COLUMNS_NO_ALIAS}", columns_raw)
            .replace("{WHERE_CLAUSE}", where_clause)
            .replace("{BASE_CLAIMS_WHERE_FILTERS}", base_claims_where_filters)
            .replace("{TABLESAMPLE}", "TABLESAMPLE (100)")
            .replace("{LIMIT}", f"LIMIT {num_rows}")
            .replace("{ORDER_BY}", f"ORDER BY {idr_pkeys_str}")
            .replace("{FILTER_OP}", "<" if progress else "")
            .replace("{LAST_TS}", _escape_sql_val(progress.last_ts) if progress else "")
        )
        logger.debug(idr_query)

        idr_values = idr_extractor.extract_many(idr_query, {})
        idr_rows = [
            {k: _fix_idr_val(v) for k, v in row.model_dump().items()}
            for batch in idr_values
            for row in batch
        ]

        log_redact_pkeys: set[str] = (
            _REDACTED_PKEYS_PER_MODEL.get(model, set()) if not allow_sensitive_logs else set()
        )
        logger.opt(lazy=True).debug(
            "idr rows: \n{}",
            lambda: json.dumps(
                [_get_row_pkey(idr_row, model_pkeys, log_redact_pkeys) for idr_row in idr_rows],
                default=str,
            ),
        )

        if len(idr_rows) == 0:
            return True

        idr_pkeys_vals_str = _comma_list(
            f"({_comma_list(_escape_sql_val(row[param_name]) for param_name in model_pkeys)})"
            for row in idr_rows
        )
        bfd_pkeys_str = _comma_list(model_pkeys)
        bfd_cols_str = _comma_list(model.insert_keys())
        bfd_query = f"""
            SELECT {bfd_cols_str} FROM {model.table()}
            WHERE ({bfd_pkeys_str}) IN ({idr_pkeys_vals_str})
            ORDER BY {bfd_pkeys_str}
            """
        logger.debug(
            bfd_query
            if allow_sensitive_logs or not log_redact_pkeys.intersection(set(model_pkeys))
            else "BFD query redacted due to sensitive column(s): {}",
            ", ".join(log_redact_pkeys),
        )
        bfd_values = bfd_extractor.extract_many(bfd_query, {})
        bfd_rows = [
            {k: _fix_bfd_val(model, k, v) for k, v in row.model_dump().items()}
            for batch in bfd_values
            for row in batch
        ]

        logger.opt(lazy=True).debug(
            "bfd rows: \n{}",
            lambda: json.dumps(
                [_get_row_pkey(bfd_row, model_pkeys, log_redact_pkeys) for bfd_row in bfd_rows],
                default=str,
            ),
        )

        logger.info(
            "received {} rows from BFD DB and {} rows from IDR",
            len(bfd_rows),
            len(idr_rows),
        )

        row_lengths_match = len(bfd_rows) == len(idr_rows)
        if not row_lengths_match:
            logger.error(
                "returned row lengths do not match; (IDR) {} != (BFD) {}",
                len(idr_rows),
                len(bfd_rows),
            )

        per_model_ignore_cols = _IGNORED_COLS_PER_MODEL.get(model, set())
        insert_keyset = set(model.insert_keys())
        cols_to_check = insert_keyset - per_model_ignore_cols
        bucketed_bfd_rows = bucket_rows(bfd_rows, model_pkeys)
        bucketed_idr_rows = bucket_rows(idr_rows, model_pkeys)
        results: list[RowResult] = []
        logger.info("verifying {} row(s)...", len(bfd_rows))
        for pkey_tupl, idr_row in bucketed_idr_rows.items():
            bfd_row = bucketed_bfd_rows.get(pkey_tupl)
            row_pkey = _get_row_pkey(idr_row, model_pkeys)
            if not bfd_row:
                logger.error(
                    "IDR row ({}) does not exist in BFD",
                    json.dumps(_get_row_pkey(idr_row, model_pkeys, log_redact_pkeys), default=str),
                )
                results.append(
                    RowResult(
                        pkey=row_pkey,
                        result=RowValidationResult.BFD_ROW_DOES_NOT_EXIST,
                        idr_row=idr_row,
                    )
                )
                continue

            mismatched_cols: list[str] = []
            for col in cols_to_check:
                idr_val = idr_row[col]
                bfd_val = bfd_row[col]

                if idr_val != bfd_val:
                    mismatched_cols.append(col)
                    if allow_sensitive_logs:
                        logger.debug(
                            "({}) {}: (IDR) {} != (BFD) {}",
                            json.dumps(
                                _get_row_pkey(bfd_row, model_pkeys, log_redact_pkeys),
                                default=str,
                            ),
                            col,
                            str(idr_val),
                            str(bfd_val),
                        )

            if mismatched_cols:
                logger.error(
                    "mismatched columns for row ({}): {}",
                    json.dumps(
                        _get_row_pkey(bfd_row, model_pkeys, log_redact_pkeys),
                        default=str,
                    ),
                    ", ".join(x for x in mismatched_cols),
                )
                if allow_sensitive_logs:
                    logger.debug(
                        "(IDR) {} != (BFD) {}",
                        json.dumps(idr_row, default=str),
                        json.dumps(bfd_row, default=str),
                    )
                results.append(
                    RowResult(
                        pkey=row_pkey,
                        result=RowValidationResult.MISMATCHED_COLUMNS,
                        result_metadata={"cols": mismatched_cols},
                        idr_row=idr_row,
                        bfd_row=bfd_row,
                    )
                )
            else:
                results.append(RowResult(row_pkey, RowValidationResult.SUCCESS))

        if all(x.result == RowValidationResult.SUCCESS for x in results):
            logger.info("all {} row(s) successfully validated, verification passed", len(idr_rows))
        else:
            logger.error(
                "{}/{} row(s) failed validation, verification failed",
                sum(x.result != RowValidationResult.SUCCESS for x in results),
                len(idr_rows),
            )

        if enable_reports:
            report_path = reports_dir.joinpath(
                f"{model.table().split('.')[-1]}.{partition.name}.json"
            )
            logger.info("Writing JSON validation report to {}", str(report_path))
            report_path.write_text(
                json.dumps(
                    asdict(
                        OverallResult(
                            model=model.table(),
                            partition=partition.name,
                            num_idr_rows=len(idr_rows),
                            num_bfd_rows=len(bfd_rows),
                            num_success=sum(
                                x.result == RowValidationResult.SUCCESS for x in results
                            ),
                            num_failed=sum(
                                x.result != RowValidationResult.SUCCESS for x in results
                            ),
                            per_row_results=results,
                        )
                    ),
                    default=str,
                    skipkeys=True,
                    indent=2,
                )
            )

        return all(x.result == RowValidationResult.SUCCESS for x in results) and row_lengths_match


def _create_dir_in_tmp(prefix: str) -> Path:
    tmpdir = tempfile.gettempdir()
    dir_path = Path(tmpdir).joinpath(
        f"{prefix}{''.join(random.choices(string.ascii_letters, k=5))}"
    )
    dir_path.mkdir(exist_ok=True)
    return dir_path


def _fix_idr_val(val: DbType) -> DbType:
    match val:
        case str() as s:
            return s.strip("\x00")  # Some columns somehow come back NUL-terminated
        case datetime() as d:
            return d.replace(tzinfo=UTC) if not d.tzinfo else d
        case _:
            return val


def _fix_bfd_val(model: type[IdrBaseModel], col: str, val: DbType) -> DbType | None:
    if model == IdrClaimItemInstitutionalNch:
        match (col, val):
            case ("clm_line_instnl_rev_ctr_dt", date() as d):
                # Some rows in the BFD DB still have `1001-01-01` when the equivalent IDR row
                # returns `null`. We need to replace those values with null as those are treated
                # as null
                return transform_default_date_to_null(d)
            case _:
                pass

    match val:
        case datetime() as d:
            # Some primery key columns (specifically bene_cmbnd_deemd_efctv_dt for
            # idr.beneficiary_low_income_subsidy_cmbnd) are stored in the BFD DB as dates but are
            # represented in the model as datetimes. These columns have no tzinfo, and so when
            # we try to compare IDR to BFD rows after fixing IDR datetimes the BFD rows are not
            # found as they _technically_ are not the same due to missing the tzinfo.
            return d.replace(tzinfo=UTC) if not d.tzinfo else d
        case _:
            return val


def _get_row_pkey(
    row: dict[str, Any], pkeys: Iterable[str], redact: Iterable[str] | None = None
) -> dict[str, Any]:
    # Some tables have primary keys that are sensitive and cannot be stored in any log store. For
    # example, idr.beneficiary_mbi_id's composite key contains the column bene_mbi_id which _is_
    # sensitive. If the operator has not specified ALLOW_SENSITIVE_LOGS to be true, then redact
    # the value. This also removes any keys that aren't primary keys so that only the primary key
    # is logged for a row.
    if not redact:
        redact = []
    return {k: (v if k not in redact else "<redacted>") for k, v in row.items() if k in pkeys}


def _escape_sql_val(val: DbType) -> str:
    if isinstance(val, str):
        return f"'{val}'"
    if isinstance(val, datetime | date):
        return f"'{val.isoformat()}'"

    return f"{val}"


def _comma_list(vals: Iterable[str]) -> str:
    return ",".join(vals)


def bucket_rows(
    rows: list[dict[str, Any]], pkeys: list[str]
) -> dict[tuple[Any, ...], dict[str, Any]]:
    buckets: dict[tuple[Any, ...], dict[str, Any]] = {}

    for row in rows:
        key = tuple(row[pk] for pk in pkeys)
        buckets[key] = row

    return buckets


def _wrap_compare(
    model: type[IdrBaseModel],
    partition: LoadPartition,
    row_limit: int,
    job_id: int,
    enable_reports: bool,
    reports_dir: Path,
    allow_sensitive_logs: bool,
    where_clauses: list[str],
    clm_where_clauses: list[str],
) -> tuple[bool, type[IdrBaseModel], LoadPartition, int]:
    return (
        _compare_table(
            model,
            partition,
            row_limit,
            job_id,
            enable_reports,
            reports_dir,
            allow_sensitive_logs,
            where_clauses,
            clm_where_clauses,
        ),
        model,
        partition,
        job_id,
    )


def _compare_all(
    tables: list[str],
    exclude_tables: list[str],
    limit: int,
    job_id: int,
    max_parallel: int,
    enable_reports: bool,
    reports_dir: Path,
    allow_sensitive_logs: bool,
    where_clauses: list[str],
    clm_where_clauses: list[str],
) -> Stage[tuple[bool, type[IdrBaseModel], LoadPartition, int]]:
    now = datetime.now(UTC)

    immutable_models = {model for model in _ALL_MODELS if not model.update_timestamp_col()}
    all_models_set = set(_ALL_MODELS)
    filtered_models = {
        y
        for y in ({x for x in all_models_set if x.table() in tables} if tables else all_models_set)
        if y.table() not in exclude_tables
    }
    models_to_compare = filtered_models - immutable_models

    models_and_partitions = [
        (model, partition)
        for model in models_to_compare
        for partition in itertools.chain.from_iterable(
            x.generate_ranges(LoadType.INCREMENTAL, now) for x in model.model_type().partitions
        )
    ]
    logger.info(
        "Running IDR -> BFD validation ({} row(s) per-model, {} max parallelism) for {} models and "
        "partitions: {}",
        limit,
        max_parallel,
        len(models_and_partitions),
        ", ".join(
            f"{model.table()}-{partition.name}" for model, partition in models_and_partitions
        ),
    )

    for model, partition in models_and_partitions:
        yield functools.partial(
            _wrap_compare,
            model,
            partition,
            limit,
            job_id,
            enable_reports,
            reports_dir,
            allow_sensitive_logs,
            where_clauses,
            clm_where_clauses,
        )


def _log_formatter(record: Record) -> str:
    return "".join(
        [
            "<green>{time:YYYY-MM-DD HH:mm:ss.SSS Z}</green> | ",
            "<level>{level: <8}</level> | ",
            "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> ",
            "<m>{extra[table]}-{extra[part]}</m> " if record["extra"] else "",
            "- <level>{message}</level>\n{exception}",
        ]
    )


def _filter_sql_logs(record: Record) -> bool:
    return record["level"].name != "SQL"


async def async_main(
    tables: list[str],
    exclude_tables: list[str],
    limit: int,
    job_id: int,
    max_parallel: int,
    enable_reports: bool,
    reports_dir: Path | None,
    allow_sensitive_logs: bool,
    where_clauses: list[str],
    clm_where_clauses: list[str],
) -> bool:
    executor = MultiprocessingExecutor(max_workers=max_parallel)
    reports_dir = reports_dir or _create_dir_in_tmp("reports_")
    if enable_reports:
        logger.info("Writing JSON validation reports to {}", str(reports_dir))
    results = [
        x
        for x in itertools.chain.from_iterable(
            await executor.execute(
                [
                    _compare_all(
                        tables,
                        exclude_tables,
                        limit,
                        job_id,
                        max_parallel,
                        enable_reports,
                        reports_dir,
                        allow_sensitive_logs,
                        where_clauses,
                        clm_where_clauses,
                    )
                ]
            )
        )
        if x
    ]
    mismatches = [x for x in results if not x[0]]

    if mismatches:
        logger.error("Some mismatches occurred, see log for detail")
        if _ALERT_SNS_TOPIC_ARN and _BFD_ENV:
            sns_client = boto3.client("sns", config=_BOTO_CONFIG)  # pyright: ignore[reportUnknownMemberType]
            alert_message = {
                "AlarmName": f"bfd-{_BFD_ENV}-idr-bfd-validator-failure",
                "AlarmDescription": (
                    f"{len(mismatches)} table(s) failed validation; see log for detail and state "
                    "reason change for failing table and partition list"
                ),
                "NewStateReason": ", ".join(f"{x[1].table()}-{x[2].name}" for x in mismatches),
                "Trigger": {"MetricName": None},
            }
            logger.info(
                "Publishing alert to {}: {}",
                _ALERT_SNS_TOPIC_ARN,
                json.dumps(alert_message, indent=2),
            )

            sns_client.publish(TopicArn=_ALERT_SNS_TOPIC_ARN, Message=json.dumps(alert_message))
        return False

    logger.info("Completed comparing all tables and found no mismatches")
    return True


@click.command
@click.option(
    "-t",
    "--tables",
    multiple=True,
    envvar="IDR_TABLES",
    type=str,
    show_default=False,
    help="List of tables to validate. Defaults to all tables if unspecified or empty",
)
@click.option(
    "-T",
    "--exclude-tables",
    multiple=True,
    envvar="IDR_EXCLUDE_TABLES",
    type=str,
    show_default=False,
    help="List of tables to exclude from validation. Defaults to no tables if unspecified or empty",
)
@click.option(
    "-l",
    "--limit",
    envvar="ROW_LIMIT",
    type=int,
    default=1000,
    show_default=True,
    help="Number of rows to load from each table when validating.",
)
@click.option(
    "-j",
    "--job-id",
    envvar="IDR_JOB_ID",
    type=int,
    default=1,
    help="IDR Pipeline Job ID to validate against.",
)
@click.option(
    "-p",
    "--max-parallel",
    envvar="MAX_PARALLELISM",
    type=int,
    default=12,
    show_default=True,
    help="Maximum number of table+partitions to validate at once.",
)
@click.option(
    "-L",
    "--log-level",
    envvar="IDR_LOG_LEVEL",
    type=click.Choice(["debug", "info", "warning", "error"], case_sensitive=False),
    default="info",
    show_default=True,
    help="Log level.",
)
@click.option(
    "-r",
    "--enable-reports/--disable-reports",
    envvar="ENABLE_REPORTS",
    type=bool,
    default=False,
    show_default=True,
    help="Enable JSON report generation for validation results.",
)
@click.option(
    "-R",
    "--reports-dir",
    envvar="REPORTS_DIR",
    type=click.Path(exists=True, file_okay=False, dir_okay=True, path_type=Path),
    default=None,
    required=False,
    help="Directory to store validation reports in. If unspecified, defaults to a temporary dir.",
)
@click.option(
    "--allow-sensitive-logs/--disallow-sensitive-logs",
    envvar="ALLOW_SENSITIVE_LOGS",
    type=bool,
    default=False,
    show_default=True,
    help="Allow logging of sensitive data to stdout.",
)
@click.option(
    "-w",
    "--where-clauses",
    multiple=True,
    envvar="ADDITIONAL_WHERE_CLAUSES",
    type=str,
    show_default=False,
    help="List of additional where clauses to append with AND to the default WHERE",
)
@click.option(
    "-W",
    "--clm-where-clauses",
    multiple=True,
    envvar="ADDITIONAL_CLAIM_WHERE_CLAUSES",
    type=str,
    show_default=False,
    help="List of additional where clauses to append with AND to the default WHERE for claims"
    "tables",
)
def main(
    tables: tuple[str],
    exclude_tables: tuple[str],
    limit: int,
    job_id: int,
    max_parallel: int,
    log_level: str,
    enable_reports: bool,
    reports_dir: Path | None,
    allow_sensitive_logs: bool,
    where_clauses: tuple[str],
    clm_where_clauses: tuple[str],
) -> None:
    configure_logger()
    logger.remove()
    logger.add(
        sink=sys.stderr,
        level=log_level.upper(),
        format=_log_formatter,
        filter=_filter_sql_logs,  # filter out SQL level messages
        enqueue=True,  # Ensures non-blocking and async+multiprocessing-safe
        diagnose=False,  # Ensures local variables are not logged for exceptions
    )

    os.environ.setdefault("IDR_ALLOW_EXTRACTOR_QUERY_LOGGING", "false")
    os.environ.setdefault("IDR_LATEST_CLAIMS", "true")

    if not anyio.run(
        async_main,
        list(tables),
        list(exclude_tables),
        limit,
        job_id,
        max_parallel,
        enable_reports,
        reports_dir,
        allow_sensitive_logs,
        list(where_clauses),
        list(clm_where_clauses),
    ):
        sys.exit(1)


if __name__ == "__main__":
    main()
