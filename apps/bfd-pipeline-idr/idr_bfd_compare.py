from datetime import UTC, datetime

from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor
from logger_config import configure_logger
from model.base_model import ALIAS_HSTRY, DbType, LoadMode, Source, T
from model.idr_beneficiary import IdrBeneficiary


def compare_table(
    idr_extractor: SnowflakeExtractor[T],
    bfd_extractor: PostgresExtractor[T],
    alias: str,
    model: type[T],
    num_rows: int,
) -> None:
    param_names = model.unique_key()
    insert_columns = ",".join(model.insert_keys())
    param_name_list = ",".join(f"{alias}.{param_name}" for param_name in param_names)
    bfd_values = bfd_extractor.extract_many(
        f"""
        WITH rows AS (
            SELECT {insert_columns} FROM {model.table()} {alias} 
            TABLESAMPLE BERNOULLI(1) LIMIT {num_rows}
        )
        SELECT * from rows {alias} ORDER BY {param_name_list}
        """,
        {},
    )
    bfd_rows = [row.model_dump() for batch in bfd_values for row in batch]

    params = ",".join(
        "(" + ",".join(escape_sql_val(row[param_name]) for param_name in param_names) + ")"
        for row in bfd_rows
    )

    query_filter = f"WHERE ({param_name_list}) IN ({params})"

    columns = ",".join(model.column_aliases())
    columns_raw = ",".join(model.columns_raw())

    idr_values = idr_extractor.extract_many(
        model.fetch_query(DEFAULT_PARTITION, datetime.now(UTC), Source.SNOWFLAKE)
        .replace("{COLUMNS}", columns)
        .replace("{COLUMNS_NO_ALIAS}", columns_raw)
        .replace("{WHERE_CLAUSE}", query_filter)
        .replace("{ORDER_BY}", f"ORDER BY {param_name_list}"),
        {},
    )
    idr_rows = [row.model_dump() for batch in idr_values for row in batch]

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


def escape_sql_val(val: DbType) -> str:
    if isinstance(val, str | datetime):
        return f"'{val}'"
    return f"{val}"


if __name__ == "__main__":
    configure_logger()
    compare_table(
        SnowflakeExtractor(IdrBeneficiary, DEFAULT_PARTITION),
        PostgresExtractor(IdrBeneficiary, DEFAULT_PARTITION, LoadMode.PROD),
        ALIAS_HSTRY,
        IdrBeneficiary,
        10,
    )
