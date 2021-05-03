CREATE TRIGGER audit_ccw_update
AFTER UPDATE ON "BeneficiaryMonthly"
FOR EACH ROW
	WHEN ( (OLD."fipsStateCntyCode",
			OLD."medicareStatusCode",
			OLD."entitlementBuyInInd",
			OLD."hmoIndicatorInd",
			OLD."partCContractNumberId",
			OLD."partCPbpNumberId",
			OLD."partCPlanTypeCode",
			OLD."partDContractNumberId",
			OLD."partDPbpNumberId",
			OLD."partDSegmentNumberId",
			OLD."partDRetireeDrugSubsidyInd",
			OLD."medicaidDualEligibilityCode",
			OLD."partDLowIncomeCostShareGroupCode")
	IS DISTINCT FROM
		   (NEW."fipsStateCntyCode",
			NEW."medicareStatusCode",
			NEW."entitlementBuyInInd",
			NEW."hmoIndicatorInd",
			NEW."partCContractNumberId",
			NEW."partCPbpNumberId",
			NEW."partCPlanTypeCode",
			NEW."partDContractNumberId",
			NEW."partDPbpNumberId",
			NEW."partDSegmentNumberId",
			NEW."partDRetireeDrugSubsidyInd",
			NEW."medicaidDualEligibilityCode",
			NEW."partDLowIncomeCostShareGroupCode")	)
			
	EXECUTE FUNCTION track_bene_monthly_change();