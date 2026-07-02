import functools
import itertools
import json
import os
import sys
from collections.abc import Iterable
from datetime import UTC, date, datetime
from typing import TYPE_CHECKING, Any

import anyio
from loguru import logger
from pydantic_partial import create_partial_model

from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor
from load_partition import LoadPartition, LoadType
from logger_config import configure_logger
from model.base_model import ALIAS_CLM, DbType, LoadMode, Source, T
from model.load_progress import LoadProgress
from parallel_executor import ParallelStagesExecutor, Stage
from pipeline_stages import (
    BENE_AUX_TABLES,
    BENE_TABLES,
    CLAIM_AUX_TABLES,
    CLAIM_TABLES,
    PRIOR_AUTH_TABLES,
)

if TYPE_CHECKING:
    from loguru import Record
else:
    Record = object

_ALLOW_SENSITIVE_LOGS = os.environ.get("ALLOW_SENSITIVE_LOGS", "false").lower() in ["1", "true"]
_TABLES_TO_LOAD = [
    y for x in os.environ.get("IDR_TABLES", "").split(",") if (y := x.lower().strip())
]
_ROW_LIMIT = int(os.environ.get("ROW_LIMIT", "1000"))
_MAX_PARALLELISM = int(os.environ.get("MAX_PARALLELISM", "12"))

