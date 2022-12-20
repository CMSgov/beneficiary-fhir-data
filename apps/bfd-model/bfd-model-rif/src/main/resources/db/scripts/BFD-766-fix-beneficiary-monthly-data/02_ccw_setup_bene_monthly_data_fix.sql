--
-- Various cleanup operations for remvoving previously created artifacts.
--
DROP TABLE IF EXISTS beneficiary_monthly_audit;
DROP TRIGGER IF EXISTS audit_ccw_insert ON "BeneficiaryMonthly";
DROP TRIGGER IF EXISTS audit_ccw_update ON "BeneficiaryMonthly";
DROP TRIGGER IF EXISTS audit_ccw_delete ON "BeneficiaryMonthly";

--
-- Create beneficiary_monthly_audit table
--
CREATE TABLE public.beneficiary_monthly_audit
(
    "parentBeneficiary"                    character varying(255) NOT NULL,
    "yearMonth"                            date NOT NULL,
    "fipsStateCntyCode"                    character varying(5),
    "medicareStatusCode"                   character varying(2),
    "entitlementBuyInInd"                  character(1),
    "hmoIndicatorInd"                      character(1),
    "partCContractNumberId"                character varying(5),
    "partCPbpNumberId"                     character varying(3) ,
    "partCPlanTypeCode"                    character varying(3),
    "partDContractNumberId"                character varying(5),
    "partDPbpNumberId"                     character varying(3),
    "partDSegmentNumberId"                 character varying(3),
    "partDRetireeDrugSubsidyInd"           char(1),
    "medicaidDualEligibilityCode"          character varying(2),
    "partDLowIncomeCostShareGroupCode"     character varying(2),
    "operation"                            char(1)   NOT NULL,
    "updateTs"                             timestamp NOT NULL,
    CONSTRAINT "BeneficiaryMonthlyAudit_pkey"   PRIMARY KEY ("parentBeneficiary", "yearMonth")
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

--
-- Create function that tracks INSERT, DELETE, and UPDATE operations
-- to the "BeneficiaryMonthly" table.
--
CREATE OR REPLACE FUNCTION public.track_bene_monthly_change() RETURNS TRIGGER
AS $beneficiary_monthly_audit$
    BEGIN
        --
        -- Create a row in beneficiary_monthly_audit to reflect the operation performed
        -- on BeneficiaryMonthly; variable TG_OP denotes the operation.
        --
        IF (TG_OP = 'UPDATE') THEN
                INSERT INTO public.beneficiary_monthly_audit VALUES (OLD.*, 'U', now());
            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN
            INSERT INTO public.beneficiary_monthly_audit VALUES(NEW.*, 'I', now());
            RETURN NEW;
        ELSIF (TG_OP = 'DELETE') THEN
            INSERT INTO public.beneficiary_monthly_audit VALUES(OLD.*, 'D', now());
            RETURN OLD;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
    END;
$beneficiary_monthly_audit$ LANGUAGE plpgsql;

--
-- Setup INSERT, DELETE and UPDATE triggers.
--
CREATE TRIGGER audit_ccw_insert
AFTER INSERT ON public."BeneficiaryMonthly"
FOR EACH ROW			
	EXECUTE FUNCTION track_bene_monthly_change();

CREATE TRIGGER audit_ccw_delete
AFTER DELETE ON public."BeneficiaryMonthly"
FOR EACH ROW			
	EXECUTE FUNCTION track_bene_monthly_change();

CREATE TRIGGER audit_ccw_update
AFTER UPDATE ON public."BeneficiaryMonthly"
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
            NEW."partDLowIncomeCostShareGroupCode")    )
            
    EXECUTE FUNCTION track_bene_monthly_change();
