DROP TABLE IF EXISTS cms_vdm_view_mdcr_prd.v2_mdcr_bene;
DROP TABLE IF EXISTS cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry;
DROP SCHEMA IF EXISTS cms_vdm_view_mdcr_prd;

CREATE SCHEMA cms_vdm_view_mdcr_prd;
CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene (
    bene_sk BIGINT NOT NULL PRIMARY KEY, 
    bene_xref_efctv_sk BIGINT NOT NULL, 
    bene_mbi_id VARCHAR(11),
    bene_1st_name VARCHAR(30),
    bene_midl_name VARCHAR(15),
    bene_last_name VARCHAR(40),
    bene_brth_dt DATE,
    bene_death_dt DATE,
    bene_vrfy_death_day_sw VARCHAR(1),
    bene_sex_cd VARCHAR(1),
    bene_race_cd VARCHAR(2),
    geo_usps_state_cd VARCHAR(2),
    geo_zip5_cd VARCHAR(5),
    geo_zip_plc_name VARCHAR(100),
    bene_line_1_adr VARCHAR(45),
    bene_line_2_adr VARCHAR(45),
    bene_line_3_adr VARCHAR(40),
    bene_line_4_adr VARCHAR(40),
    bene_line_5_adr VARCHAR(40),
    bene_line_6_adr VARCHAR(40),
    cntct_lang_cd VARCHAR(3),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry (
    bene_sk BIGINT NOT NULL,
    bene_xref_efctv_sk BIGINT NOT NULL,
    bene_mbi_id VARCHAR(11),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id (
    bene_mbi_id VARCHAR(11) NOT NULL,
    bene_mbi_efctv_dt DATE,
    bene_mbi_obslt_dt DATE,
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_mbi_id, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp (
    bene_sk BIGINT NOT NULL,
    bene_buyin_cd VARCHAR(2) NOT NULL,
    bene_tp_type_cd VARCHAR(1) NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_tp_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_stus (
    bene_sk BIGINT NOT NULL,
    bene_mdcr_stus_cd VARCHAR(2) NOT NULL,
    mdcr_stus_bgn_dt DATE NOT NULL,
    mdcr_stus_end_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, mdcr_stus_bgn_dt, mdcr_stus_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_type_cd VARCHAR(1),
    bene_mdcr_entlmt_stus_cd VARCHAR(1),
    bene_mdcr_enrlmt_rsn_cd VARCHAR(1),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_mdcr_entlmt_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt_rsn (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_rsn_cd VARCHAR(1),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_elctn_prd_usg (
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5),
    bene_pbp_num VARCHAR(3),
    bene_elctn_enrlmt_disenrlmt_cd VARCHAR(1),
    bene_elctn_aplctn_dt DATE,
    bene_enrlmt_efctv_dt DATE,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num (
    cntrct_pbp_sk BIGINT NOT NULL,
    cntrct_drug_plan_ind_cd VARCHAR(1),
    cntrct_pbp_type_cd VARCHAR(2),
    cntrct_pbp_sk_obslt_dt DATE
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm (
    clm_uniq_id BIGINT NOT NULL PRIMARY KEY,
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    bene_sk BIGINT NOT NULL,
    clm_cntl_num VARCHAR(40),
    clm_orig_cntl_num VARCHAR(40),
    clm_from_dt DATE,
    clm_thru_dt DATE,
    clm_efctv_dt DATE,
    clm_finl_actn_ind VARCHAR(1),
    clm_src_id VARCHAR(5),
    clm_query_cd VARCHAR(1),
    clm_mdcr_coinsrnc_amt NUMERIC,
    clm_blood_lblty_amt NUMERIC,
    clm_ncvrd_chrg_amt NUMERIC,
    clm_mdcr_ddctbl_amt NUMERIC,
    clm_cntrctr_num VARCHAR(5),
    clm_pmt_amt NUMERIC,
    clm_ltst_clm_ind VARCHAR(1),
    clm_atndg_prvdr_npi_num VARCHAR(10),
    clm_oprtg_prvdr_npi_num VARCHAR(10),
    clm_othr_prvdr_npi_num VARCHAR(10),
    clm_rndrg_prvdr_npi_num VARCHAR(10),
    prvdr_blg_prvdr_npi_num VARCHAR(10),
    clm_disp_cd VARCHAR(2),
    clm_sbmt_chrg_amt NUMERIC,
    clm_blood_pt_frnsh_qty INT,
    clm_nch_prmry_pyr_cd VARCHAR(1),
    clm_blg_prvdr_oscar_num VARCHAR(20),
    clm_idr_ld_dt DATE
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_dcmtn (
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    clm_nrln_ric_cd VARCHAR(1),
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_instnl (
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    clm_admsn_type_cd VARCHAR(2),
    bene_ptnt_stus_cd VARCHAR(2),
    dgns_drg_cd INT,
    clm_mdcr_instnl_mco_pd_sw VARCHAR(1),
    clm_admsn_src_cd VARCHAR(2),
    clm_bill_fac_type_cd VARCHAR(1),
    clm_bill_clsfctn_cd VARCHAR(1),
    clm_bill_freq_cd VARCHAR(1),
    clm_fi_actn_cd VARCHAR(1),
    clm_mdcr_ip_lrd_use_cnt INT,
    clm_hipps_uncompd_care_amt NUMERIC,
    clm_instnl_mdcr_coins_day_cnt INT,
    clm_instnl_ncvrd_day_cnt NUMERIC,
    clm_instnl_per_diem_amt NUMERIC,
    clm_mdcr_npmt_rsn_cd VARCHAR(2),
    clm_mdcr_ip_pps_drg_wt_num NUMERIC,
    clm_mdcr_ip_pps_dsprprtnt_amt NUMERIC,
    clm_mdcr_ip_pps_excptn_amt NUMERIC,
    clm_mdcr_ip_pps_cptl_fsp_amt NUMERIC,
    clm_mdcr_ip_pps_cptl_ime_amt NUMERIC,
    clm_mdcr_ip_pps_outlier_amt NUMERIC,
    clm_mdcr_ip_pps_cptl_hrmls_amt NUMERIC,
    clm_pps_ind_cd VARCHAR(1),
    clm_mdcr_ip_pps_cptl_tot_amt NUMERIC,
    clm_instnl_cvrd_day_cnt NUMERIC,
    clm_mdcr_instnl_prmry_pyr_amt NUMERIC,
    clm_instnl_prfnl_amt NUMERIC,
    clm_mdcr_ip_bene_ddctbl_amt NUMERIC,
    clm_instnl_drg_outlier_amt NUMERIC,
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_dt_sgntr (
    clm_dt_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_cms_proc_dt DATE,
    clm_actv_care_from_dt DATE,
    clm_dschrg_dt DATE,
    clm_submsn_dt DATE,
    clm_ncvrd_from_dt DATE,
    clm_ncvrd_thru_dt DATE,
    clm_actv_care_thru_dt DATE,
    clm_mdcr_exhstd_dt DATE
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_val (
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    clm_val_sqnc_num INT,
    clm_val_cd VARCHAR(2),
    clm_val_amt NUMERIC,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_val_sqnc_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_prod (
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    clm_val_sqnc_num INT,
    clm_dgns_prcdr_icd_ind VARCHAR(1),
    clm_dgns_cd VARCHAR(7),
    clm_prod_type_cd VARCHAR(1),
    clm_poa_ind VARCHAR(1),
    clm_prcdr_prfrm_dt DATE,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_val_sqnc_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line (
    clm_uniq_id BIGINT,
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    clm_line_num INT,
    clm_line_sbmt_chrg_amt NUMERIC,
    clm_line_alowd_chrg_amt NUMERIC,
    clm_line_ncvrd_chrg_amt NUMERIC,
    clm_line_prvdr_pmt_amt NUMERIC,
    clm_line_bene_pmt_amt NUMERIC,
    clm_line_bene_pd_amt NUMERIC,
    clm_line_cvrd_pd_amt NUMERIC,
    clm_line_blood_ddctbl_amt NUMERIC,
    clm_line_mdcr_ddctbl_amt NUMERIC,
    clm_line_hcpcs_cd VARCHAR(5),
    clm_line_ndc_cd VARCHAR(11),
    clm_line_ndc_qty INT,
    clm_line_ndc_qty_qlfyr_cd VARCHAR(2),
    clm_line_srvc_unit_qty INT,
    clm_line_rev_ctr_cd VARCHAR(4),
    hcpcs_1_mdfr_cd VARCHAR(2),
    hcpcs_2_mdfr_cd VARCHAR(2),
    hcpcs_3_mdfr_cd VARCHAR(2),
    hcpcs_4_mdfr_cd VARCHAR(2),
    hcpcs_5_mdfr_cd VARCHAR(2),
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_line_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_instnl (
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_ansi_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    clm_line_num INT,
    clm_rev_apc_hipps_cd VARCHAR(5),
    clm_ddctbl_coinsrnc_cd VARCHAR(1),
    clm_line_instnl_rate_amt NUMERIC,
    clm_line_instnl_adjstd_amt NUMERIC,
    clm_line_instnl_rdcd_amt NUMERIC,
    clm_line_instnl_msp1_pd_amt NUMERIC,
    clm_line_instnl_msp2_pd_amt NUMERIC,
    clm_line_instnl_rev_ctr_dt DATE,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_line_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_ansi_sgntr (
    clm_ansi_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_1_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_2_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_3_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_4_rev_cntr_ansi_rsn_cd VARCHAR(3)
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene(
    bene_sk, 
    bene_xref_efctv_sk, 
    bene_mbi_id,
    bene_1st_name,
    bene_midl_name,
    bene_last_name,
    bene_brth_dt,
    bene_death_dt,
    bene_vrfy_death_day_sw,
    bene_sex_cd,
    bene_race_cd,
    geo_usps_state_cd,
    geo_zip5_cd,
    geo_zip_plc_name,
    bene_line_1_adr,
    bene_line_2_adr,
    bene_line_3_adr,
    bene_line_4_adr,
    bene_line_5_adr,
    bene_line_6_adr,
    cntct_lang_cd,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts)
VALUES(
    1, -- bene_sk, 
    1, -- bene_xref_efctv_sk, 
    '1S000000000',-- bene_mbi_id,
    'CHUCK',-- bene_1st_name,
    'R',-- bene_midl_name,
    'NORRIS',-- bene_last_name,
    '1960-01-01',-- bene_brth_dt,
    null,--bene_death_dt,
    '~',--bene_vrfy_death_day_sw,
    2,-- bene_sex_cd,
    1,-- bene_race_cd,
    'VA',-- geo_usps_state_cd,
    '22473',-- geo_zip5_cd,
    'RICHMOND',-- geo_zip_plc_name,
    '100 MAIN ST',-- bene_line_1_adr,
    '',-- bene_line_2_adr,
    '',-- bene_line_3_adr,
    '',-- bene_line_4_adr,
    '',-- bene_line_5_adr,
    '',-- bene_line_6_adr,
    'ENG',-- cntct_lang_cd,
    NOW(),-- idr_trans_efctv_ts,
    '2099-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
),
(
    2, -- bene_sk, 
    2, -- bene_xref_efctv_sk, 
    '1S000000001',-- bene_mbi_id,
    'BOB',-- bene_1st_name,
    'J',-- bene_midl_name,
    'SAGET',-- bene_last_name,
    '1960-01-01',-- bene_brth_dt,
    '1990-01-01',-- bene_death_dt,
    'Y',-- bene_vrfy_death_day_sw,
    2,-- bene_sex_cd,
    1,-- bene_race_cd,
    'WA',-- geo_usps_state_cd,
    '98119',-- geo_zip5_cd,
    'SEATTLE',-- geo_zip_plc_name,
    '200 LANE ST',-- bene_line_1_adr,
    '',-- bene_line_2_adr,
    '',-- bene_line_3_adr,
    '',-- bene_line_4_adr,
    '',-- bene_line_5_adr,
    '',-- bene_line_6_adr,
    'ENG',-- cntct_lang_cd,
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry (
    bene_sk,
    bene_xref_efctv_sk,
    bene_mbi_id,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1, -- bene_sk, 
    1, -- bene_xref_efctv_sk, 
    '1S000000000',-- bene_mbi_id,
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id (
    bene_mbi_id,
    bene_mbi_efctv_dt,
    bene_mbi_obslt_dt,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    '1S000000000',-- bene_mbi_id,
    '2024-01-01',-- bene_mbi_efctv_dt
    '9999-12-31',-- bene_mbi_efctv_dt
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp (
    bene_sk,
    bene_buyin_cd,
    bene_tp_type_cd,
    bene_rng_bgn_dt,
    bene_rng_end_dt,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1, --bene_sk,
    'D', --bene_buyin_cd,
    'B', --bene_tp_type_cd,
    '2024-01-01', --bene_rng_bgn_dt
    '2024-12-31',--bene_rng_end_dt
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_stus (
    bene_sk,
    bene_mdcr_stus_cd,
    mdcr_stus_bgn_dt,
    mdcr_stus_end_dt,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1, --bene_sk,
    '10',--bene_mdcr_stus_cd
    '2024-01-01',--mdcr_stus_bgn_dt
    '2024-12-31',--mdcr_stus_end_dt
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt (
    bene_sk,
    bene_rng_bgn_dt,
    bene_rng_end_dt,
    bene_mdcr_entlmt_type_cd,
    bene_mdcr_entlmt_stus_cd,
    bene_mdcr_enrlmt_rsn_cd,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1,--bene_sk,
    '2024-01-01',--mdcr_stus_bgn_dt
    '2024-12-31',--mdcr_stus_end_dt
    'B',--bene_mdcr_entlmt_type_cd
    'Y',--bene_mdcr_entlmt_stus_cd
    'I',--bene_mdcr_enrlmt_rsn_cd,
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt_rsn (
    bene_sk,
    bene_rng_bgn_dt,
    bene_rng_end_dt,
    bene_mdcr_entlmt_rsn_cd,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1,--bene_sk,
    '2024-01-01',--bene_rng_bgn_dt
    '2024-12-31',--bene_rng_end_dt
    '1',--bene_mdcr_entlmt_rsn_cd
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);


INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_elctn_prd_usg (
    bene_sk,
    cntrct_pbp_sk,
    bene_cntrct_num,
    bene_pbp_num,
    bene_elctn_enrlmt_disenrlmt_cd,
    bene_elctn_aplctn_dt,
    bene_enrlmt_efctv_dt,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1, -- bene_sk,
    1, -- cntrct_pbp_sk,
    'S0001', -- bene_cntrct_num,
    '001', -- bene_pbp_num,
    'E', -- bene_elctn_enrlmt_disenrlmt_cd,
    NOW(),--bene_elctn_aplctn_dt DATE,
    NOW(),--bene_enrlmt_efctv_dt DATE,
    NOW(),-- idr_trans_efctv_ts
    '9999-12-31', --idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num
(
    cntrct_pbp_sk,
    cntrct_drug_plan_ind_cd,
    cntrct_pbp_type_cd,
    cntrct_pbp_sk_obslt_dt
)
VALUES (
    1, -- cntrct_pbp_sk,
    '1', -- cntrct_drug_plan_ind_cd,
    '01', --cntrct_pbp_type_cd,
    '9999-12-31' --cntrct_pbp_sk_obslt_dt DATE
);
