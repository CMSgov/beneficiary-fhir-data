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
CREATE OR REPLACE VIEW idr.valid_beneficiary AS
SELECT *
FROM idr.beneficiary b
WHERE NOT EXISTS(SELECT 1 FROM idr.beneficiary_overshare_mbi om WHERE om.bene_mbi_id = b.bene_mbi_id);