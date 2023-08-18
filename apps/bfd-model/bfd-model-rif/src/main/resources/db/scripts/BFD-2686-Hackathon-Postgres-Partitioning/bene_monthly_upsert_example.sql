/*
This script demonstrates the DELETE-UPSERT capability of postgres; it is
intended to be run interactively (i.e., by a developer in a psql session).
It does the following:

1) INSERT a record into an empty partition (BENEFICIARY_MONTHLY_2024).
2) Verifies the just inserted record by SELECT(ing) it from the parent table.
3) Update the record that exists in the (parent) BENEFICIARY_MONTHLY table
   that physically exists in the BENEFICIARY_MONTHLY_2024 partition.
4) Verify that postgres auto-magically performs a DELETE and UPSERT, and
   the record now pysically exists in the BENEFICIARY_MONTHLY_2023 partition.
*/

--
-- STEP 1
-- Add record to (parent) beneficiary_monthly table.
--
insert into beneficiary_monthly (
	  bene_id
	, year_month
	, partd_contract_number_id
	, partc_contract_number_id
	, medicare_status_code
	, fips_state_cnty_code
	, entitlement_buy_in_ind
	, medicaid_dual_eligibility_code
	, partd_pbp_number_id
	, partd_retiree_drug_subsidy_ind
	, partd_segment_number_id
	, partd_low_income_cost_share_group_code
	, partc_pbp_number_id
) values (
	  -10000010288391
	, '2024-01-01'		-- '2023-05-01'
	, 'Z0005'
	, 'Y0008'
	, '20'
	, '6065'
	, '3'
	, 'NA'
	, '803'
	, 'N'
	, '000'
	, '03'
	, '801'
);

--
-- STEP 2
-- Verify the record was added to the (parent) beneficiary_monthly table.
-- Verify the record actually resides in the BENEFICIARY_MONTHLY_2024 partition.
--
select * from beneficiary_monthly where year_month > '2023-12-31';
select * from beneficiary_monthly_2024;

--
-- STEP 3
-- Update (parent) beneficiary_monthly record that changes year_month from
-- '2024-01-01' to '2023-12-01'.
--
-- Postgres should delete the record from the BENEFICIARY_MONTHLY_2024 partition
-- and insert (or update) the record in the BENEFICIARY_MONTHLY_2023 partition.
--
update beneficiary_monthly
	set year_month = '2023-12-01'	-- 2023 rcd will be inserted (or updated)
where
	bene_id = -10000010288391
and
	year_month = '2024-01-01';		-- 2024 rcd will be deleted

--
-- STEP 4
-- Verify that postgres removed the record from the BENEFICIARY_MONTHLY_2024 partition
-- and that record now resides in the BENEFICIARY_MONTHLY_2023 partition (and is still
-- accessible from the (parent) BENEFICIARY_MONTHLY table.
--
select * from beneficiary_monthly_2024;  -- should have no resultset

select * from beneficiary_monthly		 -- should still return the record that previously
  where year_month = '2023-12-01'        -- existed in the BENEFICIARY_MONTHLY_2024.
    and bene_id = -10000010288391;

select * from beneficiary_monthly_2023	 -- should still return the record that previously
  where year_month = '2023-12-01'        -- existed in the BENEFICIARY_MONTHLY_2024 but now
    and bene_id = -10000010288391;       -- exists in the BENEFICIARY_MONTHLY_2023 partition.
