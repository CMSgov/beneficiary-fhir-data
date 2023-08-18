/*
This SELECT mimics exactly the Hibernate-generated code that fetches data
from the CARRIER_CLAIMS table in the ExplanationOfBenefit resources provder.
*/
EXPLAIN ANALYZE
select distinct
	  carriercla0_.clm_id as clm_id1_20_0_
	, lines1_.line_num as line_num1_19_1_
	, lines1_.clm_id as clm_id2_19_1_
	, carriercla0_.nch_carr_clm_alowd_amt as nch_carr2_20_0_
	, carriercla0_.bene_id as bene_id3_20_0_
	, carriercla0_.carr_clm_cash_ddctbl_apld_amt as carr_clm4_20_0_
	, carriercla0_.nch_clm_bene_pmt_amt as nch_clm_5_20_0_
	, carriercla0_.carr_clm_blg_npi_num as carr_clm6_20_0_
	, carriercla0_.carr_num as carr_num7_20_0_
	, carriercla0_.carr_clm_cntl_num as carr_clm8_20_0_
	, carriercla0_.clm_disp_cd as clm_disp9_20_0_
	, carriercla0_.carr_clm_entry_cd as carr_cl10_20_0_
	, carriercla0_.clm_grp_id as clm_grp11_20_0_
	, carriercla0_.nch_clm_type_cd as nch_clm12_20_0_
	, carriercla0_.clm_clncl_tril_num as clm_cln13_20_0_
	, carriercla0_.clm_from_dt as clm_fro14_20_0_
	, carriercla0_.clm_thru_dt as clm_thr15_20_0_
	, carriercla0_.icd_dgns_cd10 as icd_dgn16_20_0_
	, carriercla0_.icd_dgns_vrsn_cd10 as icd_dgn17_20_0_
	, carriercla0_.icd_dgns_cd11 as icd_dgn18_20_0_
	, carriercla0_.icd_dgns_vrsn_cd11 as icd_dgn19_20_0_
	, carriercla0_.icd_dgns_cd12 as icd_dgn20_20_0_
	, carriercla0_.icd_dgns_vrsn_cd12 as icd_dgn21_20_0_
	, carriercla0_.icd_dgns_cd1 as icd_dgn22_20_0_
	, carriercla0_.icd_dgns_vrsn_cd1 as icd_dgn23_20_0_
	, carriercla0_.icd_dgns_cd2 as icd_dgn24_20_0_
	, carriercla0_.icd_dgns_vrsn_cd2 as icd_dgn25_20_0_
	, carriercla0_.icd_dgns_cd3 as icd_dgn26_20_0_
	, carriercla0_.icd_dgns_vrsn_cd3 as icd_dgn27_20_0_
	, carriercla0_.icd_dgns_cd4 as icd_dgn28_20_0_
	, carriercla0_.icd_dgns_vrsn_cd4 as icd_dgn29_20_0_
	, carriercla0_.icd_dgns_cd5 as icd_dgn30_20_0_
	, carriercla0_.icd_dgns_vrsn_cd5 as icd_dgn31_20_0_
	, carriercla0_.icd_dgns_cd6 as icd_dgn32_20_0_
	, carriercla0_.icd_dgns_vrsn_cd6 as icd_dgn33_20_0_
	, carriercla0_.icd_dgns_cd7 as icd_dgn34_20_0_
	, carriercla0_.icd_dgns_vrsn_cd7 as icd_dgn35_20_0_
	, carriercla0_.icd_dgns_cd8 as icd_dgn36_20_0_
	, carriercla0_.icd_dgns_vrsn_cd8 as icd_dgn37_20_0_
	, carriercla0_.icd_dgns_cd9 as icd_dgn38_20_0_
	, carriercla0_.icd_dgns_vrsn_cd9 as icd_dgn39_20_0_
	, carriercla0_.prncpal_dgns_cd as prncpal40_20_0_
	, carriercla0_.prncpal_dgns_vrsn_cd as prncpal41_20_0_
	, carriercla0_.final_action as final_a42_20_0_
	, carriercla0_.carr_clm_hcpcs_yr_cd as carr_cl43_20_0_
	, carriercla0_.last_updated as last_up44_20_0_
	, carriercla0_.nch_near_line_rec_ident_cd as nch_nea45_20_0_
	, carriercla0_.clm_pmt_amt as clm_pmt46_20_0_
	, carriercla0_.carr_clm_pmt_dnl_cd as carr_cl47_20_0_
	, carriercla0_.carr_clm_prmry_pyr_pd_amt as carr_cl48_20_0_
	, carriercla0_.carr_clm_prvdr_asgnmt_ind_sw as carr_cl49_20_0_
	, carriercla0_.nch_clm_prvdr_pmt_amt as nch_clm50_20_0_
	, carriercla0_.rfr_physn_npi as rfr_phy51_20_0_
	, carriercla0_.rfr_physn_upin as rfr_phy52_20_0_
	, carriercla0_.carr_clm_rfrng_pin_num as carr_cl53_20_0_
	, carriercla0_.nch_carr_clm_sbmtd_chrg_amt as nch_car54_20_0_
	, carriercla0_.nch_wkly_proc_dt as nch_wkl55_20_0_
	, lines1_.line_alowd_chrg_amt as line_alo3_19_1_
	, lines1_.carr_line_ansthsa_unit_cnt as carr_lin4_19_1_
	, lines1_.line_bene_ptb_ddctbl_amt as line_ben5_19_1_
	, lines1_.line_bene_pmt_amt as line_ben6_19_1_
	, lines1_.betos_cd as betos_cd7_19_1_
	, lines1_.carr_line_clia_lab_num as carr_lin8_19_1_
	, lines1_.line_cms_type_srvc_cd as line_cms9_19_1_
	, lines1_.line_coinsrnc_amt as line_co10_19_1_
	, lines1_.line_icd_dgns_cd as line_ic11_19_1_
	, lines1_.line_icd_dgns_vrsn_cd as line_ic12_19_1_
	, lines1_.line_1st_expns_dt as line_13_19_1_
	, lines1_.hcpcs_cd as hcpcs_c14_19_1_
	, lines1_.hcpcs_1st_mdfr_cd as hcpcs_15_19_1_
	, lines1_.hcpcs_2nd_mdfr_cd as hcpcs_16_19_1_
	, lines1_.line_hct_hgb_rslt_num as line_hc17_19_1_
	, lines1_.line_hct_hgb_type_cd as line_hc18_19_1_
	, lines1_.hpsa_scrcty_ind_cd as hpsa_sc19_19_1_
	, lines1_.line_last_expns_dt as line_la20_19_1_
	, lines1_.carr_line_prcng_lclty_cd as carr_li21_19_1_
	, lines1_.carr_line_mtus_cd as carr_li22_19_1_
	, lines1_.carr_line_mtus_cnt as carr_li23_19_1_
	, lines1_.line_ndc_cd as line_nd24_19_1_
	, lines1_.org_npi_num as org_npi25_19_1_
	, lines1_.line_nch_pmt_amt as line_nc26_19_1_
	, lines1_.line_pmt_80_100_cd as line_pm27_19_1_
	, lines1_.prf_physn_npi as prf_phy28_19_1_
	, lines1_.prf_physn_upin as prf_phy29_19_1_
	, lines1_.carr_prfrng_pin_num as carr_pr30_19_1_
	, lines1_.line_place_of_srvc_cd as line_pl31_19_1_
	, lines1_.line_bene_prmry_pyr_cd as line_be32_19_1_
	, lines1_.line_bene_prmry_pyr_pd_amt as line_be33_19_1_
	, lines1_.line_prcsg_ind_cd as line_pr34_19_1_
	, lines1_.prtcptng_ind_cd as prtcptn35_19_1_
	, lines1_.line_prvdr_pmt_amt as line_pr36_19_1_
	, lines1_.prvdr_spclty as prvdr_s37_19_1_
	, lines1_.prvdr_state_cd as prvdr_s38_19_1_
	, lines1_.tax_num as tax_num39_19_1_
	, lines1_.carr_line_prvdr_type_cd as carr_li40_19_1_
	, lines1_.prvdr_zip as prvdr_z41_19_1_
	, lines1_.carr_line_rdcd_pmt_phys_astn_c as carr_li42_19_1_
	, lines1_.carr_line_rx_num as carr_li43_19_1_
	, lines1_.line_srvc_cnt as line_sr44_19_1_
	, lines1_.line_service_deductible as line_se45_19_1_
	, lines1_.line_sbmtd_chrg_amt as line_sb46_19_1_
	, lines1_.clm_id as clm_id2_19_0__
	, lines1_.line_num as line_num1_19_0__
-- NOTE:
--  FROM can be toggled between CARRIER_CLAIMS_ORIG table or the
--  (parent) CARRIER_CLAIMS table that is partitioned. Allows for
--  testing EXPLAIN ANALYZE between a partitioned table vs. a
--  non-partitioned table.
--
--from carrier_claims_orig carriercla0_  -- non-partitioned
from carrier_claims carriercla0_         -- partitioned
inner join carrier_claim_lines lines1_ on carriercla0_.clm_id=lines1_.clm_id
where carriercla0_.bene_id=-10000009979912
and carriercla0_.last_updated > '2022-06-01'
and carriercla0_.last_updated < '2022-09-01'
order by lines1_.LINE_NUM asc;