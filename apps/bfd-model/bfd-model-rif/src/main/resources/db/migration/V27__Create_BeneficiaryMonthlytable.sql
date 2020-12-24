/*
 * The column doesn't have a default value to avoid updating the column on migration. The pipeline server
 * will populate the column as new beneficaries are added or existing beneficaries are updated. 
 */

${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "BeneficiaryMonthly" (
    "parentBeneficiary" varchar(255) not null,
    "yearMonth" date not null,
    "fipsStateCntyCode" varchar(5),
    "medicareStatusCode" varchar(2),
    "entitlementBuyInInd" char(1),
    "hmoIndicatorInd" char(1),
    "partCContractNumberId" varchar(5),
    "partCPbpNumberId" varchar(3),
    "partCPlanTypeCode" varchar(3),
    "partDContractNumberId" varchar(5),
    "partDPbpNumberId" varchar(3),
    "partDSegmentNumberId" varchar(3),
    "partDRetireeDrugSubsidyInd" char(1),
    "medicaidDualEligibilityCode" varchar(2),
    "partDLowIncomeCostShareGroupCode" varchar(2),
    constraint "BeneficiaryMonthly_pkey" primary key ("parentBeneficiary", "yearMonth")
)
${logic.tablespaces-escape} tablespace "beneficiarymonthly_ts"
;

/* Give read_only_bb role read access to "BeneficiaryMonthly" and svc_bfd_pipeline_role full access. These roles are shared by our API server and pipeline service accounts. */

GRANT SELECT ON TABLE public."BeneficiaryMonthly" TO read_only_bb;
GRANT ALL ON TABLE public."BeneficiaryMonthly" TO svc_bfd_pipeline_role;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/01/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyJanCode",
    "medicareStatusJanCode", "entitlementBuyInJanInd", "hmoIndicatorJanInd",
    "partCContractNumberJanId", "partCPbpNumberJanId", "partCPlanTypeJanCode",
    "partDContractNumberJanId", "partDPbpNumberJanId", "partDSegmentNumberJanId",
    "partDRetireeDrugSubsidyJanInd", "medicaidDualEligibilityJanCode", "partDLowIncomeCostShareGroupJanCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/02/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyFebCode",
    "medicareStatusFebCode", "entitlementBuyInFebInd", "hmoIndicatorFebInd",
    "partCContractNumberFebId", "partCPbpNumberFebId", "partCPlanTypeFebCode",
    "partDContractNumberFebId", "partDPbpNumberFebId", "partDSegmentNumberFebId",
    "partDRetireeDrugSubsidyFebInd", "medicaidDualEligibilityFebCode", "partDLowIncomeCostShareGroupFebCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/03/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyMarCode",
    "medicareStatusMarCode", "entitlementBuyInMarInd", "hmoIndicatorMarInd",
    "partCContractNumberMarId", "partCPbpNumberMarId", "partCPlanTypeMarCode",
    "partDContractNumberMarId", "partDPbpNumberMarId", "partDSegmentNumberMarId",
    "partDRetireeDrugSubsidyMarInd", "medicaidDualEligibilityMarCode", "partDLowIncomeCostShareGroupMarCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/04/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyAprCode",
    "medicareStatusAprCode", "entitlementBuyInAprInd", "hmoIndicatorAprInd",
    "partCContractNumberAprId", "partCPbpNumberAprId", "partCPlanTypeAprCode",
    "partDContractNumberAprId", "partDPbpNumberAprId", "partDSegmentNumberAprId",
    "partDRetireeDrugSubsidyAprInd", "medicaidDualEligibilityAprCode", "partDLowIncomeCostShareGroupAprCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/05/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyMayCode",
    "medicareStatusMayCode", "entitlementBuyInMayInd", "hmoIndicatorMayInd",
    "partCContractNumberMayId", "partCPbpNumberMayId", "partCPlanTypeMayCode",
    "partDContractNumberMayId", "partDPbpNumberMayId", "partDSegmentNumberMayId",
    "partDRetireeDrugSubsidyMayInd", "medicaidDualEligibilityMayCode", "partDLowIncomeCostShareGroupMayCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/06/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyJunCode",
    "medicareStatusJunCode", "entitlementBuyInJunInd", "hmoIndicatorJunInd",
    "partCContractNumberJunId", "partCPbpNumberJunId", "partCPlanTypeJunCode",
    "partDContractNumberJunId", "partDPbpNumberJunId", "partDSegmentNumberJunId",
    "partDRetireeDrugSubsidyJunInd", "medicaidDualEligibilityJunCode", "partDLowIncomeCostShareGroupJunCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/07/01'), 'YYYY/MM/DD')as "yearMonth", "fipsStateCntyJulCode",
    "medicareStatusJulCode", "entitlementBuyInJulInd", "hmoIndicatorJulInd",
    "partCContractNumberJulId", "partCPbpNumberJulId", "partCPlanTypeJulCode",
    "partDContractNumberJulId", "partDPbpNumberJulId", "partDSegmentNumberJulId",
    "partDRetireeDrugSubsidyJulInd", "medicaidDualEligibilityJulCode", "partDLowIncomeCostShareGroupJulCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/08/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyAugCode",
    "medicareStatusAugCode", "entitlementBuyInAugInd", "hmoIndicatorAugInd",
    "partCContractNumberAugId", "partCPbpNumberAugId", "partCPlanTypeAugCode",
    "partDContractNumberAugId", "partDPbpNumberAugId", "partDSegmentNumberAugId",
    "partDRetireeDrugSubsidyAugInd", "medicaidDualEligibilityAugCode", "partDLowIncomeCostShareGroupAugCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/09/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntySeptCode",
    "medicareStatusSeptCode", "entitlementBuyInSeptInd", "hmoIndicatorSeptInd",
    "partCContractNumberSeptId", "partCPbpNumberSeptId", "partCPlanTypeSeptCode",
    "partDContractNumberSeptId", "partDPbpNumberSeptId", "partDSegmentNumberSeptId",
    "partDRetireeDrugSubsidySeptInd", "medicaidDualEligibilitySeptCode", "partDLowIncomeCostShareGroupSeptCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "BeneficiaryMonthly" 
   SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/10/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyOctCode",
    "medicareStatusOctCode", "entitlementBuyInOctInd", "hmoIndicatorOctInd",
    "partCContractNumberOctId", "partCPbpNumberOctId", "partCPlanTypeOctCode",
    "partDContractNumberOctId", "partDPbpNumberOctId", "partDSegmentNumberOctId",
    "partDRetireeDrugSubsidyOctInd", "medicaidDualEligibilityOctCode", "partDLowIncomeCostShareGroupOctCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;  

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/11/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyNovCode",
    "medicareStatusNovCode", "entitlementBuyInNovInd", "hmoIndicatorNovInd",
    "partCContractNumberNovId", "partCPbpNumberNovId", "partCPlanTypeNovCode",
    "partDContractNumberNovId", "partDPbpNumberNovId", "partDSegmentNumberNovId",
    "partDRetireeDrugSubsidyNovInd", "medicaidDualEligibilityNovCode", "partDLowIncomeCostShareGroupNovCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;  

INSERT INTO "BeneficiaryMonthly" 
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '/12/01'), 'YYYY/MM/DD') as "yearMonth", "fipsStateCntyDecCode",
    "medicareStatusDecCode", "entitlementBuyInDecInd", "hmoIndicatorDecInd",
    "partCContractNumberDecId", "partCPbpNumberDecId", "partCPlanTypeDecCode",
    "partDContractNumberDecId", "partDPbpNumberDecId", "partDSegmentNumberDecId",
    "partDRetireeDrugSubsidyDecInd", "medicaidDualEligibilityDecCode", "partDLowIncomeCostShareGroupDecCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

alter table "BeneficiaryMonthly" 
  add constraint "BeneficiaryMonthly_parentClaim_to_Beneficiary" foreign key ("parentBeneficiary") 
  references "Beneficiaries";
