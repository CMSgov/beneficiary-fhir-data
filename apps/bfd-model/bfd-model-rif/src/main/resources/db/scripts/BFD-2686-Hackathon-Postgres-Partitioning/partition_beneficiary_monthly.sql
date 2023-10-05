/*
Bunch of SQL to create a new partitioned BENEFICIARY_MONTHLY table
from existing BENEFICIARY_MONTHLY table.
*/

DROP TABLE IF EXISTS beneficiary_monthly_pre_2020;
DROP TABLE IF EXISTS beneficiary_monthly_2020;
DROP TABLE IF EXISTS beneficiary_monthly_2021;
DROP TABLE IF EXISTS beneficiary_monthly_2022;
DROP TABLE IF EXISTS beneficiary_monthly_2023;
DROP TABLE IF EXISTS beneficiary_monthly_2024;
DROP TABLE IF EXISTS beneficiary_monthly_0;

-- Create a partitioned BENEFICIARY_MONTHLY table.
CREATE TABLE IF NOT EXISTS beneficiary_monthly_0
(
    bene_id bigint NOT NULL,
    year_month date NOT NULL,
    partd_contract_number_id varchar(5),
    partc_contract_number_id varchar(5),
    medicare_status_code varchar(2),
    fips_state_cnty_code varchar(5),
    entitlement_buy_in_ind char(1),
    hmo_indicator_ind char(1),
    medicaid_dual_eligibility_code varchar(2),
    partd_pbp_number_id varchar(3),
    partd_retiree_drug_subsidy_ind char(1),
    partd_segment_number_id varchar(3),
    partd_low_income_cost_share_group_code varchar(2),
    partc_pbp_number_id varchar(3),
    partc_plan_type_code varchar(3),
    CONSTRAINT bene_monthly_pkey PRIMARY KEY (bene_id, year_month)
)
PARTITION BY RANGE (year_month);

