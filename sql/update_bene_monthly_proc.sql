CREATE OR REPLACE PROCEDURE update_bene_monthly(
	bene_id 					"BeneficiaryMonthly"."parentBeneficiary"%TYPE,
	yr_month					"BeneficiaryMonthly"."yearMonth"%TYPE,
	fips_cnty_code				"BeneficiaryMonthly"."fipsStateCntyCode"%TYPE,
	medi_status_code 			"BeneficiaryMonthly"."medicareStatusCode"%TYPE,
	buy_in_ind 					"BeneficiaryMonthly"."entitlementBuyInInd"%TYPE,
	hmo_ind 					"BeneficiaryMonthly"."hmoIndicatorInd"%TYPE,
	partc_contract_number_id 	"BeneficiaryMonthly"."partCContractNumberId"%TYPE,
	partc_pbp_number_id 		"BeneficiaryMonthly"."partCPbpNumberId"%TYPE,
	partc_plan_type 			"BeneficiaryMonthly"."partCPlanTypeCode"%TYPE,
	partd_contract_number_id 	"BeneficiaryMonthly"."partDContractNumberId"%TYPE,
	partd_pbp_number_id 		"BeneficiaryMonthly"."partDPbpNumberId"%TYPE,
	partd_segment_num 			"BeneficiaryMonthly"."partDSegmentNumberId"%TYPE,
	partd_retiree_mnthly 		"BeneficiaryMonthly"."partDRetireeDrugSubsidyInd"%TYPE,
	partd_low_inc_cost_share 	"BeneficiaryMonthly"."partDLowIncomeCostShareGroupCode"%TYPE,
	dual_elig_code 				"BeneficiaryMonthly"."medicaidDualEligibilityCode"%TYPE
)
AS
$$
    BEGIN
    	UPDATE "BeneficiaryMonthly"
    	SET
    		"fipsStateCntyCode"					= fips_cnty_code,
    		"medicareStatusCode"				= medi_status_code,
    		"entitlementBuyInInd"				= buy_in_ind,
    		"hmoIndicatorInd"					= hmo_ind,
    		"partCContractNumberId"				= partc_contract_number_id,
    		"partCPbpNumberId"					= partc_pbp_number_id,
    		"partCPlanTypeCode"					= partc_plan_type,
    		"partDContractNumberId"				= partd_contract_number_id,
    		"partDPbpNumberId"					= partd_pbp_number_id,
    		"partDSegmentNumberId"				= partd_segment_num,
    		"partDRetireeDrugSubsidyInd"		= partd_retiree_mnthly,
    		"partDLowIncomeCostShareGroupCode"	= partd_low_inc_cost_share,
    		"medicaidDualEligibilityCode"		= dual_elig_code
    	WHERE
    		"parentBeneficiary" = bene_id
    	AND
    		"yearMonth" = yr_month;
    END;
$$ LANGUAGE plpgsql;