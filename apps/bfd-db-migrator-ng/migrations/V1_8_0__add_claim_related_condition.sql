ALTER TABLE idr.claim_item
    ADD COLUMN clm_rlt_cond_sgntr_sk BIGINT,
    ADD COLUMN clm_rlt_cond_cd VARCHAR(20),
    ADD COLUMN clm_rlt_cond_sgntr_sqnc_num INT,
    ADD COLUMN idr_insrt_ts_rlt_cond TIMESTAMPTZ,
    ADD COLUMN idr_updt_ts_rlt_cond TIMESTAMPTZ;
