/*
 * The column doesn't have a default value to avoid updating the column on migration. The pipeline server
 * will populate the column as new beneficaries are added or existing beneficaries are updated. 
 */

${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "Enrollments" (
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
    constraint "Enrollment_pkey" primary key ("parentBeneficiary", "yearMonth")
)
${logic.tablespaces-escape} tablespace "enrollment_ts"
;



INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0101'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyJanCode",
    "medicareStatusJanCode", "entitlementBuyInJanInd", "hmoIndicatorJanInd",
    "partCContractNumberJanId", "partCPbpNumberJanId", "partCPlanTypeJanCode",
    "partDContractNumberJanId", "partDPbpNumberJanId", "partDSegmentNumberJanId",
    "partDRetireeDrugSubsidyJanInd", "medicaidDualEligibilityJanCode", "partDLowIncomeCostShareGroupJanCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0201'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyFebCode",
    "medicareStatusFebCode", "entitlementBuyInFebInd", "hmoIndicatorFebInd",
    "partCContractNumberFebId", "partCPbpNumberFebId", "partCPlanTypeFebCode",
    "partDContractNumberFebId", "partDPbpNumberFebId", "partDSegmentNumberFebId",
    "partDRetireeDrugSubsidyFebInd", "medicaidDualEligibilityFebCode", "partDLowIncomeCostShareGroupFebCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0301'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyMarCode",
    "medicareStatusMarCode", "entitlementBuyInMarInd", "hmoIndicatorMarInd",
    "partCContractNumberMarId", "partCPbpNumberMarId", "partCPlanTypeMarCode",
    "partDContractNumberMarId", "partDPbpNumberMarId", "partDSegmentNumberMarId",
    "partDRetireeDrugSubsidyMarInd", "medicaidDualEligibilityMarCode", "partDLowIncomeCostShareGroupMarCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0401'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyAprCode",
    "medicareStatusAprCode", "entitlementBuyInAprInd", "hmoIndicatorAprInd",
    "partCContractNumberAprId", "partCPbpNumberAprId", "partCPlanTypeAprCode",
    "partDContractNumberAprId", "partDPbpNumberAprId", "partDSegmentNumberAprId",
    "partDRetireeDrugSubsidyAprInd", "medicaidDualEligibilityAprCode", "partDLowIncomeCostShareGroupAprCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0501'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyMayCode",
    "medicareStatusMayCode", "entitlementBuyInMayInd", "hmoIndicatorMayInd",
    "partCContractNumberMayId", "partCPbpNumberMayId", "partCPlanTypeMayCode",
    "partDContractNumberMayId", "partDPbpNumberMayId", "partDSegmentNumberMayId",
    "partDRetireeDrugSubsidyMayInd", "medicaidDualEligibilityMayCode", "partDLowIncomeCostShareGroupMayCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0601'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyJunCode",
    "medicareStatusJunCode", "entitlementBuyInJunInd", "hmoIndicatorJunInd",
    "partCContractNumberJunId", "partCPbpNumberJunId", "partCPlanTypeJunCode",
    "partDContractNumberJunId", "partDPbpNumberJunId", "partDSegmentNumberJunId",
    "partDRetireeDrugSubsidyJunInd", "medicaidDualEligibilityJunCode", "partDLowIncomeCostShareGroupJunCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0701'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyJulCode",
    "medicareStatusJulCode", "entitlementBuyInJulInd", "hmoIndicatorJulInd",
    "partCContractNumberJulId", "partCPbpNumberJulId", "partCPlanTypeJulCode",
    "partDContractNumberJulId", "partDPbpNumberJulId", "partDSegmentNumberJulId",
    "partDRetireeDrugSubsidyJulInd", "medicaidDualEligibilityJulCode", "partDLowIncomeCostShareGroupJulCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0801'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyAugCode",
    "medicareStatusAugCode", "entitlementBuyInAugInd", "hmoIndicatorAugInd",
    "partCContractNumberAugId", "partCPbpNumberAugId", "partCPlanTypeAugCode",
    "partDContractNumberAugId", "partDPbpNumberAugId", "partDSegmentNumberAugId",
    "partDRetireeDrugSubsidyAugInd", "medicaidDualEligibilityAugCode", "partDLowIncomeCostShareGroupAugCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '0901'), 'YYYYMMDD') as "yearMonth", "fipsStateCntySeptCode",
    "medicareStatusSeptCode", "entitlementBuyInSeptInd", "hmoIndicatorSeptInd",
    "partCContractNumberSeptId", "partCPbpNumberSeptId", "partCPlanTypeSeptCode",
    "partDContractNumberSeptId", "partDPbpNumberSeptId", "partDSegmentNumberSeptId",
    "partDRetireeDrugSubsidySeptInd", "medicaidDualEligibilitySeptCode", "partDLowIncomeCostShareGroupSeptCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollments"
   SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '1001'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyOctCode",
    "medicareStatusOctCode", "entitlementBuyInOctInd", "hmoIndicatorOctInd",
    "partCContractNumberOctId", "partCPbpNumberOctId", "partCPlanTypeOctCode",
    "partDContractNumberOctId", "partDPbpNumberOctId", "partDSegmentNumberOctId",
    "partDRetireeDrugSubsidyOctInd", "medicaidDualEligibilityOctCode", "partDLowIncomeCostShareGroupOctCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;  

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '1101'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyNovCode",
    "medicareStatusNovCode", "entitlementBuyInNovInd", "hmoIndicatorNovInd",
    "partCContractNumberNovId", "partCPbpNumberNovId", "partCPlanTypeNovCode",
    "partDContractNumberNovId", "partDPbpNumberNovId", "partDSegmentNumberNovId",
    "partDRetireeDrugSubsidyNovInd", "medicaidDualEligibilityNovCode", "partDLowIncomeCostShareGroupNovCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;  

INSERT INTO "Enrollments"
    SELECT  "beneficiaryId", TO_DATE(CONCAT("beneEnrollmentReferenceYear", '1201'), 'YYYYMMDD') as "yearMonth", "fipsStateCntyDecCode",
    "medicareStatusDecCode", "entitlementBuyInDecInd", "hmoIndicatorDecInd",
    "partCContractNumberDecId", "partCPbpNumberDecId", "partCPlanTypeDecCode",
    "partDContractNumberDecId", "partDPbpNumberDecId", "partDSegmentNumberDecId",
    "partDRetireeDrugSubsidyDecInd", "medicaidDualEligibilityDecCode", "partDLowIncomeCostShareGroupDecCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

alter table "Enrollments" 
  add constraint "Enrollment_parentClaim_to_Beneficiary" foreign key ("parentBeneficiary") 
  references "Beneficiaries";
