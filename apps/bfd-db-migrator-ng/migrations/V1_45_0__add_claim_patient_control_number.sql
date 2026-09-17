ALTER TABLE idr.claim_professional_nch
ADD COLUMN clm_ptnt_cntl_num VARCHAR(38);

ALTER TABLE idr.claim_professional_ss
ADD COLUMN clm_ptnt_cntl_num VARCHAR(38);

ALTER TABLE idr.claim_institutional_nch
ADD COLUMN clm_ptnt_cntl_num VARCHAR(38);

ALTER TABLE idr.claim_institutional_ss
ADD COLUMN clm_ptnt_cntl_num VARCHAR(38);
