ALTER TABLE idr.claim_rx
ADD COLUMN bfd_tot_rx_cst_amt numeric;

ALTER TABLE idr.claim_rx
DROP COLUMN bfd_claim_updated_ts;

ALTER TABLE idr.claim_institutional_nch
DROP COLUMN bfd_claim_updated_ts;

ALTER TABLE idr.claim_institutional_ss
DROP COLUMN bfd_claim_updated_ts;

ALTER TABLE idr.claim_professional_nch
DROP COLUMN bfd_claim_updated_ts;

ALTER TABLE idr.claim_professional_ss
DROP COLUMN bfd_claim_updated_ts;
