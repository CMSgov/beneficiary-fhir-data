CREATE TABLE IF NOT EXISTS ccw.samhsa_backfill_progress (
    claim_table CHARACTER VARYING,
    last_processed_claim CHARACTER VARYING,
    total_processed BIGINT,
    total_tags BIGINT,
    PRIMARY KEY(claim_table)
)
