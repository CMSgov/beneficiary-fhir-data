-- need to drop primary key as PG does not allow us to alter
-- an existing primary key with deferrable options.
alter table beneficiary_monthly
drop constraint if exists beneficiary_monthly_pkey;

-- re-create our primary key with deferrable options.
alter table beneficiary_monthly
add constraint beneficiary_monthly_pkey primary key (year_month, parent_beneficiary) deferrable initially deferred;

-- setup for parallel processing
SET max_parallel_workers = 6;
SET max_parallel_workers_per_gather = 6;
SET parallel_leader_participation = off;
SET parallel_tuple_cost = 0;
SET parallel_setup_cost = 0;
SET min_parallel_table_scan_size = 0;

insert into beneficiary_monthly (
	year_month,
	parent_beneficiary,
	partd_contract_number_id,
	partc_contract_number_id,
	fips_state_cnty_code,
	medicare_status_code,
	entitlement_buy_in_ind,
	hmo_indicator_ind,
	partc_pbp_number_id,
	partc_plan_type_code,
	partd_pbp_number_id,
	partd_segment_number_id,
	partd_retiree_drug_subsidy_ind,
	partd_low_income_cost_share_group_code,
	medicaid_dual_eligibility_code
)
select
	"yearMonth",
	Cast("parentBeneficiary" as bigint),
	"partDContractNumberId",
	"partCContractNumberId",
	"fipsStateCntyCode",
	"medicareStatusCode",
	"entitlementBuyInInd",
	"hmoIndicatorInd",
	"partCPbpNumberId",
	"partCPlanTypeCode",
	"partDPbpNumberId",
	"partDSegmentNumberId",
	"partDRetireeDrugSubsidyInd",
	"partDLowIncomeCostShareGroupCode",
	"medicaidDualEligibilityCode"
from
	"BeneficiaryMonthly";
