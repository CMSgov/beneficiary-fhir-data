/*
Bunch of SQL to create a new partitioned CARRIER_CLAIMS table
from existing CARRIER_CLAIMS table.
*/
DROP TABLE IF EXISTS carrier_claims_pre_2020;
DROP TABLE IF EXISTS carrier_claims_2020;
DROP TABLE IF EXISTS carrier_claims_2021;
DROP TABLE IF EXISTS carrier_claims_2022;
DROP TABLE IF EXISTS carrier_claims_2023;
DROP TABLE IF EXISTS carrier_claims_2024;
DROP TABLE IF EXISTS carrier_claims_0;

-- Create a partitioned CARRIER_CLAIMS table.
CREATE TABLE IF NOT EXISTS carrier_claims_0
(
	  clm_id bigint NOT NULL
	, bene_id bigint NOT NULL
	, clm_grp_id bigint NOT NULL
	, last_updated date
	, clm_from_dt date NOT NULL
	, clm_thru_dt date NOT NULL
	, clm_disp_cd character varying(2) NOT NULL
	, final_action character(1) NOT NULL
	, clm_pmt_amt numeric(12,2) NOT NULL
	, carr_num character varying(5) NOT NULL
	, carr_clm_rfrng_pin_num character varying(14) NOT NULL
	, carr_clm_cntl_num character varying(23)
	, carr_clm_entry_cd character(1) NOT NULL
	, carr_clm_prmry_pyr_pd_amt numeric(12,2) NOT NULL
	, carr_clm_cash_ddctbl_apld_amt numeric(12,2) NOT NULL
	, carr_clm_pmt_dnl_cd character varying(2) NOT NULL
	, carr_clm_hcpcs_yr_cd character(1)
	, carr_clm_prvdr_asgnmt_ind_sw character(1)
	, nch_clm_type_cd character varying(2) NOT NULL
	, nch_near_line_rec_ident_cd character(1) NOT NULL
	, nch_wkly_proc_dt date NOT NULL
	, nch_carr_clm_sbmtd_chrg_amt numeric(12,2) NOT NULL
	, nch_carr_clm_alowd_amt numeric(12,2) NOT NULL
	, nch_clm_bene_pmt_amt numeric(12,2) NOT NULL
	, nch_clm_prvdr_pmt_amt numeric(12,2) NOT NULL
	, clm_clncl_tril_num character varying(8)
	, prncpal_dgns_cd character varying(7)
	, prncpal_dgns_vrsn_cd character(1)
	, rfr_physn_npi character varying(12)
	, rfr_physn_upin character varying(12)
	, icd_dgns_cd1 character varying(7)
	, icd_dgns_vrsn_cd1 character(1)
	, icd_dgns_cd2 character varying(7)
	, icd_dgns_vrsn_cd2 character(1)
	, icd_dgns_cd3 character varying(7)
	, icd_dgns_vrsn_cd3 character(1)
	, icd_dgns_cd4 character varying(7)
	, icd_dgns_vrsn_cd4 character(1)
	, icd_dgns_cd5 character varying(7)
	, icd_dgns_vrsn_cd5 character(1)
	, icd_dgns_cd6 character varying(7)
	, icd_dgns_vrsn_cd6 character(1)
	, icd_dgns_cd7 character varying(7)
	, icd_dgns_vrsn_cd7 character(1)
	, icd_dgns_cd8 character varying(7)
	, icd_dgns_vrsn_cd8 character(1)
	, icd_dgns_cd9 character varying(7)
	, icd_dgns_vrsn_cd9 character(1)
	, icd_dgns_cd10 character varying(7)
	, icd_dgns_vrsn_cd10 character(1)
	, icd_dgns_cd11 character varying(7)
	, icd_dgns_vrsn_cd11 character(1)
	, icd_dgns_cd12 character varying(7)
	, icd_dgns_vrsn_cd12 character(1)
	, carr_clm_blg_npi_num character varying(10)
)
PARTITION BY RANGE (last_updated);
    
ALTER TABLE carrier_claims_0 ADD PRIMARY KEY (clm_id, last_updated);

