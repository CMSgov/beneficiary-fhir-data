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
