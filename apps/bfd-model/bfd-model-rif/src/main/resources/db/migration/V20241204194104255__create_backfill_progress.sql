CREATE TABLE IF NOT EXISTS ccw.samhsa_backfill_progress (
    claim_table CHARACTER VARYING,
    last_processed_claim CHARACTER VARYING,
    PRIMARY KEY(claim_table)
)