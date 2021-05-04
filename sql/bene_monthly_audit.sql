DROP TABLE IF EXISTS public.beneficiary_monthly_audit;

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
    CONSTRAINT "BeneficiaryMonthly_pkey"   PRIMARY KEY ("parentBeneficiary", "yearMonth")
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;