from collections.abc import Iterable
from datetime import UTC, datetime

from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor
from logger_config import configure_logger
from model.base_model import DbType, LoadMode, Source, T
from model.idr_beneficiary import IdrBeneficiary
from model.load_progress import LoadProgress


def _compare_table(
    idr_extractor: SnowflakeExtractor[T],
    bfd_extractor: PostgresExtractor[T],
    load_progress_extractor: PostgresExtractor[LoadProgress],
    model: type[T],
    num_rows: int,
) -> None:
    partition_list = [p.name for p in model.model_type().partitions]
    print(partition_list, model.table())
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
        model.fetch_query(DEFAULT_PARTITION, datetime.now(UTC), Source.SNOWFLAKE)
        .replace("{COLUMNS}", columns)
        .replace("{COLUMNS_NO_ALIAS}", columns_raw)
        .replace("{WHERE_CLAUSE}", where_clause)
        .replace("{TABLESAMPLE}", "TABLESAMPLE (100)")
        .replace("{LIMIT}", f"LIMIT {num_rows}")
        .replace("{ORDER_BY}", f"ORDER BY {idr_param_names}")
    )

    idr_values = idr_extractor.extract_many(query, {})
    idr_rows = [row.model_dump() for batch in idr_values for row in batch]

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

    if len(bfd_rows) != len(idr_rows):
        print(len(bfd_rows), len(idr_rows))
    for bfd_row, idr_row in zip(bfd_rows, idr_rows, strict=True):
        print("verifying...")
        for col in bfd_row:
            idr_val = idr_row[col]
            bfd_val = bfd_row[col]

            if isinstance(idr_val, datetime):
                idr_val = idr_val.replace(tzinfo=UTC)
            if idr_val != bfd_val:
                print("mismatch", col, idr_val, bfd_val)


def _escape_sql_val(val: DbType) -> str:
    if isinstance(val, str | datetime):
        return f"'{val}'"
    return f"{val}"


def _comma_list(vals: Iterable[str]) -> str:
    return ",".join(vals)


if __name__ == "__main__":
    configure_logger()
    _compare_table(
        SnowflakeExtractor(IdrBeneficiary, DEFAULT_PARTITION),
        PostgresExtractor(IdrBeneficiary, DEFAULT_PARTITION, LoadMode.PROD),
        PostgresExtractor(LoadProgress, DEFAULT_PARTITION, LoadMode.PROD),
        IdrBeneficiary,
        10,
    )
