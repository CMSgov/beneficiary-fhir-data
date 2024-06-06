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

ALTER TABLE rda.mcs_adjustments
DROP CONSTRAINT IF EXISTS mcs_adjustments_parent;

ALTER TABLE rda.mcs_adjustments
    ADD CONSTRAINT mcs_adjustments_parent
        FOREIGN KEY (idr_clm_hd_icn)
            REFERENCES rda.mcs_claims
                (idr_clm_hd_icn)
            ON DELETE CASCADE;

ALTER TABLE rda.mcs_audits
DROP CONSTRAINT IF EXISTS mcs_audits_parent;

ALTER TABLE rda.mcs_audits
    ADD CONSTRAINT mcs_audits_parent
        FOREIGN KEY (idr_clm_hd_icn)
            REFERENCES rda.mcs_claims
                (idr_clm_hd_icn)
            ON DELETE CASCADE;

ALTER TABLE rda.mcs_details
DROP CONSTRAINT IF EXISTS mcs_details_parent;

ALTER TABLE rda.mcs_details
    ADD CONSTRAINT mcs_details_parent
        FOREIGN KEY (idr_clm_hd_icn)
            REFERENCES rda.mcs_claims
                (idr_clm_hd_icn)
            ON DELETE CASCADE;

ALTER TABLE rda.mcs_diagnosis_codes
DROP CONSTRAINT IF EXISTS mcs_diagnosis_codes_parent;

ALTER TABLE rda.mcs_diagnosis_codes
    ADD CONSTRAINT mcs_diagnosis_codes_parent
        FOREIGN KEY (idr_clm_hd_icn)
            REFERENCES rda.mcs_claims
                (idr_clm_hd_icn)
            ON DELETE CASCADE;

ALTER TABLE rda.mcs_locations
DROP CONSTRAINT IF EXISTS mcs_locations_parent;

ALTER TABLE rda.mcs_locations
    ADD CONSTRAINT mcs_locations_parent
        FOREIGN KEY (idr_clm_hd_icn)
            REFERENCES rda.mcs_claims
                (idr_clm_hd_icn)
            ON DELETE CASCADE;
