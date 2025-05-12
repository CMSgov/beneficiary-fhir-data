import csv
import psycopg
import os

tables = [
    {"csv_name": "SYNTHETIC_BENE.csv", "table": "v2_mdcr_bene"},
    {"csv_name": "SYNTHETIC_BENE_HSTRY.csv", "table": "v2_mdcr_bene_hstry"},
    {"csv_name": "SYNTHETIC_BENE_MBI_ID.csv", "table": "v2_mdcr_bene_mbi_id"},
]

conn = psycopg.connect(
    f"host={os.environ["BFD_DB_ENDPOINT"]} dbname=idr user={os.environ["BFD_DB_USERNAME"]} password={os.environ["BFD_DB_PASSWORD"]}"
)

for table in tables:
    with open(
        f"../../bfd-model/bfd-model-idr/sample-data/generator/out/{table["csv_name"]}",
        "r",
    ) as f:
        reader = csv.DictReader(f)
        cols = list(reader.fieldnames)
        cols_str = ",".join(cols)
        with conn.cursor() as cur:
            with cur.copy(
                f"COPY cms_vdm_view_mdcr_prd.{table["table"]} ({cols_str}) FROM STDIN"
            ) as copy:
                for row in reader:
                    copy.write_row([row[c] if row[c] else None for c in cols])
        conn.commit()
