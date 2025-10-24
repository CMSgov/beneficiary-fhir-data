ALTER TABLE idr.contract_pbp_number
ADD COLUMN cntrct_pbp_name VARCHAR(75);

ALTER TABLE idr.claim_professional
--v2_mdcr_clm_lctn_hstry
ADD COLUMN clm_audt_trl_stus_cd VARCHAR(2),
ADD COLUMN clm_lctn_cd_sqnc_num BIGINT,
ADD COLUMN idr_insrt_ts_lctn_hstry TIMESTAMPTZ,
ADD COLUMN idr_updt_ts_lctn_hstry TIMESTAMPTZ;

ALTER TABLE idr.claim_professional
RENAME COLUMN idr_insrt_ts TO idr_insrt_ts_clm_prfnl;

ALTER TABLE idr.claim_professional
RENAME COLUMN idr_updt_ts TO idr_updt_ts_clm_prfnl;

ALTER TABLE idr.claim_institutional
ADD COLUMN dgns_drg_outlier_cd VARCHAR(1);

ALTER TABLE idr.claim
ADD COLUMN clm_adjstmt_type_cd VARCHAR(2),
ADD COLUMN clm_bene_pd_amt NUMERIC,
ADD COLUMN clm_blg_prvdr_zip5_cd VARCHAR(5),
ADD COLUMN clm_sbmt_frmt_cd VARCHAR(1),
ADD COLUMN clm_sbmtr_cntrct_num VARCHAR(5),
ADD COLUMN clm_sbmtr_cntrct_pbp_num VARCHAR(3),
-- from v2_mdcr_clm_dcmtn
ADD COLUMN clm_bnft_enhncmt_1_cd VARCHAR(2),
ADD COLUMN clm_ngaco_pbpmt_sw VARCHAR(1),
ADD COLUMN clm_ngaco_cptatn_sw VARCHAR(1),
ADD COLUMN clm_ngaco_pdschrg_hcbs_sw VARCHAR(1),
ADD COLUMN clm_ngaco_snf_wvr_sw VARCHAR(1),
ADD COLUMN clm_ngaco_tlhlth_sw VARCHAR(1);

ALTER TABLE idr.claim_item
ADD COLUMN clm_rndrg_prvdr_type_cd VARCHAR(3),
-- from v2_mdcr_clm_line_dcmtn
ADD COLUMN clm_line_bnft_enhncmt_2_cd VARCHAR(2),
ADD COLUMN clm_line_bnft_enhncmt_3_cd VARCHAR(2),
ADD COLUMN clm_line_bnft_enhncmt_4_cd VARCHAR(2),
ADD COLUMN clm_line_bnft_enhncmt_5_cd VARCHAR(2),
ADD COLUMN clm_line_ngaco_cptatn_sw VARCHAR(1),
ADD COLUMN clm_line_ngaco_pdschrg_hcbs_sw VARCHAR(1),
ADD COLUMN clm_line_ngaco_snf_wvr_sw VARCHAR(1),
ADD COLUMN clm_line_ngaco_tlhlth_sw VARCHAR(1),
ADD COLUMN clm_line_aco_care_mgmt_hcbs_sw VARCHAR(1),
ADD COLUMN clm_line_ngaco_pbpmt_sw VARCHAR(1),
ADD COLUMN idr_insrt_ts_line_dcmtn TIMESTAMPTZ,
ADD COLUMN idr_updt_ts_line_dcmtn TIMESTAMPTZ;

CREATE TABLE idr.claim_line_rx (
    clm_uniq_id BIGINT NOT NULL,
    clm_line_num INT NOT NULL,
    clm_brnd_gnrc_cd VARCHAR(1) NOT NULL,
    clm_cmpnd_cd VARCHAR(1) NOT NULL,
    clm_ctstrphc_cvrg_ind_cd VARCHAR(1) NOT NULL,
    clm_daw_prod_slctn_cd VARCHAR(1) NOT NULL,
    clm_drug_cvrg_stus_cd VARCHAR(2) NOT NULL,
    clm_dspnsng_stus_cd VARCHAR(1) NOT NULL,
    clm_line_authrzd_fill_num VARCHAR(2),
    clm_line_days_suply_qty INT,
    clm_line_grs_above_thrshld_amt NUMERIC,
    clm_line_grs_blw_thrshld_amt NUMERIC,
    clm_line_ingrdnt_cst_amt NUMERIC,
    clm_line_lis_amt NUMERIC,
    clm_line_plro_amt NUMERIC,
    clm_line_rx_fill_num INT,
    clm_line_rx_orgn_cd VARCHAR(1) NOT NULL,
    clm_line_sls_tax_amt NUMERIC,
    clm_line_srvc_cst_amt NUMERIC,
    clm_line_troop_tot_amt NUMERIC NOT NULL,
    clm_line_vccn_admin_fee_amt NUMERIC,
    clm_ltc_dspnsng_mthd_cd VARCHAR(3) NOT NULL,
    clm_phrmcy_srvc_type_cd VARCHAR(2) NOT NULL,
    clm_prcng_excptn_cd VARCHAR(1) NOT NULL,
    clm_ptnt_rsdnc_cd VARCHAR(2) NOT NULL,
    clm_rptd_mftr_dscnt_amt NUMERIC,
    idr_insrt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    bfd_created_ts TIMESTAMPTZ,
    bfd_updated_ts TIMESTAMPTZ,
    PRIMARY KEY(clm_uniq_id, clm_line_num)
);