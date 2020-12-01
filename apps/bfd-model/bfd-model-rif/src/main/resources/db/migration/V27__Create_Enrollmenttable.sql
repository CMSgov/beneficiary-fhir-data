/*
 * The column doesn't have a default value to avoid updating the column on migration. The pipeline server
 * will populate the column as new beneficaries are added or existing beneficaries are updated. 
 */

${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "Enrollment" (
    "enrollmentId" bigint not null,
    "beneficiaryId" varchar(15) not null,
    "date" varchar(7) not null,
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
    constraint "Enrollment_pkey" primary key ("enrollmentId")
)
${logic.tablespaces-escape} tablespace "enrollment_ts"
;

create sequence enrollment_enrollmentId_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 1000;


INSERT INTO "Enrollment"
    SELECT  nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-01') as "date", "fipsStateCntyJanCode",
    "medicareStatusJanCode", "entitlementBuyInJanInd", "hmoIndicatorJanInd",
    "partCContractNumberJanId", "partCPbpNumberJanId", "partCPlanTypeJanCode",
    "partDContractNumberJanId", "partDPbpNumberJanId", "partDSegmentNumberJanId",
    "partDRetireeDrugSubsidyJanInd", "medicaidDualEligibilityJanCode", "partDLowIncomeCostShareGroupJanCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-02') as "date", "fipsStateCntyFebCode",
    "medicareStatusFebCode", "entitlementBuyInFebInd", "hmoIndicatorFebInd",
    "partCContractNumberFebId", "partCPbpNumberFebId", "partCPlanTypeFebCode",
    "partDContractNumberFebId", "partDPbpNumberFebId", "partDSegmentNumberFebId",
    "partDRetireeDrugSubsidyFebInd", "medicaidDualEligibilityFebCode", "partDLowIncomeCostShareGroupFebCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-03') as "date", "fipsStateCntyMarCode",
    "medicareStatusMarCode", "entitlementBuyInMarInd", "hmoIndicatorMarInd",
    "partCContractNumberMarId", "partCPbpNumberMarId", "partCPlanTypeMarCode",
    "partDContractNumberMarId", "partDPbpNumberMarId", "partDSegmentNumberMarId",
    "partDRetireeDrugSubsidyMarInd", "medicaidDualEligibilityMarCode", "partDLowIncomeCostShareGroupMarCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-04') as "date", "fipsStateCntyAprCode",
    "medicareStatusAprCode", "entitlementBuyInAprInd", "hmoIndicatorAprInd",
    "partCContractNumberAprId", "partCPbpNumberAprId", "partCPlanTypeAprCode",
    "partDContractNumberAprId", "partDPbpNumberAprId", "partDSegmentNumberAprId",
    "partDRetireeDrugSubsidyAprInd", "medicaidDualEligibilityAprCode", "partDLowIncomeCostShareGroupAprCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-05') as "date", "fipsStateCntyMayCode",
    "medicareStatusMayCode", "entitlementBuyInMayInd", "hmoIndicatorMayInd",
    "partCContractNumberMayId", "partCPbpNumberMayId", "partCPlanTypeMayCode",
    "partDContractNumberMayId", "partDPbpNumberMayId", "partDSegmentNumberMayId",
    "partDRetireeDrugSubsidyMayInd", "medicaidDualEligibilityMayCode", "partDLowIncomeCostShareGroupMayCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-06') as "date", "fipsStateCntyJunCode",
    "medicareStatusJunCode", "entitlementBuyInJunInd", "hmoIndicatorJunInd",
    "partCContractNumberJunId", "partCPbpNumberJunId", "partCPlanTypeJunCode",
    "partDContractNumberJunId", "partDPbpNumberJunId", "partDSegmentNumberJunId",
    "partDRetireeDrugSubsidyJunInd", "medicaidDualEligibilityJunCode", "partDLowIncomeCostShareGroupJunCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-07') as "date", "fipsStateCntyJulCode",
    "medicareStatusJulCode", "entitlementBuyInJulInd", "hmoIndicatorJulInd",
    "partCContractNumberJulId", "partCPbpNumberJulId", "partCPlanTypeJulCode",
    "partDContractNumberJulId", "partDPbpNumberJulId", "partDSegmentNumberJulId",
    "partDRetireeDrugSubsidyJulInd", "medicaidDualEligibilityJulCode", "partDLowIncomeCostShareGroupJulCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-08') as "date", "fipsStateCntyAugCode",
    "medicareStatusAugCode", "entitlementBuyInAugInd", "hmoIndicatorAugInd",
    "partCContractNumberAugId", "partCPbpNumberAugId", "partCPlanTypeAugCode",
    "partDContractNumberAugId", "partDPbpNumberAugId", "partDSegmentNumberAugId",
    "partDRetireeDrugSubsidyAugInd", "medicaidDualEligibilityAugCode", "partDLowIncomeCostShareGroupAugCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-09') as "date", "fipsStateCntySeptCode",
    "medicareStatusSeptCode", "entitlementBuyInSeptInd", "hmoIndicatorSeptInd",
    "partCContractNumberSeptId", "partCPbpNumberSeptId", "partCPlanTypeSeptCode",
    "partDContractNumberSeptId", "partDPbpNumberSeptId", "partDSegmentNumberSeptId",
    "partDRetireeDrugSubsidySeptInd", "medicaidDualEligibilitySeptCode", "partDLowIncomeCostShareGroupSeptCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

INSERT INTO "Enrollment"
   SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-10') as "date", "fipsStateCntyOctCode",
    "medicareStatusOctCode", "entitlementBuyInOctInd", "hmoIndicatorOctInd",
    "partCContractNumberOctId", "partCPbpNumberOctId", "partCPlanTypeOctCode",
    "partDContractNumberOctId", "partDPbpNumberOctId", "partDSegmentNumberOctId",
    "partDRetireeDrugSubsidyOctInd", "medicaidDualEligibilityOctCode", "partDLowIncomeCostShareGroupOctCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;  

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-11') as "date", "fipsStateCntyNovCode",
    "medicareStatusNovCode", "entitlementBuyInNovInd", "hmoIndicatorNovInd",
    "partCContractNumberNovId", "partCPbpNumberNovId", "partCPlanTypeNovCode",
    "partDContractNumberNovId", "partDPbpNumberNovId", "partDSegmentNumberNovId",
    "partDRetireeDrugSubsidyNovInd", "medicaidDualEligibilityNovCode", "partDLowIncomeCostShareGroupNovCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;  

INSERT INTO "Enrollment"
    SELECT nextval(enrollment_enrollmentId_seq), "beneficiaryId", CONCAT("beneEnrollmentReferenceYear", '-12') as "date", "fipsStateCntyDecCode",
    "medicareStatusDecCode", "entitlementBuyInDecInd", "hmoIndicatorDecInd",
    "partCContractNumberDecId", "partCPbpNumberDecId", "partCPlanTypeDecCode",
    "partDContractNumberDecId", "partDPbpNumberDecId", "partDSegmentNumberDecId",
    "partDRetireeDrugSubsidyDecInd", "medicaidDualEligibilityDecCode", "partDLowIncomeCostShareGroupDecCode"
	FROM "Beneficiaries"
	WHERE "beneEnrollmentReferenceYear" is not null;

alter table "Enrollment" 
  add constraint "Enrollment_beneficiaryId_to_Beneficiary" foreign key ("beneficiaryId") 
  references "Beneficiaries";
