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
    {"csv_name": "SYNTHETIC_CLM.csv", "table": "v2_mdcr_clm"},
    {"csv_name": "SYNTHETIC_CLM_INSTNL.csv", "table": "v2_mdcr_clm_instnl"},
    {"csv_name": "SYNTHETIC_CLM_DCMTN.csv", "table": "v2_mdcr_clm_dcmtn"},
    {"csv_name": "SYNTHETIC_CLM_DT_SGNTR.csv", "table": "v2_mdcr_clm_dt_sgntr"},
    {"csv_name": "SYNTHETIC_CLM_VAL.csv", "table": "v2_mdcr_clm_val"},
    {"csv_name": "SYNTHETIC_CLM_LINE.csv", "table": "v2_mdcr_clm_line"},
    {"csv_name": "SYNTHETIC_CLM_LINE_INSTNL.csv", "table": "v2_mdcr_clm_line_instnl"},
    {"csv_name": "SYNTHETIC_CLM_ANSI_SGNTR.csv", "table": "v2_mdcr_clm_ansi_sgntr"},
    {"csv_name": "SYNTHETIC_CLM_PROD.csv", "table": "v2_mdcr_clm_prod"},
    {"csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT.csv", "table": "v2_mdcr_bene_mdcr_entlmt"},
    {
        "csv_name": "SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv",
        "table": "v2_mdcr_bene_mdcr_entlmt_rsn",
    },
    {"csv_name": "SYNTHETIC_BENE_MDCR_STUS.csv", "table": "v2_mdcr_bene_mdcr_stus"},
    {"csv_name": "SYNTHETIC_BENE_TP.csv", "table": "v2_mdcr_bene_tp"},
]


def load_from_csv(conn: psycopg.Connection, src_folder: str) -> None:
    for table in tables:
        file = f"{src_folder}/{table['csv_name']}"
        try:
            with Path(file).open() as f:
                reader = csv.DictReader(f)

                cols = list(typing.cast(typing.Iterable[str], reader.fieldnames))
                cols_str = ",".join(cols)
                full_table = f"cms_vdm_view_mdcr_prd.{table['table']}"
                with conn.cursor() as cur:
                    # Clear out any previous data
                    cur.execute(f"TRUNCATE TABLE {full_table}")  # type: ignore
                    with cur.copy(
                        f"COPY {full_table} ({cols_str}) FROM STDIN"  # type: ignore
                    ) as copy:
                        for row in reader:
                            copy.write_row([row[c] if row[c] else None for c in cols])
                conn.commit()
        except OSError as e:
            print(f"Unable to load file, skipping: {e}")


if __name__ == "__main__":
    baseDir = (
        sys.argv[1]
        if len(sys.argv) > 1
        else "../../bfd-model/bfd-model-idr/sample-data/generator/out"
    )
    load_from_csv(
        psycopg.connect(get_connection_string()),
        baseDir,
    )
