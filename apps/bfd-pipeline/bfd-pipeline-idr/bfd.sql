DROP SCHEMA IF EXISTS idr CASCADE;

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
    bene_sex_cd VARCHAR(1) NOT NULL,
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
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE INDEX ON idr.beneficiary(bene_mbi_id);

CREATE TABLE idr.beneficiary_history(
    bene_sk BIGINT NOT NULL,
    bene_xref_efctv_sk BIGINT NOT NULL,
    bene_xref_efctv_sk_computed BIGINT NOT NULL GENERATED ALWAYS
        AS (CASE WHEN bene_xref_efctv_sk = 0 THEN bene_sk ELSE bene_xref_efctv_sk END) STORED,
    bene_mbi_id VARCHAR(11) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
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
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
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
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
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
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
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
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
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
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_election_period_usage (
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5) NOT NULL,
    bene_pbp_num VARCHAR(3) NOT NULL,
    bene_elctn_enrlmt_disenrlmt_cd VARCHAR(1) NOT NULL,
    bene_elctn_aplctn_dt DATE NOT NULL,
    bene_enrlmt_efctv_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, cntrct_pbp_sk, bene_enrlmt_efctv_dt)
);

CREATE TABLE idr.beneficiary_xref (
    bene_sk BIGINT NOT NULL,
    bene_xref_sk BIGINT NOT NULL,
    bene_hicn_num VARCHAR(11) NOT NULL,
    bene_kill_cred_cd VARCHAR(1) NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    src_rec_ctre_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_hicn_num, src_rec_ctre_ts)
);

