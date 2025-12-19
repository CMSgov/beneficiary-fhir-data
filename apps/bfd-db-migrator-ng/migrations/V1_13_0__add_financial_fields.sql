ALTER TABLE idr.claim
ADD COLUMN clm_blood_chrg_amt NUMERIC,
ADD COLUMN clm_tot_cntrctl_amt NUMERIC,
ADD COLUMN clm_bene_intrst_pd_amt NUMERIC,
ADD COLUMN clm_bene_pmt_coinsrnc_amt NUMERIC,
ADD COLUMN clm_cob_ptnt_resp_amt NUMERIC,
ADD COLUMN clm_prvdr_otaf_amt NUMERIC,
ADD COLUMN clm_othr_tp_pd_amt NUMERIC,
ADD COLUMN clm_prvdr_rmng_due_amt NUMERIC,
ADD COLUMN clm_blood_ncvrd_chrg_amt NUMERIC,
ADD COLUMN clm_prvdr_intrst_pd_amt NUMERIC;

ALTER TABLE idr.claim_institutional
ADD COLUMN clm_mdcr_ip_scnd_yr_rate_amt NUMERIC,
ADD COLUMN clm_instnl_low_vol_pmt_amt NUMERIC,
ADD COLUMN clm_hipps_readmsn_rdctn_amt NUMERIC,
ADD COLUMN clm_hipps_model_bndld_pmt_amt NUMERIC,
ADD COLUMN clm_hipps_vbp_amt NUMERIC,
ADD COLUMN clm_site_ntrl_ip_pps_pymt_amt NUMERIC,
ADD COLUMN clm_finl_stdzd_pymt_amt NUMERIC,
ADD COLUMN clm_pps_md_wvr_stdzd_val_amt NUMERIC,
ADD COLUMN clm_hac_rdctn_pymt_amt NUMERIC,
ADD COLUMN clm_mdcr_ip_1st_yr_rate_amt NUMERIC,
ADD COLUMN clm_site_ntrl_cst_bsd_pymt_amt NUMERIC;

ALTER TABLE idr.claim_professional
ADD COLUMN clm_prvdr_acnt_rcvbl_ofst_amt NUMERIC;

ALTER TABLE idr.claim_item
ADD COLUMN clm_line_othr_tp_pd_amt NUMERIC,
ADD COLUMN clm_line_ncvrd_pd_amt NUMERIC,
ADD COLUMN clm_line_otaf_amt NUMERIC;

ALTER TABLE idr.claim_line_institutional
ADD COLUMN clm_rev_cntr_tdapa_amt NUMERIC,
ADD COLUMN clm_line_non_ehr_rdctn_amt NUMERIC,
ADD COLUMN clm_line_add_on_pymt_amt NUMERIC;

ALTER TABLE idr.claim_line_professional
ADD COLUMN clm_line_prfnl_intrst_amt NUMERIC,
ADD COLUMN clm_line_carr_psych_ot_lmt_amt NUMERIC,
ADD COLUMN clm_line_carr_clncl_chrg_amt NUMERIC;

ALTER TABLE idr.claim_line_rx
ADD COLUMN clm_line_rebt_passthru_pos_amt NUMERIC,
ADD COLUMN clm_cms_calcd_mftr_dscnt_amt NUMERIC,
ADD COLUMN clm_line_grs_cvrd_cst_tot_amt NUMERIC,
ADD COLUMN clm_phrmcy_price_dscnt_at_pos_amt NUMERIC;