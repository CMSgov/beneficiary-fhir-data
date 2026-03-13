TRUNCATE TABLE idr.provider_history CASCADE;
DROP TABLE idr.provider_history CASCADE;

-- bene_xref_efctv_sk_computed will be dropped in a subsequent migration since we're computing this in the pipeline now.
CREATE INDEX ON idr.beneficiary(bene_xref_efctv_sk);
