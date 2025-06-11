DROP TABLE IF EXISTS idr.beneficiary;
DROP TABLE IF EXISTS idr.beneficiary_history;
DROP TABLE IF EXISTS idr.load_progress;
DROP SCHEMA IF EXISTS idr;

CREATE SCHEMA idr;
CREATE TABLE idr.beneficiary(
    bene_sk BIGINT NOT NULL PRIMARY KEY, 
    bene_xref_efctv_sk BIGINT NOT NULL, 
    bene_xref_efctv_sk_computed BIGINT NOT NULL GENERATED ALWAYS 
        AS (CASE WHEN bene_xref_efctv_sk = 0 THEN bene_sk ELSE bene_xref_efctv_sk END) STORED,
    bene_mbi_id VARCHAR(11) NOT NULL,
    bene_1st_name VARCHAR(30) NOT NULL,
    bene_midl_name VARCHAR(15) NOT NULL,
    bene_last_name VARCHAR(40) NOT NULL,
    bene_brth_dt DATE NOT NULL,
    bene_death_dt DATE NOT NULL,
    bene_vrfy_death_day_sw VARCHAR(1) NOT NULL,
    bene_sex_cd VARCHAR(1) NOT NULL ,
    bene_race_cd VARCHAR(2) NOT NULL,
    geo_usps_state_cd VARCHAR(2) NOT NULL,
    geo_zip5_cd VARCHAR(5) NOT NULL,
    geo_zip_plc_name VARCHAR(100) NOT NULL,
    bene_line_1_adr VARCHAR(45) NOT NULL,
    bene_line_2_adr VARCHAR(45) NOT NULL,
    bene_line_3_adr VARCHAR(40) NOT NULL,
    bene_line_4_adr VARCHAR(40) NOT NULL,
    bene_line_5_adr VARCHAR(40) NOT NULL,
    bene_line_6_adr VARCHAR(40) NOT NULL,
    cntct_lang_cd VARCHAR(3) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.beneficiary_history(
    bene_sk BIGINT NOT NULL,
    bene_xref_efctv_sk BIGINT NOT NULL,
    bene_xref_efctv_sk_computed BIGINT NOT NULL GENERATED ALWAYS
        AS (CASE WHEN bene_xref_efctv_sk = 0 THEN bene_sk ELSE bene_xref_efctv_sk END) STORED,
    bene_mbi_id VARCHAR(11) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_mbi_id (
    bene_mbi_id VARCHAR(11) NOT NULL,
    bene_mbi_efctv_dt DATE NOT NULL,
    bene_mbi_obslt_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_mbi_id, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_third_party (
    bene_sk BIGINT NOT NULL,
    bene_buyin_cd VARCHAR(2) NOT NULL,
    bene_tp_type_cd VARCHAR(1) NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_tp_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_status (
    bene_sk BIGINT NOT NULL,
    bene_mdcr_stus_cd VARCHAR(2) NOT NULL,
    mdcr_stus_bgn_dt DATE NOT NULL,
    mdcr_stus_end_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, mdcr_stus_bgn_dt, mdcr_stus_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_entitlement (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_type_cd VARCHAR(1),
    bene_mdcr_entlmt_stus_cd VARCHAR(1),
    bene_mdcr_enrlmt_rsn_cd VARCHAR(1),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_mdcr_entlmt_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_entitlement_reason (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_rsn_cd VARCHAR(1),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_election_period_usage (
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5),
    bene_pbp_num VARCHAR(3),
    bene_elctn_enrlmt_disenrlmt_cd VARCHAR(1),
    bene_elctn_aplctn_dt DATE,
    bene_enrlmt_efctv_dt DATE,
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, cntrct_pbp_sk, bene_enrlmt_efctv_dt)
);

CREATE TABLE idr.contract_pbp_number (
    cntrct_pbp_sk BIGINT NOT NULL PRIMARY KEY,
    cntrct_drug_plan_ind_cd VARCHAR(1),
    cntrct_pbp_type_cd VARCHAR(2),
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.load_progress(
    id INT GENERATED ALWAYS AS IDENTITY,
    table_name TEXT NOT NULL UNIQUE,
    last_ts TIMESTAMPTZ NOT NULL,
    batch_completion_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.claim (
    clm_uniq_id BIGINT NOT NULL PRIMARY KEY,
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    bene_sk BIGINT NOT NULL,
    clm_cntl_num VARCHAR(40),
    clm_orig_cntl_num VARCHAR(40),
    clm_from_dt DATE,
    clm_thru_dt DATE,
    clm_efctv_dt DATE,
    clm_obslt_dt DATE,
    clm_bill_clsfctn_cd VARCHAR(1),
    clm_bill_fac_type_cd VARCHAR(1),
    clm_bill_freq_cd VARCHAR(1),
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
    clm_idr_ld_dt DATE,
    clm_nrln_ric_cd VARCHAR(1),
    bfd_created_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.claim_date_signature (
    clm_dt_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_cms_proc_dt DATE,
    clm_actv_care_from_dt DATE,
    clm_dschrg_dt DATE,
    clm_submsn_dt DATE,
    clm_ncvrd_from_dt DATE,
    clm_ncvrd_thru_dt DATE,
    clm_actv_care_thru_dt DATE,
    clm_mdcr_exhstd_dt DATE,
    clm_nch_wkly_proc_dt DATE,
    bfd_created_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.claim_institutional (
    clm_uniq_id BIGINT NOT NULL PRIMARY KEY,
    clm_admsn_type_cd VARCHAR(2),
    bene_ptnt_stus_cd VARCHAR(2),
    dgns_drg_cd INT,
    clm_mdcr_instnl_mco_pd_sw VARCHAR(1),
    clm_admsn_src_cd VARCHAR(2),
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
    bfd_created_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.claim_value (
    clm_uniq_id BIGINT NOT NULL,
    clm_val_sqnc_num INT NOT NULL,
    clm_val_cd VARCHAR(2),
    clm_val_amt NUMERIC,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_val_sqnc_num)
);

CREATE TABLE idr.claim_procedure (
    clm_uniq_id BIGINT NOT NULL,
    clm_val_sqnc_num INT NOT NULL,
    clm_dgns_prcdr_icd_ind VARCHAR(1),
    clm_dgns_cd VARCHAR(7),
    clm_prcdr_cd VARCHAR(7),
    clm_prod_type_cd VARCHAR(1),
    clm_poa_ind VARCHAR(1),
    clm_prcdr_prfrm_dt DATE,
    bfd_row_num INT NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_prod_type_cd, clm_val_sqnc_num)
);

CREATE TABLE idr.claim_line (
    clm_uniq_id BIGINT,
    clm_line_num INT NOT NULL,
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
    clm_line_ndc_qty NUMERIC,
    clm_line_ndc_qty_qlfyr_cd VARCHAR(2),
    clm_line_srvc_unit_qty REAL,
    clm_line_rev_ctr_cd VARCHAR(4),
    hcpcs_1_mdfr_cd VARCHAR(2),
    hcpcs_2_mdfr_cd VARCHAR(2),
    hcpcs_3_mdfr_cd VARCHAR(2),
    hcpcs_4_mdfr_cd VARCHAR(2),
    hcpcs_5_mdfr_cd VARCHAR(2),
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_line_num)
);

CREATE TABLE idr.claim_line_institutional (
    clm_uniq_id BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_ansi_sgntr_sk BIGINT,
    clm_rev_apc_hipps_cd VARCHAR(5),
    clm_ddctbl_coinsrnc_cd VARCHAR(1),
    clm_line_instnl_rate_amt NUMERIC,
    clm_line_instnl_adjstd_amt NUMERIC,
    clm_line_instnl_rdcd_amt NUMERIC,
    clm_line_instnl_msp1_pd_amt NUMERIC,
    clm_line_instnl_msp2_pd_amt NUMERIC,
    clm_line_instnl_rev_ctr_dt DATE,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_line_num)
);

CREATE TABLE idr.claim_ansi_signature (
    clm_ansi_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_1_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_2_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_3_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_4_rev_cntr_ansi_rsn_cd VARCHAR(3),
    bfd_created_ts TIMESTAMPTZ NOT NULL
);

CREATE MATERIALIZED VIEW idr.overshare_mbis AS 
SELECT bene_mbi_id FROM idr.beneficiary
GROUP BY bene_mbi_id
HAVING COUNT(DISTINCT bene_xref_efctv_sk) > 1;

-- required to refresh view with CONCURRENTLY
CREATE UNIQUE INDEX ON idr.overshare_mbis (bene_mbi_id);
