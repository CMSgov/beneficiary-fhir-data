CREATE OR REPLACE FUNCTION track_bene_monthly_change() RETURNS TRIGGER
AS $beneficiary_monthly_audit$
    BEGIN
        --
        -- Create a row in beneficiary_monthly_audit to reflect the operation performed
        -- on BeneficiaryMonthly; variable TG_OP denotes the operation.
        --
        IF (TG_OP = 'UPDATE') THEN
        /*
        	IF (
        		OLD."parentBeneficiary" 				<> NEW."parentBeneficiary"
			OR	OLD."yearMonth"  						<> NEW."yearMonth"
			OR	OLD."fipsStateCntyCode" 				<> NEW."fipsStateCntyCode"
			OR	OLD."medicareStatusCode" 				<> NEW."medicareStatusCode"
			OR	OLD."entitlementBuyInInd" 				<> NEW."entitlementBuyInInd"
			OR	OLD."hmoIndicatorInd" 					<> NEW."hmoIndicatorInd"
			OR	OLD."partCContractNumberId" 			<> NEW."partCContractNumberId"
			OR	OLD."partCPbpNumberId" 					<> NEW."partCContractNumberId"
			OR	OLD."partCPlanTypeCode" 				<> NEW."partCPlanTypeCode"
			OR	OLD."partDContractNumberId" 			<> NEW."partDContractNumberId"
			OR	OLD."partDPbpNumberId" 					<> NEW."partDPbpNumberId"
			OR	OLD."partDSegmentNumberId" 				<> NEW."partDSegmentNumberId"
			OR	OLD."partDRetireeDrugSubsidyInd" 		<> NEW."partDRetireeDrugSubsidyInd"
			OR	OLD."medicaidDualEligibilityCode" 		<> NEW."medicaidDualEligibilityCode"
			OR	OLD."partDLowIncomeCostShareGroupCode" 	<> NEW."partDLowIncomeCostShareGroupCode"
			)
			THEN
		*/
				INSERT INTO public.beneficiary_monthly_audit VALUES (OLD.*, 'U', now());
			--END IF;
            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN
			INSERT INTO public.beneficiary_monthly_audit VALUES(NEW.*, 'I', now());
            RETURN NEW;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
    END;
$beneficiary_monthly_audit$ LANGUAGE plpgsql;
