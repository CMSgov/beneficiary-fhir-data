import argparse
import csv
import typing
from pathlib import Path

import psycopg
from loguru import logger

from .db_utils import get_connection_string
from .extractor import CsvFile, DbExecutor, PostgresExecutor, SnowflakeExecutor
from .logger_config import configure_logger
from .model.base_model import LoadMode
from .settings import SETTINGS

tables = [
    {"csv_name": "SYNTHETIC_BENE_HSTRY.csv", "table": SETTINGS.idr_bene_history_table},
    {"csv_name": "SYNTHETIC_BENE_MBI_ID.csv", "table": SETTINGS.idr_bene_mbi_table},
    {"csv_name": "SYNTHETIC_BENE_XREF.csv", "table": SETTINGS.idr_bene_xref_table},
    {"csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT.csv", "table": SETTINGS.idr_bene_entitlement_table},
    {
        "csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv",
        "table": SETTINGS.idr_bene_entitlement_reason_table,
    },
    {"csv_name": "SYNTHETIC_BENE_MDCR_STUS.csv", "table": SETTINGS.idr_bene_status_table},
    {"csv_name": "SYNTHETIC_BENE_TP.csv", "table": SETTINGS.idr_bene_third_party_table},
    {
        "csv_name": "SYNTHETIC_BENE_CMBND_DUAL_MDCR.csv",
        "table": SETTINGS.idr_bene_combined_dual_table,
    },
    {
        "csv_name": "SYNTHETIC_BENE_LIS_CMBND.csv",
        "table": SETTINGS.idr_bene_low_income_subsidy_cmbnd_table,
    },
    {"csv_name": "SYNTHETIC_BENE_MAPD_ENRLMT.csv", "table": SETTINGS.idr_bene_ma_part_d_table},
    {
        "csv_name": "SYNTHETIC_BENE_MAPD_ENRLMT_RX.csv",
        "table": SETTINGS.idr_bene_ma_part_d_rx_table,
    },
    {"csv_name": "SYNTHETIC_CLM.csv", "table": SETTINGS.idr_claim_table},
    {"csv_name": "SYNTHETIC_CLM_INSTNL.csv", "table": SETTINGS.idr_claim_institutional_table},
    {"csv_name": "SYNTHETIC_CLM_PRFNL.csv", "table": SETTINGS.idr_claim_professional_table},
    {"csv_name": "SYNTHETIC_CLM_DCMTN.csv", "table": SETTINGS.idr_claim_documentation_table},
    {
        "csv_name": "SYNTHETIC_CLM_LINE_DCMTN.csv",
        "table": SETTINGS.idr_claim_line_documentation_table,
    },
    {"csv_name": "SYNTHETIC_CLM_DT_SGNTR.csv", "table": SETTINGS.idr_claim_date_signature_table},
    {"csv_name": "SYNTHETIC_CLM_VAL.csv", "table": SETTINGS.idr_claim_val_table},
    {"csv_name": "SYNTHETIC_CLM_LINE.csv", "table": SETTINGS.idr_claim_line_table},
    {
        "csv_name": "SYNTHETIC_CLM_LINE_INSTNL.csv",
        "table": SETTINGS.idr_claim_line_institutional_table,
    },
    {
        "csv_name": "SYNTHETIC_CLM_LINE_PRFNL.csv",
        "table": SETTINGS.idr_claim_line_professional_table,
    },
    {
        "csv_name": "SYNTHETIC_CLM_ANSI_SGNTR.csv",
        "table": SETTINGS.idr_claim_ansi_signature_table,
    },
    {"csv_name": "SYNTHETIC_CLM_PROD.csv", "table": SETTINGS.idr_claim_prod_table},
    {"csv_name": "SYNTHETIC_CLM_FISS.csv", "table": SETTINGS.idr_claim_fiss_table},
    {"csv_name": "SYNTHETIC_CLM_LINE_RX.csv", "table": SETTINGS.idr_claim_line_rx_table},
    {
        "csv_name": "SYNTHETIC_CLM_LCTN_HSTRY.csv",
        "table": SETTINGS.idr_claim_location_history_table,
    },
    {
        "csv_name": "SYNTHETIC_CLM_RLT_COND_SGNTR_MBR.csv",
        "table": SETTINGS.idr_claim_related_condition_signature_table,
    },
    {"csv_name": "SYNTHETIC_PRVDR_HSTRY.csv", "table": SETTINGS.idr_provider_history_table},
    {"csv_name": "SYNTHETIC_CNTRCT_PBP_NUM.csv", "table": SETTINGS.idr_contract_pbp_num_table},
    {"csv_name": "SYNTHETIC_CNTRCT_PBP_SGMT.csv", "table": SETTINGS.idr_contract_pbp_segment_table},
    {
        "csv_name": "SYNTHETIC_CNTRCT_PBP_CNTCT.csv",
        "table": SETTINGS.idr_contract_pbp_contact_table,
    },
    {
        "csv_name": "SYNTHETIC_PRAUC.csv",
        "table": SETTINGS.idr_prior_auth_table,
    },
]


def load_from_csv(extractor: DbExecutor, src_folder: str, truncate: bool = False) -> None:
    for table in tables:
        # Clear out any previous data
        sql_table = table["table"]
        if truncate:
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
        logger.info("loading from file: {}", match)
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
                    WHERE table_name ilike %(sql_table)s
                """,
                {"sql_table": sql_table},
            )
            db_columns = [typing.cast(str, col["column_name"]).lower() for col in db_columns]
            logger.info("found {} columns", len(db_columns))

            cols = [
                col
                for col in typing.cast(typing.Iterable[str], reader.fieldnames)
                if col.lower().strip() in db_columns
            ]
            # skip empty files since we won't have any valid columns
            # which causes the COPY command below to fail
            if cols:
                extractor.copy(CsvFile(cols, full_table, match))


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
    parser.add_argument(
        "--truncate",
        action="store_true",
        default=False,
        help="Truncate tables before reloading. Default is false",
    )

    args = parser.parse_args()
    print("base dir " + args.base_dir)
    load_from_csv(
        SnowflakeExecutor()
        if args.database_type == "snowflake"
        else PostgresExecutor(psycopg.connect(get_connection_string(LoadMode.SYNTHETIC))),
        args.base_dir or default_dir,
        args.truncate,
    )
