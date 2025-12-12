ALTER TABLE idr.beneficiary
ADD COLUMN bfd_part_a_coverage_updated_ts TIMESTAMPTZ DEFAULT now();

ALTER TABLE idr.beneficiary
ADD COLUMN bfd_part_b_coverage_updated_ts TIMESTAMPTZ DEFAULT now();

ALTER TABLE idr.beneficiary
ADD COLUMN bfd_part_c_coverage_updated_ts TIMESTAMPTZ DEFAULT now();

ALTER TABLE idr.beneficiary
ADD COLUMN bfd_part_d_coverage_updated_ts TIMESTAMPTZ DEFAULT now();

ALTER TABLE idr.beneficiary
ADD COLUMN bfd_part_dual_coverage_updated_ts TIMESTAMPTZ DEFAULT now();

ALTER TABLE idr.beneficiary
ADD COLUMN bfd_patient_updated_ts TIMESTAMPTZ DEFAULT now();

ALTER TABLE idr.claim
ADD COLUMN bfd_claim_updated_ts TIMESTAMPTZ DEFAULT now();

-- To add a new a column to a view it needs to be re-created
-- beneficiary_identity depends on valid_beneficiary so it also needs to be re-created
DROP VIEW IF EXISTS idr.beneficiary_identity;

-- Re-creating valid_benficiary to add bfd_coverage_updated_ts and bfd_patient_updated_ts
CREATE OR REPLACE VIEW idr.valid_beneficiary AS
SELECT *
FROM idr.beneficiary b
WHERE NOT EXISTS(SELECT 1 FROM idr.beneficiary_overshare_mbi om WHERE om.bene_mbi_id = b.bene_mbi_id);

CREATE VIEW idr.beneficiary_identity AS
SELECT DISTINCT
    bene.bene_sk,
    bene.bene_xref_efctv_sk_computed,
    bene.bene_mbi_id,
    bene_mbi.bene_mbi_efctv_dt,
    bene_mbi.bene_mbi_obslt_dt
FROM idr.valid_beneficiary bene
LEFT JOIN idr.beneficiary_mbi_id bene_mbi
ON bene.bene_mbi_id = bene_mbi.bene_mbi_id
AND bene_mbi.idr_ltst_trans_flg = 'Y';