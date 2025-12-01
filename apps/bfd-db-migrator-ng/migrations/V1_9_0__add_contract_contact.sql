CREATE TABLE idr.contract_pbp_contact (
    cntrct_pbp_sk BIGINT NOT NULL,
    cntrct_plan_cntct_obslt_dt DATE NOT NULL,
    cntrct_plan_cntct_type_cd VARCHAR(3) NOT NULL,
    cntrct_plan_free_extnsn_num VARCHAR(7) NOT NULL,
    cntrct_plan_cntct_free_num VARCHAR(10) NOT NULL,
    cntrct_plan_cntct_extnsn_num VARCHAR(7) NOT NULL,
    cntrct_plan_cntct_tel_num VARCHAR(10) NOT NULL,
    cntrct_pbp_end_dt DATE NOT NULL,
    cntrct_pbp_bgn_dt DATE NOT NULL,
    cntrct_plan_cntct_st_1_adr VARCHAR(55) NOT NULL,
    cntrct_plan_cntct_st_2_adr VARCHAR(55) NOT NULL,
    cntrct_plan_cntct_city_name VARCHAR(30) NOT NULL,
    cntrct_plan_cntct_state_cd VARCHAR(2) NOT NULL,
    cntrct_plan_cntct_zip_cd VARCHAR(9) NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ,
    PRIMARY KEY(cntrct_pbp_sk, cntrct_pbp_bgn_dt)
);

ALTER TABLE idr.contract_pbp_number
ALTER COLUMN bfd_updated_ts DROP NOT NULL;