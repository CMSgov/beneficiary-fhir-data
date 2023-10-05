/*
Bunch of SQL to create a new partitioned PARTD_EVENTS table
from existing PARTD_EVENTS table.
*/
DROP TABLE IF EXISTS partd_events_pre_2020;
DROP TABLE IF EXISTS partd_events_2020;
DROP TABLE IF EXISTS partd_events_2021;
DROP TABLE IF EXISTS partd_events_2022;
DROP TABLE IF EXISTS partd_events_2023;
DROP TABLE IF EXISTS partd_events_2024;
DROP TABLE IF EXISTS partd_events_0;

-- Create a partitioned PARTD_EVENTS table.
CREATE TABLE IF NOT EXISTS partd_events_0
(
    pde_id bigint NOT NULL,
    bene_id bigint NOT NULL references beneficiaries,
    clm_grp_id bigint NOT NULL,
    last_updated date not null,
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
    adjstmt_dltn_cd character(1) ,
    brnd_gnrc_cd character(1) ,
    ctstrphc_cvrg_cd character(1) ,
    dspnsng_stus_cd character(1) ,
    nstd_frmt_cd character(1) ,
    prcng_excptn_cd character(1) ,
    rx_orgn_cd character(1) ,
    submsn_clr_cd character varying(2)
)
PARTITION BY RANGE (last_updated);

ALTER TABLE partd_events_0 ADD PRIMARY KEY (pde_id, last_updated);

