DO $$ DECLARE
    r record;
BEGIN
    -- This schema will already exist in a live environment, but
    -- we'll need to create it here manually when running against localhost
    IF NOT EXISTS(
        SELECT schema_name
            FROM information_schema.schemata
            WHERE schema_name = 'cms_vdm_view_mdcr_prd'
    )
    THEN
        CREATE SCHEMA cms_vdm_view_mdcr_prd;
    END IF;

    -- In a live environment, we'll clean out whatever's there and replace it.
    -- The tables in this schema are only meant for staging so there's nothing in here
    -- that needs to persist between loads.
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'cms_vdm_view_mdcr_prd') 
    LOOP
        EXECUTE 'DROP TABLE cms_vdm_view_mdcr_prd.' || quote_ident(r.tablename) || ' CASCADE';
    END LOOP;
END $$;

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry (
    bene_sk BIGINT NOT NULL, 
    bene_xref_sk BIGINT NOT NULL,
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
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id (
    bene_mbi_id VARCHAR(11) NOT NULL,
    bene_mbi_efctv_dt DATE,
    bene_mbi_obslt_dt DATE,
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_mbi_id, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp (
    bene_sk BIGINT NOT NULL,
    bene_buyin_cd VARCHAR(2) NOT NULL,
    bene_tp_type_cd VARCHAR(1) NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_tp_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_stus (
    bene_sk BIGINT NOT NULL,
    bene_mdcr_stus_cd VARCHAR(2) NOT NULL,
    mdcr_stus_bgn_dt DATE NOT NULL,
    mdcr_stus_end_dt DATE NOT NULL,
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
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
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_mdcr_entlmt_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt_rsn (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_rsn_cd VARCHAR(1),
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref (
    bene_hicn_num VARCHAR(11) NOT NULL,
    bene_sk BIGINT NOT NULL,
    bene_xref_sk BIGINT NOT NULL,
    bene_kill_cred_cd VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    src_rec_crte_ts TIMESTAMPTZ NOT NULL,
    src_rec_updt_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_hicn_num, src_rec_crte_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_cmbnd_dual_mdcr (
    bene_sk BIGINT NOT NULL,
    bene_mdcd_elgblty_bgn_dt DATE NOT NULL,
    bene_mdcd_elgblty_end_dt DATE NOT NULL,
    bene_dual_stus_cd VARCHAR(2) NOT NULL,
    bene_dual_type_cd VARCHAR(1) NOT NULL,
    geo_usps_state_cd VARCHAR(2) NOT NULL,
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_mdcd_elgblty_bgn_dt, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num (
    cntrct_pbp_sk BIGINT NOT NULL,
    cntrct_drug_plan_ind_cd VARCHAR(1) NOT NULL,
    cntrct_pbp_type_cd VARCHAR(2) NOT NULL,
    cntrct_pbp_sk_efctv_dt DATE NOT NULL,
    cntrct_pbp_end_dt DATE NOT NULL,
    cntrct_pbp_sk_obslt_dt DATE NOT NULL,
    cntrct_pbp_name VARCHAR(75) NOT NULL,
    cntrct_num VARCHAR(5) NOT NULL,
    cntrct_pbp_num VARCHAR(3) NOT NULL
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm (
    clm_uniq_id BIGINT NOT NULL,
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_type_cd INT,
    clm_num_sk BIGINT,
    bene_sk BIGINT NOT NULL,
    clm_alowd_chrg_amt NUMERIC,
    clm_bene_pmt_amt NUMERIC,
    clm_cntl_num VARCHAR(40),
    clm_prnt_cntl_num VARCHAR(40),
    clm_orig_cntl_num VARCHAR(40),
    clm_from_dt DATE,
    clm_thru_dt DATE,
    clm_efctv_dt DATE,
    clm_obslt_dt DATE,
    clm_pd_dt DATE,
    clm_finl_actn_ind VARCHAR(1),
    clm_bill_clsfctn_cd VARCHAR(1),
    clm_bill_freq_cd VARCHAR(1),
    clm_bill_fac_type_cd VARCHAR(1),
    clm_src_id VARCHAR(5),
    clm_query_cd VARCHAR(1),
    clm_mdcr_coinsrnc_amt NUMERIC,
    clm_blood_lblty_amt NUMERIC,
    clm_ncvrd_chrg_amt NUMERIC,
    clm_mdcr_ddctbl_amt NUMERIC,
    clm_prvdr_pmt_amt NUMERIC,
    clm_cntrctr_num VARCHAR(5),
    clm_pmt_amt NUMERIC,
    clm_ltst_clm_ind VARCHAR(1),
    clm_atndg_prvdr_npi_num VARCHAR(10),
    clm_atndg_prvdr_last_name VARCHAR(60),
    clm_oprtg_prvdr_npi_num VARCHAR(10),
    clm_oprtg_prvdr_last_name VARCHAR(60),
    clm_blg_prvdr_last_name VARCHAR(60),
    clm_rfrg_prvdr_last_name VARCHAR(60),
    clm_prscrbng_prvdr_last_name VARCHAR(60),
    clm_othr_prvdr_npi_num VARCHAR(10),
    clm_othr_prvdr_last_name VARCHAR(60),
    clm_ric_cd VARCHAR(1),
    clm_rndrg_prvdr_npi_num VARCHAR(10),
    clm_rndrg_prvdr_last_name VARCHAR(60),
    prvdr_blg_prvdr_npi_num VARCHAR(10),
    prvdr_prscrbng_prvdr_npi_num VARCHAR(10),
    prvdr_rfrg_prvdr_npi_num VARCHAR(10),
    prvdr_srvc_prvdr_npi_num VARCHAR(10),
    prvdr_othr_prvdr_npi_num VARCHAR(10),
    prvdr_atndg_prvdr_npi_num VARCHAR(10),
    prvdr_rndrng_prvdr_npi_num VARCHAR(10),
    prvdr_oprtg_prvdr_npi_num VARCHAR(10),
    clm_rfrg_prvdr_pin_num VARCHAR(14),
    clm_disp_cd VARCHAR(2),
    clm_sbmt_chrg_amt NUMERIC,
    clm_srvc_prvdr_gnrc_id_num VARCHAR(20),
    clm_blood_pt_frnsh_qty INT,
    clm_nch_prmry_pyr_cd VARCHAR(1),
    clm_blg_prvdr_oscar_num VARCHAR(20),
    clm_idr_ld_dt DATE NOT NULL,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    clm_adjstmt_type_cd VARCHAR(2),
    clm_bene_pd_amt NUMERIC,
    clm_blg_prvdr_zip5_cd VARCHAR(5),
    clm_sbmt_frmt_cd VARCHAR(1),
    clm_sbmtr_cntrct_num VARCHAR(5),
    clm_sbmtr_cntrct_pbp_num VARCHAR(3),
    clm_rlt_cond_sgntr_sk BIGINT,
    clm_blood_chrg_amt NUMERIC,
    clm_tot_cntrctl_amt NUMERIC,
    clm_bene_intrst_pd_amt NUMERIC,
    clm_bene_pmt_coinsrnc_amt NUMERIC,
    clm_cob_ptnt_resp_amt NUMERIC,
    clm_prvdr_otaf_amt NUMERIC,
    clm_othr_tp_pd_amt NUMERIC,
    clm_prvdr_rmng_due_amt NUMERIC,
    clm_blood_ncvrd_chrg_amt NUMERIC,
    clm_prvdr_intrst_pd_amt NUMERIC,
    meta_src_sk NUMERIC,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_rlt_cond_sgntr_mbr (
      clm_rlt_cond_sgntr_sk BIGINT NOT NULL,
      clm_rlt_cond_sgntr_sqnc_num INT NOT NULL,
      clm_rlt_cond_cd VARCHAR(20) NOT NULL,
      idr_insrt_ts TIMESTAMPTZ,
      idr_updt_ts TIMESTAMPTZ,
      PRIMARY KEY(clm_rlt_cond_sgntr_sk, clm_rlt_cond_sgntr_sqnc_num, clm_rlt_cond_cd)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_dcmtn (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_nrln_ric_cd VARCHAR(1),
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    clm_bnft_enhncmt_1_cd VARCHAR(2),
    clm_bnft_enhncmt_2_cd VARCHAR(2),
    clm_bnft_enhncmt_3_cd VARCHAR(2),
    clm_bnft_enhncmt_4_cd VARCHAR(2),
    clm_bnft_enhncmt_5_cd VARCHAR(2),
    clm_ngaco_pbpmt_sw VARCHAR(1),
    clm_ngaco_cptatn_sw VARCHAR(1),
    clm_ngaco_pdschrg_hcbs_sw VARCHAR(1),
    clm_ngaco_snf_wvr_sw VARCHAR(1),
    clm_ngaco_tlhlth_sw VARCHAR(1),
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_fiss (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_crnt_stus_cd VARCHAR(1),
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_instnl (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_admsn_type_cd VARCHAR(2),
    bene_ptnt_stus_cd VARCHAR(2),
    -- SAMHSA (DRG)
    dgns_drg_cd INT,
    clm_mdcr_instnl_mco_pd_sw VARCHAR(1),
    clm_admsn_src_cd VARCHAR(2),
    clm_fi_actn_cd VARCHAR(1),
    clm_mdcr_ip_lrd_use_cnt INT,
    clm_hha_rfrl_cd VARCHAR(1),
    clm_hha_lup_ind_cd VARCHAR(1),
    clm_hipps_uncompd_care_amt NUMERIC,
    clm_instnl_mdcr_coins_day_cnt INT,
    clm_instnl_ncvrd_day_cnt NUMERIC,
    clm_instnl_per_diem_amt NUMERIC,
    clm_mdcr_npmt_rsn_cd VARCHAR(2),
    clm_mdcr_hha_tot_visit_cnt NUMERIC,
    clm_mdcr_ip_pps_drg_wt_num NUMERIC,
    clm_mdcr_ip_pps_dsprprtnt_amt NUMERIC,
    clm_mdcr_ip_pps_excptn_amt NUMERIC,
    clm_mdcr_ip_pps_cptl_fsp_amt NUMERIC,
    clm_mdcr_ip_pps_cptl_ime_amt NUMERIC,
    clm_mdcr_ip_pps_outlier_amt NUMERIC,
    clm_mdcr_ip_pps_cptl_hrmls_amt NUMERIC,
    clm_mdcr_instnl_bene_pd_amt NUMERIC,
    clm_mdcr_hospc_prd_cnt INT,
    clm_pps_ind_cd VARCHAR(1),
    clm_mdcr_ip_pps_cptl_tot_amt NUMERIC,
    clm_instnl_cvrd_day_cnt NUMERIC,
    clm_mdcr_instnl_prmry_pyr_amt NUMERIC,
    clm_instnl_prfnl_amt NUMERIC,
    clm_mdcr_ip_bene_ddctbl_amt NUMERIC,
    clm_instnl_drg_outlier_amt NUMERIC,
    dgns_drg_outlier_cd VARCHAR(1),
    clm_mdcr_ip_scnd_yr_rate_amt NUMERIC,
    clm_instnl_low_vol_pmt_amt NUMERIC,
    clm_hipps_readmsn_rdctn_amt NUMERIC,
    clm_hipps_model_bndld_pmt_amt NUMERIC,
    clm_hipps_vbp_amt NUMERIC,
    clm_site_ntrl_ip_pps_pymt_amt NUMERIC,
    clm_finl_stdzd_pymt_amt NUMERIC,
    clm_pps_md_wvr_stdzd_val_amt NUMERIC,
    clm_hac_rdctn_pymt_amt NUMERIC,
    clm_mdcr_ip_1st_yr_rate_amt NUMERIC,
    clm_site_ntrl_cst_bsd_pymt_amt NUMERIC,
    clm_ss_outlier_std_pymt_amt NUMERIC,
    clm_op_srvc_type_cd VARCHAR(1),
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
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
    clm_mdcr_exhstd_dt DATE,
    clm_nch_wkly_proc_dt DATE,
    clm_qlfy_stay_from_dt DATE,
    clm_qlfy_stay_thru_dt DATE,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_val (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_val_sqnc_num INT,
    clm_val_cd VARCHAR(2),
    clm_val_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_val_sqnc_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_prod (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_val_sqnc_num INT NOT NULL,
    -- SAMHSA (ICD)
    clm_prcdr_cd VARCHAR(7),
    clm_dgns_prcdr_icd_ind VARCHAR(1),
    -- SAMHSA (ICD)
    clm_dgns_cd VARCHAR(7),
    clm_prod_type_cd VARCHAR(1),
    clm_poa_ind VARCHAR(1),
    clm_prcdr_prfrm_dt DATE,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_prod_type_cd, clm_val_sqnc_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_prfnl (
    clm_carr_pmt_dnl_cd VARCHAR(2),
    clm_clncl_tril_num VARCHAR(8),
    clm_mdcr_prfnl_prmry_pyr_amt NUMERIC,
    clm_mdcr_prfnl_prvdr_asgnmt_sw VARCHAR(2),
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_prvdr_acnt_rcvbl_ofst_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_num_sk, clm_type_cd)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line (
    clm_uniq_id BIGINT NOT NULL,
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_line_ansthsa_unit_cnt NUMERIC,
    clm_line_num INT NOT NULL,
    clm_from_dt DATE NOT NULL,
    clm_line_sbmt_chrg_amt NUMERIC,
    clm_line_alowd_chrg_amt NUMERIC,
    clm_line_mdcr_coinsrnc_amt NUMERIC,
    clm_line_ncvrd_chrg_amt NUMERIC,
    clm_line_prvdr_pmt_amt NUMERIC,
    clm_line_bene_pmt_amt NUMERIC,
    clm_line_bene_pd_amt NUMERIC,
    clm_line_cvrd_pd_amt NUMERIC,
    clm_line_blood_ddctbl_amt NUMERIC,
    -- Note: this is redundant with the value in V2_MDCR_CLM_PROD, it's just used to match the two claim lines
    clm_line_dgns_cd VARCHAR(7),
    clm_line_from_dt DATE,
    clm_line_mdcr_ddctbl_amt NUMERIC,
    clm_line_rx_num VARCHAR(30),
    clm_line_thru_dt DATE,
    clm_rndrg_prvdr_prtcptg_cd VARCHAR(1),
    clm_rndrg_prvdr_tax_num VARCHAR(10),
    clm_rndrg_prvdr_npi_num VARCHAR(10),
    prvdr_rndrng_prvdr_npi_num VARCHAR(10),
    clm_pos_cd VARCHAR(2),
    -- SAMHSA (HCPCS/CPT)
    clm_line_hcpcs_cd VARCHAR(5),
    clm_line_ndc_cd VARCHAR(11),
    clm_line_ndc_qty NUMERIC,
    clm_line_ndc_qty_qlfyr_cd VARCHAR(2),
    clm_line_srvc_unit_qty NUMERIC,
    clm_line_rev_ctr_cd VARCHAR(4),
    hcpcs_1_mdfr_cd VARCHAR(2),
    hcpcs_2_mdfr_cd VARCHAR(2),
    hcpcs_3_mdfr_cd VARCHAR(2),
    hcpcs_4_mdfr_cd VARCHAR(2),
    hcpcs_5_mdfr_cd VARCHAR(2),
    clm_rndrg_prvdr_type_cd VARCHAR(3),
    clm_line_pmd_uniq_trkng_num VARCHAR(14),
    clm_line_othr_tp_pd_amt NUMERIC,
    clm_line_ncvrd_pd_amt NUMERIC,
    clm_line_otaf_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_line_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_instnl (
    geo_bene_sk BIGINT,
    clm_dt_sgntr_sk BIGINT,
    clm_ansi_sgntr_sk BIGINT,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_otaf_one_ind_cd VARCHAR(1),
    clm_rev_apc_hipps_cd VARCHAR(5),
    clm_rev_dscnt_ind_cd VARCHAR(1),
    clm_rev_packg_ind_cd VARCHAR(1),
    clm_rev_cntr_stus_cd VARCHAR(2),
    clm_rev_pmt_mthd_cd VARCHAR(2),
    clm_ddctbl_coinsrnc_cd VARCHAR(1),
    clm_line_instnl_rate_amt NUMERIC,
    clm_line_instnl_adjstd_amt NUMERIC,
    clm_line_instnl_rdcd_amt NUMERIC,
    clm_line_instnl_msp1_pd_amt NUMERIC,
    clm_line_instnl_msp2_pd_amt NUMERIC,
    clm_line_instnl_rev_ctr_dt DATE,
    clm_rev_cntr_tdapa_amt NUMERIC,
    clm_line_non_ehr_rdctn_amt NUMERIC,
    clm_line_add_on_pymt_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_line_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_prfnl (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_bene_prmry_pyr_pd_amt NUMERIC,
    clm_fed_type_srvc_cd VARCHAR(1),
    clm_line_carr_clncl_lab_num VARCHAR(10),
    clm_line_carr_hpsa_scrcty_cd VARCHAR(1),
    clm_line_dmerc_scrn_svgs_amt NUMERIC,
    clm_line_hct_hgb_rslt_num NUMERIC,
    clm_line_hct_hgb_type_cd VARCHAR(2),
    clm_line_prfnl_dme_price_amt NUMERIC,
    clm_line_prfnl_mtus_cnt NUMERIC,
    clm_mtus_ind_cd VARCHAR(1),
    clm_physn_astnt_cd VARCHAR(1),
    clm_pmt_80_100_cd VARCHAR(1),
    clm_prcng_lclty_cd VARCHAR(2),
    clm_prcsg_ind_cd VARCHAR(2),
    clm_prmry_pyr_cd VARCHAR(1),
    clm_prvdr_spclty_cd VARCHAR(2),
    clm_srvc_ddctbl_sw VARCHAR(1),
    clm_suplr_type_cd VARCHAR(1),
    clm_line_prfnl_intrst_amt NUMERIC,
    clm_line_carr_psych_ot_lmt_amt NUMERIC,
    clm_line_carr_clncl_chrg_amt NUMERIC,
    clm_mdcr_prmry_pyr_alowd_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY (geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_line_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_ansi_sgntr (
    clm_ansi_sgntr_sk BIGINT NOT NULL PRIMARY KEY,
    clm_1_rev_cntr_ansi_grp_cd VARCHAR(2),
    clm_2_rev_cntr_ansi_grp_cd VARCHAR(2),
    clm_3_rev_cntr_ansi_grp_cd VARCHAR(2),
    clm_4_rev_cntr_ansi_grp_cd VARCHAR(2),
    clm_1_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_2_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_3_rev_cntr_ansi_rsn_cd VARCHAR(3),
    clm_4_rev_cntr_ansi_rsn_cd VARCHAR(3),
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_lctn_hstry (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_audt_trl_stus_cd VARCHAR(2),
    clm_lctn_cd_sqnc_num BIGINT NOT NULL,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_lctn_cd_sqnc_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_rx (
    clm_uniq_id BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_brnd_gnrc_cd VARCHAR(1) NOT NULL,
    clm_cmpnd_cd VARCHAR(1) NOT NULL,
    clm_ctstrphc_cvrg_ind_cd VARCHAR(1),
    clm_daw_prod_slctn_cd VARCHAR(1),
    clm_drug_cvrg_stus_cd VARCHAR(2),
    clm_dspnsng_stus_cd VARCHAR(1),
    clm_line_authrzd_fill_num VARCHAR(2),
    clm_line_days_suply_qty INT,
    clm_line_grs_above_thrshld_amt NUMERIC,
    clm_line_grs_blw_thrshld_amt NUMERIC,
    clm_line_ingrdnt_cst_amt NUMERIC,
    clm_line_lis_amt NUMERIC,
    clm_line_plro_amt NUMERIC,
    clm_line_rx_fill_num INT,
    clm_line_rx_orgn_cd VARCHAR(1),
    clm_line_sls_tax_amt NUMERIC,
    clm_line_srvc_cst_amt NUMERIC,
    clm_line_troop_tot_amt NUMERIC,
    clm_line_vccn_admin_fee_amt NUMERIC,
    clm_ltc_dspnsng_mthd_cd VARCHAR(3),
    clm_phrmcy_srvc_type_cd VARCHAR(2),
    clm_prcng_excptn_cd VARCHAR(1),
    clm_ptnt_rsdnc_cd VARCHAR(2),
    clm_rptd_mftr_dscnt_amt NUMERIC,
    clm_line_rebt_passthru_pos_amt NUMERIC,
    clm_cms_calcd_mftr_dscnt_amt NUMERIC,
    clm_line_grs_cvrd_cst_tot_amt NUMERIC,
    clm_phrmcy_price_dscnt_at_pos_amt NUMERIC,
    clm_line_rptd_gap_dscnt_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(clm_uniq_id, clm_line_num)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_dcmtn (
    geo_bene_sk BIGINT NOT NULL,
    clm_dt_sgntr_sk BIGINT NOT NULL,
    clm_type_cd INT NOT NULL,
    clm_num_sk BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_line_bnft_enhncmt_1_cd VARCHAR(2),
    clm_line_bnft_enhncmt_2_cd VARCHAR(2),
    clm_line_bnft_enhncmt_3_cd VARCHAR(2),
    clm_line_bnft_enhncmt_4_cd VARCHAR(2),
    clm_line_bnft_enhncmt_5_cd VARCHAR(2),
    clm_line_ngaco_cptatn_sw VARCHAR(1),
    clm_line_ngaco_pdschrg_hcbs_sw VARCHAR(1),
    clm_line_ngaco_snf_wvr_sw VARCHAR(1),
    clm_line_ngaco_tlhlth_sw VARCHAR(1),
    clm_line_aco_care_mgmt_hcbs_sw VARCHAR(1),
    clm_line_ngaco_pbpmt_sw VARCHAR(1),
    clm_line_pa_uniq_trkng_num VARCHAR(50),
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(geo_bene_sk, clm_dt_sgntr_sk, clm_type_cd, clm_num_sk, clm_line_num)
);


CREATE INDEX
	ON cms_vdm_view_mdcr_prd.v2_mdcr_clm (clm_from_dt);

CREATE INDEX
    ON cms_vdm_view_mdcr_prd.v2_mdcr_clm(idr_insrt_ts, idr_updt_ts, clm_idr_ld_dt);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry (
    prvdr_npi_num VARCHAR(10) PRIMARY KEY,
    prvdr_sk BIGINT NOT NULL,
    prvdr_hstry_efctv_dt DATE NOT NULL,
    prvdr_mdl_name VARCHAR(25),
    prvdr_type_cd VARCHAR(2) NOT NULL,
    prvdr_txnmy_cmpst_cd VARCHAR(150),
    prvdr_oscar_num VARCHAR(13) NOT NULL,
    prvdr_1st_name VARCHAR(35),
    prvdr_name VARCHAR(70),
    prvdr_hstry_obslt_dt DATE NOT NULL,
    prvdr_lgl_name VARCHAR(100),
    prvdr_emplr_id_num VARCHAR(10),
    prvdr_last_name VARCHAR(35),
    meta_sk BIGINT NOT NULL,
    meta_lst_updt_sk BIGINT NOT NULL
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_cntct (
    cntrct_pbp_sk BIGINT NOT NULL,
    cntrct_plan_cntct_obslt_dt DATE NOT NULL,
    cntrct_plan_cntct_type_cd VARCHAR(3),
    cntrct_plan_free_extnsn_num VARCHAR(7),
    cntrct_plan_cntct_free_num VARCHAR(10),
    cntrct_plan_cntct_extnsn_num VARCHAR(7),
    cntrct_plan_cntct_tel_num VARCHAR(10),
    cntrct_pbp_end_dt DATE NOT NULL,
    cntrct_pbp_bgn_dt DATE NOT NULL,
    cntrct_plan_cntct_st_1_adr VARCHAR(55),
    cntrct_plan_cntct_st_2_adr VARCHAR(55),
    cntrct_plan_cntct_city_name VARCHAR(30),
    cntrct_plan_cntct_state_cd VARCHAR(2),
    cntrct_plan_cntct_zip_cd VARCHAR(9)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mapd_enrlmt(
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5) NOT NULL,
    bene_pbp_num VARCHAR(3) NOT NULL,
    bene_enrlmt_bgn_dt DATE NOT NULL,
    bene_enrlmt_end_dt DATE,
    bene_cvrg_type_cd VARCHAR(2) NOT NULL,
    bene_enrlmt_pgm_type_cd VARCHAR(4) NOT NULL,
    bene_enrlmt_emplr_sbsdy_sw VARCHAR(1),
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_enrlmt_bgn_dt, bene_enrlmt_pgm_type_cd)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_mapd_enrlmt_rx(
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5) NOT NULL,
    bene_pbp_num VARCHAR(3) NOT NULL,
    bene_enrlmt_bgn_dt DATE NOT NULL,
    bene_pdp_enrlmt_mmbr_id_num VARCHAR(20) NOT NULL,
    bene_pdp_enrlmt_grp_num VARCHAR(15) NOT NULL,
    bene_pdp_enrlmt_prcsr_num VARCHAR(10) NOT NULL,
    bene_pdp_enrlmt_bank_id_num VARCHAR(6),
    bene_enrlmt_pdp_rx_info_bgn_dt DATE NOT NULL,
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_enrlmt_pdp_rx_info_bgn_dt)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_lis(
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_lis_copmt_lvl_cd VARCHAR(1) NOT NULL,
    bene_lis_ptd_prm_pct VARCHAR(3),
    idr_ltst_trans_flg VARCHAR(1) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_insrt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_sgmt(
    cntrct_pbp_sk BIGINT NOT NULL,
    cntrct_pbp_sgmt_num VARCHAR(3) NOT NULL,
    PRIMARY KEY(cntrct_pbp_sk, cntrct_pbp_sgmt_num)
);
