ALTER TABLE idr.claim
-- from v2_mdcr_clm_dcmtn
ADD COLUMN clm_bnft_enhncmt_2_cd VARCHAR(2),
ADD COLUMN clm_bnft_enhncmt_3_cd VARCHAR(2),
ADD COLUMN clm_bnft_enhncmt_4_cd VARCHAR(2),
ADD COLUMN clm_bnft_enhncmt_5_cd VARCHAR(2);

ALTER TABLE idr.claim_item
-- from v2_mdcr_clm_line_dcmtn
ADD COLUMN clm_line_bnft_enhncmt_1_cd VARCHAR(2);
