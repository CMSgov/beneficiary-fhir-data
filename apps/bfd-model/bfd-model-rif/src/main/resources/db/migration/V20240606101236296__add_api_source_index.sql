CREATE INDEX IF NOT EXISTS fiss_claims_api_source_idx
    ON rda.fiss_claims USING btree (api_source);
CREATE INDEX IF NOT EXISTS mcs_claims_api_source_idx
    ON rda.mcs_claims USING btree (api_source);