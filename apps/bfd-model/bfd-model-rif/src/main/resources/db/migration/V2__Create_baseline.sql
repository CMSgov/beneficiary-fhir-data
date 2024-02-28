

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.beneficiaries (
    bene_id bigint NOT NULL,
    mbi_num character varying(11),
    rfrnc_yr numeric(4,0),
    a_mo_cnt numeric(3,0),
    b_mo_cnt numeric(3,0),
    buyin_mo_cnt numeric(3,0),
    plan_cvrg_mo_cnt numeric(3,0),
    rds_mo_cnt numeric(3,0),
    hmo_mo_cnt numeric(3,0),
    dual_mo_cnt numeric(3,0),
    efctv_bgn_dt date,
    efctv_end_dt date,
    covstart date,
    bene_county_cd character varying(10) NOT NULL,
    bene_entlmt_rsn_curr character(1),
    bene_entlmt_rsn_orig character(1),
    bene_mdcr_status_cd character varying(2),
    bene_pta_trmntn_cd character(1),
    bene_ptb_trmntn_cd character(1),
    efivepct character(1),
    enrl_src character varying(3),
    sample_group character varying(2),
    v_dod_sw character(1),
    bene_esrd_ind character(1),
    crnt_bic character varying(2),
    mbi_hash character varying(64),
    hicn_unhashed character varying(11),
    bene_crnt_hic_num character varying(64) NOT NULL,
    bene_srnm_name character varying(24) NOT NULL,
    bene_gvn_name character varying(15) NOT NULL,
    bene_mdl_name character(1),
    bene_birth_dt date NOT NULL,
    bene_sex_ident_cd character(1) NOT NULL,
    state_code character varying(2) NOT NULL,
    bene_zip_cd character varying(9) NOT NULL,
    bene_race_cd character(1),
    age numeric(3,0),
    death_dt date,
    rti_race_cd character(1),
    drvd_line_1_adr character varying(40),
    drvd_line_2_adr character varying(40),
    drvd_line_3_adr character varying(40),
    drvd_line_4_adr character varying(40),
    drvd_line_5_adr character varying(40),
    drvd_line_6_adr character varying(40),
    city_name character varying(100),
    state_cd character varying(2),
    state_cnty_zip_cd character varying(9),
    last_updated timestamp with time zone,
    bene_link_key numeric(38,0),
    cst_shr_grp_jan_cd character varying(2),
    cst_shr_grp_feb_cd character varying(2),
    cst_shr_grp_mar_cd character varying(2),
    cst_shr_grp_apr_cd character varying(2),
    cst_shr_grp_may_cd character varying(2),
    cst_shr_grp_jun_cd character varying(2),
    cst_shr_grp_jul_cd character varying(2),
    cst_shr_grp_aug_cd character varying(2),
    cst_shr_grp_sept_cd character varying(2),
    cst_shr_grp_oct_cd character varying(2),
    cst_shr_grp_nov_cd character varying(2),
    cst_shr_grp_dec_cd character varying(2),
    fips_state_cnty_jan_cd character varying(5),
    fips_state_cnty_feb_cd character varying(5),
    fips_state_cnty_mar_cd character varying(5),
    fips_state_cnty_apr_cd character varying(5),
    fips_state_cnty_may_cd character varying(5),
    fips_state_cnty_jun_cd character varying(5),
    fips_state_cnty_jul_cd character varying(5),
    fips_state_cnty_aug_cd character varying(5),
    fips_state_cnty_sept_cd character varying(5),
    fips_state_cnty_oct_cd character varying(5),
    fips_state_cnty_nov_cd character varying(5),
    fips_state_cnty_dec_cd character varying(5),
    hmo_1_ind character(1),
    hmo_2_ind character(1),
    hmo_3_ind character(1),
    hmo_4_ind character(1),
    hmo_5_ind character(1),
    hmo_6_ind character(1),
    hmo_7_ind character(1),
    hmo_8_ind character(1),
    hmo_9_ind character(1),
    hmo_10_ind character(1),
    hmo_11_ind character(1),
    hmo_12_ind character(1),
    mdcr_entlmt_buyin_1_ind character(1),
    mdcr_entlmt_buyin_2_ind character(1),
    mdcr_entlmt_buyin_3_ind character(1),
    mdcr_entlmt_buyin_4_ind character(1),
    mdcr_entlmt_buyin_5_ind character(1),
    mdcr_entlmt_buyin_6_ind character(1),
    mdcr_entlmt_buyin_7_ind character(1),
    mdcr_entlmt_buyin_8_ind character(1),
    mdcr_entlmt_buyin_9_ind character(1),
    mdcr_entlmt_buyin_10_ind character(1),
    mdcr_entlmt_buyin_11_ind character(1),
    mdcr_entlmt_buyin_12_ind character(1),
    mdcr_stus_jan_cd character varying(2),
    mdcr_stus_feb_cd character varying(2),
    mdcr_stus_mar_cd character varying(2),
    mdcr_stus_apr_cd character varying(2),
    mdcr_stus_may_cd character varying(2),
    mdcr_stus_jun_cd character varying(2),
    mdcr_stus_jul_cd character varying(2),
    mdcr_stus_aug_cd character varying(2),
    mdcr_stus_sept_cd character varying(2),
    mdcr_stus_oct_cd character varying(2),
    mdcr_stus_nov_cd character varying(2),
    mdcr_stus_dec_cd character varying(2),
    meta_dual_elgbl_stus_jan_cd character varying(2),
    meta_dual_elgbl_stus_feb_cd character varying(2),
    meta_dual_elgbl_stus_mar_cd character varying(2),
    meta_dual_elgbl_stus_apr_cd character varying(2),
    meta_dual_elgbl_stus_may_cd character varying(2),
    meta_dual_elgbl_stus_jun_cd character varying(2),
    meta_dual_elgbl_stus_jul_cd character varying(2),
    meta_dual_elgbl_stus_aug_cd character varying(2),
    meta_dual_elgbl_stus_sept_cd character varying(2),
    meta_dual_elgbl_stus_oct_cd character varying(2),
    meta_dual_elgbl_stus_nov_cd character varying(2),
    meta_dual_elgbl_stus_dec_cd character varying(2),
    ptc_cntrct_jan_id character varying(5),
    ptc_cntrct_feb_id character varying(5),
    ptc_cntrct_mar_id character varying(5),
    ptc_cntrct_apr_id character varying(5),
    ptc_cntrct_may_id character varying(5),
    ptc_cntrct_jun_id character varying(5),
    ptc_cntrct_jul_id character varying(5),
    ptc_cntrct_aug_id character varying(5),
    ptc_cntrct_sept_id character varying(5),
    ptc_cntrct_oct_id character varying(5),
    ptc_cntrct_nov_id character varying(5),
    ptc_cntrct_dec_id character varying(5),
    ptc_pbp_jan_id character varying(3),
    ptc_pbp_feb_id character varying(3),
    ptc_pbp_mar_id character varying(3),
    ptc_pbp_apr_id character varying(3),
    ptc_pbp_may_id character varying(3),
    ptc_pbp_jun_id character varying(3),
    ptc_pbp_jul_id character varying(3),
    ptc_pbp_aug_id character varying(3),
    ptc_pbp_sept_id character varying(3),
    ptc_pbp_oct_id character varying(3),
    ptc_pbp_nov_id character varying(3),
    ptc_pbp_dec_id character varying(3),
    ptc_plan_type_jan_cd character varying(3),
    ptc_plan_type_feb_cd character varying(3),
    ptc_plan_type_mar_cd character varying(3),
    ptc_plan_type_apr_cd character varying(3),
    ptc_plan_type_may_cd character varying(3),
    ptc_plan_type_jun_cd character varying(3),
    ptc_plan_type_jul_cd character varying(3),
    ptc_plan_type_aug_cd character varying(3),
    ptc_plan_type_sept_cd character varying(3),
    ptc_plan_type_oct_cd character varying(3),
    ptc_plan_type_nov_cd character varying(3),
    ptc_plan_type_dec_cd character varying(3),
    ptd_cntrct_jan_id character varying(5),
    ptd_cntrct_feb_id character varying(5),
    ptd_cntrct_mar_id character varying(5),
    ptd_cntrct_apr_id character varying(5),
    ptd_cntrct_may_id character varying(5),
    ptd_cntrct_jun_id character varying(5),
    ptd_cntrct_jul_id character varying(5),
    ptd_cntrct_aug_id character varying(5),
    ptd_cntrct_sept_id character varying(5),
    ptd_cntrct_oct_id character varying(5),
    ptd_cntrct_nov_id character varying(5),
    ptd_cntrct_dec_id character varying(5),
    ptd_pbp_jan_id character varying(3),
    ptd_pbp_feb_id character varying(3),
    ptd_pbp_mar_id character varying(3),
    ptd_pbp_apr_id character varying(3),
    ptd_pbp_may_id character varying(3),
    ptd_pbp_jun_id character varying(3),
    ptd_pbp_jul_id character varying(3),
    ptd_pbp_aug_id character varying(3),
    ptd_pbp_sept_id character varying(3),
    ptd_pbp_oct_id character varying(3),
    ptd_pbp_nov_id character varying(3),
    ptd_pbp_dec_id character varying(3),
    ptd_sgmt_jan_id character varying(3),
    ptd_sgmt_feb_id character varying(3),
    ptd_sgmt_mar_id character varying(3),
    ptd_sgmt_apr_id character varying(3),
    ptd_sgmt_may_id character varying(3),
    ptd_sgmt_jun_id character varying(3),
    ptd_sgmt_jul_id character varying(3),
    ptd_sgmt_aug_id character varying(3),
    ptd_sgmt_sept_id character varying(3),
    ptd_sgmt_oct_id character varying(3),
    ptd_sgmt_nov_id character varying(3),
    ptd_sgmt_dec_id character varying(3),
    rds_jan_ind character(1),
    rds_feb_ind character(1),
    rds_mar_ind character(1),
    rds_apr_ind character(1),
    rds_may_ind character(1),
    rds_jun_ind character(1),
    rds_jul_ind character(1),
    rds_aug_ind character(1),
    rds_sept_ind character(1),
    rds_oct_ind character(1),
    rds_nov_ind character(1),
    rds_dec_ind character(1),
    pta_cvrg_strt_dt date,
    pta_cvrg_end_dt date,
    ptb_cvrg_strt_dt date,
    ptb_cvrg_end_dt date,
    ptd_cvrg_strt_dt date,
    ptd_cvrg_end_dt date,
	constraint beneficiaries_pkey
	primary key (bene_id)
);

