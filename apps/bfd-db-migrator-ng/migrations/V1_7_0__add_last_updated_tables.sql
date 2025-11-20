CREATE TABLE idr.beneficiary_updates(
    bene_sk BIGINT PRIMARY KEY,
    last_updated TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_beneficiary_updates_last_updated
    ON idr.beneficiary_updates (last_updated);