CREATE TABLE idr.contract_pbp_number (
    cntrct_pbp_sk BIGINT NOT NULL PRIMARY KEY,
    cntrct_drug_plan_ind_cd VARCHAR(1) NOT NULL,
    cntrct_pbp_type_cd VARCHAR(2) NOT NULL,
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
    clm_cntl_num VARCHAR(40) NOT NULL,
    clm_orig_cntl_num VARCHAR(40) NOT NULL,
    clm_from_dt DATE NOT NULL,
    clm_thru_dt DATE NOT NULL,
    clm_efctv_dt DATE NOT NULL,
    clm_obslt_dt DATE NOT NULL,
    clm_bill_clsfctn_cd VARCHAR(1) NOT NULL,
    clm_bill_fac_type_cd VARCHAR(1) NOT NULL,
    clm_bill_freq_cd VARCHAR(1) NOT NULL,
    clm_finl_actn_ind VARCHAR(1) NOT NULL,
    clm_src_id VARCHAR(5) NOT NULL,
    clm_query_cd VARCHAR(1) NOT NULL,
    clm_mdcr_coinsrnc_amt NUMERIC NOT NULL,
    clm_blood_lblty_amt NUMERIC NOT NULL,
    clm_ncvrd_chrg_amt NUMERIC NOT NULL,
    clm_mdcr_ddctbl_amt NUMERIC NOT NULL,
    clm_prvdr_pmt_amt NUMERIC NOT NULL,
    clm_cntrctr_num VARCHAR(5) NOT NULL,
    clm_pmt_amt NUMERIC NOT NULL,
    clm_ltst_clm_ind VARCHAR(1) NOT NULL,
    clm_atndg_prvdr_npi_num VARCHAR(10) NOT NULL,
    clm_atndg_prvdr_last_name VARCHAR(60) NOT NULL,
    clm_oprtg_prvdr_npi_num VARCHAR(10) NOT NULL,
    clm_oprtg_prvdr_last_name VARCHAR(60) NOT NULL,
    clm_othr_prvdr_npi_num VARCHAR(10) NOT NULL,
    clm_othr_prvdr_last_name VARCHAR(60) NOT NULL,
    clm_rndrg_prvdr_npi_num VARCHAR(10) NOT NULL,
    clm_rndrg_prvdr_last_name VARCHAR(60) NOT NULL,
    prvdr_blg_prvdr_npi_num VARCHAR(10) NOT NULL,
    clm_disp_cd VARCHAR(2) NOT NULL,
    clm_sbmt_chrg_amt NUMERIC NOT NULL,
    clm_blood_pt_frnsh_qty INT NOT NULL,
    clm_nch_prmry_pyr_cd VARCHAR(1) NOT NULL,
    clm_blg_prvdr_oscar_num VARCHAR(20) NOT NULL,
    clm_idr_ld_dt DATE NOT NULL,
    clm_nrln_ric_cd VARCHAR(1) NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE INDEX on idr.claim(bene_sk);

CREATE TABLE idr.claim_date_signature (
    clm_dt_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_cms_proc_dt DATE NOT NULL,
    clm_actv_care_from_dt DATE NOT NULL,
    clm_dschrg_dt DATE NOT NULL,
    clm_submsn_dt DATE NOT NULL,
    clm_ncvrd_from_dt DATE NOT NULL,
    clm_ncvrd_thru_dt DATE NOT NULL,
    clm_actv_care_thru_dt DATE NOT NULL,
    clm_mdcr_exhstd_dt DATE NOT NULL,
    clm_nch_wkly_proc_dt DATE NOT NULL,
    clm_qlfy_stay_from_dt DATE NOT NULL,
    clm_qlfy_stay_thru_dt DATE NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.claim_institutional (
    clm_uniq_id BIGINT NOT NULL PRIMARY KEY,
    clm_admsn_type_cd VARCHAR(2) NOT NULL,
    bene_ptnt_stus_cd VARCHAR(2) NOT NULL,
    dgns_drg_cd INT NOT NULL,
    clm_mdcr_instnl_mco_pd_sw VARCHAR(1) NOT NULL,
    clm_admsn_src_cd VARCHAR(2) NOT NULL,
    clm_fi_actn_cd VARCHAR(1) NOT NULL,
    clm_mdcr_ip_lrd_use_cnt INT NOT NULL,
    clm_hha_rfrl_cd VARCHAR(1) NOT NULL,
    clm_hha_lup_ind_cd VARCHAR(1) NOT NULL,
    clm_hipps_uncompd_care_amt NUMERIC NOT NULL,
    clm_instnl_mdcr_coins_day_cnt INT NOT NULL,
    clm_instnl_ncvrd_day_cnt NUMERIC NOT NULL,
    clm_instnl_per_diem_amt NUMERIC NOT NULL,
    clm_mdcr_hha_tot_visit_cnt NUMERIC NOT NULL,
    clm_mdcr_npmt_rsn_cd VARCHAR(2) NOT NULL,
    clm_mdcr_ip_pps_drg_wt_num NUMERIC NOT NULL,
    clm_mdcr_ip_pps_dsprprtnt_amt NUMERIC NOT NULL,
    clm_mdcr_ip_pps_excptn_amt NUMERIC NOT NULL,
    clm_mdcr_ip_pps_cptl_fsp_amt NUMERIC NOT NULL,
    clm_mdcr_ip_pps_cptl_ime_amt NUMERIC NOT NULL,
    clm_mdcr_ip_pps_outlier_amt NUMERIC NOT NULL,
    clm_mdcr_ip_pps_cptl_hrmls_amt NUMERIC NOT NULL,
    clm_mdcr_instnl_bene_pd_amt NUMERIC NOT NULL,
    clm_mdcr_hospc_prd_cnt INT NOT NULL,
    clm_pps_ind_cd VARCHAR(1) NOT NULL,
    clm_mdcr_ip_pps_cptl_tot_amt NUMERIC NOT NULL,
    clm_instnl_cvrd_day_cnt NUMERIC NOT NULL,
    clm_mdcr_instnl_prmry_pyr_amt NUMERIC NOT NULL,
    clm_instnl_prfnl_amt NUMERIC NOT NULL,
    clm_mdcr_ip_bene_ddctbl_amt NUMERIC NOT NULL,
    clm_instnl_drg_outlier_amt NUMERIC NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.claim_value (
    clm_uniq_id BIGINT NOT NULL,
    clm_val_sqnc_num INT NOT NULL,
    clm_val_cd VARCHAR(2) NOT NULL,
    clm_val_amt NUMERIC NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_val_sqnc_num)
);

CREATE TABLE idr.claim_procedure (
    clm_uniq_id BIGINT NOT NULL,
    clm_val_sqnc_num INT NOT NULL,
    clm_dgns_prcdr_icd_ind VARCHAR(1) NOT NULL,
    clm_dgns_cd VARCHAR(7) NOT NULL,
    clm_prcdr_cd VARCHAR(7) NOT NULL,
    clm_prod_type_cd VARCHAR(1) NOT NULL,
    clm_poa_ind VARCHAR(1) NOT NULL,
    clm_prcdr_prfrm_dt DATE NOT NULL,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    bfd_row_num INT NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_prod_type_cd, clm_val_sqnc_num)
);

CREATE TABLE idr.claim_line (
    clm_uniq_id BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_line_sbmt_chrg_amt NUMERIC NOT NULL,
    clm_line_alowd_chrg_amt NUMERIC NOT NULL,
    clm_line_ncvrd_chrg_amt NUMERIC NOT NULL,
    clm_line_prvdr_pmt_amt NUMERIC NOT NULL,
    clm_line_bene_pmt_amt NUMERIC NOT NULL,
    clm_line_bene_pd_amt NUMERIC NOT NULL,
    clm_line_cvrd_pd_amt NUMERIC NOT NULL,
    clm_line_blood_ddctbl_amt NUMERIC NOT NULL,
    clm_line_mdcr_ddctbl_amt NUMERIC NOT NULL,
    clm_line_hcpcs_cd VARCHAR(5) NOT NULL,
    clm_line_ndc_cd VARCHAR(11) NOT NULL,
    clm_line_ndc_qty NUMERIC NOT NULL,
    clm_line_ndc_qty_qlfyr_cd VARCHAR(2) NOT NULL,
    clm_line_srvc_unit_qty NUMERIC NOT NULL,
    clm_line_rev_ctr_cd VARCHAR(4) NOT NULL,
    hcpcs_1_mdfr_cd VARCHAR(2) NOT NULL,
    hcpcs_2_mdfr_cd VARCHAR(2) NOT NULL,
    hcpcs_3_mdfr_cd VARCHAR(2) NOT NULL,
    hcpcs_4_mdfr_cd VARCHAR(2) NOT NULL,
    hcpcs_5_mdfr_cd VARCHAR(2) NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_line_num)
);