CREATE INDEX IF NOT EXISTS beneficiaries_mbi_hash_idx
    ON ccw.beneficiaries (mbi_hash);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.beneficiaries_history (
    bene_history_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    hicn_unhashed character varying(11),
    mbi_num character varying(11),
    efctv_bgn_dt date,
    efctv_end_dt date,
    bene_crnt_hic_num character varying(64) NOT NULL,
    mbi_hash character varying(64),
    bene_birth_dt date NOT NULL,
    bene_sex_ident_cd character(1) NOT NULL,
    last_updated timestamp with time zone,
    CONSTRAINT beneficiaries_history_pkey PRIMARY KEY (bene_history_id),
	CONSTRAINT beneficiaries_history_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS beneficiaries_history_bene_id_idx
    ON ccw.beneficiaries_history (bene_id);
    
CREATE INDEX IF NOT EXISTS beneficiaries_history_hicn_idx
    ON ccw.beneficiaries_history (bene_crnt_hic_num);
    
CREATE INDEX IF NOT EXISTS beneficiaries_history_mbi_hash_idx
    ON ccw.beneficiaries_history (mbi_hash);
    
CREATE SEQUENCE IF NOT EXISTS ccw.beneficiaryhistory_beneficiaryhistoryid_seq
    INCREMENT 50 START 1 MINVALUE 1 MAXVALUE 9223372036854775807;  
    
-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.beneficiary_monthly (
    bene_id bigint NOT NULL,
    year_month date NOT NULL,
    partd_contract_number_id character varying(5),
    partc_contract_number_id character varying(5),
    medicare_status_code character varying(2),
    fips_state_cnty_code character varying(5),
    entitlement_buy_in_ind character(1),
    hmo_indicator_ind character(1),
    medicaid_dual_eligibility_code character varying(2),
    partd_pbp_number_id character varying(3),
    partd_retiree_drug_subsidy_ind character(1),
    partd_segment_number_id character varying(3),
    partd_low_income_cost_share_group_code character varying(2),
    partc_pbp_number_id character varying(3),
    partc_plan_type_code character varying(3),
    CONSTRAINT beneficiary_monthly_pkey PRIMARY KEY (bene_id, year_month),
    CONSTRAINT beneficiary_monthly_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS beneficiary_monthly_partd_contract_year_month_bene_id_idx
	ON ccw.beneficiary_monthly (partd_contract_number_id, year_month, bene_id);
	
-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.beneficiary_monthly_audit (
    bene_id bigint NOT NULL,
    year_month date NOT NULL,
    partd_contract_number_id character varying(5),
    partc_contract_number_id character varying(5),
    medicare_status_code character varying(2),
    fips_state_cnty_code character varying(5),
    entitlement_buy_in_ind character(1),
    hmo_indicator_ind character(1),
    medicaid_dual_eligibility_code character varying(2),
    partd_pbp_number_id character varying(3),
    partd_retiree_drug_subsidy_ind character(1),
    partd_segment_number_id character varying(3),
    partd_low_income_cost_share_group_code character varying(2),
    partc_pbp_number_id character varying(3),
    partc_plan_type_code character varying(3),
    operation character(1) NOT NULL,
    update_ts timestamp without time zone NOT NULL,
    seq_id bigint NOT NULL,
    CONSTRAINT beneficiary_monthly_audit_pkey PRIMARY KEY (seq_id)
);

CREATE INDEX IF NOT EXISTS beneficiary_monthly_audit_bene_id_year_month_idx
	ON ccw.beneficiary_monthly_audit (bene_id, year_month);
	
CREATE SEQUENCE IF NOT EXISTS ccw.bene_monthly_audit_seq
    START WITH 1000 INCREMENT BY 1 MINVALUE 1000 NO MAXVALUE;

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.carrier_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    clm_disp_cd character varying(2) NOT NULL,
    final_action character(1) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    carr_num character varying(5) NOT NULL,
    carr_clm_rfrng_pin_num character varying(14) NOT NULL,
    carr_clm_cntl_num character varying(23),
    carr_clm_entry_cd character(1) NOT NULL,
    carr_clm_prmry_pyr_pd_amt numeric(12,2) NOT NULL,
    carr_clm_cash_ddctbl_apld_amt numeric(12,2) NOT NULL,
    carr_clm_pmt_dnl_cd character varying(2) NOT NULL,
    carr_clm_hcpcs_yr_cd character(1),
    carr_clm_prvdr_asgnmt_ind_sw character(1),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_wkly_proc_dt date NOT NULL,
    nch_carr_clm_sbmtd_chrg_amt numeric(12,2) NOT NULL,
    nch_carr_clm_alowd_amt numeric(12,2) NOT NULL,
    nch_clm_bene_pmt_amt numeric(12,2) NOT NULL,
    nch_clm_prvdr_pmt_amt numeric(12,2) NOT NULL,
    clm_clncl_tril_num character varying(8),
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    rfr_physn_npi character varying(12),
    rfr_physn_upin character varying(12),
    icd_dgns_cd1 character varying(7),
    icd_dgns_vrsn_cd1 character(1),
    icd_dgns_cd2 character varying(7),
    icd_dgns_vrsn_cd2 character(1),
    icd_dgns_cd3 character varying(7),
    icd_dgns_vrsn_cd3 character(1),
    icd_dgns_cd4 character varying(7),
    icd_dgns_vrsn_cd4 character(1),
    icd_dgns_cd5 character varying(7),
    icd_dgns_vrsn_cd5 character(1),
    icd_dgns_cd6 character varying(7),
    icd_dgns_vrsn_cd6 character(1),
    icd_dgns_cd7 character varying(7),
    icd_dgns_vrsn_cd7 character(1),
    icd_dgns_cd8 character varying(7),
    icd_dgns_vrsn_cd8 character(1),
    icd_dgns_cd9 character varying(7),
    icd_dgns_vrsn_cd9 character(1),
    icd_dgns_cd10 character varying(7),
    icd_dgns_vrsn_cd10 character(1),
    icd_dgns_cd11 character varying(7),
    icd_dgns_vrsn_cd11 character(1),
    icd_dgns_cd12 character varying(7),
    icd_dgns_vrsn_cd12 character(1),
    carr_clm_blg_npi_num character varying(10),
    CONSTRAINT carrier_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT carrier_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS carrier_claims_bene_id_idx
	ON ccw.carrier_claims (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.carrier_claim_lines (
    clm_id bigint NOT NULL,
    line_num smallint NOT NULL,
    carr_line_rdcd_pmt_phys_astn_c character(1) NOT NULL,
    carr_line_prvdr_type_cd character(1) NOT NULL,
    carr_line_prcng_lclty_cd character varying(2) NOT NULL,
    carr_line_mtus_cnt numeric NOT NULL,
    carr_line_ansthsa_unit_cnt numeric NOT NULL,
    carr_prfrng_pin_num character varying(15) NOT NULL,
    carr_line_mtus_cd character(1),
    carr_line_rx_num character varying(30),
    carr_line_clia_lab_num character varying(10),
    line_1st_expns_dt date,
    line_last_expns_dt date,
    line_alowd_chrg_amt numeric(12,2) NOT NULL,
    line_coinsrnc_amt numeric(12,2) NOT NULL,
    line_sbmtd_chrg_amt numeric(12,2) NOT NULL,
    line_bene_pmt_amt numeric(12,2) NOT NULL,
    line_prvdr_pmt_amt numeric(12,2) NOT NULL,
    line_bene_prmry_pyr_cd character(1),
    line_bene_prmry_pyr_pd_amt numeric(12,2) NOT NULL,
    line_bene_ptb_ddctbl_amt numeric(12,2) NOT NULL,
    line_place_of_srvc_cd character varying(2) NOT NULL,
    line_pmt_80_100_cd character(1),
    line_srvc_cnt numeric NOT NULL,
    line_cms_type_srvc_cd character(1) NOT NULL,
    line_hct_hgb_type_cd character varying(2),
    line_hct_hgb_rslt_num numeric(4,1) NOT NULL,
    line_ndc_cd character varying(11),
    line_nch_pmt_amt numeric(12,2) NOT NULL,
    line_icd_dgns_cd character varying(7),
    line_icd_dgns_vrsn_cd character(1),
    line_prcsg_ind_cd character varying(2),
    line_service_deductible character(1),
    betos_cd character varying(3),
    hcpcs_cd character varying(5),
    hcpcs_1st_mdfr_cd character varying(5),
    hcpcs_2nd_mdfr_cd character varying(5),
    hpsa_scrcty_ind_cd character(1),
    prvdr_state_cd character varying(2),
    prvdr_spclty character varying(3),
    prvdr_zip character varying(9),
    tax_num character varying(10) NOT NULL,
    org_npi_num character varying(10),
    prf_physn_npi character varying(12),
    prf_physn_upin character varying(12),
    prtcptng_ind_cd character(1),
    CONSTRAINT carrier_claim_lines_pkey PRIMARY KEY (clm_id, line_num),
    CONSTRAINT carrier_claim_lines_clm_id_to_carrier_claims FOREIGN KEY (clm_id) REFERENCES ccw.carrier_claims(clm_id)
);
	
-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.dme_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    clm_disp_cd character varying(2) NOT NULL,
    final_action character(1) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    carr_num character varying(5) NOT NULL,
    carr_clm_cntl_num character varying(23),
    carr_clm_prvdr_asgnmt_ind_sw character(1) NOT NULL,
    carr_clm_entry_cd character(1) NOT NULL,
    carr_clm_prmry_pyr_pd_amt numeric(12,2) NOT NULL,
    carr_clm_cash_ddctbl_apld_amt numeric(12,2) NOT NULL,
    carr_clm_pmt_dnl_cd character varying(2) NOT NULL,
    carr_clm_hcpcs_yr_cd character(1),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_wkly_proc_dt date NOT NULL,
    nch_carr_clm_alowd_amt numeric(12,2) NOT NULL,
    nch_carr_clm_sbmtd_chrg_amt numeric(12,2) NOT NULL,
    nch_clm_bene_pmt_amt numeric(12,2) NOT NULL,
    nch_clm_prvdr_pmt_amt numeric(12,2) NOT NULL,
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    rfr_physn_npi character varying(12),
    rfr_physn_upin character varying(12),
    clm_clncl_tril_num character varying(8),
    icd_dgns_cd1 character varying(7),
    icd_dgns_vrsn_cd1 character(1),
    icd_dgns_cd2 character varying(7),
    icd_dgns_vrsn_cd2 character(1),
    icd_dgns_cd3 character varying(7),
    icd_dgns_vrsn_cd3 character(1),
    icd_dgns_cd4 character varying(7),
    icd_dgns_vrsn_cd4 character(1),
    icd_dgns_cd5 character varying(7),
    icd_dgns_vrsn_cd5 character(1),
    icd_dgns_cd6 character varying(7),
    icd_dgns_vrsn_cd6 character(1),
    icd_dgns_cd7 character varying(7),
    icd_dgns_vrsn_cd7 character(1),
    icd_dgns_cd8 character varying(7),
    icd_dgns_vrsn_cd8 character(1),
    icd_dgns_cd9 character varying(7),
    icd_dgns_vrsn_cd9 character(1),
    icd_dgns_cd10 character varying(7),
    icd_dgns_vrsn_cd10 character(1),
    icd_dgns_cd11 character varying(7),
    icd_dgns_vrsn_cd11 character(1),
    icd_dgns_cd12 character varying(7),
    icd_dgns_vrsn_cd12 character(1),
    CONSTRAINT dme_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT dme_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS dme_claims_bene_id_idx
	ON ccw.dme_claims (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.dme_claim_lines (
    clm_id bigint NOT NULL,
    line_num smallint NOT NULL,
    line_1st_expns_dt date,
    line_last_expns_dt date,
    line_alowd_chrg_amt numeric(12,2) NOT NULL,
    line_coinsrnc_amt numeric(12,2) NOT NULL,
    line_sbmtd_chrg_amt numeric(12,2) NOT NULL,
    line_bene_pmt_amt numeric(12,2) NOT NULL,
    line_prvdr_pmt_amt numeric(12,2) NOT NULL,
    line_bene_prmry_pyr_cd character(1),
    line_bene_prmry_pyr_pd_amt numeric(12,2) NOT NULL,
    line_bene_ptb_ddctbl_amt numeric(12,2) NOT NULL,
    line_place_of_srvc_cd character varying(2) NOT NULL,
    line_pmt_80_100_cd character(1),
    line_srvc_cnt numeric NOT NULL,
    line_cms_type_srvc_cd character(1) NOT NULL,
    line_hct_hgb_type_cd character varying(2),
    line_hct_hgb_rslt_num numeric(3,1) NOT NULL,
    line_ndc_cd character varying(11),
    line_nch_pmt_amt numeric(12,2) NOT NULL,
    line_icd_dgns_cd character varying(7),
    line_icd_dgns_vrsn_cd character(1),
    line_dme_prchs_price_amt numeric(12,2) NOT NULL,
    line_prmry_alowd_chrg_amt numeric(12,2) NOT NULL,
    line_prcsg_ind_cd character varying(2),
    line_service_deductible character(1),
    betos_cd character varying(3),
    hcpcs_cd character varying(5),
    hcpcs_1st_mdfr_cd character varying(5),
    hcpcs_2nd_mdfr_cd character varying(5),
    hcpcs_3rd_mdfr_cd character varying(5),
    hcpcs_4th_mdfr_cd character varying(5),
    dmerc_line_mtus_cd character(1),
    dmerc_line_mtus_cnt numeric NOT NULL,
    dmerc_line_prcng_state_cd character varying(2),
    dmerc_line_scrn_svgs_amt numeric(12,2),
    dmerc_line_supplr_type_cd character(1),
    prtcptng_ind_cd character(1),
    prvdr_npi character varying(12),
    prvdr_num character varying(10),
    prvdr_spclty character varying(3),
    prvdr_state_cd character varying(2) NOT NULL,
    tax_num character varying(10) NOT NULL,
    CONSTRAINT dme_claim_lines_pkey PRIMARY KEY (clm_id, line_num),
    CONSTRAINT dme_claim_lines_clm_id_to_dme_claims FOREIGN KEY (clm_id) REFERENCES ccw.dme_claims(clm_id)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.hha_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    final_action character(1) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    clm_fac_type_cd character(1) NOT NULL,
    clm_freq_cd character(1) NOT NULL,
    clm_srvc_clsfctn_type_cd character(1) NOT NULL,
    clm_tot_chrg_amt numeric(12,2) NOT NULL,
    clm_hha_tot_visit_cnt numeric(4,0) NOT NULL,
    clm_admsn_dt date,
    clm_pps_ind_cd character(1) NOT NULL,
    clm_hha_lupa_ind_cd character(1),
    clm_hha_rfrl_cd character(1),
    clm_mdcr_non_pmt_rsn_cd character varying(2),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_wkly_proc_dt date NOT NULL,
    nch_prmry_pyr_cd character(1),
    nch_prmry_pyr_clm_pd_amt numeric(12,2) NOT NULL,
    prvdr_num character varying(9) NOT NULL,
    prvdr_state_cd character varying(2) NOT NULL,
    fi_num character varying(5),
    fi_orig_clm_cntl_num character varying(23),
    fi_clm_proc_dt date,
    fi_doc_clm_cntl_num character varying(23),
    at_physn_npi character varying(10),
    org_npi_num character varying(10),
    at_physn_upin character varying(9),
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    ptnt_dschrg_stus_cd character varying(2) NOT NULL,
    fst_dgns_e_cd character varying(7),
    fst_dgns_e_vrsn_cd character(1),
    icd_dgns_cd1 character varying(7),
    icd_dgns_vrsn_cd1 character(1),
    icd_dgns_e_cd1 character varying(7),
    icd_dgns_e_vrsn_cd1 character(1),
    icd_dgns_cd2 character varying(7),
    icd_dgns_vrsn_cd2 character(1),
    icd_dgns_e_cd2 character varying(7),
    icd_dgns_e_vrsn_cd2 character(1),
    icd_dgns_cd3 character varying(7),
    icd_dgns_vrsn_cd3 character(1),
    icd_dgns_e_cd3 character varying(7),
    icd_dgns_e_vrsn_cd3 character(1),
    icd_dgns_cd4 character varying(7),
    icd_dgns_vrsn_cd4 character(1),
    icd_dgns_e_cd4 character varying(7),
    icd_dgns_e_vrsn_cd4 character(1),
    icd_dgns_cd5 character varying(7),
    icd_dgns_vrsn_cd5 character(1),
    icd_dgns_e_cd5 character varying(7),
    icd_dgns_e_vrsn_cd5 character(1),
    icd_dgns_cd6 character varying(7),
    icd_dgns_vrsn_cd6 character(1),
    icd_dgns_e_cd6 character varying(7),
    icd_dgns_e_vrsn_cd6 character(1),
    icd_dgns_cd7 character varying(7),
    icd_dgns_vrsn_cd7 character(1),
    icd_dgns_e_cd7 character varying(7),
    icd_dgns_e_vrsn_cd7 character(1),
    icd_dgns_cd8 character varying(7),
    icd_dgns_vrsn_cd8 character(1),
    icd_dgns_e_cd8 character varying(7),
    icd_dgns_e_vrsn_cd8 character(1),
    icd_dgns_cd9 character varying(7),
    icd_dgns_vrsn_cd9 character(1),
    icd_dgns_e_cd9 character varying(7),
    icd_dgns_e_vrsn_cd9 character(1),
    icd_dgns_cd10 character varying(7),
    icd_dgns_vrsn_cd10 character(1),
    icd_dgns_e_cd10 character varying(7),
    icd_dgns_e_vrsn_cd10 character(1),
    icd_dgns_cd11 character varying(7),
    icd_dgns_vrsn_cd11 character(1),
    icd_dgns_e_cd11 character varying(7),
    icd_dgns_e_vrsn_cd11 character(1),
    icd_dgns_cd12 character varying(7),
    icd_dgns_vrsn_cd12 character(1),
    icd_dgns_e_cd12 character varying(7),
    icd_dgns_e_vrsn_cd12 character(1),
    icd_dgns_cd13 character varying(7),
    icd_dgns_vrsn_cd13 character(1),
    icd_dgns_cd14 character varying(7),
    icd_dgns_vrsn_cd14 character(1),
    icd_dgns_cd15 character varying(7),
    icd_dgns_vrsn_cd15 character(1),
    icd_dgns_cd16 character varying(7),
    icd_dgns_vrsn_cd16 character(1),
    icd_dgns_cd17 character varying(7),
    icd_dgns_vrsn_cd17 character(1),
    icd_dgns_cd18 character varying(7),
    icd_dgns_vrsn_cd18 character(1),
    icd_dgns_cd19 character varying(7),
    icd_dgns_vrsn_cd19 character(1),
    icd_dgns_cd20 character varying(7),
    icd_dgns_vrsn_cd20 character(1),
    icd_dgns_cd21 character varying(7),
    icd_dgns_vrsn_cd21 character(1),
    icd_dgns_cd22 character varying(7),
    icd_dgns_vrsn_cd22 character(1),
    icd_dgns_cd23 character varying(7),
    icd_dgns_vrsn_cd23 character(1),
    icd_dgns_cd24 character varying(7),
    icd_dgns_vrsn_cd24 character(1),
    icd_dgns_cd25 character varying(7),
    icd_dgns_vrsn_cd25 character(1),
    claim_query_code character(1),
    CONSTRAINT hha_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT hha_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS hha_claims_bene_id_idx
	ON ccw.hha_claims (bene_id);
	
-- =====================================================
	
CREATE TABLE IF NOT EXISTS ccw.hha_claim_lines (
    clm_id bigint NOT NULL,
    clm_line_num smallint NOT NULL,
    rev_cntr character varying(4) NOT NULL,
    rev_cntr_unit_cnt numeric NOT NULL,
    rev_cntr_tot_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_rate_amt numeric(12,2) NOT NULL,
    rev_cntr_ncvrd_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_ddctbl_coinsrnc_cd character(1),
    rev_cntr_pmt_amt_amt numeric(12,2) NOT NULL,
    rev_cntr_dt date,
    rev_cntr_ndc_qty_qlfr_cd character varying(2),
    rev_cntr_ndc_qty numeric,
    rev_cntr_1st_ansi_cd character varying(5),
    rev_cntr_apc_hipps_cd character varying(5),
    rev_cntr_pmt_mthd_ind_cd character varying(2),
    rev_cntr_stus_ind_cd character varying(2),
    hcpcs_cd character varying(5),
    hcpcs_1st_mdfr_cd character varying(5),
    hcpcs_2nd_mdfr_cd character varying(5),
    rndrng_physn_npi character varying(12),
    rndrng_physn_upin character varying(12),
    CONSTRAINT hha_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num),
    CONSTRAINT hha_claim_lines_clm_id_to_hha_claims FOREIGN KEY (clm_id) REFERENCES ccw.hha_claims(clm_id)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.hospice_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    final_action character(1) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    clm_fac_type_cd character(1) NOT NULL,
    clm_freq_cd character(1) NOT NULL,
    clm_srvc_clsfctn_type_cd character(1) NOT NULL,
    clm_tot_chrg_amt numeric(12,2) NOT NULL,
    clm_utlztn_day_cnt numeric NOT NULL,
    clm_hospc_start_dt_id date,
    clm_mdcr_non_pmt_rsn_cd character varying(2),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_wkly_proc_dt date NOT NULL,
    nch_prmry_pyr_clm_pd_amt numeric(12,2) NOT NULL,
    nch_prmry_pyr_cd character(1),
    nch_ptnt_status_ind_cd character(1),
    nch_bene_dschrg_dt date,
    prvdr_num character varying(9) NOT NULL,
    prvdr_state_cd character varying(2) NOT NULL,
    ptnt_dschrg_stus_cd character varying(2) NOT NULL,
    fi_num character varying(5),
    fi_orig_clm_cntl_num character varying(23),
    fi_clm_proc_dt date,
    fi_doc_clm_cntl_num character varying(23),
    at_physn_npi character varying(10),
    at_physn_upin character varying(9),
    org_npi_num character varying(10),
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    bene_hospc_prd_cnt numeric(2,0),
    fst_dgns_e_cd character varying(7),
    fst_dgns_e_vrsn_cd character(1),
    icd_dgns_cd1 character varying(7),
    icd_dgns_vrsn_cd1 character(1),
    icd_dgns_e_cd1 character varying(7),
    icd_dgns_e_vrsn_cd1 character(1),
    icd_dgns_cd2 character varying(7),
    icd_dgns_vrsn_cd2 character(1),
    icd_dgns_e_cd2 character varying(7),
    icd_dgns_e_vrsn_cd2 character(1),
    icd_dgns_cd3 character varying(7),
    icd_dgns_vrsn_cd3 character(1),
    icd_dgns_e_cd3 character varying(7),
    icd_dgns_e_vrsn_cd3 character(1),
    icd_dgns_cd4 character varying(7),
    icd_dgns_vrsn_cd4 character(1),
    icd_dgns_e_cd4 character varying(7),
    icd_dgns_e_vrsn_cd4 character(1),
    icd_dgns_cd5 character varying(7),
    icd_dgns_vrsn_cd5 character(1),
    icd_dgns_e_cd5 character varying(7),
    icd_dgns_e_vrsn_cd5 character(1),
    icd_dgns_cd6 character varying(7),
    icd_dgns_vrsn_cd6 character(1),
    icd_dgns_e_cd6 character varying(7),
    icd_dgns_e_vrsn_cd6 character(1),
    icd_dgns_cd7 character varying(7),
    icd_dgns_vrsn_cd7 character(1),
    icd_dgns_e_cd7 character varying(7),
    icd_dgns_e_vrsn_cd7 character(1),
    icd_dgns_cd8 character varying(7),
    icd_dgns_vrsn_cd8 character(1),
    icd_dgns_e_cd8 character varying(7),
    icd_dgns_e_vrsn_cd8 character(1),
    icd_dgns_cd9 character varying(7),
    icd_dgns_vrsn_cd9 character(1),
    icd_dgns_e_cd9 character varying(7),
    icd_dgns_e_vrsn_cd9 character(1),
    icd_dgns_cd10 character varying(7),
    icd_dgns_vrsn_cd10 character(1),
    icd_dgns_e_cd10 character varying(7),
    icd_dgns_e_vrsn_cd10 character(1),
    icd_dgns_cd11 character varying(7),
    icd_dgns_vrsn_cd11 character(1),
    icd_dgns_e_cd11 character varying(7),
    icd_dgns_e_vrsn_cd11 character(1),
    icd_dgns_cd12 character varying(7),
    icd_dgns_vrsn_cd12 character(1),
    icd_dgns_e_cd12 character varying(7),
    icd_dgns_e_vrsn_cd12 character(1),
    icd_dgns_cd13 character varying(7),
    icd_dgns_vrsn_cd13 character(1),
    icd_dgns_cd14 character varying(7),
    icd_dgns_vrsn_cd14 character(1),
    icd_dgns_cd15 character varying(7),
    icd_dgns_vrsn_cd15 character(1),
    icd_dgns_cd16 character varying(7),
    icd_dgns_vrsn_cd16 character(1),
    icd_dgns_cd17 character varying(7),
    icd_dgns_vrsn_cd17 character(1),
    icd_dgns_cd18 character varying(7),
    icd_dgns_vrsn_cd18 character(1),
    icd_dgns_cd19 character varying(7),
    icd_dgns_vrsn_cd19 character(1),
    icd_dgns_cd20 character varying(7),
    icd_dgns_vrsn_cd20 character(1),
    icd_dgns_cd21 character varying(7),
    icd_dgns_vrsn_cd21 character(1),
    icd_dgns_cd22 character varying(7),
    icd_dgns_vrsn_cd22 character(1),
    icd_dgns_cd23 character varying(7),
    icd_dgns_vrsn_cd23 character(1),
    icd_dgns_cd24 character varying(7),
    icd_dgns_vrsn_cd24 character(1),
    icd_dgns_cd25 character varying(7),
    icd_dgns_vrsn_cd25 character(1),
    claim_query_code character(1),
    CONSTRAINT hospice_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT hospice_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS hospice_claims_bene_id_idx
	ON ccw.hospice_claims (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.hospice_claim_lines (
    clm_id bigint NOT NULL,
    clm_line_num smallint NOT NULL,
    rev_cntr character varying(4) NOT NULL,
    rev_cntr_unit_cnt numeric NOT NULL,
    rev_cntr_tot_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_rate_amt numeric(12,2) NOT NULL,
    rev_cntr_ncvrd_chrg_amt numeric(12,2),
    rev_cntr_ddctbl_coinsrnc_cd character(1),
    rev_cntr_bene_pmt_amt numeric(12,2) NOT NULL,
    rev_cntr_pmt_amt_amt numeric(12,2) NOT NULL,
    rev_cntr_prvdr_pmt_amt numeric(12,2) NOT NULL,
    rev_cntr_dt date,
    rev_cntr_ndc_qty_qlfr_cd character varying(2),
    rev_cntr_ndc_qty numeric,
    hcpcs_cd character varying(5),
    hcpcs_1st_mdfr_cd character varying(5),
    hcpcs_2nd_mdfr_cd character varying(5),
    rndrng_physn_npi character varying(12),
    rndrng_physn_upin character varying(12),
    CONSTRAINT hospice_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num),
    CONSTRAINT hospice_claim_lines_clm_id_to_hospice_claims FOREIGN KEY (clm_id) REFERENCES ccw.hospice_claims(clm_id)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.inpatient_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    clm_fac_type_cd character(1) NOT NULL,
    clm_freq_cd character(1) NOT NULL,
    final_action character(1) NOT NULL,
    clm_tot_chrg_amt numeric(12,2) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    clm_pass_thru_per_diem_amt numeric(12,2) NOT NULL,
    clm_admsn_dt date,
    clm_drg_cd character varying(4),
    clm_ip_admsn_type_cd character(1) NOT NULL,
    clm_mco_pd_sw character(1),
    clm_mdcr_non_pmt_rsn_cd character varying(2),
    clm_non_utlztn_days_cnt numeric NOT NULL,
    clm_utlztn_day_cnt numeric NOT NULL,
    clm_drg_outlier_stay_cd character(1) NOT NULL,
    claim_query_code character(1) NOT NULL,
    clm_srvc_clsfctn_type_cd character(1) NOT NULL,
    clm_pps_ind_cd character(1),
    clm_pps_cptl_drg_wt_num numeric(7,4),
    clm_pps_cptl_dsprprtnt_shr_amt numeric(12,2),
    clm_pps_cptl_excptn_amt numeric(12,2),
    clm_pps_cptl_fsp_amt numeric(12,2),
    clm_pps_cptl_ime_amt numeric(12,2),
    clm_pps_cptl_outlier_amt numeric(12,2),
    clm_pps_old_cptl_hld_hrmls_amt numeric(12,2),
    clm_uncompd_care_pmt_amt numeric(38,2),
    clm_tot_pps_cptl_amt numeric(12,2),
    clm_src_ip_admsn_cd character(1),
    bene_lrd_used_cnt numeric,
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    prvdr_num character varying(9) NOT NULL,
    prvdr_state_cd character varying(2) NOT NULL,
    ptnt_dschrg_stus_cd character varying(2) NOT NULL,
    bene_tot_coinsrnc_days_cnt numeric NOT NULL,
    admtg_dgns_cd character varying(7),
    admtg_dgns_vrsn_cd character(1),
    at_physn_npi character varying(10),
    at_physn_upin character varying(9),
    fi_clm_actn_cd character(1),
    fi_clm_proc_dt date,
    fi_doc_clm_cntl_num character varying(23),
    fi_num character varying(5),
    fi_orig_clm_cntl_num character varying(23),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_actv_or_cvrd_lvl_care_thru date,
    nch_bene_dschrg_dt date,
    nch_blood_pnts_frnshd_qty numeric NOT NULL,
    nch_bene_blood_ddctbl_lblty_am numeric(12,2) NOT NULL,
    nch_bene_ip_ddctbl_amt numeric(12,2) NOT NULL,
    nch_bene_pta_coinsrnc_lblty_am numeric(12,2) NOT NULL,
    nch_ip_ncvrd_chrg_amt numeric(12,2) NOT NULL,
    nch_ip_tot_ddctn_amt numeric(12,2) NOT NULL,
    nch_prmry_pyr_cd character(1),
    nch_prmry_pyr_clm_pd_amt numeric(12,2) NOT NULL,
    nch_profnl_cmpnt_chrg_amt numeric(12,2) NOT NULL,
    nch_drg_outlier_aprvd_pmt_amt numeric(12,2),
    nch_bene_mdcr_bnfts_exhtd_dt_i date,
    nch_ptnt_status_ind_cd character(1),
    nch_wkly_proc_dt date NOT NULL,
    nch_vrfd_ncvrd_stay_from_dt date,
    nch_vrfd_ncvrd_stay_thru_dt date,
    dsh_op_clm_val_amt numeric(12,2),
    ime_op_clm_val_amt numeric(12,2),
    op_physn_npi character varying(10),
    op_physn_upin character varying(9),
    org_npi_num character varying(10),
    ot_physn_npi character varying(10),
    ot_physn_upin character varying(9),
    fst_dgns_e_cd character varying(7),
    fst_dgns_e_vrsn_cd character(1),
    prcdr_dt1 date,
    icd_prcdr_cd1 character varying(7),
    icd_prcdr_vrsn_cd1 character(1),
    icd_dgns_cd1 character varying(7),
    icd_dgns_e_cd1 character varying(7),
    icd_dgns_vrsn_cd1 character(1),
    icd_dgns_e_vrsn_cd1 character(1),
    clm_poa_ind_sw1 character(1),
    clm_e_poa_ind_sw1 character(1),
    prcdr_dt2 date,
    icd_prcdr_cd2 character varying(7),
    icd_prcdr_vrsn_cd2 character(1),
    icd_dgns_cd2 character varying(7),
    icd_dgns_e_cd2 character varying(7),
    icd_dgns_vrsn_cd2 character(1),
    icd_dgns_e_vrsn_cd2 character(1),
    clm_poa_ind_sw2 character(1),
    clm_e_poa_ind_sw2 character(1),
    prcdr_dt3 date,
    icd_prcdr_cd3 character varying(7),
    icd_prcdr_vrsn_cd3 character(1),
    icd_dgns_cd3 character varying(7),
    icd_dgns_e_cd3 character varying(7),
    icd_dgns_vrsn_cd3 character(1),
    icd_dgns_e_vrsn_cd3 character(1),
    clm_poa_ind_sw3 character(1),
    clm_e_poa_ind_sw3 character(1),
    prcdr_dt4 date,
    icd_prcdr_cd4 character varying(7),
    icd_prcdr_vrsn_cd4 character(1),
    icd_dgns_cd4 character varying(7),
    icd_dgns_e_cd4 character varying(7),
    icd_dgns_vrsn_cd4 character(1),
    icd_dgns_e_vrsn_cd4 character(1),
    clm_poa_ind_sw4 character(1),
    clm_e_poa_ind_sw4 character(1),
    prcdr_dt5 date,
    icd_prcdr_cd5 character varying(7),
    icd_prcdr_vrsn_cd5 character(1),
    icd_dgns_cd5 character varying(7),
    icd_dgns_e_cd5 character varying(7),
    icd_dgns_vrsn_cd5 character(1),
    icd_dgns_e_vrsn_cd5 character(1),
    clm_poa_ind_sw5 character(1),
    clm_e_poa_ind_sw5 character(1),
    prcdr_dt6 date,
    icd_prcdr_cd6 character varying(7),
    icd_prcdr_vrsn_cd6 character(1),
    icd_dgns_cd6 character varying(7),
    icd_dgns_e_cd6 character varying(7),
    icd_dgns_vrsn_cd6 character(1),
    icd_dgns_e_vrsn_cd6 character(1),
    clm_poa_ind_sw6 character(1),
    clm_e_poa_ind_sw6 character(1),
    prcdr_dt7 date,
    icd_prcdr_cd7 character varying(7),
    icd_prcdr_vrsn_cd7 character(1),
    icd_dgns_cd7 character varying(7),
    icd_dgns_e_cd7 character varying(7),
    icd_dgns_vrsn_cd7 character(1),
    icd_dgns_e_vrsn_cd7 character(1),
    clm_poa_ind_sw7 character(1),
    clm_e_poa_ind_sw7 character(1),
    prcdr_dt8 date,
    icd_prcdr_cd8 character varying(7),
    icd_prcdr_vrsn_cd8 character(1),
    icd_dgns_cd8 character varying(7),
    icd_dgns_e_cd8 character varying(7),
    icd_dgns_vrsn_cd8 character(1),
    icd_dgns_e_vrsn_cd8 character(1),
    clm_poa_ind_sw8 character(1),
    clm_e_poa_ind_sw8 character(1),
    prcdr_dt9 date,
    icd_prcdr_cd9 character varying(7),
    icd_prcdr_vrsn_cd9 character(1),
    icd_dgns_cd9 character varying(7),
    icd_dgns_e_cd9 character varying(7),
    icd_dgns_vrsn_cd9 character(1),
    icd_dgns_e_vrsn_cd9 character(1),
    clm_poa_ind_sw9 character(1),
    clm_e_poa_ind_sw9 character(1),
    prcdr_dt10 date,
    icd_prcdr_cd10 character varying(7),
    icd_prcdr_vrsn_cd10 character(1),
    icd_dgns_cd10 character varying(7),
    icd_dgns_e_cd10 character varying(7),
    icd_dgns_vrsn_cd10 character(1),
    icd_dgns_e_vrsn_cd10 character(1),
    clm_poa_ind_sw10 character(1),
    clm_e_poa_ind_sw10 character(1),
    prcdr_dt11 date,
    icd_prcdr_cd11 character varying(7),
    icd_prcdr_vrsn_cd11 character(1),
    icd_dgns_cd11 character varying(7),
    icd_dgns_e_cd11 character varying(7),
    icd_dgns_vrsn_cd11 character(1),
    icd_dgns_e_vrsn_cd11 character(1),
    clm_poa_ind_sw11 character(1),
    clm_e_poa_ind_sw11 character(1),
    prcdr_dt12 date,
    icd_prcdr_cd12 character varying(7),
    icd_prcdr_vrsn_cd12 character(1),
    icd_dgns_cd12 character varying(7),
    icd_dgns_e_cd12 character varying(7),
    icd_dgns_vrsn_cd12 character(1),
    icd_dgns_e_vrsn_cd12 character(1),
    clm_poa_ind_sw12 character(1),
    clm_e_poa_ind_sw12 character(1),
    prcdr_dt13 date,
    icd_prcdr_cd13 character varying(7),
    icd_prcdr_vrsn_cd13 character(1),
    icd_dgns_cd13 character varying(7),
    icd_dgns_vrsn_cd13 character(1),
    clm_poa_ind_sw13 character(1),
    prcdr_dt14 date,
    icd_prcdr_cd14 character varying(7),
    icd_prcdr_vrsn_cd14 character(1),
    icd_dgns_cd14 character varying(7),
    icd_dgns_vrsn_cd14 character(1),
    clm_poa_ind_sw14 character(1),
    prcdr_dt15 date,
    icd_prcdr_cd15 character varying(7),
    icd_prcdr_vrsn_cd15 character(1),
    icd_dgns_cd15 character varying(7),
    icd_dgns_vrsn_cd15 character(1),
    clm_poa_ind_sw15 character(1),
    prcdr_dt16 date,
    icd_prcdr_cd16 character varying(7),
    icd_prcdr_vrsn_cd16 character(1),
    icd_dgns_cd16 character varying(7),
    icd_dgns_vrsn_cd16 character(1),
    clm_poa_ind_sw16 character(1),
    prcdr_dt17 date,
    icd_prcdr_cd17 character varying(7),
    icd_prcdr_vrsn_cd17 character(1),
    icd_dgns_cd17 character varying(7),
    icd_dgns_vrsn_cd17 character(1),
    clm_poa_ind_sw17 character(1),
    prcdr_dt18 date,
    icd_prcdr_cd18 character varying(7),
    icd_prcdr_vrsn_cd18 character(1),
    icd_dgns_cd18 character varying(7),
    icd_dgns_vrsn_cd18 character(1),
    clm_poa_ind_sw18 character(1),
    prcdr_dt19 date,
    icd_prcdr_cd19 character varying(7),
    icd_prcdr_vrsn_cd19 character(1),
    icd_dgns_cd19 character varying(7),
    icd_dgns_vrsn_cd19 character(1),
    clm_poa_ind_sw19 character(1),
    prcdr_dt20 date,
    icd_prcdr_cd20 character varying(7),
    icd_prcdr_vrsn_cd20 character(1),
    icd_dgns_cd20 character varying(7),
    icd_dgns_vrsn_cd20 character(1),
    clm_poa_ind_sw20 character(1),
    prcdr_dt21 date,
    icd_prcdr_cd21 character varying(7),
    icd_prcdr_vrsn_cd21 character(1),
    icd_dgns_cd21 character varying(7),
    icd_dgns_vrsn_cd21 character(1),
    clm_poa_ind_sw21 character(1),
    prcdr_dt22 date,
    icd_prcdr_cd22 character varying(7),
    icd_prcdr_vrsn_cd22 character(1),
    icd_dgns_cd22 character varying(7),
    icd_dgns_vrsn_cd22 character(1),
    clm_poa_ind_sw22 character(1),
    prcdr_dt23 date,
    icd_prcdr_cd23 character varying(7),
    icd_prcdr_vrsn_cd23 character(1),
    icd_dgns_cd23 character varying(7),
    icd_dgns_vrsn_cd23 character(1),
    clm_poa_ind_sw23 character(1),
    prcdr_dt24 date,
    icd_prcdr_cd24 character varying(7),
    icd_prcdr_vrsn_cd24 character(1),
    icd_dgns_cd24 character varying(7),
    icd_dgns_vrsn_cd24 character(1),
    clm_poa_ind_sw24 character(1),
    prcdr_dt25 date,
    icd_prcdr_cd25 character varying(7),
    icd_prcdr_vrsn_cd25 character(1),
    icd_dgns_cd25 character varying(7),
    icd_dgns_vrsn_cd25 character(1),
    clm_poa_ind_sw25 character(1),
    CONSTRAINT inpatient_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT inpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS inpatient_claims_bene_id_idx
	ON ccw.inpatient_claims (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.inpatient_claim_lines (
    clm_id bigint NOT NULL,
    clm_line_num smallint NOT NULL,
    rev_cntr character varying(4) NOT NULL,
    rev_cntr_unit_cnt numeric NOT NULL,
    rev_cntr_rate_amt numeric(12,2) NOT NULL,
    rev_cntr_tot_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_ncvrd_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_ddctbl_coinsrnc_cd character(1),
    rev_cntr_ndc_qty_qlfr_cd character varying(2),
    rev_cntr_ndc_qty numeric,
    hcpcs_cd character varying(5),
    rndrng_physn_npi character varying(12),
    rndrng_physn_upin character varying(12),
    CONSTRAINT inpatient_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num),
    CONSTRAINT inpatient_claim_lines_clm_id_to_inpatient_claims FOREIGN KEY (clm_id) REFERENCES ccw.inpatient_claims(clm_id)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.outpatient_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    clm_fac_type_cd character(1) NOT NULL,
    clm_freq_cd character(1) NOT NULL,
    final_action character(1) NOT NULL,
    clm_tot_chrg_amt numeric(12,2) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    clm_op_bene_pmt_amt numeric(12,2) NOT NULL,
    clm_op_prvdr_pmt_amt numeric(12,2) NOT NULL,
    claim_query_code character(1) NOT NULL,
    clm_srvc_clsfctn_type_cd character(1) NOT NULL,
    clm_mdcr_non_pmt_rsn_cd character varying(2),
    clm_mco_pd_sw character(1),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_wkly_proc_dt date NOT NULL,
    nch_bene_blood_ddctbl_lblty_am numeric(12,2) NOT NULL,
    nch_bene_ptb_coinsrnc_amt numeric(12,2) NOT NULL,
    nch_bene_ptb_ddctbl_amt numeric(12,2) NOT NULL,
    nch_prmry_pyr_clm_pd_amt numeric(12,2) NOT NULL,
    nch_profnl_cmpnt_chrg_amt numeric(12,2) NOT NULL,
    nch_prmry_pyr_cd character(1),
    prvdr_num character varying(9) NOT NULL,
    prvdr_state_cd character varying(2) NOT NULL,
    at_physn_npi character varying(10),
    at_physn_upin character varying(9),
    op_physn_npi character varying(10),
    op_physn_upin character varying(9),
    ot_physn_npi character varying(10),
    ot_physn_upin character varying(9),
    org_npi_num character varying(10),
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    ptnt_dschrg_stus_cd character varying(2),
    fi_clm_proc_dt date,
    fi_doc_clm_cntl_num character varying(23),
    fi_num character varying(5),
    fi_orig_clm_cntl_num character varying(23),
    fst_dgns_e_cd character varying(7),
    fst_dgns_e_vrsn_cd character(1),
    rsn_visit_cd1 character varying(7),
    rsn_visit_vrsn_cd1 character(1),
    rsn_visit_cd2 character varying(7),
    rsn_visit_vrsn_cd2 character(1),
    rsn_visit_cd3 character varying(7),
    rsn_visit_vrsn_cd3 character(1),
    prcdr_dt1 date,
    icd_dgns_cd1 character varying(7),
    icd_dgns_vrsn_cd1 character(1),
    icd_prcdr_cd1 character varying(7),
    icd_prcdr_vrsn_cd1 character(1),
    icd_dgns_e_cd1 character varying(7),
    icd_dgns_e_vrsn_cd1 character(1),
    prcdr_dt2 date,
    icd_dgns_cd2 character varying(7),
    icd_dgns_vrsn_cd2 character(1),
    icd_prcdr_cd2 character varying(7),
    icd_prcdr_vrsn_cd2 character(1),
    icd_dgns_e_cd2 character varying(7),
    icd_dgns_e_vrsn_cd2 character(1),
    prcdr_dt3 date,
    icd_dgns_cd3 character varying(7),
    icd_dgns_vrsn_cd3 character(1),
    icd_prcdr_cd3 character varying(7),
    icd_prcdr_vrsn_cd3 character(1),
    icd_dgns_e_cd3 character varying(7),
    icd_dgns_e_vrsn_cd3 character(1),
    prcdr_dt4 date,
    icd_dgns_cd4 character varying(7),
    icd_dgns_vrsn_cd4 character(1),
    icd_prcdr_cd4 character varying(7),
    icd_prcdr_vrsn_cd4 character(1),
    icd_dgns_e_cd4 character varying(7),
    icd_dgns_e_vrsn_cd4 character(1),
    prcdr_dt5 date,
    icd_dgns_cd5 character varying(7),
    icd_dgns_vrsn_cd5 character(1),
    icd_prcdr_cd5 character varying(7),
    icd_prcdr_vrsn_cd5 character(1),
    icd_dgns_e_cd5 character varying(7),
    icd_dgns_e_vrsn_cd5 character(1),
    prcdr_dt6 date,
    icd_dgns_cd6 character varying(7),
    icd_dgns_vrsn_cd6 character(1),
    icd_prcdr_cd6 character varying(7),
    icd_prcdr_vrsn_cd6 character(1),
    icd_dgns_e_cd6 character varying(7),
    icd_dgns_e_vrsn_cd6 character(1),
    prcdr_dt7 date,
    icd_dgns_cd7 character varying(7),
    icd_dgns_vrsn_cd7 character(1),
    icd_prcdr_cd7 character varying(7),
    icd_prcdr_vrsn_cd7 character(1),
    icd_dgns_e_cd7 character varying(7),
    icd_dgns_e_vrsn_cd7 character(1),
    prcdr_dt8 date,
    icd_dgns_cd8 character varying(7),
    icd_dgns_vrsn_cd8 character(1),
    icd_prcdr_cd8 character varying(7),
    icd_prcdr_vrsn_cd8 character(1),
    icd_dgns_e_cd8 character varying(7),
    icd_dgns_e_vrsn_cd8 character(1),
    prcdr_dt9 date,
    icd_dgns_cd9 character varying(7),
    icd_dgns_vrsn_cd9 character(1),
    icd_prcdr_cd9 character varying(7),
    icd_prcdr_vrsn_cd9 character(1),
    icd_dgns_e_cd9 character varying(7),
    icd_dgns_e_vrsn_cd9 character(1),
    prcdr_dt10 date,
    icd_dgns_cd10 character varying(7),
    icd_dgns_vrsn_cd10 character(1),
    icd_prcdr_cd10 character varying(7),
    icd_prcdr_vrsn_cd10 character(1),
    icd_dgns_e_cd10 character varying(7),
    icd_dgns_e_vrsn_cd10 character(1),
    prcdr_dt11 date,
    icd_dgns_cd11 character varying(7),
    icd_dgns_vrsn_cd11 character(1),
    icd_prcdr_cd11 character varying(7),
    icd_prcdr_vrsn_cd11 character(1),
    icd_dgns_e_cd11 character varying(7),
    icd_dgns_e_vrsn_cd11 character(1),
    prcdr_dt12 date,
    icd_dgns_cd12 character varying(7),
    icd_dgns_vrsn_cd12 character(1),
    icd_prcdr_cd12 character varying(7),
    icd_prcdr_vrsn_cd12 character(1),
    icd_dgns_e_cd12 character varying(7),
    icd_dgns_e_vrsn_cd12 character(1),
    prcdr_dt13 date,
    icd_dgns_cd13 character varying(7),
    icd_dgns_vrsn_cd13 character(1),
    icd_prcdr_cd13 character varying(7),
    icd_prcdr_vrsn_cd13 character(1),
    prcdr_dt14 date,
    icd_dgns_cd14 character varying(7),
    icd_dgns_vrsn_cd14 character(1),
    icd_prcdr_cd14 character varying(7),
    icd_prcdr_vrsn_cd14 character(1),
    prcdr_dt15 date,
    icd_dgns_cd15 character varying(7),
    icd_dgns_vrsn_cd15 character(1),
    icd_prcdr_cd15 character varying(7),
    icd_prcdr_vrsn_cd15 character(1),
    prcdr_dt16 date,
    icd_dgns_cd16 character varying(7),
    icd_dgns_vrsn_cd16 character(1),
    icd_prcdr_cd16 character varying(7),
    icd_prcdr_vrsn_cd16 character(1),
    prcdr_dt17 date,
    icd_dgns_cd17 character varying(7),
    icd_dgns_vrsn_cd17 character(1),
    icd_prcdr_cd17 character varying(7),
    icd_prcdr_vrsn_cd17 character(1),
    prcdr_dt18 date,
    icd_dgns_cd18 character varying(7),
    icd_dgns_vrsn_cd18 character(1),
    icd_prcdr_cd18 character varying(7),
    icd_prcdr_vrsn_cd18 character(1),
    prcdr_dt19 date,
    icd_dgns_cd19 character varying(7),
    icd_dgns_vrsn_cd19 character(1),
    icd_prcdr_cd19 character varying(7),
    icd_prcdr_vrsn_cd19 character(1),
    prcdr_dt20 date,
    icd_dgns_cd20 character varying(7),
    icd_dgns_vrsn_cd20 character(1),
    icd_prcdr_cd20 character varying(7),
    icd_prcdr_vrsn_cd20 character(1),
    prcdr_dt21 date,
    icd_dgns_cd21 character varying(7),
    icd_dgns_vrsn_cd21 character(1),
    icd_prcdr_cd21 character varying(7),
    icd_prcdr_vrsn_cd21 character(1),
    prcdr_dt22 date,
    icd_dgns_cd22 character varying(7),
    icd_dgns_vrsn_cd22 character(1),
    icd_prcdr_cd22 character varying(7),
    icd_prcdr_vrsn_cd22 character(1),
    prcdr_dt23 date,
    icd_dgns_cd23 character varying(7),
    icd_dgns_vrsn_cd23 character(1),
    icd_prcdr_cd23 character varying(7),
    icd_prcdr_vrsn_cd23 character(1),
    prcdr_dt24 date,
    icd_dgns_cd24 character varying(7),
    icd_dgns_vrsn_cd24 character(1),
    icd_prcdr_cd24 character varying(7),
    icd_prcdr_vrsn_cd24 character(1),
    prcdr_dt25 date,
    icd_dgns_cd25 character varying(7),
    icd_dgns_vrsn_cd25 character(1),
    icd_prcdr_cd25 character varying(7),
    icd_prcdr_vrsn_cd25 character(1),
    CONSTRAINT outpatient_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT outpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS outpatient_claims_bene_id_idx
	ON ccw.outpatient_claims (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.outpatient_claim_lines (
    clm_id bigint NOT NULL,
    clm_line_num smallint NOT NULL,
    rev_cntr character varying(4) NOT NULL,
    rev_cntr_unit_cnt numeric NOT NULL,
    rev_cntr_rate_amt numeric(12,2) NOT NULL,
    rev_cntr_tot_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_ncvrd_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_coinsrnc_wge_adjstd_c numeric(12,2) NOT NULL,
    rev_cntr_bene_pmt_amt numeric(12,2) NOT NULL,
    rev_cntr_blood_ddctbl_amt numeric(12,2) NOT NULL,
    rev_cntr_cash_ddctbl_amt numeric(12,2) NOT NULL,
    rev_cntr_rdcd_coinsrnc_amt numeric(12,2) NOT NULL,
    rev_cntr_pmt_amt_amt numeric(12,2) NOT NULL,
    rev_cntr_prvdr_pmt_amt numeric(12,2) NOT NULL,
    rev_cntr_dt date,
    rev_cntr_ptnt_rspnsblty_pmt numeric(12,2) NOT NULL,
    rev_cntr_ndc_qty_qlfr_cd character varying(2),
    rev_cntr_ndc_qty numeric,
    rev_cntr_1st_ansi_cd character varying(5),
    rev_cntr_1st_msp_pd_amt numeric(12,2) NOT NULL,
    rev_cntr_2nd_ansi_cd character varying(5),
    rev_cntr_2nd_msp_pd_amt numeric(12,2) NOT NULL,
    rev_cntr_3rd_ansi_cd character varying(5),
    rev_cntr_4th_ansi_cd character varying(5),
    rev_cntr_apc_hipps_cd character varying(5),
    rev_cntr_dscnt_ind_cd character(1),
    rev_cntr_ide_ndc_upc_num character varying(24),
    rev_cntr_otaf_pmt_cd character(1),
    rev_cntr_packg_ind_cd character(1),
    rev_cntr_pmt_mthd_ind_cd character varying(2),
    rev_cntr_stus_ind_cd character varying(2),
    hcpcs_cd character varying(5),
    hcpcs_2nd_mdfr_cd character varying(5),
    hcpcs_1st_mdfr_cd character varying(5),
    rndrng_physn_npi character varying(12),
    rndrng_physn_upin character varying(12),
    CONSTRAINT outpatient_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num),
    CONSTRAINT outpatient_claim_lines_clm_id_to_outpatient_claims FOREIGN KEY (clm_id) REFERENCES ccw.outpatient_claims(clm_id)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.partd_events (
    pde_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    final_action character(1) NOT NULL,
    cmpnd_cd integer NOT NULL,
    drug_cvrg_stus_cd character(1) NOT NULL,
    days_suply_num numeric NOT NULL,
    srvc_dt date NOT NULL,
    pd_dt date,
    fill_num numeric NOT NULL,
    qty_dspnsd_num numeric(10,3) NOT NULL,
    cvrd_d_plan_pd_amt numeric(8,2) NOT NULL,
    gdc_abv_oopt_amt numeric(8,2) NOT NULL,
    gdc_blw_oopt_amt numeric(8,2) NOT NULL,
    lics_amt numeric(8,2) NOT NULL,
    ncvrd_plan_pd_amt numeric(8,2) NOT NULL,
    othr_troop_amt numeric(8,2) NOT NULL,
    plro_amt numeric(8,2) NOT NULL,
    ptnt_pay_amt numeric(8,2) NOT NULL,
    rptd_gap_dscnt_num numeric(8,2) NOT NULL,
    ptnt_rsdnc_cd character varying(2) NOT NULL,
    tot_rx_cst_amt numeric(8,2) NOT NULL,
    daw_prod_slctn_cd character(1) NOT NULL,
    phrmcy_srvc_type_cd character varying(2) NOT NULL,
    plan_cntrct_rec_id character varying(5) NOT NULL,
    plan_pbp_rec_num character varying(3) NOT NULL,
    prod_srvc_id character varying(19) NOT NULL,
    prscrbr_id character varying(15) NOT NULL,
    prscrbr_id_qlfyr_cd character varying(2) NOT NULL,
    rx_srvc_rfrnc_num numeric(12,0) NOT NULL,
    srvc_prvdr_id character varying(15) NOT NULL,
    srvc_prvdr_id_qlfyr_cd character varying(2) NOT NULL,
    adjstmt_dltn_cd character(1),
    brnd_gnrc_cd character(1),
    ctstrphc_cvrg_cd character(1),
    dspnsng_stus_cd character(1),
    nstd_frmt_cd character(1),
    prcng_excptn_cd character(1),
    rx_orgn_cd character(1),
    submsn_clr_cd character varying(2),
    CONSTRAINT partd_events_pkey PRIMARY KEY (pde_id),
    CONSTRAINT partd_events_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS partd_events_bene_id_idx
	ON ccw.partd_events (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.snf_claims (
    clm_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated timestamp with time zone,
    clm_from_dt date NOT NULL,
    clm_thru_dt date NOT NULL,
    claim_query_code character(1) NOT NULL,
    clm_admsn_dt date,
    clm_fac_type_cd character(1) NOT NULL,
    clm_freq_cd character(1) NOT NULL,
    clm_ip_admsn_type_cd character(1) NOT NULL,
    clm_non_utlztn_days_cnt numeric NOT NULL,
    clm_utlztn_day_cnt numeric NOT NULL,
    bene_tot_coinsrnc_days_cnt numeric NOT NULL,
    clm_srvc_clsfctn_type_cd character(1) NOT NULL,
    final_action character(1) NOT NULL,
    clm_tot_chrg_amt numeric(12,2) NOT NULL,
    clm_pmt_amt numeric(12,2) NOT NULL,
    clm_pps_ind_cd character(1),
    clm_pps_cptl_dsprprtnt_shr_amt numeric(12,2),
    clm_pps_cptl_excptn_amt numeric(12,2),
    clm_pps_cptl_fsp_amt numeric(12,2),
    clm_pps_cptl_ime_amt numeric(12,2),
    clm_pps_cptl_outlier_amt numeric(12,2),
    clm_pps_old_cptl_hld_hrmls_amt numeric(12,2),
    nch_clm_type_cd character varying(2) NOT NULL,
    nch_near_line_rec_ident_cd character(1) NOT NULL,
    nch_wkly_proc_dt date NOT NULL,
    nch_blood_pnts_frnshd_qty numeric NOT NULL,
    nch_prmry_pyr_cd character(1),
    nch_prmry_pyr_clm_pd_amt numeric(12,2) NOT NULL,
    nch_bene_blood_ddctbl_lblty_am numeric(12,2) NOT NULL,
    nch_bene_ip_ddctbl_amt numeric(12,2) NOT NULL,
    nch_bene_pta_coinsrnc_lblty_am numeric(12,2) NOT NULL,
    nch_ip_ncvrd_chrg_amt numeric(12,2) NOT NULL,
    nch_ip_tot_ddctn_amt numeric(12,2) NOT NULL,
    nch_vrfd_ncvrd_stay_from_dt date,
    nch_vrfd_ncvrd_stay_thru_dt date,
    nch_qlfyd_stay_from_dt date,
    nch_qlfyd_stay_thru_dt date,
    nch_actv_or_cvrd_lvl_care_thru date,
    nch_bene_dschrg_dt date,
    nch_bene_mdcr_bnfts_exhtd_dt_i date,
    nch_ptnt_status_ind_cd character(1),
    clm_drg_cd character varying(4),
    clm_mco_pd_sw character(1),
    clm_mdcr_non_pmt_rsn_cd character varying(2),
    clm_src_ip_admsn_cd character(1),
    admtg_dgns_cd character varying(7),
    admtg_dgns_vrsn_cd character(1),
    at_physn_npi character varying(10),
    at_physn_upin character varying(9),
    op_physn_npi character varying(10),
    op_physn_upin character varying(9),
    org_npi_num character varying(10),
    ot_physn_npi character varying(10),
    ot_physn_upin character varying(9),
    prncpal_dgns_cd character varying(7),
    prncpal_dgns_vrsn_cd character(1),
    prvdr_num character varying(9) NOT NULL,
    prvdr_state_cd character varying(2) NOT NULL,
    ptnt_dschrg_stus_cd character varying(2) NOT NULL,
    fi_clm_actn_cd character(1),
    fi_clm_proc_dt date,
    fi_doc_clm_cntl_num character varying(23),
    fi_num character varying(5),
    fi_orig_clm_cntl_num character varying(23),
    fst_dgns_e_cd character varying(7),
    fst_dgns_e_vrsn_cd character(1),
    icd_dgns_cd1 character varying(7),
    icd_dgns_cd2 character varying(7),
    icd_dgns_cd3 character varying(7),
    icd_dgns_cd4 character varying(7),
    icd_dgns_cd5 character varying(7),
    icd_dgns_cd6 character varying(7),
    icd_dgns_cd7 character varying(7),
    icd_dgns_cd8 character varying(7),
    icd_dgns_cd9 character varying(7),
    icd_dgns_cd10 character varying(7),
    icd_dgns_cd11 character varying(7),
    icd_dgns_cd12 character varying(7),
    icd_dgns_cd13 character varying(7),
    icd_dgns_cd14 character varying(7),
    icd_dgns_cd15 character varying(7),
    icd_dgns_cd16 character varying(7),
    icd_dgns_cd17 character varying(7),
    icd_dgns_cd18 character varying(7),
    icd_dgns_cd19 character varying(7),
    icd_dgns_cd20 character varying(7),
    icd_dgns_cd21 character varying(7),
    icd_dgns_cd22 character varying(7),
    icd_dgns_cd23 character varying(7),
    icd_dgns_cd24 character varying(7),
    icd_dgns_cd25 character varying(7),
    icd_dgns_e_cd1 character varying(7),
    icd_dgns_e_cd2 character varying(7),
    icd_dgns_e_cd3 character varying(7),
    icd_dgns_e_cd4 character varying(7),
    icd_dgns_e_cd5 character varying(7),
    icd_dgns_e_cd6 character varying(7),
    icd_dgns_e_cd7 character varying(7),
    icd_dgns_e_cd8 character varying(7),
    icd_dgns_e_cd9 character varying(7),
    icd_dgns_e_cd10 character varying(7),
    icd_dgns_e_cd11 character varying(7),
    icd_dgns_e_cd12 character varying(7),
    icd_dgns_e_vrsn_cd1 character(1),
    icd_dgns_e_vrsn_cd2 character(1),
    icd_dgns_e_vrsn_cd3 character(1),
    icd_dgns_e_vrsn_cd4 character(1),
    icd_dgns_e_vrsn_cd5 character(1),
    icd_dgns_e_vrsn_cd6 character(1),
    icd_dgns_e_vrsn_cd7 character(1),
    icd_dgns_e_vrsn_cd8 character(1),
    icd_dgns_e_vrsn_cd9 character(1),
    icd_dgns_e_vrsn_cd10 character(1),
    icd_dgns_e_vrsn_cd11 character(1),
    icd_dgns_e_vrsn_cd12 character(1),
    icd_dgns_vrsn_cd1 character(1),
    icd_dgns_vrsn_cd2 character(1),
    icd_dgns_vrsn_cd3 character(1),
    icd_dgns_vrsn_cd4 character(1),
    icd_dgns_vrsn_cd5 character(1),
    icd_dgns_vrsn_cd6 character(1),
    icd_dgns_vrsn_cd7 character(1),
    icd_dgns_vrsn_cd8 character(1),
    icd_dgns_vrsn_cd9 character(1),
    icd_dgns_vrsn_cd10 character(1),
    icd_dgns_vrsn_cd11 character(1),
    icd_dgns_vrsn_cd12 character(1),
    icd_dgns_vrsn_cd13 character(1),
    icd_dgns_vrsn_cd14 character(1),
    icd_dgns_vrsn_cd15 character(1),
    icd_dgns_vrsn_cd16 character(1),
    icd_dgns_vrsn_cd17 character(1),
    icd_dgns_vrsn_cd18 character(1),
    icd_dgns_vrsn_cd19 character(1),
    icd_dgns_vrsn_cd20 character(1),
    icd_dgns_vrsn_cd21 character(1),
    icd_dgns_vrsn_cd22 character(1),
    icd_dgns_vrsn_cd23 character(1),
    icd_dgns_vrsn_cd24 character(1),
    icd_dgns_vrsn_cd25 character(1),
    icd_prcdr_cd1 character varying(7),
    icd_prcdr_cd2 character varying(7),
    icd_prcdr_cd3 character varying(7),
    icd_prcdr_cd4 character varying(7),
    icd_prcdr_cd5 character varying(7),
    icd_prcdr_cd6 character varying(7),
    icd_prcdr_cd7 character varying(7),
    icd_prcdr_cd8 character varying(7),
    icd_prcdr_cd9 character varying(7),
    icd_prcdr_cd10 character varying(7),
    icd_prcdr_cd11 character varying(7),
    icd_prcdr_cd12 character varying(7),
    icd_prcdr_cd13 character varying(7),
    icd_prcdr_cd14 character varying(7),
    icd_prcdr_cd15 character varying(7),
    icd_prcdr_cd16 character varying(7),
    icd_prcdr_cd17 character varying(7),
    icd_prcdr_cd18 character varying(7),
    icd_prcdr_cd19 character varying(7),
    icd_prcdr_cd20 character varying(7),
    icd_prcdr_cd21 character varying(7),
    icd_prcdr_cd22 character varying(7),
    icd_prcdr_cd23 character varying(7),
    icd_prcdr_cd24 character varying(7),
    icd_prcdr_cd25 character varying(7),
    icd_prcdr_vrsn_cd1 character(1),
    icd_prcdr_vrsn_cd2 character(1),
    icd_prcdr_vrsn_cd3 character(1),
    icd_prcdr_vrsn_cd4 character(1),
    icd_prcdr_vrsn_cd5 character(1),
    icd_prcdr_vrsn_cd6 character(1),
    icd_prcdr_vrsn_cd7 character(1),
    icd_prcdr_vrsn_cd8 character(1),
    icd_prcdr_vrsn_cd9 character(1),
    icd_prcdr_vrsn_cd10 character(1),
    icd_prcdr_vrsn_cd11 character(1),
    icd_prcdr_vrsn_cd12 character(1),
    icd_prcdr_vrsn_cd13 character(1),
    icd_prcdr_vrsn_cd14 character(1),
    icd_prcdr_vrsn_cd15 character(1),
    icd_prcdr_vrsn_cd16 character(1),
    icd_prcdr_vrsn_cd17 character(1),
    icd_prcdr_vrsn_cd18 character(1),
    icd_prcdr_vrsn_cd19 character(1),
    icd_prcdr_vrsn_cd20 character(1),
    icd_prcdr_vrsn_cd21 character(1),
    icd_prcdr_vrsn_cd22 character(1),
    icd_prcdr_vrsn_cd23 character(1),
    icd_prcdr_vrsn_cd24 character(1),
    icd_prcdr_vrsn_cd25 character(1),
    prcdr_dt1 date,
    prcdr_dt2 date,
    prcdr_dt3 date,
    prcdr_dt4 date,
    prcdr_dt5 date,
    prcdr_dt6 date,
    prcdr_dt7 date,
    prcdr_dt8 date,
    prcdr_dt9 date,
    prcdr_dt10 date,
    prcdr_dt11 date,
    prcdr_dt12 date,
    prcdr_dt13 date,
    prcdr_dt14 date,
    prcdr_dt15 date,
    prcdr_dt16 date,
    prcdr_dt17 date,
    prcdr_dt18 date,
    prcdr_dt19 date,
    prcdr_dt20 date,
    prcdr_dt21 date,
    prcdr_dt22 date,
    prcdr_dt23 date,
    prcdr_dt24 date,
    prcdr_dt25 date,
    CONSTRAINT snf_claims_pkey PRIMARY KEY (clm_id),
    CONSTRAINT snf_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES ccw.beneficiaries(bene_id)
);

CREATE INDEX IF NOT EXISTS snf_claims_bene_id_idx
	ON ccw.snf_claims (bene_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.snf_claim_lines (
    clm_id bigint NOT NULL,
    clm_line_num smallint NOT NULL,
    rev_cntr character varying(4) NOT NULL,
    rev_cntr_unit_cnt integer NOT NULL,
    rev_cntr_tot_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_rate_amt numeric(12,2) NOT NULL,
    rev_cntr_ncvrd_chrg_amt numeric(12,2) NOT NULL,
    rev_cntr_ddctbl_coinsrnc_cd character(1),
    rev_cntr_ndc_qty_qlfr_cd character varying(2),
    rev_cntr_ndc_qty numeric,
    hcpcs_cd character varying(5),
    rndrng_physn_npi character varying(12),
    rndrng_physn_upin character varying(12),
    CONSTRAINT snf_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num),
    CONSTRAINT snf_claim_lines_clm_id_to_snf_claims FOREIGN KEY (clm_id) REFERENCES ccw.snf_claims(clm_id)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS ccw.loaded_files (
    loaded_file_id bigint NOT NULL,
    rif_type character varying(48) NOT NULL,
    created timestamp with time zone NOT NULL,
    s3_manifest_id bigint,
    s3_file_index smallint,
    CONSTRAINT loaded_files_pkey PRIMARY KEY (loaded_file_id)
);

CREATE TABLE IF NOT EXISTS ccw.loaded_batches (
    loaded_batch_id bigint NOT NULL,
    loaded_file_id bigint NOT NULL,
    beneficiaries character varying(20000) NOT NULL,
    created timestamp with time zone NOT NULL,
    CONSTRAINT loaded_batches_pkey PRIMARY KEY (loaded_batch_id),
    CONSTRAINT loaded_batches_loaded_file_id FOREIGN KEY (loaded_file_id) REFERENCES ccw.loaded_files(loaded_file_id)
);

CREATE INDEX IF NOT EXISTS loaded_batches_created_idx
    ON ccw.loaded_batches (created);
    
CREATE INDEX IF NOT EXISTS loaded_batches_loaded_file_id_idx
    ON ccw.loaded_batches (loaded_file_id DESC);
    
CREATE SEQUENCE IF NOT EXISTS ccw.loadedbatches_loadedbatchid_seq
    START WITH 1 INCREMENT BY 20 NO MINVALUE NO MAXVALUE CYCLE;

CREATE SEQUENCE IF NOT EXISTS ccw.loadedfiles_loadedfileid_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CYCLE;

-- =============================================
-- Start RDA db artifacts
-- =============================================

CREATE SCHEMA IF NOT EXISTS rda;

ALTER SCHEMA rda OWNER TO svc_fhirdb_migrator;

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.mbi_cache (
    mbi_id bigint NOT NULL GENERATED BY DEFAULT AS IDENTITY (
        SEQUENCE NAME rda.mbi_cache_mbi_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1),
    mbi character varying(11) NOT NULL,
    hash character varying(64) NOT NULL,
    old_hash character varying(64),
    last_updated timestamp with time zone,
    CONSTRAINT mbi_cache_pkey PRIMARY KEY (mbi_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS mbi_cache_hash_idx ON rda.mbi_cache (hash);

CREATE INDEX IF NOT EXISTS mbi_cache_mbi_idx ON rda.mbi_cache (mbi);

CREATE INDEX IF NOT EXISTS mbi_cache_old_hash_idx ON rda.mbi_cache (old_hash);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.claim_message_meta_data (
    claim_type character(1) NOT NULL,
    sequence_number bigint NOT NULL,
    claim_id character varying(43) NOT NULL,
    mbi_id bigint,
    claim_state character varying(1),
    transaction_date date,
    claim_location json,
    last_updated timestamp with time zone NOT NULL,
    phase smallint,
    phase_seq_num smallint,
    extract_date date,
    transmission_timestamp timestamp with time zone,
    CONSTRAINT claim_message_meta_data_pkey PRIMARY KEY (claim_type, sequence_number),
    CONSTRAINT claim_message_meta_data_mbi FOREIGN KEY (mbi_id) REFERENCES rda.mbi_cache(mbi_id)

);

CREATE INDEX IF NOT EXISTS claim_message_meta_data_last_updated_idx
	ON rda.claim_message_meta_data (last_updated);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.fiss_audit_trails (
    claim_id character varying(43) NOT NULL,
    rda_position smallint NOT NULL,
    badt_status character varying(1),
    badt_loc character varying(5),
    badt_oper_id character varying(9),
    badt_reas character varying(5),
    badt_curr_date date,
    CONSTRAINT fiss_audit_trails_key PRIMARY KEY (claim_id, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.fiss_claims (
    claim_id character varying(43) NOT NULL,
    hic_no character varying(12) NOT NULL,
    curr_status character(1) NOT NULL,
    curr_loc1 character(1) NOT NULL,
    curr_loc2 character varying(5) NOT NULL,
    meda_prov_id character varying(13),
    total_charge_amount numeric(11,2),
    received_date date,
    curr_tran_date date,
    admit_diag_code character varying(7),
    principle_diag character varying(7),
    npi_number character varying(10),
    fed_tax_number character varying(10),
    last_updated timestamp with time zone,
    prac_loc_addr1 text,
    prac_loc_addr2 text,
    prac_loc_city text,
    prac_loc_state character varying(2),
    prac_loc_zip character varying(15),
    meda_prov_6 character varying(6),
    stmt_cov_from_date date,
    stmt_cov_to_date date,
    lob_cd character varying(1),
    serv_type_cd_mapping character varying(20),
    serv_type_cd character varying(1),
    freq_cd character varying(1),
    bill_typ_cd character varying(3),
    sequence_number bigint NOT NULL,
    api_source character varying(24),
    reject_cd character varying(5),
    full_part_den_ind character varying(1),
    non_pay_ind character varying(2),
    xref_dcn_nbr character varying(23),
    adj_req_cd character varying(1),
    adj_reas_cd character varying(2),
    cancel_xref_dcn character varying(23),
    cancel_date date,
    canc_adj_cd character varying(1),
    original_xref_dcn character varying(23),
    paid_dt date,
    adm_date date,
    adm_source character varying(1),
    primary_payer_code character varying(1),
    attend_phys_id character varying(16),
    attend_phys_lname character varying(17),
    attend_phys_fname character varying(18),
    attend_phys_mint character varying(1),
    attend_phys_flag character varying(1),
    operating_phys_id character varying(16),
    oper_phys_lname character varying(17),
    oper_phys_fname character varying(18),
    oper_phys_mint character varying(1),
    oper_phys_flag character varying(1),
    oth_phys_id character varying(16),
    oth_phys_lname character varying(17),
    oth_phys_fname character varying(18),
    oth_phys_mint character varying(1),
    oth_phys_flag character varying(1),
    xref_hic_nbr character varying(12),
    proc_new_hic_ind character varying(1),
    new_hic character varying(12),
    repos_ind character varying(1),
    repos_hic character varying(12),
    mbi_subm_bene_ind character varying(1),
    adj_mbi_ind character varying(1),
    adj_mbi character varying(11),
    medical_record_no character varying(17),
    prov_state_cd character varying(2),
    prov_typ_facil_cd character varying(1),
    prov_emer_ind character varying(1),
    prov_dept_id character varying(3),
    mbi_id bigint,
    adm_date_text character varying(10),
    stmt_cov_from_date_text character varying(10),
    stmt_cov_to_date_text character varying(10),
    received_date_text character varying(10),
    curr_tran_date_text character varying(10),
    drg_cd character varying(4),
    group_code character varying(2),
    clm_typ_ind character varying(1),
    dcn character varying(23) NOT NULL,
    intermediary_nb character varying(5) NOT NULL,
    CONSTRAINT fiss_claims_pkey PRIMARY KEY (claim_id)
);

CREATE INDEX IF NOT EXISTS fiss_claims_last_updated_idx ON rda.fiss_claims (last_updated);

CREATE INDEX IF NOT EXISTS fiss_claims_mbi_id_idx ON rda.fiss_claims (mbi_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.fiss_diagnosis_codes (
    claim_id character varying(43) NOT NULL,
    rda_position smallint NOT NULL,
    diag_cd2 character varying(7),
    diag_poa_ind character varying(1),
    bit_flags character varying(4),
    CONSTRAINT fiss_diagnosis_codes_key PRIMARY KEY (claim_id, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.fiss_payers (
    claim_id character varying(43) NOT NULL,
    rda_position smallint NOT NULL,
    payer_type character varying(20),
    payers_id character varying(1),
    payers_name character varying(32),
    rel_ind character varying(1),
    assign_ind character varying(1),
    provider_number character varying(13),
    adj_dcn_icn character varying(23),
    prior_pmt numeric(11,2),
    est_amt_due numeric(11,2),
    bene_rel character varying(2),
    bene_last_name character varying(15),
    bene_first_name character varying(10),
    bene_mid_init character varying(1),
    bene_ssn_hic character varying(19),
    insured_rel character varying(2),
    insured_name character varying(25),
    insured_ssn_hic character varying(19),
    insured_group_name character varying(17),
    insured_group_nbr character varying(20),
    bene_dob date,
    bene_sex character varying(1),
    treat_auth_cd character varying(18),
    insured_sex character varying(1),
    insured_rel_x12 character varying(2),
    insured_dob date,
    insured_dob_text character varying(9),
    CONSTRAINT fiss_payers_key PRIMARY KEY (claim_id, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.fiss_proc_codes (
    claim_id character varying(43) NOT NULL,
    rda_position smallint NOT NULL,
    proc_code character varying(10) NOT NULL,
    proc_flag character varying(4),
    proc_date date,
    CONSTRAINT fiss_proc_codes_key PRIMARY KEY (claim_id, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.fiss_revenue_lines (
    claim_id character varying(43) NOT NULL,
    rda_position smallint NOT NULL,
    non_bill_rev_code character varying(1),
    rev_cd character varying(4),
    rev_units_billed integer,
    rev_serv_unit_cnt integer,
    serv_dt_cymd date,
    serv_dt_cymd_text character varying(10),
    hcpc_cd character varying(5),
    hcpc_ind character varying(1),
    hcpc_modifier character varying(2),
    hcpc_modifier2 character varying(2),
    hcpc_modifier3 character varying(2),
    hcpc_modifier4 character varying(2),
    hcpc_modifier5 character varying(2),
    apc_hcpcs_apc character varying(5),
    aco_red_rarc character varying(5),
    aco_red_carc character varying(3),
    aco_red_cagc character varying(2),
    CONSTRAINT fiss_revenue_lines_key PRIMARY KEY (claim_id, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.mcs_adjustments (
    idr_clm_hd_icn character varying(15) NOT NULL,
    rda_position smallint NOT NULL,
    idr_adj_date date,
    idr_xref_icn character varying(15),
    idr_adj_clerk character varying(4),
    idr_init_ccn character varying(15),
    idr_adj_chk_wrt_dt date,
    idr_adj_b_eomb_amt numeric(7,2),
    idr_adj_p_eomb_amt numeric(7,2),
    CONSTRAINT mcs_adjustments_key PRIMARY KEY (idr_clm_hd_icn, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS  rda.mcs_audits (
    idr_clm_hd_icn character varying(15) NOT NULL,
    rda_position smallint NOT NULL,
    idr_j_audit_num integer,
    idr_j_audit_ind character varying(1),
    idr_j_audit_disp character varying(1),
    CONSTRAINT mcs_audits_key PRIMARY KEY (idr_clm_hd_icn, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.mcs_claims (
    idr_clm_hd_icn character varying(15) NOT NULL,
    idr_contr_id character varying(5) NOT NULL,
    idr_hic character varying(12),
    idr_claim_type character varying(1) NOT NULL,
    idr_dtl_cnt integer,
    idr_bene_last_1_6 character varying(6),
    idr_bene_first_init character varying(1),
    idr_bene_mid_init character varying(1),
    idr_bene_sex character varying(1),
    idr_status_code character varying(1),
    idr_status_date date,
    idr_bill_prov_npi character varying(10),
    idr_bill_prov_num character varying(10),
    idr_bill_prov_ein character varying(10),
    idr_bill_prov_type character varying(2),
    idr_bill_prov_spec character varying(2),
    idr_bill_prov_group_ind character varying(1),
    idr_bill_prov_price_spec character varying(2),
    idr_bill_prov_county character varying(2),
    idr_bill_prov_loc character varying(2),
    idr_tot_allowed numeric(7,2),
    idr_coinsurance numeric(7,2),
    idr_deductible numeric(7,2),
    idr_bill_prov_status_cd character varying(1),
    idr_tot_billed_amt numeric(7,2),
    idr_claim_receipt_date date,
    idr_hdr_from_date_of_svc date,
    idr_hdr_to_date_of_svc date,
    last_updated timestamp with time zone,
    sequence_number bigint NOT NULL,
    api_source character varying(24),
    idr_assignment character varying(1),
    idr_clm_level_ind character varying(1),
    idr_hdr_audit integer,
    idr_hdr_audit_ind character varying(1),
    idr_u_split_reason character varying(1),
    idr_j_referring_prov_npi character varying(10),
    idr_j_fac_prov_npi character varying(10),
    idr_u_demo_prov_npi character varying(10),
    idr_u_super_npi character varying(10),
    idr_u_fcadj_bil_npi character varying(10),
    idr_amb_pickup_addres_line1 character varying(25),
    idr_amb_pickup_addres_line2 character varying(20),
    idr_amb_pickup_city character varying(20),
    idr_amb_pickup_state character varying(2),
    idr_amb_pickup_zipcode character varying(9),
    idr_amb_dropoff_name character varying(24),
    idr_amb_dropoff_addr_line1 character varying(25),
    idr_amb_dropoff_addr_line2 character varying(20),
    idr_amb_dropoff_city character varying(20),
    idr_amb_dropoff_state character varying(2),
    idr_amb_dropoff_zipcode character varying(9),
    mbi_id bigint,
    CONSTRAINT mcs_claims_pkey PRIMARY KEY (idr_clm_hd_icn)
);

CREATE INDEX IF NOT EXISTS mcs_claims_last_updated_idx ON rda.mcs_claims (last_updated);

CREATE INDEX IF NOT EXISTS mcs_claims_mbi_id_idx ON rda.mcs_claims (mbi_id);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.mcs_details (
    idr_clm_hd_icn character varying(15) NOT NULL,
    idr_dtl_number smallint NOT NULL,
    idr_dtl_status character varying(1),
    idr_dtl_from_date date,
    idr_dtl_to_date date,
    idr_proc_code character varying(5),
    idr_mod_one character varying(2),
    idr_mod_two character varying(2),
    idr_mod_three character varying(2),
    idr_mod_four character varying(2),
    idr_dtl_diag_icd_type character varying(1),
    idr_dtl_primary_diag_code character varying(7),
    idr_k_pos_lname_org character varying(60),
    idr_k_pos_fname character varying(35),
    idr_k_pos_mname character varying(25),
    idr_k_pos_addr1 character varying(55),
    idr_k_pos_addr2_1st character varying(30),
    idr_k_pos_addr2_2nd character varying(25),
    idr_k_pos_city character varying(30),
    idr_k_pos_state character varying(2),
    idr_k_pos_zip character varying(15),
    idr_tos character varying(1),
    idr_two_digit_pos character varying(2),
    idr_dtl_rend_type character varying(2),
    idr_dtl_rend_spec character varying(2),
    idr_dtl_rend_npi character varying(10),
    idr_dtl_rend_prov character varying(10),
    idr_k_dtl_fac_prov_npi character varying(10),
    idr_dtl_amb_pickup_addres1 character varying(25),
    idr_dtl_amb_pickup_addres2 character varying(20),
    idr_dtl_amb_pickup_city character varying(20),
    idr_dtl_amb_pickup_state character varying(2),
    idr_dtl_amb_pickup_zipcode character varying(9),
    idr_dtl_amb_dropoff_name character varying(24),
    idr_dtl_amb_dropoff_addr_l1 character varying(25),
    idr_dtl_amb_dropoff_addr_l2 character varying(20),
    idr_dtl_amb_dropoff_city character varying(20),
    idr_dtl_amb_dropoff_state character varying(2),
    idr_dtl_amb_dropoff_zipcode character varying(9),
    CONSTRAINT mcs_details_key PRIMARY KEY (idr_clm_hd_icn, idr_dtl_number)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.mcs_diagnosis_codes (
    idr_clm_hd_icn character varying(15) NOT NULL,
    rda_position smallint NOT NULL,
    idr_diag_icd_type character varying(1),
    idr_diag_code character varying(7) NOT NULL,
    CONSTRAINT mcs_diagnosis_codes_key PRIMARY KEY (idr_clm_hd_icn, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.mcs_locations (
    idr_clm_hd_icn character varying(15) NOT NULL,
    rda_position smallint NOT NULL,
    idr_loc_clerk character varying(4),
    idr_loc_code character varying(3),
    idr_loc_date date,
    idr_loc_actv_code character varying(1),
    CONSTRAINT mcs_locations_key PRIMARY KEY (idr_clm_hd_icn, rda_position)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.message_errors (
    sequence_number bigint NOT NULL,
    claim_type character varying(20) NOT NULL,
    claim_id character varying(43) NOT NULL,
    api_source character varying(24) NOT NULL,
    created_date timestamp with time zone NOT NULL,
    updated_date timestamp with time zone NOT NULL,
    errors jsonb NOT NULL,
    message jsonb NOT NULL,
    status character varying(20) DEFAULT 'UNRESOLVED'::character varying NOT NULL,
    CONSTRAINT message_errors_pkey PRIMARY KEY (sequence_number, claim_type)
);

-- =====================================================

CREATE TABLE IF NOT EXISTS rda.rda_api_progress (
    claim_type character varying(20) NOT NULL,
    last_sequence_number bigint NOT NULL,
    last_updated timestamp with time zone,
    CONSTRAINT rda_api_progress_pkey PRIMARY KEY (claim_type)
);

-- =============================================

CREATE OR REPLACE FUNCTION ccw.check_claims_mask(v_bene_id bigint) RETURNS integer
    LANGUAGE plpgsql
    AS $func$
 DECLARE
    v_rslt           integer  := 0;       
                                     -- Java definitions
    V_CARRIER        integer  := 1;    -- public static final int V_CARRIER_HAS_DATA     = (1 << 0);
    V_INPATIENT      integer  := 2;    -- public static final int V_INPATIENT_HAS_DATA   = (1 << 1);
    V_OUTPATIENT     integer  := 4;    -- public static final int V_OUTPATIENT_HAS_DATA  = (1 << 2);
    V_SNF            integer  := 8;    -- public static final int V_SNF_HAS_DATA         = (1 << 3);
    V_DME            integer  := 16;   -- public static final int V_DME_HAS_DATA         = (1 << 4);
    V_HHA            integer  := 32;   -- public static final int V_HHA_HAS_DATA         = (1 << 5);
    V_HOSPICE        integer  := 64;   -- public static final int V_HOSPICE_HAS_DATA     = (1 << 6);
    V_PART_D         integer  := 128;  -- public static final int V_PART_D_HAS_DATA      = (1 << 7);
 BEGIN
   PERFORM 1 FROM carrier_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = V_CARRIER;
   END IF;
   PERFORM 1 FROM inpatient_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_INPATIENT;
   END IF;
   PERFORM 1 FROM outpatient_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_OUTPATIENT;
   END IF;
   PERFORM 1 FROM snf_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_SNF;
   END IF;
   PERFORM 1 FROM dme_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_DME;
   END IF;
   PERFORM 1 FROM hha_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_HHA;
   END IF;
   PERFORM 1 FROM hospice_claims WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_HOSPICE;
   END IF;
   PERFORM 1 FROM partd_events WHERE bene_id = v_bene_id limit 1;
   IF FOUND THEN
     v_rslt = v_rslt + V_PART_D;
   END IF;
   RETURN v_rslt;
  END;
$func$;

GRANT EXECUTE ON FUNCTION ccw.check_claims_mask(v_bene_id bigint)  TO bfd_reader_role;

-- =====================================================

CREATE OR REPLACE FUNCTION ccw.track_bene_monthly_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $func$
     BEGIN
        IF (TG_OP = 'UPDATE') THEN
            INSERT INTO ccw.beneficiary_monthly_audit VALUES (OLD.*, 'U', now(), nextval('ccw.bene_monthly_audit_seq'));
            RETURN NEW;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
     END;
 $func$;

GRANT EXECUTE ON FUNCTION ccw.check_claims_mask(v_bene_id bigint)  TO bfd_writer_role;

CREATE OR REPLACE TRIGGER audit_ccw_update
	AFTER UPDATE ON ccw.beneficiary_monthly
	FOR EACH ROW
		WHEN (((((old.partd_contract_number_id)::text <> '0'::text)
		OR ((old.partc_contract_number_id)::text <> '0'::text))
		AND (((((((((((((old.partd_contract_number_id)::text IS DISTINCT FROM (new.partd_contract_number_id)::text)
		OR ((old.partc_contract_number_id)::text IS DISTINCT FROM (new.partc_contract_number_id)::text))
		OR ((old.medicare_status_code)::text IS DISTINCT FROM (new.medicare_status_code)::text))
		OR ((old.fips_state_cnty_code)::text IS DISTINCT FROM (new.fips_state_cnty_code)::text))
		OR (old.entitlement_buy_in_ind IS DISTINCT FROM new.entitlement_buy_in_ind))
		OR (old.hmo_indicator_ind IS DISTINCT FROM new.hmo_indicator_ind))
		OR ((old.medicaid_dual_eligibility_code)::text IS DISTINCT FROM (new.medicaid_dual_eligibility_code)::text))
		OR ((old.partd_pbp_number_id)::text IS DISTINCT FROM (new.partd_pbp_number_id)::text))
		OR ((old.partd_segment_number_id)::text IS DISTINCT FROM (new.partd_segment_number_id)::text))
		OR ((old.partd_low_income_cost_share_group_code)::text IS DISTINCT FROM (new.partd_low_income_cost_share_group_code)::text)) 
		OR ((old.partc_pbp_number_id)::text IS DISTINCT FROM (new.partc_pbp_number_id)::text))
		OR ((old.partc_plan_type_code)::text IS DISTINCT FROM (new.partc_plan_type_code)::text))))
			EXECUTE FUNCTION ccw.track_bene_monthly_change();

--GRANT EXECUTE ON TRIGGER audit_ccw_update TO bfd_writer_role;

--
-- Name: find_beneficiary(text, text); Type: FUNCTION; Schema: ccw; Owner: svc_fhirdb_migrator
--
CREATE OR REPLACE FUNCTION ccw.find_beneficiary(p_type text, p_value text) RETURNS text
    LANGUAGE plpgsql
    AS $func$
DECLARE
	v_rslt           text;
	v_type           text   := lower(p_type);
BEGIN
	IF v_type = 'mbi' THEN
		SELECT string_agg(a.bene_id::text, ',') INTO v_rslt
		FROM beneficiaries a,
		(
			select distinct b.bene_id from beneficiaries_history b
			where b.mbi_num = p_value
		) t1
		where a.mbi_num = p_value
		or a.bene_id = t1.bene_id;
	
	ELSIF v_type = 'mbi-hash' THEN
		SELECT string_agg(a.bene_id::text, ',') INTO v_rslt
		FROM beneficiaries a,
		(
			select distinct b.bene_id from beneficiaries_history b
			where b.mbi_hash = p_value
		) t1
		where a.mbi_hash = p_value
		or a.bene_id = t1.bene_id;
	
	ELSIF v_type = 'hicn-hash' THEN
		SELECT string_agg(a.bene_id::text, ',') INTO v_rslt
		FROM beneficiaries a,
		(
			select distinct b.bene_id from beneficiaries_history b
			where b.bene_crnt_hic_num = p_value
		) t1
		where a.bene_crnt_hic_num = p_value
		or a.bene_id = t1.bene_id;
	END IF;
	RETURN v_rslt;
END;
$func$;

GRANT EXECUTE ON FUNCTION ccw.find_beneficiary(p_type text, p_value text) TO bfd_reader_role;

--
-- Name: pg_idfromdate(text, text, timestamp without time zone); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_idfromdate(text, text, timestamp without time zone) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    date_column_name ALIAS FOR $2;
    timestamp_needed ALIAS FOR $3;
    timestamp_needed_epoch bigint;
    id_low bigint;
    id_high bigint;
    id_middle bigint;
    id_middle_date bigint;
    
BEGIN
    -- Fetch Min ID
    SELECT pg_IFD_MIN_ID(table_name) INTO id_low;

    -- Fetch Max ID
    SELECT pg_IFD_MAX_ID(table_name) INTO id_high;
      
    -- Convert requested date to epoch
    SELECT round(EXTRACT(EPOCH FROM timestamp_needed)) INTO timestamp_needed_epoch;
    -- RAISE NOTICE 'Date requested: %', timestamp_needed;
    -- RAISE NOTICE 'Date requested converted to epoch : %', timestamp_needed_epoch;

    LOOP
        -- Get middle ID between low ID and high ID
        SELECT pg_IFD_MIDDLE_ID(id_low, id_high) INTO id_middle;
        -- RAISE NOTICE 'Middle ID: %', id_middle;

        -- Search Date for current ID
        SELECT pg_IFD_EPOCH_FROM_ID(table_name, date_column_name, id_middle, '='::text) INTO id_middle_date;

        -- If date found is null, we will search a date close from this ID
        IF id_middle_date IS NULL THEN
            -- RAISE NOTICE 'NULL Value found for ROW # %', id_middle;
            SELECT pg_IFD_EPOCH_FROM_ID(table_name, date_column_name, id_middle, '>='::text) INTO id_middle_date;
        END IF;

        -- If We were not able to find a valid ID close to this row
        IF id_middle_date IS NULL THEN
            -- RAISE NOTICE 'NULL Value also found for rows close to ROW # %', id_middle;
            RETURN NULL;
        END IF;
        
        -- Shift ID high or ID low to eliminate all rows not needed anymore
        IF id_middle_date = timestamp_needed_epoch THEN -- Perfect match
		    RAISE NOTICE 'Perfect match: %', id_middle;
            RETURN id_middle;
        ELSEIF id_middle_date > timestamp_needed_epoch THEN -- Date of middle ID is greater than date needed
			RAISE NOTICE 'Date Middle >=: %', id_middle;
            SELECT id_middle INTO id_high;
        ELSEIF id_middle_date < timestamp_needed_epoch THEN -- Date of middle ID is lesser than date needed
			RAISE NOTICE 'Date Middle <=: %', id_middle;
            SELECT id_middle INTO id_low;
        ELSE
            RETURN NULL;
        END IF;
        
        -- Notice
        -- RAISE NOTICE 'Shift search to: id_low=% & id_high=%', id_low, id_high;

        -- If there is less than 50 IDs between id_low and id_high
        -- We will make one query to search between these IDs and find the row with the closest date from the date needed
        IF id_high - id_low <= 50 THEN
            SELECT pg_IFD_FETCH_ROW_FROM_SMALL_RANGE(table_name, date_column_name, id_low, id_high, timestamp_needed) INTO id_middle;
            RETURN id_middle;
        END IF;
    END LOOP;

    RETURN 1;
END;
$func$;

--
-- Name: pg_ifd_epoch_from_id(text, text, bigint, text); Type: FUNCTION; Schema: ccw; Owner: bfduser
--
CREATE OR REPLACE FUNCTION public.pg_ifd_epoch_from_id(text, text, bigint, text) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    date_column_name ALIAS FOR $2;
    current_id ALIAS FOR $3;
    operator ALIAS FOR $4;
    timesp bigint;
BEGIN
   EXECUTE ' SELECT round(EXTRACT(EPOCH FROM ' || quote_ident(date_column_name) || ')) FROM ' || quote_ident(table_name) || ' WHERE clm_id ' || operator || ' ' || current_id || ' AND ' || quote_ident(date_column_name) || ' IS NOT NULL LIMIT 1'
      INTO timesp;
   RETURN timesp;
END;
$func$;

--
-- Name: pg_ifd_epoch_from_id(text, text, text, bigint, text); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_epoch_from_id(text, text, text, bigint, text) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    date_column_name ALIAS FOR $2;
	id_name ALIAS FOR $3;
    current_id ALIAS FOR $4;
    operator ALIAS FOR $5;
    timesp bigint;
BEGIN
    EXECUTE ' SELECT round(EXTRACT(EPOCH FROM ' || quote_ident(date_column_name) || ')) FROM ' || quote_ident(table_name) || ' WHERE ' || quote_ident(id_name) || ' ' || operator || ' ' || current_id || ' AND ' || quote_ident(date_column_name) || ' IS NOT NULL LIMIT 1'
   --EXECUTE ' SELECT round(EXTRACT(EPOCH FROM (select to_timestamp(''' || quote_ident(date_column_name) || ''', ''YYYY-MM-DD"T"HH24:MI"Z"\'') at time zone ''UTC''))) FROM ' || quote_ident(table_name) || ' WHERE ' || quote_ident(id_name) || ' ' || operator || ' ' || current_id || ' AND ' || quote_ident(date_column_name) || ' IS NOT NULL LIMIT 1'
   	INTO timesp;
        RAISE NOTICE 'pg_IFD_EPOCH_FROM_ID: %', timesp;
   RETURN timesp;
END;
$func$;

--
-- Name: pg_ifd_fetch_row_from_small_range(text, text, bigint, bigint, timestamp without time zone); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_fetch_row_from_small_range(text, text, bigint, bigint, timestamp without time zone) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    date_column_name ALIAS FOR $2;
    id_low ALIAS FOR $3;
    id_high ALIAS FOR $4;
    timestamp_needed ALIAS FOR $5;
    selected_id bigint;
BEGIN
   RAISE NOTICE 'id_low: %', id_low;
   RAISE NOTICE 'id_high: %', id_high;
   EXECUTE 'SELECT clm_id
            FROM ' || quote_ident(table_name) || '
            WHERE clm_id >= ' || id_low || ' AND clm_id <= ' || id_high || '
                  AND ABS(
                               EXTRACT(EPOCH FROM ' || quote_ident(date_column_name) || ')
                                - 
                               EXTRACT(EPOCH FROM ' || quote_literal(timestamp_needed) || '::timestamp)
                           ) < 43200 -- The maximum tolerated difference between epoch is 12 hours
            ORDER BY ABS(
                         EXTRACT(EPOCH FROM ' || quote_ident(date_column_name) || ')
                          - 
                         EXTRACT(EPOCH FROM ' || quote_literal(timestamp_needed) || '::timestamp)
                     )
            LIMIT 1'
   INTO selected_id;
   RETURN selected_id;
END;
$func$;

--
-- Name: pg_ifd_fetch_row_from_small_range(text, text, text, bigint, bigint, timestamp without time zone); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_fetch_row_from_small_range(text, text, text, bigint, bigint, timestamp without time zone) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    date_column_name ALIAS FOR $2;
	id_name ALIAS FOR $3;
    id_low ALIAS FOR $4;
    id_high ALIAS FOR $5;
    timestamp_needed ALIAS FOR $6;
    selected_id bigint;
BEGIN
   EXECUTE 'SELECT ' || quote_ident(id_name) || 
            ' FROM ' || quote_ident(table_name) || '
            WHERE ' || quote_ident(id_name) || ' >= ' || id_low || ' AND ' || quote_ident(id_name) || ' <= ' || id_high || '
                  AND ABS(
                               EXTRACT(EPOCH FROM ' || quote_ident(date_column_name) || '::timestamp)  
                                - 
                               EXTRACT(EPOCH FROM ' || quote_literal(timestamp_needed) || '::timestamp)
                           ) < 43200 -- The maximum tolerated difference between epoch is 12 hours
            ORDER BY ABS(
                         EXTRACT(EPOCH FROM ' || quote_ident(date_column_name) || '::timestamp)
                          - 
                         EXTRACT(EPOCH FROM ' || quote_literal(timestamp_needed) || '::timestamp)
                     )
            LIMIT 1'
   INTO selected_id;
   RETURN selected_id;
END;
$func$;

--
-- Name: pg_ifd_max_id(text); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_max_id(text) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    maxid bigint;
BEGIN
   EXECUTE ' SELECT MAX(clm_id) FROM ' || quote_ident(table_name)
   INTO maxid;
   RETURN maxid;
END;
$func$;

--
-- Name: pg_ifd_max_id(text, text); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_max_id(text, text) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
	id_name ALIAS FOR $2;
    maxid bigint;
BEGIN
   EXECUTE ' SELECT MAX(' || quote_ident(id_name) || ') FROM ' || quote_ident(table_name)
   INTO maxid;
   RETURN maxid;
END;
$func$;

--
-- Name: pg_ifd_middle_id(bigint, bigint); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_middle_id(bigint, bigint) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    id_low ALIAS FOR $1;
    id_high ALIAS FOR $2;
    id_middle bigint;
BEGIN
   SELECT round((id_high - id_low) / 2 + id_low) INTO id_middle;
   RETURN id_middle;
END;
$func$;

--
-- Name: pg_ifd_min_id(text); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_min_id(text) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
    minid bigint;
BEGIN
   EXECUTE ' SELECT MIN(clm_id) FROM ' || quote_ident(table_name)
   INTO minid;
   RAISE NOTICE 'minid: %', minid;
   RETURN minid;
END;
$func$;

--
-- Name: pg_ifd_min_id(text, text); Type: FUNCTION; Schema: ccw; Owner: bfduser
--

CREATE OR REPLACE FUNCTION public.pg_ifd_min_id(text, text) RETURNS bigint
    LANGUAGE plpgsql
    AS $func$
DECLARE
    table_name ALIAS FOR $1;
	id_name ALIAS FOR $2;
    minid bigint;
BEGIN
   EXECUTE ' SELECT MIN(' || quote_ident(id_name) || ') FROM ' || quote_ident(table_name)
   INTO minid;
   RETURN minid;
END;
$func$;

--
-- Name: synthea_load_pre_validate(bigint, integer, bigint, bigint, bigint, bigint, bigint, text, text); Type: FUNCTION; Schema: ccw; Owner: svc_fhirdb_migrator
--

CREATE OR REPLACE FUNCTION ccw.synthea_load_pre_validate(p_beg_bene_id bigint, p_num_benes_to_generate integer, p_clm_id bigint, p_beg_clm_grp_id bigint, p_pde_id_start bigint, p_carr_clm_cntl_num_start bigint, p_fi_doc_cntl_num_start bigint, p_hicn_start text, p_mbi_start text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    -- the end bene_id is calc'd from bene_id start + num benes to generate
    END_BENE_ID constant bigint := p_beg_bene_id - p_num_benes_to_generate;

    -- end of the batch 1 relocated ids; should be nothing between the generated start and this
    CLM_GRP_ID_END constant bigint := -99999831003;

    -- cast p_fi_doc_cntl_num_start to a varchar, which is how it will be
    -- defined in the various tables that include it.
    FI_DOC_CNTL_NUM varchar := p_fi_doc_cntl_num_start::varchar;
    
    RSLT integer := 0;

BEGIN    
    select sum(v2.cnt) into RSLT
    from
        (
        select '01_beneficiaries', count(v1.bene_id) as "cnt"
        from (
            select bene_id from beneficiaries
            where bene_id <= p_beg_bene_id and bene_id > END_BENE_ID
            limit 1
        ) v1
        union
        select '02_carrier_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from carrier_claims
            where clm_id <= p_clm_id
            and carr_clm_cntl_num::bigint <= p_carr_clm_cntl_num_start
            limit 1
        ) v1
        union
        select '03_carrier_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from carrier_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '04_inpatient_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from inpatient_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '05_outpatient_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from outpatient_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '06_snf_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from snf_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '07_dme_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from dme_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '08_hha_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from hha_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '09_hospice_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from hospice_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '10_partd_events', count(v1.pde_id) as "cnt"
        from (
            select pde_id from partd_events
            where pde_id <= p_pde_id_start
            and clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        -- look for hicn or mbi collisions...only need 1 collision to trigger a problem
        select '11_bene_hicn_mbi', count(v1.bene_id)
        from (
            select a.bene_id from beneficiaries a
            where (a.hicn_unhashed = p_hicn_start or a.mbi_num = p_mbi_start)
            limit 1    
        ) v1
        union
        --
        -- add support for fi_cntl_num here; not the most efficient step. This
        -- could be refactored as its own function that allows a 'fail fast'
        -- operational model.
        --
        select '12a_fi_num_outpatient', count(v1.clm_id)
        from (
            select a.clm_id from outpatient_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12b_fi_num_inpatient', count(v1.clm_id)
        from (
            select a.clm_id from inpatient_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12c_fi_num_hha', count(v1.clm_id)
        from (
            select a.clm_id from hha_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12d_fi_num_snf', count(v1.clm_id)
        from (
            select a.clm_id from snf_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12e_fi_num_hospice', count(v1.clm_id)
        from (
            select a.clm_id from hospice_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        -- this also is a candidate for a 'fail fast' function if it is ever
        -- determined that the whole pre-validation process is too slow!
        select '13_dupl_bene_id', v3.cnt
        from (
            select count(*) as "cnt" from
            (
              select count(*) bene_id_count from (
                select bene_id, mbi_num 
                from ccw.beneficiaries 
                where bene_id < 0 and mbi_num IS NOT NULL
               union 
                select distinct bene_id, mbi_num 
                from ccw.beneficiaries_history 
                where bene_id < 0 and mbi_num IS NOT NULL
              ) as foo 
              group by mbi_num 
              having count(*) > 1
            ) v1
        ) v3
        where v3.cnt > 1    -- account for duplicate in db (on purpose) for testing 
    ) v2;
    RETURN rslt;
END;
$$;

--
-- Name: synthea_load_pre_validate(bigint, bigint, bigint, bigint, bigint, bigint, bigint, text, text); Type: FUNCTION; Schema: ccw; Owner: svc_fhirdb_migrator
--

CREATE OR REPLACE FUNCTION ccw.synthea_load_pre_validate(p_beg_bene_id bigint, p_end_bene_id bigint, p_clm_id bigint, p_beg_clm_grp_id bigint, p_pde_id_start bigint, p_carr_clm_cntl_num_start bigint, p_fi_doc_cntl_num_start bigint, p_hicn_start text, p_mbi_start text) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    -- end of the batch 1 relocated ids; should be nothing between the generated start and this
    CLM_GRP_ID_END constant bigint := -99999831003;

    -- cast p_fi_doc_cntl_num_start to a varchar, which is how it will be
    -- defined in the various tables that include it.
    FI_DOC_CNTL_NUM varchar := p_fi_doc_cntl_num_start::varchar;
    
    RSLT integer := 0;

BEGIN    
    select sum(v2.cnt) into RSLT
    from
        (
        select '01_beneficiaries', count(v1.bene_id) as "cnt"
        from (
            select bene_id from beneficiaries
            where bene_id <= p_beg_bene_id and bene_id > p_end_bene_id
            limit 1
        ) v1
        union
        select '02_carrier_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from carrier_claims
            where clm_id <= p_clm_id
            and carr_clm_cntl_num::bigint <= p_carr_clm_cntl_num_start
            limit 1
        ) v1
        union
        select '03_carrier_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from carrier_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '04_inpatient_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from inpatient_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '05_outpatient_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from outpatient_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '06_snf_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from snf_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '07_dme_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from dme_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '08_hha_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from hha_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '09_hospice_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from hospice_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '10_partd_events', count(v1.pde_id) as "cnt"
        from (
            select pde_id from partd_events
            where pde_id <= p_pde_id_start
            and clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        -- look for hicn or mbi collisions...only need 1 collision to trigger a problem
        select '11_bene_hicn_mbi', count(v1.bene_id)
        from (
            select a.bene_id from beneficiaries a
            where (a.hicn_unhashed = p_hicn_start or a.mbi_num = p_mbi_start)
            limit 1    
        ) v1
        union
        --
        -- add support for fi_cntl_num here; not the most efficient step since
        -- fi_doc_clm_cntl_num is not an indexed field (therefore causing a
        -- full table scan). This could be refactored as its own function that
        -- allows a 'fail fast' operational model thereby bypassing any further
        -- checking of subsequent table(s). 
        --
        select '12a_fi_num_outpatient', count(v1.clm_id)
        from (
            select a.clm_id from outpatient_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12b_fi_num_inpatient', count(v1.clm_id)
        from (
            select a.clm_id from inpatient_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12c_fi_num_hha', count(v1.clm_id)
        from (
            select a.clm_id from hha_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12d_fi_num_snf', count(v1.clm_id)
        from (
            select a.clm_id from snf_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12e_fi_num_hospice', count(v1.clm_id)
        from (
            select a.clm_id from hospice_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        -- this also is a candidate for a 'fail fast' function!
        select '13_dupl_bene_id', v3.cnt
        from (
            select count(*) as "cnt" from
            (
              select count(*) bene_id_count from (
                select bene_id, mbi_num 
                from ccw.beneficiaries 
                where bene_id < 0 and mbi_num IS NOT NULL
               union 
                select distinct bene_id, mbi_num 
                from ccw.beneficiaries_history 
                where bene_id < 0 and mbi_num IS NOT NULL
              ) as foo 
              group by mbi_num 
              having count(*) > 1
            ) v1
        ) v3
        where v3.cnt > 1    -- account for duplicate in db (on purpose) for testing 
    ) v2;
    RETURN rslt;
END;
$$;

--
-- Name: s3_manifest_files; Type: TABLE; Schema: ccw; Owner: svc_fhirdb_migrator
--

CREATE TABLE IF NOT EXISTS ccw.s3_manifest_files (
    manifest_id bigint NOT NULL, 
    s3_key character varying(1024) NOT NULL, 
    status character varying(24) NOT NULL, 
    status_timestamp timestamp with time zone, 
    manifest_timestamp timestamp with time zone NOT NULL, 
    discovery_timestamp timestamp with time zone NOT NULL, 
    CONSTRAINT pk_s3_manifest_files PRIMARY KEY (manifest_id) 
);

--
-- Sequence used to generate primary key manifest_id
--
CREATE SEQUENCE IF NOT EXISTS ccw.s3_manifest_files_manifest_id_seq
    INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807;

ALTER SEQUENCE ccw.s3_manifest_files_manifest_id_seq OWNER TO svc_fhirdb_migrator;
GRANT ALL    ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq TO svc_fhirdb_migrator;
GRANT ALL    ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq TO bfd_migrator_role;
GRANT SELECT ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq TO bfd_reader_role;
GRANT USAGE  ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq TO bfd_writer_role;

--
-- Name: s3_data_files; Type: TABLE; Schema: ccw; Owner: svc_fhirdb_migrator
--

CREATE TABLE IF NOT EXISTS ccw.s3_data_files (
    manifest_id bigint NOT NULL,
    index smallint NOT NULL,
    file_name character varying(128) NOT NULL,
    file_type character varying(50) NOT NULL,
    s3_key character varying(1024) NOT NULL,
    status character varying(24) NOT NULL,
    status_timestamp timestamp with time zone,
    discovery_timestamp timestamp with time zone NOT NULL,
    last_record_number bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_s3_data_files PRIMARY KEY (manifest_id, file_name), 
    CONSTRAINT fk_s3_data_files_s3_manifest_files 
       FOREIGN KEY (manifest_id) REFERENCES ccw.s3_manifest_files(manifest_id) 
);

/*
 * Index used when checking status of a file from S3 bucket
 */
CREATE UNIQUE INDEX IF NOT EXISTS idx_s3_data_files_s3_key on ccw.s3_data_files(s3_key);

--
-- Name: skipped_rif_records; Type: TABLE; Schema: ccw; Owner: svc_fhirdb_migrator
--

CREATE TABLE IF NOT EXISTS ccw.skipped_rif_records (
    record_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    skip_reason character varying(50) NOT NULL,
    rif_file_timestamp timestamp with time zone NOT NULL,
    rif_file_type character varying(48) NOT NULL,
    dml_ind character varying(6) NOT NULL,
    rif_data text NOT NULL
);

--
-- Name: skipped_rif_records_record_id_seq; Type: SEQUENCE; Schema: ccw; Owner: svc_fhirdb_migrator
--

CREATE SEQUENCE IF NOT EXISTS ccw.skipped_rif_records_record_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

GRANT ALL ON SCHEMA   ccw TO bfduser;
GRANT USAGE ON SCHEMA ccw TO bfd_reader_role;
GRANT USAGE ON SCHEMA ccw TO bfd_writer_role;
GRANT ALL ON SCHEMA   ccw TO bfd_migrator_role;
GRANT USAGE ON SCHEMA rda TO svc_bfd_server_0;
GRANT USAGE ON SCHEMA rda TO svc_bfd_server_1;
GRANT USAGE ON SCHEMA rda TO svc_bfd_pipeline_0;
GRANT USAGE ON SCHEMA rda TO paca_reader_role;
GRANT USAGE ON SCHEMA rda TO paca_writer_role;
GRANT ALL ON SCHEMA   rda TO paca_migrator_role;

/*
GRANT ALL ON FUNCTION ccw.add_db_group_if_not_exists(db_name text, group_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.add_migrator_role_to_schema(role_name text, schema_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.add_reader_role_to_schema(role_name text, schema_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.add_writer_role_to_schema(role_name text, schema_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.check_claims_mask(v_bene_id bigint) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.create_role_if_not_exists(role_name text) TO bfd_migrator_role;
*/

/*
GRANT ALL ON FUNCTION ccw.pg_idfromdate(text, text, timestamp without time zone) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_idfromdate(text, text, text, timestamp without time zone) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_epoch_from_id(text, text, bigint, text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_epoch_from_id(text, text, text, bigint, text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_fetch_row_from_small_range(text, text, bigint, bigint, timestamp without time zone) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_fetch_row_from_small_range(text, text, text, bigint, bigint, timestamp without time zone) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_max_id(text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_max_id(text, text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_middle_id(bigint, bigint) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_min_id(text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_ifd_min_id(text, text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_stat_statements(showtext boolean, OUT userid oid, OUT dbid oid, OUT toplevel boolean, OUT queryid bigint, OUT query text, OUT plans bigint, OUT total_plan_time double precision, OUT min_plan_time double precision, OUT max_plan_time double precision, OUT mean_plan_time double precision, OUT stddev_plan_time double precision, OUT calls bigint, OUT total_exec_time double precision, OUT min_exec_time double precision, OUT max_exec_time double precision, OUT mean_exec_time double precision, OUT stddev_exec_time double precision, OUT rows bigint, OUT shared_blks_hit bigint, OUT shared_blks_read bigint, OUT shared_blks_dirtied bigint, OUT shared_blks_written bigint, OUT local_blks_hit bigint, OUT local_blks_read bigint, OUT local_blks_dirtied bigint, OUT local_blks_written bigint, OUT temp_blks_read bigint, OUT temp_blks_written bigint, OUT blk_read_time double precision, OUT blk_write_time double precision, OUT wal_records bigint, OUT wal_fpi bigint, OUT wal_bytes numeric) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_stat_statements_info(OUT dealloc bigint, OUT stats_reset timestamp with time zone) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.pg_stat_statements_reset(userid oid, dbid oid, queryid bigint) TO bfd_migrator_role;

GRANT ALL ON FUNCTION ccw.revoke_db_privs(db_name text, role_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.revoke_schema_privs(schema_name text, role_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.set_fhirdb_owner(role_name text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_cipher() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_client_cert_present() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_client_dn() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_client_dn_field(text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_client_serial() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_extension_info(OUT name text, OUT value text, OUT critical boolean) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_is_used() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_issuer_dn() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_issuer_field(text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.ssl_version() TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.synthea_load_pre_validate(p_beg_bene_id bigint, p_num_benes_to_generate integer, p_clm_id bigint, p_beg_clm_grp_id bigint, p_pde_id_start bigint, p_carr_clm_cntl_num_start bigint, p_fi_doc_cntl_num_start bigint, p_hicn_start text, p_mbi_start text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.synthea_load_pre_validate(p_beg_bene_id bigint, p_end_bene_id bigint, p_clm_id bigint, p_beg_clm_grp_id bigint, p_pde_id_start bigint, p_carr_clm_cntl_num_start bigint, p_fi_doc_cntl_num_start bigint, p_hicn_start text, p_mbi_start text) TO bfd_migrator_role;
GRANT ALL ON FUNCTION ccw.synthea_load_pre_validate(p_beg_bene_id bigint, p_end_bene_id bigint, p_clm_id bigint, p_beg_clm_grp_id bigint, p_pde_id_start bigint, p_carr_clm_cntl_num_start bigint, p_fi_doc_cntl_num_start bigint, p_hicn_start character varying, p_mbi_start character varying) TO bfd_migrator_role;

REVOKE ALL ON FUNCTION ccw.track_bene_monthly_change() FROM svc_fhirdb_migrator;

*/

/*
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.beneficiaries             TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.beneficiaries_history     TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.beneficiary_monthly       TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.beneficiary_monthly_audit TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.carrier_claim_lines       TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.carrier_claims            TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.dme_claim_lines           TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.dme_claims                TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.hha_claim_lines           TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.hha_claims                TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.hospice_claim_lines       TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.inpatient_claim_lines     TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.inpatient_claims          TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.loaded_batches            TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.loaded_files              TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.outpatient_claim_lines    TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.outpatient_claims         TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.partd_events              TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.s3_data_files             TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.s3_manifest_files         TO bfd_writer_role;
--GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.schema_version          TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.skipped_rif_records       TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.snf_claim_lines           TO bfd_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ccw.snf_claims                TO bfd_writer_role;
--GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE pg_stat_statements            TO bfd_writer_role;
--GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE pg_stat_statements_info       TO bfd_writer_role;

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.claim_message_meta_data   TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_audit_trails         TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_claims               TO svc_bfd_pipeline_0;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_claims               TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_diagnosis_codes      TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_payers               TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_proc_codes           TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_revenue_lines        TO svc_bfd_pipeline_0;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.fiss_revenue_lines        TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mbi_cache                 TO svc_bfd_pipeline_0;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mbi_cache                 TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_adjustments           TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_audits                TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_claims                TO svc_bfd_pipeline_0;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_claims                TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_details               TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_diagnosis_codes       TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.mcs_locations             TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.message_errors            TO paca_writer_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.rda_api_progress          TO svc_bfd_pipeline_0;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE rda.rda_api_progress          TO paca_writer_role;

GRANT SELECT ON TABLE ccw.beneficiaries                               TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.beneficiaries                               TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.beneficiaries_history                       TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.beneficiaries_history                       TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.beneficiary_monthly                         TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.beneficiary_monthly                         TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.beneficiary_monthly_audit                   TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.beneficiary_monthly_audit                   TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.carrier_claim_lines                         TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.carrier_claim_lines                         TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.carrier_claims                              TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.carrier_claims                              TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.dme_claim_lines                             TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.dme_claim_lines                             TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.dme_claims                                  TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.dme_claims                                  TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.hha_claim_lines                             TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.hha_claim_lines                             TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.hha_claims                                  TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.hha_claims                                  TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.hospice_claim_lines                         TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.hospice_claim_lines                         TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.hospice_claims                              TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.hospice_claims                              TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.inpatient_claim_lines                       TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.inpatient_claim_lines                       TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.inpatient_claims                            TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.inpatient_claims                            TO bfd_migrator_role;

GRANT ALL    ON TABLE ccw.loaded_batches                              TO svc_bfd_pipeline_0;
GRANT SELECT ON TABLE ccw.loaded_batches                              TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.loaded_batches                              TO bfd_migrator_role;

GRANT ALL    ON TABLE ccw.loaded_files                                TO svc_bfd_pipeline_0;
GRANT SELECT ON TABLE ccw.loaded_files                                TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.loaded_files                                TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.outpatient_claim_lines                      TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.outpatient_claim_lines                      TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.outpatient_claims                           TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.outpatient_claims                           TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.partd_events                                TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.partd_events                                TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.s3_data_files                               TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.s3_data_files                               TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.s3_manifest_files                           TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.s3_manifest_files                           TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.skipped_rif_records                         TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.skipped_rif_records                         TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.snf_claim_lines                             TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.snf_claim_lines                             TO bfd_migrator_role;

GRANT SELECT ON TABLE ccw.snf_claims                                  TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.snf_claims                                  TO bfd_migrator_role;

GRANT SELECT ON SEQUENCE ccw.beneficiaryhistory_beneficiaryhistoryid_seq TO bfd_reader_role;
GRANT USAGE  ON SEQUENCE ccw.beneficiaryhistory_beneficiaryhistoryid_seq TO bfd_writer_role;
GRANT ALL    ON SEQUENCE ccw.beneficiaryhistory_beneficiaryhistoryid_seq TO bfd_migrator_role;

GRANT SELECT ON SEQUENCE ccw.bene_monthly_audit_seq TO bfd_reader_role;
GRANT USAGE  ON SEQUENCE ccw.bene_monthly_audit_seq TO bfd_writer_role;
GRANT ALL    ON SEQUENCE ccw.bene_monthly_audit_seq TO bfd_migrator_role;

GRANT SELECT ON SEQUENCE ccw.loadedbatches_loadedbatchid_seq             TO bfd_reader_role;
GRANT USAGE  ON SEQUENCE ccw.loadedbatches_loadedbatchid_seq             TO bfd_writer_role;
GRANT ALL    ON SEQUENCE ccw.loadedbatches_loadedbatchid_seq             TO bfd_migrator_role;

GRANT SELECT ON SEQUENCE ccw.loadedfiles_loadedfileid_seq                TO bfd_reader_role;
GRANT USAGE  ON SEQUENCE ccw.loadedfiles_loadedfileid_seq                TO bfd_writer_role;
GRANT ALL    ON SEQUENCE ccw.loadedfiles_loadedfileid_seq                TO bfd_migrator_role;

GRANT SELECT ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq           TO bfd_reader_role;
GRANT USAGE  ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq           TO bfd_writer_role;
GRANT ALL    ON SEQUENCE ccw.s3_manifest_files_manifest_id_seq           TO bfd_migrator_role;
*/

--GRANT SELECT ON TABLE pg_stat_statements                          TO bfd_reader_role;
--GRANT ALL    ON TABLE pg_stat_statements                          TO bfd_migrator_role;

--GRANT SELECT ON TABLE pg_stat_statements_info                     TO bfd_reader_role;
--GRANT ALL    ON TABLE pg_stat_statements_info                     TO bfd_migrator_role;

/*
GRANT SELECT ON TABLE ccw.schema_version                                 TO svc_bfd_pipeline_0;
GRANT SELECT ON TABLE ccw.schema_version                                 TO bfd_reader_role;
GRANT ALL    ON TABLE ccw.schema_version                                 TO bfd_migrator_role;
*/

GRANT SELECT ON SEQUENCE ccw.skipped_rif_records_record_id_seq     TO svc_bfd_server_0;
GRANT SELECT ON SEQUENCE ccw.skipped_rif_records_record_id_seq     TO svc_bfd_server_1;
GRANT ALL          ON SEQUENCE ccw.skipped_rif_records_record_id_seq     TO svc_bfd_pipeline_0;
GRANT SELECT       ON SEQUENCE ccw.skipped_rif_records_record_id_seq     TO bfd_reader_role;
GRANT USAGE        ON SEQUENCE ccw.skipped_rif_records_record_id_seq     TO bfd_writer_role;
GRANT ALL          ON SEQUENCE ccw.skipped_rif_records_record_id_seq     TO bfd_migrator_role;

-- ==============================================

/*
GRANT SELECT ON TABLE rda.claim_message_meta_data          TO paca_reader_role;
GRANT ALL    ON TABLE rda.claim_message_meta_data          TO paca_migrator_role;

GRANT SELECT ON TABLE rda.fiss_audit_trails                TO paca_reader_role;
GRANT ALL    ON TABLE rda.fiss_audit_trails                TO paca_migrator_role;

GRANT SELECT ON TABLE rda.fiss_claims                      TO svc_bfd_server_0;
GRANT SELECT ON TABLE rda.fiss_claims                      TO svc_bfd_server_1;

GRANT SELECT ON TABLE rda.fiss_claims                      TO paca_reader_role;
GRANT ALL    ON TABLE rda.fiss_claims                      TO paca_migrator_role;

GRANT SELECT ON TABLE rda.fiss_diagnosis_codes             TO paca_reader_role;
GRANT ALL    ON TABLE rda.fiss_diagnosis_codes             TO paca_migrator_role;

GRANT SELECT ON TABLE rda.fiss_payers                      TO paca_reader_role;
GRANT ALL    ON TABLE rda.fiss_payers                      TO paca_migrator_role;

GRANT SELECT ON TABLE rda.fiss_proc_codes                  TO paca_reader_role;
GRANT ALL    ON TABLE rda.fiss_proc_codes                  TO paca_migrator_role;

GRANT ALL    ON TABLE rda.fiss_revenue_lines               TO svc_bfd_pipeline_1;
GRANT SELECT ON TABLE rda.fiss_revenue_lines               TO svc_bfd_server_0;
GRANT SELECT ON TABLE rda.fiss_revenue_lines               TO svc_bfd_server_1;
GRANT SELECT ON TABLE rda.fiss_revenue_lines               TO paca_reader_role;
GRANT ALL    ON TABLE rda.fiss_revenue_lines               TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mbi_cache                        TO svc_bfd_server_0;
GRANT SELECT ON TABLE rda.mbi_cache                        TO svc_bfd_server_1;

GRANT SELECT ON TABLE rda.mbi_cache                        TO paca_reader_role;
GRANT ALL    ON TABLE rda.mbi_cache                        TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mcs_adjustments                  TO paca_reader_role;
GRANT ALL    ON TABLE rda.mcs_adjustments                  TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mcs_audits                       TO paca_reader_role;
GRANT ALL    ON TABLE rda.mcs_audits                       TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mcs_claims                       TO svc_bfd_server_0;
GRANT SELECT ON TABLE rda.mcs_claims                       TO svc_bfd_server_1;

GRANT SELECT ON TABLE rda.mcs_claims                       TO paca_reader_role;
GRANT ALL    ON TABLE rda.mcs_claims                       TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mcs_details                      TO paca_reader_role;
GRANT ALL    ON TABLE rda.mcs_details                      TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mcs_diagnosis_codes              TO paca_reader_role;
GRANT ALL    ON TABLE rda.mcs_diagnosis_codes              TO paca_migrator_role;

GRANT SELECT ON TABLE rda.mcs_locations                    TO paca_reader_role;
GRANT ALL    ON TABLE rda.mcs_locations                    TO paca_migrator_role;

GRANT SELECT ON TABLE rda.message_errors                   TO paca_reader_role;
GRANT ALL    ON TABLE rda.message_errors                   TO paca_migrator_role;

GRANT SELECT ON TABLE rda.rda_api_progress                 TO svc_bfd_server_0;
GRANT SELECT ON TABLE rda.rda_api_progress                 TO svc_bfd_server_1;
GRANT SELECT ON TABLE rda.rda_api_progress                 TO paca_reader_role;
GRANT ALL    ON TABLE rda.rda_api_progress                 TO paca_migrator_role;

GRANT SELECT ON SEQUENCE rda.mbi_cache_mbi_id_seq          TO svc_bfd_server_0;
GRANT SELECT ON SEQUENCE rda.mbi_cache_mbi_id_seq          TO svc_bfd_server_1;
GRANT USAGE  ON SEQUENCE rda.mbi_cache_mbi_id_seq          TO svc_bfd_pipeline_0;
GRANT SELECT ON SEQUENCE rda.mbi_cache_mbi_id_seq          TO paca_reader_role;
GRANT USAGE  ON SEQUENCE rda.mbi_cache_mbi_id_seq          TO paca_writer_role;
GRANT ALL    ON SEQUENCE rda.mbi_cache_mbi_id_seq          TO paca_migrator_role;
*/

-- =================================
--    svc_fhirdb_migrator privs
-- =================================
/*
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT SELECT ON SEQUENCES  TO bfd_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT USAGE  ON SEQUENCES  TO bfd_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT ALL    ON SEQUENCES  TO bfd_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT ALL    ON FUNCTIONS  TO bfd_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT SELECT ON TABLES     TO bfd_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT ALL    ON TABLES     TO bfd_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT SELECT ON SEQUENCES  TO paca_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT USAGE  ON SEQUENCES  TO paca_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT ALL    ON SEQUENCES  TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT ALL    ON FUNCTIONS  TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT SELECT ON TABLES     TO paca_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT ALL    ON TABLES     TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA ccw GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO bfd_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_fhirdb_migrator IN SCHEMA rda GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO paca_writer_role;

-- =================================
--    svc_bfd_pipeline_1 privs
-- =================================
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT SELECT ON SEQUENCES  TO bfd_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT USAGE  ON SEQUENCES  TO bfd_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT ALL    ON SEQUENCES  TO bfd_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT ALL    ON FUNCTIONS  TO bfd_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT SELECT ON TABLES     TO bfd_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT ALL    ON TABLES     TO bfd_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT SELECT ON SEQUENCES  TO paca_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT USAGE  ON SEQUENCES  TO paca_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT ALL    ON SEQUENCES  TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT ALL    ON FUNCTIONS  TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT SELECT ON TABLES     TO paca_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT ALL    ON TABLES     TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA ccw GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO bfd_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_bfd_pipeline_1 IN SCHEMA rda GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO paca_writer_role;

-- =================================
--    bfduser privs
-- =================================
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT ON SEQUENCES  TO svc_bfd_server_0;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT ON SEQUENCES  TO svc_bfd_server_1;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT USAGE ON SEQUENCES   TO svc_bfd_pipeline_0;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT USAGE ON SEQUENCES   TO svc_bfd_pipeline_1;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT ON SEQUENCES  TO paca_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT USAGE ON SEQUENCES   TO paca_writer_role;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT ALL ON SEQUENCES     TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT ALL ON FUNCTIONS     TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT ON TABLES     TO svc_bfd_server_0;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT ON TABLES     TO svc_bfd_server_1;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT ON TABLES     TO paca_reader_role;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT ALL ON TABLES        TO paca_migrator_role;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO svc_bfd_pipeline_0;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO svc_bfd_pipeline_1;
ALTER DEFAULT PRIVILEGES FOR ROLE bfduser IN SCHEMA rda GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO paca_writer_role;
*/


-- free reign for the migrator; GRANT ALL allows:
--
--     SELECT     : Allows the role to retrieve data from the table. 
--     INSERT     : Allows the role to add new rows to the table. 
--     UPDATE     : Allows the role to modify existing rows in the table. 
--     DELETE     : Allows the role to remove rows from the table. 
--     TRUNCATE   : Allows the role to truncate the entire table, removing all rows. 
--     REFERENCES : Allows the role to create a foreign key referencing the table. 
--     TRIGGER    : Allows the role to create triggers on the table. 
--     ALTER      : Allows the role to alter the table, e.g., add or remove columns. 
--     INDEX      : Allows the role to create indexes on the table. 
--     USAGE      : Allows the role to use sequences associated with the table (if any). 
--     RULE       : Allows the role to create rules on the table. 

GRANT ALL ON ALL TABLES  IN SCHEMA ccw TO bfd_migrator_role;
GRANT ALL ON ALL TABLES  IN SCHEMA rda TO bfd_migrator_role;

-- ETL can perform updates to schema TABLES and USAGE to schema SEQUENCES
--
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES    IN SCHEMA ccw TO bfd_writer_role;
GRANT SELECT, USAGE                  ON ALL SEQUENCES IN SCHEMA ccw TO bfd_writer_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES    IN SCHEMA rda TO paca_writer_role;
GRANT SELECT, USAGE                  ON ALL SEQUENCES IN SCHEMA rda TO paca_writer_role;

GRANT bfd_writer_role   TO svc_bfd_pipeline_0;
GRANT bfd_writer_role   TO svc_bfd_pipeline_1;
GRANT paca_writer_role  TO svc_bfd_pipeline_0;
GRANT paca_writer_role  TO svc_bfd_pipeline_1;

-- allow read (SELECT, USAGE) on all tables and sequences for ccw and rda;
-- support all reader roles for both schemas.
--
GRANT SELECT ON ALL TABLES     IN SCHEMA ccw     TO bfd_reader_role;
GRANT SELECT ON ALL SEQUENCES  IN SCHEMA ccw     TO bfd_reader_role;
GRANT SELECT ON ALL TABLES     IN SCHEMA rda     TO paca_reader_role;
GRANT SELECT ON ALL SEQUENCES  IN SCHEMA rda     TO paca_reader_role;
/*
GRANT SELECT ON ALL TABLES     IN SCHEMA rda     TO bfd_reader_role;
GRANT SELECT ON ALL SEQUENCES  IN SCHEMA rda     TO bfd_reader_role;
GRANT SELECT ON ALL TABLES     IN SCHEMA rda     TO paca_reader_role;
GRANT SELECT ON ALL SEQUENCES  IN SCHEMA rda     TO paca_reader_role;
*/

-- BFD and RDA share the same server....allow RO access to both schemas
--
--GRANT bfd_reader_role   TO svc_bfd_server_0 WITH INHERIT TRUE;
--GRANT bfd_reader_role   TO svc_bfd_server_1 WITH INHERIT TRUE;
--GRANT paca_reader_role  TO svc_bfd_server_0 WITH INHERIT TRUE;
--GRANT paca_reader_role  TO svc_bfd_server_1 WITH INHERIT TRUE;

-- make life easier for a role; set default schema when USER with that ROLE logs in
--
ALTER ROLE bfd_reader_role    SET search_path to ccw;
ALTER ROLE bfd_writer_role    SET search_path to ccw;
ALTER ROLE paca_reader_role   SET search_path to rda;
ALTER ROLE paca_writer_role   SET search_path to rda;