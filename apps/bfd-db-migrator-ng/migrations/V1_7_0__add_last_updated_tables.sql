CREATE TABLE idr.beneficiary_updates(
    bene_sk BIGINT PRIMARY KEY,
    last_updated TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_beneficiary_updates_last_updated
    ON idr.beneficiary_updates (last_updated);


CREATE TABLE idr.claim_updates(
    clm_uniq_ID BIGINT PRIMARY KEY,
    last_updated TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_claim_updates_last_updated
    ON idr.claim_updates (last_updated);