-- =================================================
-- add FK constraint to beneficiaries table.
-- bene_id references beneficiaries (bene_id).
--
-- NOTE: holds exclusive lock
-- =================================================
ALTER TABLE partd_events_0
    ADD CONSTRAINT partd_events_0_bene_id_to_beneficiaries FOREIGN KEY (bene_id)
        REFERENCES beneficiaries (bene_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;

-- =================================================
-- create a partition table for data before 2020
-- =================================================
CREATE TABLE IF NOT EXISTS partd_events_pre_2020
(
    pde_id bigint NOT NULL,
    bene_id bigint NOT NULL,
    clm_grp_id bigint NOT NULL,
    last_updated date not null,
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
    adjstmt_dltn_cd character(1) ,
    brnd_gnrc_cd character(1) ,
    ctstrphc_cvrg_cd character(1) ,
    dspnsng_stus_cd character(1) ,
    nstd_frmt_cd character(1) ,
    prcng_excptn_cd character(1) ,
    rx_orgn_cd character(1) ,
    submsn_clr_cd character varying(2)
);
ALTER TABLE partd_events_pre_2020 ADD PRIMARY KEY (pde_id, last_updated);


-- =================================================
-- create yearly partition tables for data; we simply
-- use the already defined table structure and then
-- add the primary key.
--
-- NOTE: adding primary key holds exclusive lock;
-- this is done on an empty table so not too big a hit.
-- =================================================
CREATE TABLE IF NOT EXISTS partd_events_2020 (
	like partd_events_pre_2020 including constraints including storage);
ALTER TABLE partd_events_2020 ADD PRIMARY KEY (pde_id, last_updated);
	
CREATE TABLE IF NOT EXISTS partd_events_2021 (
	like partd_events_pre_2020 including constraints including storage);
ALTER TABLE partd_events_2021 ADD PRIMARY KEY (pde_id, last_updated);

CREATE TABLE IF NOT EXISTS partd_events_2022 (
	like partd_events_pre_2020 including constraints including storage);
ALTER TABLE partd_events_2022 ADD PRIMARY KEY (pde_id, last_updated);
	
CREATE TABLE IF NOT EXISTS partd_events_2023 (
	like partd_events_pre_2020 including constraints including storage);
ALTER TABLE partd_events_2023 ADD PRIMARY KEY (pde_id, last_updated);

-- we won't data for 2024, but we create that table as a means to
-- test/verify postgres ability to automatically route data updates
-- to the correct partition.	
CREATE TABLE IF NOT EXISTS partd_events_2024 (
	like partd_events_pre_2020 including constraints including storage);
ALTER TABLE partd_events_2024 ADD PRIMARY KEY (pde_id, last_updated);
  
-- =================================================
-- painful data migration into yearly partitions
-- (this may take some time depending on the db env)
--
-- NOTE:
-- since data already exists in the PARTD_EVENTS
-- table, we'll be able to run somewhat 'fast and
-- loose' since pde_id is already being used as a
-- primary key. Once the data is loaded, we'll add
-- in our constraints.
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
			pde_id
			, bene_id
			, clm_grp_id
			-- some data has a null LAST_UPDATED which we need for allocating data to
			-- a partition; if LAST_UPDATED is null, then use the non-nullable SRVC_DT.
			, last_updated
			, final_action
			, cmpnd_cd
			, drug_cvrg_stus_cd
			, days_suply_num
			, srvc_dt
			, pd_dt
			, fill_num
			, qty_dspnsd_num
			, cvrd_d_plan_pd_amt
			, gdc_abv_oopt_amt
			, gdc_blw_oopt_amt
			, lics_amt
			, ncvrd_plan_pd_amt
			, othr_troop_amt
			, plro_amt
			, ptnt_pay_amt
			, rptd_gap_dscnt_num
			, ptnt_rsdnc_cd
			, tot_rx_cst_amt
			, daw_prod_slctn_cd
			, phrmcy_srvc_type_cd
			, plan_cntrct_rec_id
			, plan_pbp_rec_num
			, prod_srvc_id
			, prscrbr_id
			, prscrbr_id_qlfyr_cd
			, rx_srvc_rfrnc_num
			, srvc_prvdr_id
			, srvc_prvdr_id_qlfyr_cd
			, adjstmt_dltn_cd
			, brnd_gnrc_cd
			, ctstrphc_cvrg_cd
			, dspnsng_stus_cd
			, nstd_frmt_cd
			, prcng_excptn_cd
			, rx_orgn_cd
			, submsn_clr_cd
		FROM
			PARTD_EVENTS
   LOOP
      IF rec.last_updated < p_2020_lo THEN
      	INSERT INTO partd_events_pre_2020 values (
				  rec.pde_id
				, rec.bene_id
				, rec.clm_grp_id
				, rec.last_updated
				, rec.final_action
				, rec.cmpnd_cd
				, rec.drug_cvrg_stus_cd
				, rec.days_suply_num
				, rec.srvc_dt
				, rec.pd_dt date
				, rec.fill_num
				, rec.qty_dspnsd_num
				, rec.cvrd_d_plan_pd_amt
				, rec.gdc_abv_oopt_amt
				, rec.gdc_blw_oopt_amt
				, rec.lics_amt
				, rec.ncvrd_plan_pd_amt
				, rec.othr_troop_amt
				, rec.plro_amt
				, rec.ptnt_pay_amt
				, rec.rptd_gap_dscnt_num
				, rec.ptnt_rsdnc_cd
				, rec.tot_rx_cst_amt
				, rec.daw_prod_slctn_cd
				, rec.phrmcy_srvc_type_cd
				, rec.plan_cntrct_rec_id
				, rec.plan_pbp_rec_num
				, rec.prod_srvc_id
				, rec.prscrbr_id
				, rec.prscrbr_id_qlfyr_cd
				, rec.rx_srvc_rfrnc_num
				, rec.srvc_prvdr_id
				, rec.srvc_prvdr_id_qlfyr_cd
				, rec.adjstmt_dltn_cd
				, rec.brnd_gnrc_cd
				, rec.ctstrphc_cvrg_cd
				, rec.dspnsng_stus_cd
				, rec.nstd_frmt_cd
				, rec.prcng_excptn_cd
				, rec.rx_orgn_cd
				, rec.submsn_clr_cd
			);
	  ELSIF rec.last_updated < p_2021_lo THEN
      	INSERT INTO partd_events_2020 values (
				  rec.pde_id
				, rec.bene_id
				, rec.clm_grp_id
				, rec.last_updated
				, rec.final_action
				, rec.cmpnd_cd
				, rec.drug_cvrg_stus_cd
				, rec.days_suply_num
				, rec.srvc_dt
				, rec.pd_dt date
				, rec.fill_num
				, rec.qty_dspnsd_num
				, rec.cvrd_d_plan_pd_amt
				, rec.gdc_abv_oopt_amt
				, rec.gdc_blw_oopt_amt
				, rec.lics_amt
				, rec.ncvrd_plan_pd_amt
				, rec.othr_troop_amt
				, rec.plro_amt
				, rec.ptnt_pay_amt
				, rec.rptd_gap_dscnt_num
				, rec.ptnt_rsdnc_cd
				, rec.tot_rx_cst_amt
				, rec.daw_prod_slctn_cd
				, rec.phrmcy_srvc_type_cd
				, rec.plan_cntrct_rec_id
				, rec.plan_pbp_rec_num
				, rec.prod_srvc_id
				, rec.prscrbr_id
				, rec.prscrbr_id_qlfyr_cd
				, rec.rx_srvc_rfrnc_num
				, rec.srvc_prvdr_id
				, rec.srvc_prvdr_id_qlfyr_cd
				, rec.adjstmt_dltn_cd
				, rec.brnd_gnrc_cd
				, rec.ctstrphc_cvrg_cd
				, rec.dspnsng_stus_cd
				, rec.nstd_frmt_cd
				, rec.prcng_excptn_cd
				, rec.rx_orgn_cd
				, rec.submsn_clr_cd
			);
	  ELSIF rec.last_updated < p_2022_lo THEN
	    INSERT INTO partd_events_2021 values (
				  rec.pde_id
				, rec.bene_id
				, rec.clm_grp_id
				, rec.last_updated
				, rec.final_action
				, rec.cmpnd_cd
				, rec.drug_cvrg_stus_cd
				, rec.days_suply_num
				, rec.srvc_dt
				, rec.pd_dt date
				, rec.fill_num
				, rec.qty_dspnsd_num
				, rec.cvrd_d_plan_pd_amt
				, rec.gdc_abv_oopt_amt
				, rec.gdc_blw_oopt_amt
				, rec.lics_amt
				, rec.ncvrd_plan_pd_amt
				, rec.othr_troop_amt
				, rec.plro_amt
				, rec.ptnt_pay_amt
				, rec.rptd_gap_dscnt_num
				, rec.ptnt_rsdnc_cd
				, rec.tot_rx_cst_amt
				, rec.daw_prod_slctn_cd
				, rec.phrmcy_srvc_type_cd
				, rec.plan_cntrct_rec_id
				, rec.plan_pbp_rec_num
				, rec.prod_srvc_id
				, rec.prscrbr_id
				, rec.prscrbr_id_qlfyr_cd
				, rec.rx_srvc_rfrnc_num
				, rec.srvc_prvdr_id
				, rec.srvc_prvdr_id_qlfyr_cd
				, rec.adjstmt_dltn_cd
				, rec.brnd_gnrc_cd
				, rec.ctstrphc_cvrg_cd
				, rec.dspnsng_stus_cd
				, rec.nstd_frmt_cd
				, rec.prcng_excptn_cd
				, rec.rx_orgn_cd
				, rec.submsn_clr_cd
			);
	  ELSIF rec.last_updated < p_2023_lo THEN
	    INSERT INTO partd_events_2022 values (
				  rec.pde_id
				, rec.bene_id
				, rec.clm_grp_id
				, rec.last_updated
				, rec.final_action
				, rec.cmpnd_cd
				, rec.drug_cvrg_stus_cd
				, rec.days_suply_num
				, rec.srvc_dt
				, rec.pd_dt date
				, rec.fill_num
				, rec.qty_dspnsd_num
				, rec.cvrd_d_plan_pd_amt
				, rec.gdc_abv_oopt_amt
				, rec.gdc_blw_oopt_amt
				, rec.lics_amt
				, rec.ncvrd_plan_pd_amt
				, rec.othr_troop_amt
				, rec.plro_amt
				, rec.ptnt_pay_amt
				, rec.rptd_gap_dscnt_num
				, rec.ptnt_rsdnc_cd
				, rec.tot_rx_cst_amt
				, rec.daw_prod_slctn_cd
				, rec.phrmcy_srvc_type_cd
				, rec.plan_cntrct_rec_id
				, rec.plan_pbp_rec_num
				, rec.prod_srvc_id
				, rec.prscrbr_id
				, rec.prscrbr_id_qlfyr_cd
				, rec.rx_srvc_rfrnc_num
				, rec.srvc_prvdr_id
				, rec.srvc_prvdr_id_qlfyr_cd
				, rec.adjstmt_dltn_cd
				, rec.brnd_gnrc_cd
				, rec.ctstrphc_cvrg_cd
				, rec.dspnsng_stus_cd
				, rec.nstd_frmt_cd
				, rec.prcng_excptn_cd
				, rec.rx_orgn_cd
				, rec.submsn_clr_cd
			);
	  ELSE
	    INSERT INTO partd_events_2023 values (
				  rec.pde_id
				, rec.bene_id
				, rec.clm_grp_id
				, rec.last_updated
				, rec.final_action
				, rec.cmpnd_cd
				, rec.drug_cvrg_stus_cd
				, rec.days_suply_num
				, rec.srvc_dt
				, rec.pd_dt date
				, rec.fill_num
				, rec.qty_dspnsd_num
				, rec.cvrd_d_plan_pd_amt
				, rec.gdc_abv_oopt_amt
				, rec.gdc_blw_oopt_amt
				, rec.lics_amt
				, rec.ncvrd_plan_pd_amt
				, rec.othr_troop_amt
				, rec.plro_amt
				, rec.ptnt_pay_amt
				, rec.rptd_gap_dscnt_num
				, rec.ptnt_rsdnc_cd
				, rec.tot_rx_cst_amt
				, rec.daw_prod_slctn_cd
				, rec.phrmcy_srvc_type_cd
				, rec.plan_cntrct_rec_id
				, rec.plan_pbp_rec_num
				, rec.prod_srvc_id
				, rec.prscrbr_id
				, rec.prscrbr_id_qlfyr_cd
				, rec.rx_srvc_rfrnc_num
				, rec.srvc_prvdr_id
				, rec.srvc_prvdr_id_qlfyr_cd
				, rec.adjstmt_dltn_cd
				, rec.brnd_gnrc_cd
				, rec.ctstrphc_cvrg_cd
				, rec.dspnsng_stus_cd
				, rec.nstd_frmt_cd
				, rec.prcng_excptn_cd
				, rec.rx_orgn_cd
				, rec.submsn_clr_cd
			);        
      END IF;
   END LOOP;
END $$


-- =================================================
-- setup index on bene_id for yearly partitions
-- =================================================
CREATE INDEX IF NOT EXISTS partd_events_pre_2020_bene_id_idx
    ON partd_events_pre_2020 (bene_id);
CREATE INDEX IF NOT EXISTS partd_events_2020_bene_id_idx
    ON partd_events_2020 (bene_id);
CREATE INDEX IF NOT EXISTS partd_events_2021_bene_id_idx
    ON partd_events_2021 (bene_id);
CREATE INDEX IF NOT EXISTS partd_events_2022_bene_id_idx
    ON partd_events_2022 (bene_id); 
CREATE INDEX IF NOT EXISTS partd_events_2023_bene_id_idx
    ON partd_events_2023 (bene_id); 
CREATE INDEX IF NOT EXISTS partd_events_2024_bene_id_idx
    ON partd_events_2024 (bene_id);
CREATE INDEX IF NOT EXISTS partd_events_0_bene_id_idx
    ON partd_events_0 (bene_id);
	
-- =================================================
-- setup index on last_updated for yearly partitions
-- =================================================
CREATE INDEX IF NOT EXISTS partd_events_pre_2020_last_updated_idx
    ON partd_events_pre_2020 (last_updated);
CREATE INDEX IF NOT EXISTS partd_events_2020_last_updated_idx
    ON partd_events_2020 (last_updated);
CREATE INDEX IF NOT EXISTS partd_events_2021_last_updated_idx
    ON partd_events_2021 (last_updated);
CREATE INDEX IF NOT EXISTS partd_events_2022_last_updated_idx
    ON partd_events_2022 (last_updated); 
CREATE INDEX IF NOT EXISTS partd_events_2023_last_updated_idx
    ON partd_events_2023 (last_updated); 
CREATE INDEX IF NOT EXISTS partd_events_2024_last_updated_idx
    ON partd_events_2024 (last_updated);
CREATE INDEX IF NOT EXISTS partd_events_0_last_updated_idx
    ON partd_events_0 (last_updated);
    
-- =================================================
-- attach yearly partitions to parent table
--
-- You may have noticed that the range set in the
-- initial table includes the first day of the year
-- and the first day of the next year. This is
-- because Postgres partitions have an inclusive
-- lower bound and an exclusive upper bound. 
-- =================================================
ALTER TABLE partd_events_0
  ATTACH PARTITION partd_events_pre_2020
    FOR VALUES FROM ('1995-01-01') TO ('2020-01-01');
    
ALTER TABLE partd_events_0
  ATTACH PARTITION partd_events_2020
    FOR VALUES FROM ('2020-01-01') TO ('2020-12-31');
    
ALTER TABLE partd_events_0
  ATTACH PARTITION partd_events_2021
    FOR VALUES FROM ('2021-01-01') TO ('2021-12-31');
    
ALTER TABLE partd_events_0
  ATTACH PARTITION partd_events_2022
    FOR VALUES FROM ('2022-01-01') TO ('2022-12-31');
    
ALTER TABLE partd_events_0
  ATTACH PARTITION partd_events_2023
    FOR VALUES FROM ('2023-01-01') TO ('2023-12-31');
    
ALTER TABLE partd_events_0
  ATTACH PARTITION partd_events_2024
    FOR VALUES FROM ('2024-01-01') TO ('2024-12-31');
    
--
-- Do some renaming of tables so that we can test partitioned
-- vs. non-partitioned data handling.
--
-- non-paritioned
ALTER TABLE partd_events RENAME TO partd_events_orig;
--
-- paritioned
ALTER TABLE partd_events_0 RENAME TO partd_events;