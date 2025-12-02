import csv
import sys
import typing
from pathlib import Path

import psycopg

from loader import get_connection_string

tables = [
    {"csv_name": "SYNTHETIC_BENE_HSTRY.csv", "table": "v2_mdcr_bene_hstry"},
    {"csv_name": "SYNTHETIC_BENE_MBI_ID.csv", "table": "v2_mdcr_bene_mbi_id"},
    {"csv_name": "SYNTHETIC_BENE_XREF.csv", "table": "v2_mdcr_bene_xref"},
    {"csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT.csv", "table": "v2_mdcr_bene_mdcr_entlmt"},
    {
        "csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv",
        "table": "v2_mdcr_bene_mdcr_entlmt_rsn",
    },
    {"csv_name": "SYNTHETIC_BENE_MDCR_STUS.csv", "table": "v2_mdcr_bene_mdcr_stus"},
    {"csv_name": "SYNTHETIC_BENE_TP.csv", "table": "v2_mdcr_bene_tp"},
    {"csv_name": "SYNTHETIC_BENE_CMBND_DUAL_MDCR.csv", "table": "v2_mdcr_bene_cmbnd_dual_mdcr"},
    {"csv_name": "SYNTHETIC_CLM.csv", "table": "v2_mdcr_clm"},
    {"csv_name": "SYNTHETIC_CLM_INSTNL.csv", "table": "v2_mdcr_clm_instnl"},
    {"csv_name": "SYNTHETIC_CLM_PRFNL.csv", "table": "v2_mdcr_clm_prfnl"},
    {"csv_name": "SYNTHETIC_CLM_DCMTN.csv", "table": "v2_mdcr_clm_dcmtn"},
    {"csv_name": "SYNTHETIC_CLM_DT_SGNTR.csv", "table": "v2_mdcr_clm_dt_sgntr"},
    {"csv_name": "SYNTHETIC_CLM_VAL.csv", "table": "v2_mdcr_clm_val"},
    {"csv_name": "SYNTHETIC_CLM_LINE.csv", "table": "v2_mdcr_clm_line"},
    {"csv_name": "SYNTHETIC_CLM_LINE_INSTNL.csv", "table": "v2_mdcr_clm_line_instnl"},
    {"csv_name": "SYNTHETIC_CLM_LINE_PRFNL.csv", "table": "v2_mdcr_clm_line_prfnl"},
    {"csv_name": "SYNTHETIC_CLM_ANSI_SGNTR.csv", "table": "v2_mdcr_clm_ansi_sgntr"},
    {"csv_name": "SYNTHETIC_CLM_PROD.csv", "table": "v2_mdcr_clm_prod"},
    {"csv_name": "SYNTHETIC_CLM_FISS.csv", "table": "v2_mdcr_clm_fiss"},
    {"csv_name": "SYNTHETIC_CLM_LINE_RX.csv", "table": "v2_mdcr_clm_line_rx"},
    {"csv_name": "SYNTHETIC_CLM_LCTN_HSTRY.csv", "table": "v2_mdcr_clm_lctn_hstry"},
    {"csv_name": "SYNTHETIC_CLM_RLT_COND_SGNTR_MBR.csv", "table": "v2_mdcr_clm_rlt_cond_sgntr_mbr"},
    {"csv_name": "SYNTHETIC_PRVDR_HSTRY.csv", "table": "v2_mdcr_prvdr_hstry"},
]


def load_from_csv(conn: psycopg.Connection, src_folder: str) -> None:
    for table in tables:
        with conn.cursor() as cur:
            # Clear out any previous data
            sql_table = table["table"]
            full_table = f"cms_vdm_view_mdcr_prd.{sql_table}"
            cur.execute(f"TRUNCATE TABLE {full_table}")  # type: ignore
            file = table["csv_name"]
            _load_file(cur, src_folder, file, sql_table, full_table)
        conn.commit()


def _load_file(
    cur: psycopg.Cursor, src_folder: str, file: str, sql_table: str, full_table: str
) -> None:
    for match in Path(src_folder).glob(f"./**/{file}"):
        print(f"loading {match}")
        with match.open() as f:
            reader = csv.DictReader(f)
            # skip empty files
            if reader.fieldnames is None:
                continue

            # fetch the list of columns from the database and filter them out
            # so we don't get errors trying to insert extra columns
            db_columns = cur.execute(
                f"""
                    SELECT column_name FROM INFORMATION_SCHEMA.COLUMNS 
                    WHERE table_name = '{sql_table}'
                """  # type: ignore
            )
            db_columns = [typing.cast(str, col[0]).lower() for col in db_columns]

            cols = [
                col
                for col in typing.cast(typing.Iterable[str], reader.fieldnames)
                if col.lower().strip() in db_columns
            ]
            if cols:
                cols_str = ",".join(cols)
                with cur.copy(
                    f"COPY {full_table} ({cols_str}) FROM STDIN"  # type: ignore
                ) as copy:
                    for row in reader:
                        copy.write_row([row[c] if row[c] else None for c in cols])


if __name__ == "__main__":
    baseDir = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] != "" else "../../bfd-model-idr/out"
    load_from_csv(
        psycopg.connect(get_connection_string()),
        baseDir,
    )
