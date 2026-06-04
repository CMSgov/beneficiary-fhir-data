from datetime import UTC, datetime

from constants import DEFAULT_PARTITION
from extractor import PostgresExtractor, SnowflakeExtractor
from logger_config import configure_logger
from model.base_model import ALIAS_HSTRY, LoadMode, Source, T
from model.idr_beneficiary import IdrBeneficiary


def compare_table(
    idr_extractor: SnowflakeExtractor[T],
    bfd_extractor: PostgresExtractor[T],
    alias: str,
    param_names: list[str],
    model: type[T],
) -> None:
    insert_columns = ",".join(model.insert_keys())
    bfd_values = bfd_extractor.extract_many(
        f"SELECT {insert_columns} FROM {model.table()} hstry TABLESAMPLE BERNOULLI(1) LIMIT 1", {}
    )
    bfd_row = next(bfd_values)[0].model_dump()
    params = {name: bfd_row[name] for name in param_names}
    query_filter = "WHERE " + " AND ".join(f"{alias}.{param} = {params[param]}" for param in params)
    columns = ",".join(model.column_aliases())
    columns_raw = ",".join(model.columns_raw())

    idr_values = idr_extractor.extract_many(
        model.fetch_query(DEFAULT_PARTITION, datetime.now(UTC), Source.SNOWFLAKE)
        .replace("{COLUMNS}", columns)
        .replace("{COLUMNS_NO_ALIAS}", columns_raw)
        .replace("{WHERE_CLAUSE}", query_filter)
        .replace("{ORDER_BY}", ""),
        {},
    )
    idr_row = next(idr_values)[0].model_dump()

    for col in idr_row:
        idr_val = idr_row[col]
        bfd_val = bfd_row[col]
        print(col)
        if isinstance(idr_val, datetime):
            idr_val = idr_val.replace(tzinfo=UTC)
        assert idr_val == bfd_val


if __name__ == "__main__":
    configure_logger()
    compare_table(
        SnowflakeExtractor(IdrBeneficiary, DEFAULT_PARTITION),
        PostgresExtractor(IdrBeneficiary, DEFAULT_PARTITION, LoadMode.PROD),
        ALIAS_HSTRY,
        ["bene_sk"],
        IdrBeneficiary,
    )
