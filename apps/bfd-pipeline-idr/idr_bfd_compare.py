import functools
import itertools
import json
from collections.abc import Iterable
from datetime import UTC, date, datetime

import anyio
from loguru import logger

from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor
from load_partition import LoadPartition, LoadType
from logger_config import configure_logger
from model.base_model import DbType, LoadMode, Source, T
from model.idr_claim_item_institutional_nch import IdrClaimItemInstitutionalNch
from model.idr_contract_pbp_contact import IdrContractPbpContact
from model.idr_contract_pbp_number import IdrContractPbpNumber
from model.load_progress import LoadProgress
from parallel_executor import ParallelStagesExecutor, Stage
from pipeline_stages import _BENE_AUX_TABLES, _BENE_TABLES, _CLAIM_AUX_TABLES, _CLAIM_TABLES


def _compare_table(
    model: type[T],
    partition: LoadPartition,
    num_rows: int,
) -> bool:
    idr_extractor = SnowflakeExtractor(model, partition)
    bfd_extractor = PostgresExtractor(model, partition, LoadMode.PROD)
    load_progress_extractor = PostgresExtractor(LoadProgress, DEFAULT_PARTITION, LoadMode.PROD)

    partition_list = [p.name for p in model.model_type().partitions]
    logger.info("{}: partitions: {}", model.table(), ", ".join(partition_list))
    progress = load_progress_extractor.extract_single(
        f"""
        SELECT DISTINCT ON (last_ts) *
        FROM {LoadProgress.table()}
        WHERE batch_partition = ANY(%(partition_list)s)
        AND table_name = %(table)s
        ORDER BY last_ts
        """,
        {"partition_list": partition_list, "table": model.table()},
    )

    batch_timestamp_clause = idr_extractor.build_filter_columns(progress)
    param_names = model.ordered_pkeys()
    columns = _comma_list(model.column_aliases())
    columns_raw = _comma_list(model.columns_raw())

    idr_param_names = _comma_list(model.format_aliases(model.ordered_pkeys()))
    where_clause = (
        "WHERE TRUE"
        if progress is None
        else f"WHERE ({batch_timestamp_clause} < {_escape_sql_val(progress.last_ts)})"
    )
    query = (
        model.fetch_query(partition, datetime.now(UTC), Source.SNOWFLAKE)
        .replace("{COLUMNS}", columns)
        .replace("{COLUMNS_NO_ALIAS}", columns_raw)
        .replace("{WHERE_CLAUSE}", where_clause)
        .replace("{TABLESAMPLE}", "TABLESAMPLE (100)")
        .replace("{LIMIT}", f"LIMIT {num_rows}")
        .replace("{ORDER_BY}", f"ORDER BY {idr_param_names}")
        .replace("{FILTER_OP}", "<" if progress else "")
        .replace("{LAST_TS}", _escape_sql_val(progress.last_ts) if progress else "")
    )
    logger.debug(query)

    idr_values = idr_extractor.extract_many(query, {})
    idr_rows = [row.model_dump() for batch in idr_values for row in batch]
    logger.opt(lazy=True).debug(
        "{}: idr rows: \n[{}]",
        model.table,
        lambda: ", ".join(
            f"({', '.join(f'{x}: {idr_row[x]}' for x in model.ordered_pkeys())})"
            for idr_row in idr_rows
        ),
    )
    if len(idr_rows) == 0:
        return True

    params = _comma_list(
        f"({_comma_list(_escape_sql_val(row[param_name]) for param_name in param_names)})"
        for row in idr_rows
    )
    bfd_param_names = _comma_list(param_names)

    insert_columns = _comma_list(model.insert_keys())
    param_name_list = _comma_list(param_names)
    bfd_values = bfd_extractor.extract_many(
        f"""
        SELECT {insert_columns} FROM {model.table()}
        WHERE ({bfd_param_names}) IN ({params})
        ORDER BY {param_name_list}
        """,
        {},
    )
    bfd_rows = [row.model_dump() for batch in bfd_values for row in batch]
    logger.opt(lazy=True).debug(
        "{}: bfd rows: \n[{}]",
        model.table,
        lambda: ", ".join(
            f"({', '.join(f'{x}: {bfd_row[x]}' for x in model.ordered_pkeys())})"
            for bfd_row in bfd_rows
        ),
    )

    logger.info(
        "{}: received {} rows from BFD DB and {} rows from IDR",
        model.table(),
        len(bfd_rows),
        len(idr_rows),
    )

    if len(bfd_rows) != len(idr_rows):
        logger.error(
            "{}: row length does not match; {} != {}", model.table(), len(bfd_rows), len(idr_rows)
        )
        return False

    any_mismatch = False

    logger.info("{}: verifying {} row(s)...", model.table(), len(bfd_rows))
    for bfd_row, idr_row in zip(bfd_rows, idr_rows, strict=True):
        mismatched_cols: list[str] = []
        for col in bfd_row:
            idr_val = idr_row[col]
            bfd_val = bfd_row[col]

            if isinstance(idr_val, datetime) and not idr_val.tzinfo:
                bfd_val = idr_val.replace(tzinfo=None)

            if idr_val != bfd_val:
                mismatched_cols.append(col)

        if mismatched_cols:
            logger.error(
                "{}: mismatched columns for row ({}): {}",
                model.table(),
                ", ".join(f"{x}: {bfd_row[x]}" for x in model.ordered_pkeys()),
                ", ".join(x for x in mismatched_cols),
            )
            any_mismatch = True

    if not any_mismatch:
        logger.info("{}: no mismatches, verification passed", model.table())

    return not any_mismatch


def _escape_sql_val(val: DbType) -> str:
    if isinstance(val, str):
        return f"'{val}'"
    if isinstance(val, datetime | date):
        return f"'{val.isoformat()}'"

    return f"{val}"


def _comma_list(vals: Iterable[str]) -> str:
    return ",".join(vals)


def _compare_all() -> Stage[bool]:
    for model in set([*_BENE_TABLES, *_BENE_AUX_TABLES, *_CLAIM_AUX_TABLES, *_CLAIM_TABLES]) - set(
        [IdrContractPbpNumber, IdrContractPbpContact]
    ):
        partitions = itertools.chain.from_iterable(
            x.generate_ranges(LoadType.INCREMENTAL, datetime.now(UTC))
            for x in model.model_type().partitions
        )
        for partition in partitions:
            yield functools.partial(
                _compare_table,
                model,
                partition,
                1000,
            )


async def main() -> None:
    executor = ParallelStagesExecutor(max_workers=12)
    no_mismatches = all(itertools.chain.from_iterable(await executor.execute([_compare_all()])))

    if no_mismatches:
        logger.info("Completed comparing all tables and found no mismatches")
    else:
        logger.error("Some mismatches occurred, see log for detail")


if __name__ == "__main__":
    configure_logger()
    anyio.run(main)
