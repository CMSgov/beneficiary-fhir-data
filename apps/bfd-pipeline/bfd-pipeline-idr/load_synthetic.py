import csv
import psycopg
import os
import typing

tables = [
    {"csv_name": "SYNTHETIC_BENE.csv", "table": "v2_mdcr_bene"},
    {"csv_name": "SYNTHETIC_BENE_HSTRY.csv", "table": "v2_mdcr_bene_hstry"},
    {"csv_name": "SYNTHETIC_BENE_MBI_ID.csv", "table": "v2_mdcr_bene_mbi_id"},
    {"csv_name": "SYNTHETIC_CLM.csv", "table": "v2_mdcr_clm"},
    {"csv_name": "SYNTHETIC_CLM_INSTNL.csv", "table": "v2_mdcr_clm_instnl"},
    {"csv_name": "SYNTHETIC_CLM_DCMTN.csv", "table": "v2_mdcr_clm_dcmtn"},
    {"csv_name": "SYNTHETIC_CLM_DT_SGNTR.csv", "table": "v2_mdcr_clm_dt_sgntr"},
    {"csv_name": "SYNTHETIC_CLM_VAL.csv", "table": "v2_mdcr_clm_val"},
    {"csv_name": "SYNTHETIC_CLM_LINE.csv", "table": "v2_mdcr_clm_line"},
    {"csv_name": "SYNTHETIC_CLM_LINE_INSTNL.csv", "table": "v2_mdcr_clm_line_instnl"},
    {"csv_name": "SYNTHETIC_CLM_ANSI_SGNTR.csv", "table": "v2_mdcr_clm_ansi_sgntr"},
    {"csv_name": "SYNTHETIC_CLM_PROD.csv", "table": "v2_mdcr_clm_prod"},
]


def load_from_csv(conn: psycopg.Connection, src_folder: str):
    for table in tables:
        with open(f"{src_folder}/{table["csv_name"]}", "r") as f:
            reader = csv.DictReader(f)

            cols = list(typing.cast(typing.Iterable, reader.fieldnames))
            cols_str = ",".join(cols)
            with conn.cursor() as cur:
                with cur.copy(
                    f"COPY cms_vdm_view_mdcr_prd.{table["table"]} ({cols_str}) FROM STDIN"  # type: ignore
                ) as copy:
                    for row in reader:
                        copy.write_row([row[c] if row[c] else None for c in cols])
            conn.commit()


if __name__ == "__main__":
    load_from_csv(
        psycopg.connect(
            f"host={os.environ["BFD_DB_ENDPOINT"]} dbname=idr user={os.environ["BFD_DB_USERNAME"]} password={os.environ["BFD_DB_PASSWORD"]}"
        ),
        "../../bfd-model/bfd-model-idr/sample-data/generator/out",
    )
