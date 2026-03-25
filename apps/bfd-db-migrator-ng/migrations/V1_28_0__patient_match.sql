CREATE INDEX ON idr.beneficiary(RIGHT(bene_ssn_num, 4));

ALTER TABLE idr.beneficiary
ADD COLUMN bfd_normalized_address TEXT;

-- To add a new a column to a view it needs to be re-created
CREATE OR REPLACE VIEW idr.valid_beneficiary AS
SELECT *
FROM idr.beneficiary b
WHERE NOT EXISTS(SELECT 1 FROM idr.beneficiary_overshare_mbi om WHERE om.bene_mbi_id = b.bene_mbi_id);

CREATE INDEX ON idr.beneficiary(bfd_normalized_address);