-- =================================================
-- add FK constraint to beneficiaries table.
-- bene_id references beneficiaries (bene_id).
--
-- NOTE: holds exclusive lock
-- =================================================
ALTER TABLE beneficiary_monthly_0
    ADD CONSTRAINT beneficiary_monthly_0_bene_id_to_beneficiaries FOREIGN KEY (bene_id)
        REFERENCES beneficiaries (bene_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;

-- =================================================
-- create a partition table for data before 2020
-- =================================================
CREATE TABLE IF NOT EXISTS beneficiary_monthly_pre_2020
(
    bene_id bigint NOT NULL,
    year_month date NOT NULL,
    partd_contract_number_id varchar(5),
    partc_contract_number_id varchar(5),
    medicare_status_code varchar(2),
    fips_state_cnty_code varchar(5),
    entitlement_buy_in_ind char(1),
    hmo_indicator_ind char(1),
    medicaid_dual_eligibility_code varchar(2),
    partd_pbp_number_id varchar(3),
    partd_retiree_drug_subsidy_ind char(1),
    partd_segment_number_id varchar(3),
    partd_low_income_cost_share_group_code varchar(2),
    partc_pbp_number_id varchar(3),
    partc_plan_type_code varchar(3),
    CONSTRAINT beneficiary_monthly_pre_2020_pkey PRIMARY KEY (bene_id, year_month)
);

-- =================================================
-- create yearly partition tables for data; we simply
-- use the already defined table structure and then
-- add the primary key.
--
-- NOTE: adding primary key holds exclusive lock;
-- this is done on an empty table so not too big a hit.
-- =================================================
CREATE TABLE IF NOT EXISTS beneficiary_monthly_2020 (
	like beneficiary_monthly_pre_2020 including constraints including storage);
ALTER TABLE beneficiary_monthly_2020 ADD PRIMARY KEY (bene_id, year_month);
	
CREATE TABLE IF NOT EXISTS beneficiary_monthly_2021 (
	like beneficiary_monthly_pre_2020 including constraints including storage);
ALTER TABLE beneficiary_monthly_2021 ADD PRIMARY KEY (bene_id, year_month);

CREATE TABLE IF NOT EXISTS beneficiary_monthly_2022 (
	like beneficiary_monthly_pre_2020 including constraints including storage);
ALTER TABLE beneficiary_monthly_2022 ADD PRIMARY KEY (bene_id, year_month);
	
CREATE TABLE IF NOT EXISTS beneficiary_monthly_2023 (
	like beneficiary_monthly_pre_2020 including constraints including storage);
ALTER TABLE beneficiary_monthly_2023 ADD PRIMARY KEY (bene_id, year_month);

-- we won't data for 2024, but we create that table as a means to
-- test/verify postgres ability to automatically route data updates
-- to the correct partition.
CREATE TABLE IF NOT EXISTS beneficiary_monthly_2024 (
	like beneficiary_monthly_pre_2020 including constraints including storage);
ALTER TABLE beneficiary_monthly_2024 ADD PRIMARY KEY (bene_id, year_month);
  
-- =================================================
-- painful data migration into yearly partitions
-- (this may take some time depending on the db env)
--
-- NOTE:
-- since data already exists in the BENEFICIARY_MONTHLY
-- table, we'll be able to run somewhat 'fast and
-- loose' since BENE_ID,YEAR_MONTH is already being used as a
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
			bene_id,
			year_month,
			partd_contract_number_id,
			partc_contract_number_id,
			medicare_status_code,
			fips_state_cnty_code,
			entitlement_buy_in_ind,
			hmo_indicator_ind,
			medicaid_dual_eligibility_code,
			partd_pbp_number_id,
			partd_retiree_drug_subsidy_ind,
			partd_segment_number_id,
			partd_low_income_cost_share_group_code,
			partc_pbp_number_id,
			partc_plan_type_code
	  from beneficiary_monthly;

   LOOP
      IF rec.year_month < p_2020_lo THEN
      	INSERT INTO beneficiary_monthly_pre_2020 values (
			  rec.bene_id
			, rec.year_month
			, rec.partd_contract_number_id
			, rec.partc_contract_number_id
			, rec.medicare_status_code
			, rec.fips_state_cnty_code
			, rec.entitlement_buy_in_ind
			, rec.hmo_indicator_ind
			, rec.medicaid_dual_eligibility_code
			, rec.partd_pbp_number_id
			, rec.partd_retiree_drug_subsidy_ind
			, rec.partd_segment_number_id
			, rec.partd_low_income_cost_share_group_code
			, rec.partc_pbp_number_id
			, rec.partc_plan_type_code
		);
	  ELSIF rec.year_month < p_2021_lo THEN
      	INSERT INTO beneficiary_monthly_2020 values (
			  rec.bene_id
			, rec.year_month
			, rec.partd_contract_number_id
			, rec.partc_contract_number_id
			, rec.medicare_status_code
			, rec.fips_state_cnty_code
			, rec.entitlement_buy_in_ind
			, rec.hmo_indicator_ind
			, rec.medicaid_dual_eligibility_code
			, rec.partd_pbp_number_id
			, rec.partd_retiree_drug_subsidy_ind
			, rec.partd_segment_number_id
			, rec.partd_low_income_cost_share_group_code
			, rec.partc_pbp_number_id
			, rec.partc_plan_type_code
		);
	  ELSIF rec.year_month < p_2022_lo THEN
	    INSERT INTO beneficiary_monthly_2021 values (
			  rec.bene_id
			, rec.year_month
			, rec.partd_contract_number_id
			, rec.partc_contract_number_id
			, rec.medicare_status_code
			, rec.fips_state_cnty_code
			, rec.entitlement_buy_in_ind
			, rec.hmo_indicator_ind
			, rec.medicaid_dual_eligibility_code
			, rec.partd_pbp_number_id
			, rec.partd_retiree_drug_subsidy_ind
			, rec.partd_segment_number_id
			, rec.partd_low_income_cost_share_group_code
			, rec.partc_pbp_number_id
			, rec.partc_plan_type_code
		);
	  ELSIF rec.year_month < p_2023_lo THEN
	    INSERT INTO beneficiary_monthly_2022 values (
			  rec.bene_id
			, rec.year_month
			, rec.partd_contract_number_id
			, rec.partc_contract_number_id
			, rec.medicare_status_code
			, rec.fips_state_cnty_code
			, rec.entitlement_buy_in_ind
			, rec.hmo_indicator_ind
			, rec.medicaid_dual_eligibility_code
			, rec.partd_pbp_number_id
			, rec.partd_retiree_drug_subsidy_ind
			, rec.partd_segment_number_id
			, rec.partd_low_income_cost_share_group_code
			, rec.partc_pbp_number_id
			, rec.partc_plan_type_code
		);
	  ELSE
	    INSERT INTO beneficiary_monthly_2023 values (
			  rec.bene_id
			, rec.year_month
			, rec.partd_contract_number_id
			, rec.partc_contract_number_id
			, rec.medicare_status_code
			, rec.fips_state_cnty_code
			, rec.entitlement_buy_in_ind
			, rec.hmo_indicator_ind
			, rec.medicaid_dual_eligibility_code
			, rec.partd_pbp_number_id
			, rec.partd_retiree_drug_subsidy_ind
			, rec.partd_segment_number_id
			, rec.partd_low_income_cost_share_group_code
			, rec.partc_pbp_number_id
			, rec.partc_plan_type_code
		);     
      END IF;
   END LOOP;
END $$


-- =================================================
-- setup index on bene_id for yearly partitions
-- =================================================
CREATE INDEX IF NOT EXISTS beneficiary_monthly_pre_2020_bene_id_idx
    ON beneficiary_monthly_pre_2020 (bene_id);
CREATE INDEX IF NOT EXISTS beneficiary_monthly_2020_bene_id_idx
    ON beneficiary_monthly_2020 (bene_id);
CREATE INDEX IF NOT EXISTS beneficiary_monthly_2021_bene_id_idx
    ON beneficiary_monthly_2021 (bene_id);
CREATE INDEX IF NOT EXISTS beneficiary_monthly_2022_bene_id_idx
    ON beneficiary_monthly_2022 (bene_id); 
CREATE INDEX IF NOT EXISTS beneficiary_monthly_2023_bene_id_idx
    ON beneficiary_monthly_2023 (bene_id); 
CREATE INDEX IF NOT EXISTS beneficiary_monthly_2024_bene_id_idx
    ON beneficiary_monthly_2024 (bene_id);
CREATE INDEX IF NOT EXISTS beneficiary_monthly_0_bene_id_idx
    ON beneficiary_monthly_0 (bene_id);
	
    
-- =================================================
-- attach yearly partitions to parent table
--
-- You may have noticed that the range set in the
-- initial table includes the first day of the year
-- and the first day of the next year. This is
-- because Postgres partitions have an inclusive
-- lower bound and an exclusive upper bound. 
-- =================================================
ALTER TABLE beneficiary_monthly_0
  ATTACH PARTITION beneficiary_monthly_pre_2020
    FOR VALUES FROM ('003-01-01') TO ('2020-01-01');
    
ALTER TABLE beneficiary_monthly_0
  ATTACH PARTITION beneficiary_monthly_2020
    FOR VALUES FROM ('2020-01-01') TO ('2020-12-31');
    
ALTER TABLE beneficiary_monthly_0
  ATTACH PARTITION beneficiary_monthly_2021
    FOR VALUES FROM ('2021-01-01') TO ('2021-12-31');
    
ALTER TABLE beneficiary_monthly_0
  ATTACH PARTITION beneficiary_monthly_2022
    FOR VALUES FROM ('2022-01-01') TO ('2022-12-31');
    
ALTER TABLE beneficiary_monthly_0
  ATTACH PARTITION beneficiary_monthly_2023
    FOR VALUES FROM ('2023-01-01') TO ('2023-12-31');
    
ALTER TABLE beneficiary_monthly_0
  ATTACH PARTITION beneficiary_monthly_2024
    FOR VALUES FROM ('2024-01-01') TO ('2024-12-31');
    
--
-- Do some renaming of tables so that we can test partitioned
-- vs. non-partitioned data handling.
--
-- non-paritioned
ALTER TABLE beneficiary_monthly RENAME TO beneficiary_monthly_orig;
--
-- paritioned
ALTER TABLE beneficiary_monthly_0 RENAME TO beneficiary_monthly;