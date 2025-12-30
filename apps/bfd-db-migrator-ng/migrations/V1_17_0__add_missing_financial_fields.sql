ALTER TABLE idr.claim_line_professional
ADD COLUMN clm_mdcr_prmry_pyr_alowd_amt NUMERIC;

ALTER TABLE idr.claim_institutional
ADD COLUMN clm_ss_outlier_std_pymt_amt NUMERIC;