-- =================================================
-- add FK constraint to beneficiaries table.
-- bene_id references beneficiaries (bene_id).
--
-- NOTE: holds exclusive lock
-- =================================================
ALTER TABLE carrier_claims_0
    ADD CONSTRAINT carrier_claims_0_bene_id_to_beneficiaries FOREIGN KEY (bene_id)
        REFERENCES beneficiaries (bene_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
        
-- =================================================
-- create a partition table for data before 2020
-- =================================================

CREATE TABLE IF NOT EXISTS carrier_claims_pre_2020
(
	  clm_id bigint NOT NULL
	, bene_id bigint NOT NULL
	, clm_grp_id bigint NOT NULL
	, last_updated date
	, clm_from_dt date NOT NULL
	, clm_thru_dt date NOT NULL
	, clm_disp_cd character varying(2) NOT NULL
	, final_action character(1) NOT NULL
	, clm_pmt_amt numeric(12,2) NOT NULL
	, carr_num character varying(5) NOT NULL
	, carr_clm_rfrng_pin_num character varying(14) NOT NULL
	, carr_clm_cntl_num character varying(23)
	, carr_clm_entry_cd character(1) NOT NULL
	, carr_clm_prmry_pyr_pd_amt numeric(12,2) NOT NULL
	, carr_clm_cash_ddctbl_apld_amt numeric(12,2) NOT NULL
	, carr_clm_pmt_dnl_cd character varying(2) NOT NULL
	, carr_clm_hcpcs_yr_cd character(1)
	, carr_clm_prvdr_asgnmt_ind_sw character(1)
	, nch_clm_type_cd character varying(2) NOT NULL
	, nch_near_line_rec_ident_cd character(1) NOT NULL
	, nch_wkly_proc_dt date NOT NULL
	, nch_carr_clm_sbmtd_chrg_amt numeric(12,2) NOT NULL
	, nch_carr_clm_alowd_amt numeric(12,2) NOT NULL
	, nch_clm_bene_pmt_amt numeric(12,2) NOT NULL
	, nch_clm_prvdr_pmt_amt numeric(12,2) NOT NULL
	, clm_clncl_tril_num character varying(8)
	, prncpal_dgns_cd character varying(7)
	, prncpal_dgns_vrsn_cd character(1)
	, rfr_physn_npi character varying(12)
	, rfr_physn_upin character varying(12)
	, icd_dgns_cd1 character varying(7)
	, icd_dgns_vrsn_cd1 character(1)
	, icd_dgns_cd2 character varying(7)
	, icd_dgns_vrsn_cd2 character(1)
	, icd_dgns_cd3 character varying(7)
	, icd_dgns_vrsn_cd3 character(1)
	, icd_dgns_cd4 character varying(7)
	, icd_dgns_vrsn_cd4 character(1)
	, icd_dgns_cd5 character varying(7)
	, icd_dgns_vrsn_cd5 character(1)
	, icd_dgns_cd6 character varying(7)
	, icd_dgns_vrsn_cd6 character(1)
	, icd_dgns_cd7 character varying(7)
	, icd_dgns_vrsn_cd7 character(1)
	, icd_dgns_cd8 character varying(7)
	, icd_dgns_vrsn_cd8 character(1)
	, icd_dgns_cd9 character varying(7)
	, icd_dgns_vrsn_cd9 character(1)
	, icd_dgns_cd10 character varying(7)
	, icd_dgns_vrsn_cd10 character(1)
	, icd_dgns_cd11 character varying(7)
	, icd_dgns_vrsn_cd11 character(1)
	, icd_dgns_cd12 character varying(7)
	, icd_dgns_vrsn_cd12 character(1)
	, carr_clm_blg_npi_num character varying(10)
);
ALTER TABLE carrier_claims_pre_2020 ADD PRIMARY KEY (clm_id, last_updated);

-- =================================================
-- create yearly partition tables for data
--
-- NOTE: adding primary key holds exclusive lock;
-- this is done on an empty table so not too big a hit.
-- =================================================
CREATE TABLE IF NOT EXISTS carrier_claims_2020 (
	like carrier_claims_pre_2020 including constraints including storage);
ALTER TABLE carrier_claims_2020 ADD PRIMARY KEY (clm_id, last_updated);
	
CREATE TABLE IF NOT EXISTS carrier_claims_2021 (
	like carrier_claims_pre_2020 including constraints including storage);
ALTER TABLE carrier_claims_2021 ADD PRIMARY KEY (clm_id, last_updated);
	
CREATE TABLE IF NOT EXISTS carrier_claims_2022 (
	like carrier_claims_pre_2020 including constraints including storage);
ALTER TABLE carrier_claims_2022 ADD PRIMARY KEY (clm_id, last_updated);

CREATE TABLE IF NOT EXISTS carrier_claims_2023 (
	like carrier_claims_pre_2020 including constraints including storage);
ALTER TABLE carrier_claims_2023 ADD PRIMARY KEY (clm_id, last_updated);	

-- we won't data for 2024, but we create that table as a means to
-- test/verify postgres ability to automatically route data updates
-- to the correct partition.
CREATE TABLE IF NOT EXISTS carrier_claims_2024 (
	like carrier_claims_pre_2020 including constraints including storage);
ALTER TABLE carrier_claims_2024 ADD PRIMARY KEY (clm_id, last_updated);

-- =================================================
-- painful data migration into yearly partitions
-- (this may take some time depending on the db env)
--
-- NOTE:
-- since data already exists in the CARRIER_CLAIMS
-- table, we'll be able to run somewhat 'fast and
-- loose' since clm_id is already being used as a
-- primary key.
-- =================================================

DO $$

declare
  p_2020_lo DATE := '2020-01-01';
  p_2021_lo DATE := '2021-01-01';
  p_2022_lo DATE := '2022-01-01';
  p_2023_lo DATE := '2023-01-01';
	
  rec RECORD;
  
BEGIN   
   FOR rec IN
	   select
		  clm_id
		, bene_id
		, clm_grp_id
		-- some data has a null LAST_UPDATED which we need for allocating data to
		-- a partition; if LAST_UPDATED is null, then use the non-nullable CLM_FROM_DT.
		, coalesce(last_updated, last_updated::DATE, clm_from_dt)::DATE as last_updated
		, clm_from_dt
		, clm_from_dt
		, clm_thru_dt
		, clm_disp_cd
		, final_action
		, clm_pmt_amt
		, carr_num
		, carr_clm_rfrng_pin_num
		, carr_clm_cntl_num
		, carr_clm_entry_cd
		, carr_clm_prmry_pyr_pd_amt
		, carr_clm_cash_ddctbl_apld_amt
		, carr_clm_pmt_dnl_cd
		, carr_clm_hcpcs_yr_cd
		, carr_clm_prvdr_asgnmt_ind_sw
		, nch_clm_type_cd
		, nch_near_line_rec_ident_cd
		, nch_wkly_proc_dt
		, nch_carr_clm_sbmtd_chrg_amt
		, nch_carr_clm_alowd_amt
		, nch_clm_bene_pmt_amt
		, nch_clm_prvdr_pmt_amt
		, clm_clncl_tril_num
		, prncpal_dgns_cd
		, prncpal_dgns_vrsn_cd
		, rfr_physn_npi
		, rfr_physn_upin
		, icd_dgns_cd1
		, icd_dgns_vrsn_cd1
		, icd_dgns_cd2
		, icd_dgns_vrsn_cd2
		, icd_dgns_cd3
		, icd_dgns_vrsn_cd3
		, icd_dgns_cd4
		, icd_dgns_vrsn_cd4
		, icd_dgns_cd5
		, icd_dgns_vrsn_cd5
		, icd_dgns_cd6
		, icd_dgns_vrsn_cd6
		, icd_dgns_cd7
		, icd_dgns_vrsn_cd7
		, icd_dgns_cd8
		, icd_dgns_vrsn_cd8
		, icd_dgns_cd9
		, icd_dgns_vrsn_cd9
		, icd_dgns_cd10
		, icd_dgns_vrsn_cd10
		, icd_dgns_cd11
		, icd_dgns_vrsn_cd11
		, icd_dgns_cd12
		, icd_dgns_vrsn_cd12
		, carr_clm_blg_npi_num   
	  from carrier_claims

   LOOP
      IF rec.last_updated < p_2020_lo THEN
      	INSERT INTO carrier_claims_pre_2020 values (
			  rec.clm_id
			, rec.bene_id
			, rec.clm_grp_id
			, rec.last_updated
			, rec.clm_from_dt
			, rec.clm_thru_dt
			, rec.clm_disp_cd
			, rec.final_action
			, rec.clm_pmt_amt
			, rec.carr_num
			, rec.carr_clm_rfrng_pin_num
			, rec.carr_clm_cntl_num
			, rec.carr_clm_entry_cd
			, rec.carr_clm_prmry_pyr_pd_amt
			, rec.carr_clm_cash_ddctbl_apld_amt
			, rec.carr_clm_pmt_dnl_cd
			, rec.carr_clm_hcpcs_yr_cd
			, rec.carr_clm_prvdr_asgnmt_ind_sw
			, rec.nch_clm_type_cd
			, rec.nch_near_line_rec_ident_cd
			, rec.nch_wkly_proc_dt
			, rec.nch_carr_clm_sbmtd_chrg_amt
			, rec.nch_carr_clm_alowd_amt
			, rec.nch_clm_bene_pmt_amt
			, rec.nch_clm_prvdr_pmt_amt
			, rec.clm_clncl_tril_num
			, rec.prncpal_dgns_cd
			, rec.prncpal_dgns_vrsn_cd
			, rec.rfr_physn_npi
			, rec.rfr_physn_upin
			, rec.icd_dgns_cd1
			, rec.icd_dgns_vrsn_cd1
			, rec.icd_dgns_cd2
			, rec.icd_dgns_vrsn_cd2
			, rec.icd_dgns_cd3
			, rec.icd_dgns_vrsn_cd3
			, rec.icd_dgns_cd4
			, rec.icd_dgns_vrsn_cd4
			, rec.icd_dgns_cd5
			, rec.icd_dgns_vrsn_cd5
			, rec.icd_dgns_cd6
			, rec.icd_dgns_vrsn_cd6
			, rec.icd_dgns_cd7
			, rec.icd_dgns_vrsn_cd7
			, rec.icd_dgns_cd8
			, rec.icd_dgns_vrsn_cd8
			, rec.icd_dgns_cd9
			, rec.icd_dgns_vrsn_cd9
			, rec.icd_dgns_cd10
			, rec.icd_dgns_vrsn_cd10
			, rec.icd_dgns_cd11
			, rec.icd_dgns_vrsn_cd11
			, rec.icd_dgns_cd12
			, rec.icd_dgns_vrsn_cd12
			, rec.carr_clm_blg_npi_num
			);
	  ELSIF rec.last_updated < p_2021_lo THEN
      	INSERT INTO carrier_claims_pre_2020 values (
			  rec.clm_id
			, rec.bene_id
			, rec.clm_grp_id
			, rec.last_updated
			, rec.clm_from_dt
			, rec.clm_thru_dt
			, rec.clm_disp_cd
			, rec.final_action
			, rec.clm_pmt_amt
			, rec.carr_num
			, rec.carr_clm_rfrng_pin_num
			, rec.carr_clm_cntl_num
			, rec.carr_clm_entry_cd
			, rec.carr_clm_prmry_pyr_pd_amt
			, rec.carr_clm_cash_ddctbl_apld_amt
			, rec.carr_clm_pmt_dnl_cd
			, rec.carr_clm_hcpcs_yr_cd
			, rec.carr_clm_prvdr_asgnmt_ind_sw
			, rec.nch_clm_type_cd
			, rec.nch_near_line_rec_ident_cd
			, rec.nch_wkly_proc_dt
			, rec.nch_carr_clm_sbmtd_chrg_amt
			, rec.nch_carr_clm_alowd_amt
			, rec.nch_clm_bene_pmt_amt
			, rec.nch_clm_prvdr_pmt_amt
			, rec.clm_clncl_tril_num
			, rec.prncpal_dgns_cd
			, rec.prncpal_dgns_vrsn_cd
			, rec.rfr_physn_npi
			, rec.rfr_physn_upin
			, rec.icd_dgns_cd1
			, rec.icd_dgns_vrsn_cd1
			, rec.icd_dgns_cd2
			, rec.icd_dgns_vrsn_cd2
			, rec.icd_dgns_cd3
			, rec.icd_dgns_vrsn_cd3
			, rec.icd_dgns_cd4
			, rec.icd_dgns_vrsn_cd4
			, rec.icd_dgns_cd5
			, rec.icd_dgns_vrsn_cd5
			, rec.icd_dgns_cd6
			, rec.icd_dgns_vrsn_cd6
			, rec.icd_dgns_cd7
			, rec.icd_dgns_vrsn_cd7
			, rec.icd_dgns_cd8
			, rec.icd_dgns_vrsn_cd8
			, rec.icd_dgns_cd9
			, rec.icd_dgns_vrsn_cd9
			, rec.icd_dgns_cd10
			, rec.icd_dgns_vrsn_cd10
			, rec.icd_dgns_cd11
			, rec.icd_dgns_vrsn_cd11
			, rec.icd_dgns_cd12
			, rec.icd_dgns_vrsn_cd12
			, rec.carr_clm_blg_npi_num
			);
	  ELSIF rec.last_updated < p_2022_lo THEN
	    INSERT INTO carrier_claims_2021 values (
			  rec.clm_id
			, rec.bene_id
			, rec.clm_grp_id
			, rec.last_updated
			, rec.clm_from_dt
			, rec.clm_thru_dt
			, rec.clm_disp_cd
			, rec.final_action
			, rec.clm_pmt_amt
			, rec.carr_num
			, rec.carr_clm_rfrng_pin_num
			, rec.carr_clm_cntl_num
			, rec.carr_clm_entry_cd
			, rec.carr_clm_prmry_pyr_pd_amt
			, rec.carr_clm_cash_ddctbl_apld_amt
			, rec.carr_clm_pmt_dnl_cd
			, rec.carr_clm_hcpcs_yr_cd
			, rec.carr_clm_prvdr_asgnmt_ind_sw
			, rec.nch_clm_type_cd
			, rec.nch_near_line_rec_ident_cd
			, rec.nch_wkly_proc_dt
			, rec.nch_carr_clm_sbmtd_chrg_amt
			, rec.nch_carr_clm_alowd_amt
			, rec.nch_clm_bene_pmt_amt
			, rec.nch_clm_prvdr_pmt_amt
			, rec.clm_clncl_tril_num
			, rec.prncpal_dgns_cd
			, rec.prncpal_dgns_vrsn_cd
			, rec.rfr_physn_npi
			, rec.rfr_physn_upin
			, rec.icd_dgns_cd1
			, rec.icd_dgns_vrsn_cd1
			, rec.icd_dgns_cd2
			, rec.icd_dgns_vrsn_cd2
			, rec.icd_dgns_cd3
			, rec.icd_dgns_vrsn_cd3
			, rec.icd_dgns_cd4
			, rec.icd_dgns_vrsn_cd4
			, rec.icd_dgns_cd5
			, rec.icd_dgns_vrsn_cd5
			, rec.icd_dgns_cd6
			, rec.icd_dgns_vrsn_cd6
			, rec.icd_dgns_cd7
			, rec.icd_dgns_vrsn_cd7
			, rec.icd_dgns_cd8
			, rec.icd_dgns_vrsn_cd8
			, rec.icd_dgns_cd9
			, rec.icd_dgns_vrsn_cd9
			, rec.icd_dgns_cd10
			, rec.icd_dgns_vrsn_cd10
			, rec.icd_dgns_cd11
			, rec.icd_dgns_vrsn_cd11
			, rec.icd_dgns_cd12
			, rec.icd_dgns_vrsn_cd12
			, rec.carr_clm_blg_npi_num
			);
	  ELSIF rec.last_updated < p_2023_lo THEN
	    INSERT INTO carrier_claims_2022 values (
			  rec.clm_id
			, rec.bene_id
			, rec.clm_grp_id
			, rec.last_updated
			, rec.clm_from_dt
			, rec.clm_thru_dt
			, rec.clm_disp_cd
			, rec.final_action
			, rec.clm_pmt_amt
			, rec.carr_num
			, rec.carr_clm_rfrng_pin_num
			, rec.carr_clm_cntl_num
			, rec.carr_clm_entry_cd
			, rec.carr_clm_prmry_pyr_pd_amt
			, rec.carr_clm_cash_ddctbl_apld_amt
			, rec.carr_clm_pmt_dnl_cd
			, rec.carr_clm_hcpcs_yr_cd
			, rec.carr_clm_prvdr_asgnmt_ind_sw
			, rec.nch_clm_type_cd
			, rec.nch_near_line_rec_ident_cd
			, rec.nch_wkly_proc_dt
			, rec.nch_carr_clm_sbmtd_chrg_amt
			, rec.nch_carr_clm_alowd_amt
			, rec.nch_clm_bene_pmt_amt
			, rec.nch_clm_prvdr_pmt_amt
			, rec.clm_clncl_tril_num
			, rec.prncpal_dgns_cd
			, rec.prncpal_dgns_vrsn_cd
			, rec.rfr_physn_npi
			, rec.rfr_physn_upin
			, rec.icd_dgns_cd1
			, rec.icd_dgns_vrsn_cd1
			, rec.icd_dgns_cd2
			, rec.icd_dgns_vrsn_cd2
			, rec.icd_dgns_cd3
			, rec.icd_dgns_vrsn_cd3
			, rec.icd_dgns_cd4
			, rec.icd_dgns_vrsn_cd4
			, rec.icd_dgns_cd5
			, rec.icd_dgns_vrsn_cd5
			, rec.icd_dgns_cd6
			, rec.icd_dgns_vrsn_cd6
			, rec.icd_dgns_cd7
			, rec.icd_dgns_vrsn_cd7
			, rec.icd_dgns_cd8
			, rec.icd_dgns_vrsn_cd8
			, rec.icd_dgns_cd9
			, rec.icd_dgns_vrsn_cd9
			, rec.icd_dgns_cd10
			, rec.icd_dgns_vrsn_cd10
			, rec.icd_dgns_cd11
			, rec.icd_dgns_vrsn_cd11
			, rec.icd_dgns_cd12
			, rec.icd_dgns_vrsn_cd12
			, rec.carr_clm_blg_npi_num			);
	  ELSE
	    INSERT INTO carrier_claims_2023 values (
			  rec.clm_id
			, rec.bene_id
			, rec.clm_grp_id
			, rec.last_updated
			, rec.clm_from_dt
			, rec.clm_thru_dt
			, rec.clm_disp_cd
			, rec.final_action
			, rec.clm_pmt_amt
			, rec.carr_num
			, rec.carr_clm_rfrng_pin_num
			, rec.carr_clm_cntl_num
			, rec.carr_clm_entry_cd
			, rec.carr_clm_prmry_pyr_pd_amt
			, rec.carr_clm_cash_ddctbl_apld_amt
			, rec.carr_clm_pmt_dnl_cd
			, rec.carr_clm_hcpcs_yr_cd
			, rec.carr_clm_prvdr_asgnmt_ind_sw
			, rec.nch_clm_type_cd
			, rec.nch_near_line_rec_ident_cd
			, rec.nch_wkly_proc_dt
			, rec.nch_carr_clm_sbmtd_chrg_amt
			, rec.nch_carr_clm_alowd_amt
			, rec.nch_clm_bene_pmt_amt
			, rec.nch_clm_prvdr_pmt_amt
			, rec.clm_clncl_tril_num
			, rec.prncpal_dgns_cd
			, rec.prncpal_dgns_vrsn_cd
			, rec.rfr_physn_npi
			, rec.rfr_physn_upin
			, rec.icd_dgns_cd1
			, rec.icd_dgns_vrsn_cd1
			, rec.icd_dgns_cd2
			, rec.icd_dgns_vrsn_cd2
			, rec.icd_dgns_cd3
			, rec.icd_dgns_vrsn_cd3
			, rec.icd_dgns_cd4
			, rec.icd_dgns_vrsn_cd4
			, rec.icd_dgns_cd5
			, rec.icd_dgns_vrsn_cd5
			, rec.icd_dgns_cd6
			, rec.icd_dgns_vrsn_cd6
			, rec.icd_dgns_cd7
			, rec.icd_dgns_vrsn_cd7
			, rec.icd_dgns_cd8
			, rec.icd_dgns_vrsn_cd8
			, rec.icd_dgns_cd9
			, rec.icd_dgns_vrsn_cd9
			, rec.icd_dgns_cd10
			, rec.icd_dgns_vrsn_cd10
			, rec.icd_dgns_cd11
			, rec.icd_dgns_vrsn_cd11
			, rec.icd_dgns_cd12
			, rec.icd_dgns_vrsn_cd12
			, rec.carr_clm_blg_npi_num
			);        
      END IF;
   END LOOP;
END $$


-- =================================================
-- setup index on bene_id for yearly partitions
-- =================================================
CREATE INDEX IF NOT EXISTS carrier_claims_pre_2020_bene_id_idx
    ON carrier_claims_pre_2020 (bene_id);
CREATE INDEX IF NOT EXISTS carrier_claims_2020_bene_id_idx
    ON carrier_claims_2020 (bene_id);
CREATE INDEX IF NOT EXISTS carrier_claims_2021_bene_id_idx
    ON carrier_claims_2021 (bene_id);
CREATE INDEX IF NOT EXISTS carrier_claims_2022_bene_id_idx
    ON carrier_claims_2022 (bene_id); 
CREATE INDEX IF NOT EXISTS carrier_claims_2023_bene_id_idx
    ON carrier_claims_2023 (bene_id); 
CREATE INDEX IF NOT EXISTS carrier_claims_2024_bene_id_idx
    ON carrier_claims_2024 (bene_id);
CREATE INDEX IF NOT EXISTS carrier_claims_0_bene_id_idx
    ON carrier_claims_0 (bene_id);
	
-- =================================================
-- setup index on last_updated for yearly partitions
-- =================================================
CREATE INDEX IF NOT EXISTS carrier_claims_pre_2020_last_updated_idx
    ON carrier_claims_pre_2020 (last_updated);
CREATE INDEX IF NOT EXISTS carrier_claims_2020_last_updated_idx
    ON carrier_claims_2020 (last_updated);
CREATE INDEX IF NOT EXISTS carrier_claims_2021_last_updated_idx
    ON carrier_claims_2021 (last_updated);
CREATE INDEX IF NOT EXISTS carrier_claims_2022_last_updated_idx
    ON carrier_claims_2022 (last_updated); 
CREATE INDEX IF NOT EXISTS carrier_claims_2023_last_updated_idx
    ON carrier_claims_2023 (last_updated); 
CREATE INDEX IF NOT EXISTS carrier_claims_2024_last_updated_idx
    ON carrier_claims_2024 (last_updated);
CREATE INDEX IF NOT EXISTS carrier_claims_0_last_updated_idx
    ON carrier_claims_0 (last_updated);
    
-- =================================================
-- attach yearly partitions to parent table
--
-- You may have noticed that the range set in the
-- initial table includes the first day of the year
-- and the first day of the next year. This is
-- because Postgres partitions have an inclusive
-- lower bound and an exclusive upper bound. 
-- =================================================
ALTER TABLE carrier_claims_0
  ATTACH PARTITION carrier_claims_pre_2020
    FOR VALUES FROM ('1995-01-01') TO ('2020-01-01');
    
ALTER TABLE carrier_claims_0
  ATTACH PARTITION carrier_claims_2020
    FOR VALUES FROM ('2020-01-01') TO ('2020-12-31');
    
ALTER TABLE carrier_claims_0
  ATTACH PARTITION carrier_claims_2021
    FOR VALUES FROM ('2021-01-01') TO ('2021-12-31');
    
ALTER TABLE carrier_claims_0
  ATTACH PARTITION carrier_claims_2022
    FOR VALUES FROM ('2022-01-01') TO ('2022-12-31');
    
ALTER TABLE carrier_claims_0
  ATTACH PARTITION carrier_claims_2023
    FOR VALUES FROM ('2023-01-01') TO ('2023-12-31');
    
ALTER TABLE carrier_claims_0
  ATTACH PARTITION carrier_claims_2024
    FOR VALUES FROM ('2024-01-01') TO ('2024-12-31');

--
-- Do some renaming of tables so that we can test partitioned
-- vs. non-partitioned data handling.
--
-- non-paritioned
ALTER TABLE carrier_claims RENAME TO carrier_claims_orig;
--
-- paritioned
ALTER TABLE carrier_claims_0 RENAME TO carrier_claims;