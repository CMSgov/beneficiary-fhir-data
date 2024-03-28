ALTER TABLE rda.fiss_audit_trails
DROP CONSTRAINT IF EXISTS fiss_audit_trails_parent;

ALTER TABLE rda.fiss_audit_trails
    ADD CONSTRAINT fiss_audit_trails_parent
        FOREIGN KEY (claim_id)
            REFERENCES rda.fiss_claims
                (claim_id)
            ON DELETE CASCADE;

ALTER TABLE rda.fiss_diagnosis_codes
DROP CONSTRAINT IF EXISTS fiss_diagnosis_codes_parent;

ALTER TABLE rda.fiss_diagnosis_codes
    ADD CONSTRAINT fiss_diagnosis_codes_parent
        FOREIGN KEY (claim_id)
            REFERENCES rda.fiss_claims
                (claim_id)
            ON DELETE CASCADE;

ALTER TABLE rda.fiss_payers
DROP CONSTRAINT IF EXISTS fiss_payers_parent;

ALTER TABLE rda.fiss_payers
    ADD CONSTRAINT fiss_payers_parent
        FOREIGN KEY (claim_id)
            REFERENCES rda.fiss_claims
                (claim_id)
            ON DELETE CASCADE;

ALTER TABLE rda.fiss_proc_codes
DROP CONSTRAINT IF EXISTS fiss_proc_codes_parent;

ALTER TABLE rda.fiss_proc_codes
    ADD CONSTRAINT fiss_proc_codes_parent
        FOREIGN KEY (claim_id)
            REFERENCES rda.fiss_claims
                (claim_id)
            ON DELETE CASCADE;

ALTER TABLE rda.fiss_revenue_lines
DROP CONSTRAINT IF EXISTS fiss_revenue_lines_parent;

ALTER TABLE rda.fiss_revenue_lines
    ADD CONSTRAINT fiss_revenue_lines_parent
        FOREIGN KEY (claim_id)
            REFERENCES rda.fiss_claims
                (claim_id)
            ON DELETE CASCADE;