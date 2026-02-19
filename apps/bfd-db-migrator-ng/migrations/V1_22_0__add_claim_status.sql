ALTER TABLE idr.claim
--v2_mdcr_clm_lctn_hstry
ADD COLUMN clm_audt_trl_stus_cd VARCHAR(2),
ADD COLUMN clm_audt_trl_lctn_cd VARCHAR(5),
ADD COLUMN idr_insrt_ts_lctn_hstry TIMESTAMPTZ,
ADD COLUMN idr_updt_ts_lctn_hstry TIMESTAMPTZ;
