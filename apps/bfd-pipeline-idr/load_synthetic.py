import argparse
import csv
import logging
import typing
from pathlib import Path

import psycopg

from constants import (
    IDR_BENE_COMBINED_DUAL_TABLE,
    IDR_BENE_ENTITLEMENT_REASON_TABLE,
    IDR_BENE_ENTITLEMENT_TABLE,
    IDR_BENE_HISTORY_TABLE,
    IDR_BENE_LOW_INCOME_SUBSIDY_TABLE,
    IDR_BENE_MA_PART_D_RX_TABLE,
    IDR_BENE_MA_PART_D_TABLE,
    IDR_BENE_MBI_TABLE,
    IDR_BENE_STATUS_TABLE,
    IDR_BENE_THIRD_PARTY_TABLE,
    IDR_BENE_XREF_TABLE,
    IDR_CLAIM_ANSI_SIGNATURE_TABLE,
    IDR_CLAIM_DATE_SIGNATURE_TABLE,
    IDR_CLAIM_DOCUMENTATION_TABLE,
    IDR_CLAIM_FISS_TABLE,
    IDR_CLAIM_INSTITUTIONAL_TABLE,
    IDR_CLAIM_LINE_DOCUMENTATION_TABLE,
    IDR_CLAIM_LINE_INSTITUTIONAL_TABLE,
    IDR_CLAIM_LINE_PROFESSIONAL_TABLE,
    IDR_CLAIM_LINE_RX_TABLE,
    IDR_CLAIM_LINE_TABLE,
    IDR_CLAIM_LOCATION_HISTORY_TABLE,
    IDR_CLAIM_PROD_TABLE,
    IDR_CLAIM_PROFESSIONAL_TABLE,
    IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE,
    IDR_CLAIM_TABLE,
    IDR_CLAIM_VAL_TABLE,
    IDR_CONTRACT_PBP_CONTACT_TABLE,
    IDR_CONTRACT_PBP_NUM_TABLE,
    IDR_PROVIDER_HISTORY_TABLE,
)
from extractor import CsvFile, DbExecutor, PostgresExecutor, SnowflakeExecutor
from loader import get_connection_string
from logger_config import configure_logger
from model.base_model import LoadMode

logger = logging.getLogger(__name__)

tables = [
    {"csv_name": "SYNTHETIC_BENE_HSTRY.csv", "table": IDR_BENE_HISTORY_TABLE},
    {"csv_name": "SYNTHETIC_BENE_MBI_ID.csv", "table": IDR_BENE_MBI_TABLE},
    {"csv_name": "SYNTHETIC_BENE_XREF.csv", "table": IDR_BENE_XREF_TABLE},
    {"csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT.csv", "table": IDR_BENE_ENTITLEMENT_TABLE},
    {
        "csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv",
        "table": IDR_BENE_ENTITLEMENT_REASON_TABLE,
    },
    {"csv_name": "SYNTHETIC_BENE_MDCR_STUS.csv", "table": IDR_BENE_STATUS_TABLE},
    {"csv_name": "SYNTHETIC_BENE_TP.csv", "table": IDR_BENE_THIRD_PARTY_TABLE},
    {
        "csv_name": "SYNTHETIC_BENE_CMBND_DUAL_MDCR.csv",
        "table": IDR_BENE_COMBINED_DUAL_TABLE,
    },
    {"csv_name": "SYNTHETIC_BENE_LIS.csv", "table": IDR_BENE_LOW_INCOME_SUBSIDY_TABLE},
    {"csv_name": "SYNTHETIC_BENE_MAPD_ENRLMT.csv", "table": IDR_BENE_MA_PART_D_TABLE},
    {
        "csv_name": "SYNTHETIC_BENE_MAPD_ENRLMT_RX.csv",
        "table": IDR_BENE_MA_PART_D_RX_TABLE,
    },
    {"csv_name": "SYNTHETIC_CLM.csv", "table": IDR_CLAIM_TABLE},
    {"csv_name": "SYNTHETIC_CLM_INSTNL.csv", "table": IDR_CLAIM_INSTITUTIONAL_TABLE},
    {"csv_name": "SYNTHETIC_CLM_PRFNL.csv", "table": IDR_CLAIM_PROFESSIONAL_TABLE},
    {"csv_name": "SYNTHETIC_CLM_DCMTN.csv", "table": IDR_CLAIM_DOCUMENTATION_TABLE},
    {
        "csv_name": "SYNTHETIC_CLM_LINE_DCMTN.csv",
        "table": IDR_CLAIM_LINE_DOCUMENTATION_TABLE,
    },
    {"csv_name": "SYNTHETIC_CLM_DT_SGNTR.csv", "table": IDR_CLAIM_DATE_SIGNATURE_TABLE},
    {"csv_name": "SYNTHETIC_CLM_VAL.csv", "table": IDR_CLAIM_VAL_TABLE},
    {"csv_name": "SYNTHETIC_CLM_LINE.csv", "table": IDR_CLAIM_LINE_TABLE},
    {
        "csv_name": "SYNTHETIC_CLM_LINE_INSTNL.csv",
        "table": IDR_CLAIM_LINE_INSTITUTIONAL_TABLE,
    },
    {
        "csv_name": "SYNTHETIC_CLM_LINE_PRFNL.csv",
        "table": IDR_CLAIM_LINE_PROFESSIONAL_TABLE,
    },
    {
        "csv_name": "SYNTHETIC_CLM_ANSI_SGNTR.csv",
        "table": IDR_CLAIM_ANSI_SIGNATURE_TABLE,
    },
    {"csv_name": "SYNTHETIC_CLM_PROD.csv", "table": IDR_CLAIM_PROD_TABLE},
    {"csv_name": "SYNTHETIC_CLM_FISS.csv", "table": IDR_CLAIM_FISS_TABLE},
    {"csv_name": "SYNTHETIC_CLM_LINE_RX.csv", "table": IDR_CLAIM_LINE_RX_TABLE},
    {
        "csv_name": "SYNTHETIC_CLM_LCTN_HSTRY.csv",
        "table": IDR_CLAIM_LOCATION_HISTORY_TABLE,
    },
    {
        "csv_name": "SYNTHETIC_CLM_RLT_COND_SGNTR_MBR.csv",
        "table": IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE,
    },
    {"csv_name": "SYNTHETIC_PRVDR_HSTRY.csv", "table": IDR_PROVIDER_HISTORY_TABLE},
    {"csv_name": "SYNTHETIC_CNTRCT_PBP_NUM.csv", "table": IDR_CONTRACT_PBP_NUM_TABLE},
    {
        "csv_name": "SYNTHETIC_CNTRCT_PBP_CNTCT.csv",
        "table": IDR_CONTRACT_PBP_CONTACT_TABLE,
    },
]