_ALL_MODELS = [*CLAIM_AUX_TABLES, *CLAIM_TABLES, *BENE_TABLES, *BENE_AUX_TABLES, *PRIOR_AUTH_TABLES]
_IGNORED_COLS_PER_MODEL = {
    k: v
    for keys, v in {
        # Some columns in the claims tables are updated too frequently for us to track updates and
        # so they may differ. This is an accepted compromise, so we must avoid checking them
        tuple([*CLAIM_TABLES, *CLAIM_AUX_TABLES]): {
            "bfd_prvdr_prscrbng_careteam_name",
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
    for keys, v in {tuple([*BENE_TABLES, *BENE_AUX_TABLES]): {"bene_mbi_id"}}.items()
    for k in keys
}


def _compare_table(
    model: type[T],
    partition: LoadPartition,
    num_rows: int,
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
        partition_list = [p.name for p in model.model_type().partitions]
        progress = load_progress_extractor.extract_single(
            f"""
            SELECT DISTINCT ON (last_ts) *
            FROM {LoadProgress.table()}
            WHERE batch_partition IN ({", ".join(_escape_sql_val(x) for x in partition_list)})
            AND table_name = %(table)s
            ORDER BY last_ts
            """,
            {"table": model.table()},
        )

        batch_timestamp_clause = idr_extractor.build_filter_columns(progress)
        model_pkeys = model.ordered_pkeys()
        columns = _comma_list(model.column_aliases())
        columns_raw = _comma_list(model.columns_raw())

        idr_pkeys_str = _comma_list(model.format_aliases(model.ordered_pkeys()))
        where_clause = (
            "WHERE TRUE"
            if progress is None
            else f"WHERE ({batch_timestamp_clause} < {_escape_sql_val(progress.last_ts)})"
        )
        base_claims_where_filters = (
            ""
            if progress is None
            else (
                f"""
                AND (
                    {ALIAS_CLM}.idr_updt_ts < {_escape_sql_val(progress.last_ts)}
                )
                """
            )
        )
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
        idr_rows = [row.model_dump() for batch in idr_values for row in batch]

        log_redact_pkeys: set[str] = (
            _REDACTED_PKEYS_PER_MODEL.get(model, set()) if not _ALLOW_SENSITIVE_LOGS else set()
        )
        logger.opt(lazy=True).debug(
            "idr rows: \n{}",
            lambda: json.dumps(
                [_prep_row_for_log(idr_row, model_pkeys, log_redact_pkeys) for idr_row in idr_rows],
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
            if _ALLOW_SENSITIVE_LOGS or not log_redact_pkeys.intersection(set(model_pkeys))
            else "BFD query redacted due to sensitive column(s): {}",
            ", ".join(log_redact_pkeys),
        )
        bfd_values = bfd_extractor.extract_many(bfd_query, {})
        bfd_rows = [row.model_dump() for batch in bfd_values for row in batch]

        logger.opt(lazy=True).debug(
            "bfd rows: \n{}",
            lambda: json.dumps(
                [_prep_row_for_log(bfd_row, model_pkeys, log_redact_pkeys) for bfd_row in bfd_rows],
                default=str,
            ),
        )

        logger.info(
            "received {} rows from BFD DB and {} rows from IDR", len(bfd_rows), len(idr_rows)
        )

        if len(bfd_rows) != len(idr_rows):
            logger.error("row length does not match; {} != {}", len(bfd_rows), len(idr_rows))
            return False

        any_mismatch = False

        per_model_ignore_cols = _IGNORED_COLS_PER_MODEL.get(model, set())
        insert_keyset = set(model.insert_keys())
        cols_to_check = insert_keyset - per_model_ignore_cols
        logger.info("verifying {} row(s)...", len(bfd_rows))
        for bfd_row, idr_row in zip(bfd_rows, idr_rows, strict=True):
            mismatched_cols: list[str] = []
            for col in cols_to_check:
                idr_val = idr_row[col]
                bfd_val = bfd_row[col]

                if isinstance(idr_val, datetime) and not idr_val.tzinfo:
                    bfd_val = idr_val.replace(tzinfo=None)

                if idr_val != bfd_val:
                    mismatched_cols.append(col)
                    if _ALLOW_SENSITIVE_LOGS:
                        logger.debug(
                            "({}) {}: (IDR) {} != (BFD) {}",
                            json.dumps(
                                _prep_row_for_log(bfd_row, model_pkeys, log_redact_pkeys),
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
                        _prep_row_for_log(bfd_row, model_pkeys, log_redact_pkeys), default=str
                    ),
                    ", ".join(x for x in mismatched_cols),
                )
                if _ALLOW_SENSITIVE_LOGS:
                    logger.debug(
                        "(IDR) {} != (BFD) {}",
                        json.dumps(idr_row, default=str),
                        json.dumps(bfd_row, default=str),
                    )
                any_mismatch = True

        if not any_mismatch:
            logger.info("no mismatches, verification passed")

        return not any_mismatch


def _prep_row_for_log(
    row: dict[str, Any], pkeys: Iterable[str], redact: Iterable[str]
) -> dict[str, Any]:
    # Some tables have primary keys that are sensitive and cannot be stored in any log store. For
    # example, idr.beneficiary_mbi_id's composite key contains the column bene_mbi_id which _is_
    # sensitive. If the operator has not specified ALLOW_SENSITIVE_LOGS to be true, then redact
    # the value. This also removes any keys that aren't primary keys so that only the primary key
    # is logged for a row.
    return {k: (v if k not in redact else "<redacted>") for k, v in row.items() if k in pkeys}


def _escape_sql_val(val: DbType) -> str:
    if isinstance(val, str):
        return f"'{val}'"
    if isinstance(val, datetime | date):
        return f"'{val.isoformat()}'"

    return f"{val}"


def _comma_list(vals: Iterable[str]) -> str:
    return ",".join(vals)


def _compare_all() -> Stage[bool]:
    now = datetime.now(UTC)

    immutable_models = set(model for model in _ALL_MODELS if not model.update_timestamp_col())
    all_models_set = set(_ALL_MODELS)
    filtered_models = (
        {x for x in all_models_set if x.table() in _TABLES_TO_LOAD}
        if _TABLES_TO_LOAD
        else all_models_set
    )
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
        _ROW_LIMIT,
        _MAX_PARALLELISM,
        len(models_and_partitions),
        ", ".join(
            f"{model.table()}-{partition.name}" for model, partition in models_and_partitions
        ),
    )
    for model, partition in models_and_partitions:
        yield functools.partial(
            _compare_table,
            model,
            partition,
            _ROW_LIMIT,
        )


async def main() -> bool:
    executor = ParallelStagesExecutor(max_workers=_MAX_PARALLELISM)
    any_mismatches = not all(
        itertools.chain.from_iterable(await executor.execute([_compare_all()]))
    )

    if any_mismatches:
        logger.error("Some mismatches occurred, see log for detail")
        return False

    logger.info("Completed comparing all tables and found no mismatches")
    return True


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


if __name__ == "__main__":
    configure_logger()
    logger.remove()
    logger.add(
        sink=sys.stderr,
        level=os.getenv("IDR_LOG_LEVEL", "INFO").upper(),
        format=_log_formatter,
        enqueue=True,  # Ensures non-blocking and async+multiprocessing-safe
        diagnose=False,  # Ensures local variables are not logged for exceptions
    )

    os.environ.setdefault("IDR_ALLOW_EXTRACTOR_QUERY_LOGGING", "false")
    os.environ.setdefault("IDR_LATEST_CLAIMS", "true")

    if not anyio.run(main):
        sys.exit(1)
