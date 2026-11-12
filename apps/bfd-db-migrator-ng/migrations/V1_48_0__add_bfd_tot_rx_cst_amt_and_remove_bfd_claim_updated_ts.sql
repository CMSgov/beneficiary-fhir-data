ALTER TABLE idr.claim_rx
ADD COLUMN bfd_tot_rx_cst_amt numeric;

ALTER TABLE idr.claim_rx
DROP COLUMN bfd_claim_updated_ts;
