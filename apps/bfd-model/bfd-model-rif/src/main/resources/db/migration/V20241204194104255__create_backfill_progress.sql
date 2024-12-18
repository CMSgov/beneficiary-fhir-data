CREATE TABLE IF NOT EXISTS ccw.samhsa_backfill_progress (
    claim_table CHARACTER VARYING,
    last_processed_claim CHARACTER VARYING,
    table_thread_number INTEGER,
    total_processed BIGINT,
    total_tags BIGINT,
    PRIMARY KEY(claim_table, table_thread_number)
)