CREATE TABLE idr.claim_line_institutional (
    clm_uniq_id BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_ansi_sgntr_sk BIGINT NOT NULL,
    clm_rev_apc_hipps_cd VARCHAR(5) NOT NULL,
    clm_otaf_one_ind_cd VARCHAR(1) NOT NULL,
    clm_rev_dscnt_ind_cd VARCHAR(1) NOT NULL,
    clm_rev_packg_ind_cd VARCHAR(1) NOT NULL,
    clm_rev_cntr_stus_cd VARCHAR(2) NOT NULL,
    clm_rev_pmt_mthd_cd VARCHAR(2) NOT NULL,
    clm_ddctbl_coinsrnc_cd VARCHAR(1) NOT NULL,
    clm_line_instnl_rate_amt NUMERIC NOT NULL,
    clm_line_instnl_adjstd_amt NUMERIC NOT NULL,
    clm_line_instnl_rdcd_amt NUMERIC NOT NULL,
    clm_line_instnl_msp1_pd_amt NUMERIC NOT NULL,
    clm_line_instnl_msp2_pd_amt NUMERIC NOT NULL,
    clm_line_instnl_rev_ctr_dt DATE NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(clm_uniq_id, clm_line_num)
);

CREATE TABLE idr.claim_ansi_signature (
    clm_ansi_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_1_rev_cntr_ansi_rsn_cd VARCHAR(3) NOT NULL,
    clm_2_rev_cntr_ansi_rsn_cd VARCHAR(3) NOT NULL,
    clm_3_rev_cntr_ansi_rsn_cd VARCHAR(3) NOT NULL,
    clm_4_rev_cntr_ansi_rsn_cd VARCHAR(3) NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE MATERIALIZED VIEW idr.overshare_mbis AS 
SELECT bene_mbi_id FROM idr.beneficiary
WHERE bene_xref_efctv_sk != 0
GROUP BY bene_mbi_id
HAVING COUNT(DISTINCT bene_xref_efctv_sk) > 1;

-- required to refresh view with CONCURRENTLY
CREATE UNIQUE INDEX ON idr.overshare_mbis (bene_mbi_id);
