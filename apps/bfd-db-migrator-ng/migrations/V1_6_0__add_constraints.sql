ALTER TABLE idr.claim_professional
DROP COLUMN clm_lctn_cd_sqnc_num;

ALTER TABLE idr.claim
ALTER COLUMN clm_adjstmt_type_cd SET NOT NULL,
ALTER COLUMN clm_sbmt_frmt_cd SET NOT NULL,
ALTER COLUMN clm_sbmtr_cntrct_num SET NOT NULL,
ALTER COLUMN clm_sbmtr_cntrct_pbp_num SET NOT NULL;

ALTER TABLE idr.claim_institutional
ALTER COLUMN dgns_drg_outlier_cd SET NOT NULL;

ALTER TABLE idr.contract_pbp_number
ALTER COLUMN cntrct_pbp_num SET NOT NULL;