ALTER TABLE idr.claim
-- from v2_mdcr_clm_dcmtn
ADD COLUMN clm_bnft_enhncmt_2_cd VARCHAR(2),
ADD COLUMN clm_bnft_enhncmt_3_cd VARCHAR(2),
ADD COLUMN clm_bnft_enhncmt_4_cd VARCHAR(2),
ADD COLUMN clm_bnft_enhncmt_5_cd VARCHAR(2);

ALTER TABLE idr.claim_item
-- from v2_mdcr_clm_line_dcmtn
ADD COLUMN clm_line_bnft_enhncmt_1_cd VARCHAR(2);

CREATE TABLE idr.claim_related_condition (
     clm_uniq_id BIGINT NOT NULL,
     clm_rlt_cond_sgntr_sqnc_num INT NOT NULL,
     clm_rlt_cond_cd VARCHAR(20) NOT NULL,
     idr_insrt_ts TIMESTAMPTZ NOT NULL,
     idr_updt_ts TIMESTAMPTZ NOT NULL,
     bfd_created_ts TIMESTAMPTZ NOT NULL,
     bfd_updated_ts TIMESTAMPTZ NOT NULL,
     PRIMARY KEY(clm_uniq_id, clm_rlt_cond_sgntr_sqnc_num)
);
