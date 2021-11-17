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
	"BeneficiaryMonthly"
on conflict on constraint
	beneficiary_monthly_pkey
do nothing;