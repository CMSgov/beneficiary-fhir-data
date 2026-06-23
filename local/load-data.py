"""BFD v3 IDR Data Loader - loads synthetic CSVs into PostgreSQL."""

import csv
import io
import os
import sys
import time

import psycopg2

DB_HOST = "db"
DB_PORT = 5432
DB_NAME = "fhirdb"
DB_USER = os.environ.get("DB_USER", "bfd")
DB_PASS = os.environ.get("DB_PASSWORD", "InsecureLocalDev")

# Mapping: CSV filename (without SYNTHETIC_ prefix and .csv suffix) -> DB table name
TABLE_MAP = {
    "BENE_HSTRY": "beneficiary",
    "BENE_MBI_ID": "beneficiary_mbi_id",
    "BENE_CMBND_DUAL_MDCR": "beneficiary_dual_eligibility",
    "BENE_LIS": "beneficiary_low_income_subsidy",
    "BENE_MAPD_ENRLMT": "beneficiary_ma_part_d_enrollment",
    "BENE_MAPD_ENRLMT_RX": "beneficiary_ma_part_d_enrollment_rx",
    "BENE_MDCR_ENTLMT": "beneficiary_entitlement",
    "BENE_MDCR_ENTLMT_RSN": "beneficiary_entitlement_reason",
    "BENE_MDCR_STUS": "beneficiary_status",
    "BENE_TP": "beneficiary_third_party",
    "CNTRCT_PBP_CNTCT": "contract_pbp_contact",
    "CNTRCT_PBP_NUM": "contract_pbp_number",
}

DATA_DIR = "/app/idr-data"


def wait_for_db():
    """Wait until PostgreSQL is ready."""
    for _ in range(30):
        try:
            conn = psycopg2.connect(
                host=DB_HOST, port=DB_PORT, dbname=DB_NAME, user=DB_USER, password=DB_PASS
            )
            conn.close()
            return
        except psycopg2.OperationalError:
            time.sleep(2)
    print("ERROR: Could not connect to database", file=sys.stderr)
    sys.exit(1)


def get_db_columns(conn, table_name):
    """Get the set of column names for a table in the idr schema."""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT column_name FROM information_schema.columns "
            "WHERE table_schema = 'idr' AND table_name = %s",
            (table_name,),
        )
        return {row[0] for row in cur.fetchall()}


def drop_not_null_constraints(conn, table_name, columns_in_csv):
    """Drop NOT NULL constraints (except PKs) to allow flexible data loading.

    This is a local dev convenience — real environments enforce constraints.
    """
    with conn.cursor() as cur:
        # Get primary key columns (can't drop NOT NULL on these)
        cur.execute(
            "SELECT a.attname FROM pg_index i "
            "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) "
            "WHERE i.indrelid = 'idr.%s'::regclass AND i.indisprimary" % table_name
        )
        pk_cols = {row[0] for row in cur.fetchall()}

        cur.execute(
            "SELECT column_name FROM information_schema.columns "
            "WHERE table_schema = 'idr' AND table_name = %s AND is_nullable = 'NO'",
            (table_name,),
        )
        not_null_cols = [row[0] for row in cur.fetchall() if row[0] not in pk_cols]
        for col in not_null_cols:
            cur.execute(
                f"ALTER TABLE idr.{table_name} ALTER COLUMN {col} DROP NOT NULL"
            )
    conn.commit()


def load_csv(conn, csv_path, table_name):
    """Load a CSV file into a table, matching columns by name (case-insensitive).

    Skips CSV columns that don't exist in the DB table.
    """
    db_columns = get_db_columns(conn, table_name)
    if not db_columns:
        print(f"  SKIPPED: table idr.{table_name} has no columns (doesn't exist?)")
        return False

    with open(csv_path, "r", newline="") as f:
        reader = csv.DictReader(f)
        csv_columns = [c.lower() for c in reader.fieldnames]

        # Filter to only columns that exist in the DB
        valid_columns = [c for c in csv_columns if c in db_columns]
        if not valid_columns:
            print(f"  SKIPPED: no matching columns for idr.{table_name}")
            return False

        skipped = set(csv_columns) - set(valid_columns)
        if skipped:
            print(f"  (skipping {len(skipped)} CSV columns not in DB: {', '.join(sorted(skipped))})")

        # Drop NOT NULL constraints for columns not in the CSV
        drop_not_null_constraints(conn, table_name, valid_columns)

        # Build a CSV buffer with only the valid columns
        buf = io.StringIO()
        writer = csv.writer(buf)
        row_count = 0
        for row in reader:
            writer.writerow([row[c.upper()] if c.upper() in row else row.get(c, "") for c in valid_columns])
            row_count += 1

        buf.seek(0)
        col_list = ", ".join(valid_columns)
        copy_sql = f"COPY idr.{table_name}({col_list}) FROM STDIN WITH (FORMAT csv)"

        with conn.cursor() as cur:
            cur.copy_expert(copy_sql, buf)
        conn.commit()

        print(f"  OK: {row_count} rows loaded into idr.{table_name}")
        return True


def main():
    print("=== BFD v3 IDR Data Loader ===")
    print("Loading synthetic IDR data into the database...")
    print()

    wait_for_db()

    conn = psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME, user=DB_USER, password=DB_PASS
    )

    loaded = 0
    failed = 0

    for csv_key, table_name in TABLE_MAP.items():
        csv_path = os.path.join(DATA_DIR, f"SYNTHETIC_{csv_key}.csv")
        if not os.path.exists(csv_path):
            print(f"  SKIPPED: {csv_path} not found")
            continue

        print(f"Loading idr.{table_name} from SYNTHETIC_{csv_key}.csv...")
        try:
            if load_csv(conn, csv_path, table_name):
                loaded += 1
            else:
                failed += 1
        except Exception as e:
            conn.rollback()
            print(f"  FAILED: {e}")
            failed += 1

    conn.close()

    print()
    if failed > 0:
        print(f"=== Data loading finished: {loaded} succeeded, {failed} failed ===")
    else:
        print(f"=== Data loading finished: {loaded} tables loaded successfully ===")


if __name__ == "__main__":
    main()
