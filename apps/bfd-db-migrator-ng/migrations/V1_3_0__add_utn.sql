ALTER TABLE idr.claim_item
ADD COLUMN clm_line_pmd_uniq_trkng_num VARCHAR(14),
-- from v2_mdcr_clm_line_dcmtn
ADD COLUMN clm_line_pa_uniq_trkng_num VARCHAR(50);