def load_from_csv(extractor: DbExecutor, src_folder: str) -> None:
    for table in tables:
        # Clear out any previous data
        sql_table = table["table"]
        extractor.execute(f"TRUNCATE TABLE {sql_table}")
        file = table["csv_name"]
        _load_file(extractor, src_folder, file, sql_table)
        extractor.commit()


def _load_file(extractor: DbExecutor, src_folder: str, file: str, full_table: str) -> None:
    path = Path(src_folder)
    # `glob` will return nothing for an invalid path so we'll explicitly make sure you supplied a
    # valid path
    if not path.exists():
        raise OSError(f"path {src_folder} not found")
    paths = [path] if path.is_file() and src_folder.endswith(file) else path.glob(f"./**/{file}")

    for match in paths:
        logger.info("loading from file: %s", match)
        with match.open() as f:
            reader = csv.DictReader(f)
            # skip empty files
            if reader.fieldnames is None:
                continue
            sql_table = full_table.split(".")[1]
            # fetch the list of columns from the database and filter them out
            # so we don't get errors trying to insert extra columns
            db_columns = extractor.query(
                """
                    SELECT column_name FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = %(sql_table)s
                """,
                {"sql_table": sql_table},
            )
            db_columns = [typing.cast(str, col["column_name"]).lower() for col in db_columns]

            cols = [
                col
                for col in typing.cast(typing.Iterable[str], reader.fieldnames)
                if col.lower().strip() in db_columns
            ]
            # skip empty files since we won't have any valid columns
            # which causes the COPY command below to fail
            if cols:
                extractor.copy(CsvFile(cols, sql_table, match))


if __name__ == "__main__":
    configure_logger()
    default_dir = "../bfd-model-idr/out"
    parser = argparse.ArgumentParser(description="Loads synthetic data")
    parser.add_argument("database_type", default="postgres", choices=["postgres", "snowflake"])
    parser.add_argument(
        "base_dir",
        default=default_dir,
        help="base directory to load files from",
    )

    args = parser.parse_args()
    print("base dir " + args.base_dir)
    load_from_csv(
        SnowflakeExecutor()
        if args.database_type == "snowflake"
        else PostgresExecutor(psycopg.connect(get_connection_string(LoadMode.SYNTHETIC))),
        args.base_dir or default_dir,
    )
