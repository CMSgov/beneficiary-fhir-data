/*
 * This migration has two primary goals:
 *
 * First, we want to convert the "beneficiaryId" and "claimId" columns to `bigint`, rather than their current
 * `varchar(15)` types. Per the PostgreSQL documentation, a `varchar(15)` column requires 16 bytes per record
 * on disk [1], and a `bigint` column only requires 8 bytes. Accordingly, this change roughly halves the size
 * of our indexes using those columns.
 *
 * Second, we're _finally_ moving to a more idiomatic (and user-friendly) naming scheme for these tables.
 *
 * In addition to those primary goals, this schema migration is also being used as a good opportunity to
 * address all of these other less major schema changes:
 * 
 * * TODO
 *
 * Overall, these migrations will be done in three steps:
 *
 * 1. Clone the tables to new tables with all of the desired schema changes, copying the original data over.
 * 2. Deploy a new version of the application that only uses those new tables.
 * 3. Remove the original tables.
 * 
 * This script is for Step 1. 
 *
 * [1]: https://www.postgresql.org/docs/10/datatype-character.html
 * [2]: https://www.postgresql.org/docs/10/datatype-numeric.html
 */


-- TODO: Right now, this is just a dump of the current BFD schema in prod, less all of the objects that I'm pretty sure we don't want to touch. 

--
-- Name: Beneficiaries; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."Beneficiaries" (
    "beneficiaryId" character varying(15) NOT NULL,
    "birthDate" date NOT NULL,
    "countyCode" character varying(10) NOT NULL,
    "endStageRenalDiseaseCode" character(1),
    "entitlementCodeCurrent" character(1),
    "entitlementCodeOriginal" character(1),
    hicn character varying(64) NOT NULL,
    "medicareEnrollmentStatusCode" character varying(2),
    "nameGiven" character varying(15) NOT NULL,
    "nameMiddleInitial" character(1),
    "nameSurname" character varying(24) NOT NULL,
    "partATerminationCode" character(1),
    "partBTerminationCode" character(1),
    "postalCode" character varying(9) NOT NULL,
    race character(1),
    sex character(1) NOT NULL,
    "stateCode" character varying(2) NOT NULL,
    "hicnUnhashed" character varying(11),
    "medicareBeneficiaryId" character varying(11),
    "beneficiaryDateOfDeath" date,
    "beneEnrollmentReferenceYear" numeric(4,0),
    "partAMonthsCount" numeric(3,0),
    "partBMonthsCount" numeric(3,0),
    "stateBuyInCoverageCount" numeric(3,0),
    "hmoCoverageCount" numeric(3,0),
    "monthsRetireeDrugSubsidyCoverage" numeric(3,0),
    "sourceOfEnrollmentData" character varying(3),
    "sampleMedicareGroupIndicator" character varying(2),
    "enhancedMedicareSampleIndicator" character(1),
    "currentBeneficiaryIdCode" character varying(2),
    "ageOfBeneficiary" numeric(3,0),
    "medicareCoverageStartDate" date,
    "monthsOfDualEligibility" numeric(3,0),
    "fipsStateCntyJanCode" character varying(5),
    "fipsStateCntyFebCode" character varying(5),
    "fipsStateCntyMarCode" character varying(5),
    "fipsStateCntyAprCode" character varying(5),
    "fipsStateCntyMayCode" character varying(5),
    "fipsStateCntyJunCode" character varying(5),
    "fipsStateCntyJulCode" character varying(5),
    "fipsStateCntyAugCode" character varying(5),
    "fipsStateCntySeptCode" character varying(5),
    "fipsStateCntyOctCode" character varying(5),
    "fipsStateCntyNovCode" character varying(5),
    "fipsStateCntyDecCode" character varying(5),
    "validDateOfDeathSw" character(1),
    "rtiRaceCode" character(1),
    "medicareStatusJanCode" character varying(2),
    "medicareStatusFebCode" character varying(2),
    "medicareStatusMarCode" character varying(2),
    "medicareStatusAprCode" character varying(2),
    "medicareStatusMayCode" character varying(2),
    "medicareStatusJunCode" character varying(2),
    "medicareStatusJulCode" character varying(2),
    "medicareStatusAugCode" character varying(2),
    "medicareStatusSeptCode" character varying(2),
    "medicareStatusOctCode" character varying(2),
    "medicareStatusNovCode" character varying(2),
    "medicareStatusDecCode" character varying(2),
    "partDMonthsCount" numeric(3,0),
    "entitlementBuyInJanInd" character(1),
    "entitlementBuyInFebInd" character(1),
    "entitlementBuyInMarInd" character(1),
    "entitlementBuyInAprInd" character(1),
    "entitlementBuyInMayInd" character(1),
    "entitlementBuyInJunInd" character(1),
    "entitlementBuyInJulInd" character(1),
    "entitlementBuyInAugInd" character(1),
    "entitlementBuyInSeptInd" character(1),
    "entitlementBuyInOctInd" character(1),
    "entitlementBuyInNovInd" character(1),
    "entitlementBuyInDecInd" character(1),
    "hmoIndicatorJanInd" character(1),
    "hmoIndicatorFebInd" character(1),
    "hmoIndicatorMarInd" character(1),
    "hmoIndicatorAprInd" character(1),
    "hmoIndicatorMayInd" character(1),
    "hmoIndicatorJunInd" character(1),
    "hmoIndicatorJulInd" character(1),
    "hmoIndicatorAugInd" character(1),
    "hmoIndicatorSeptInd" character(1),
    "hmoIndicatorOctInd" character(1),
    "hmoIndicatorNovInd" character(1),
    "hmoIndicatorDecInd" character(1),
    "partCContractNumberJanId" character varying(5),
    "partCContractNumberFebId" character varying(5),
    "partCContractNumberMarId" character varying(5),
    "partCContractNumberAprId" character varying(5),
    "partCContractNumberMayId" character varying(5),
    "partCContractNumberJunId" character varying(5),
    "partCContractNumberJulId" character varying(5),
    "partCContractNumberAugId" character varying(5),
    "partCContractNumberSeptId" character varying(5),
    "partCContractNumberOctId" character varying(5),
    "partCContractNumberNovId" character varying(5),
    "partCContractNumberDecId" character varying(5),
    "partCPbpNumberJanId" character varying(3),
    "partCPbpNumberFebId" character varying(3),
    "partCPbpNumberMarId" character varying(3),
    "partCPbpNumberAprId" character varying(3),
    "partCPbpNumberMayId" character varying(3),
    "partCPbpNumberJunId" character varying(3),
    "partCPbpNumberJulId" character varying(3),
    "partCPbpNumberAugId" character varying(3),
    "partCPbpNumberSeptId" character varying(3),
    "partCPbpNumberOctId" character varying(3),
    "partCPbpNumberNovId" character varying(3),
    "partCPbpNumberDecId" character varying(3),
    "partCPlanTypeJanCode" character varying(3),
    "partCPlanTypeFebCode" character varying(3),
    "partCPlanTypeMarCode" character varying(3),
    "partCPlanTypeAprCode" character varying(3),
    "partCPlanTypeMayCode" character varying(3),
    "partCPlanTypeJunCode" character varying(3),
    "partCPlanTypeJulCode" character varying(3),
    "partCPlanTypeAugCode" character varying(3),
    "partCPlanTypeSeptCode" character varying(3),
    "partCPlanTypeOctCode" character varying(3),
    "partCPlanTypeNovCode" character varying(3),
    "partCPlanTypeDecCode" character varying(3),
    "partDContractNumberJanId" character varying(5),
    "partDContractNumberFebId" character varying(5),
    "partDContractNumberMarId" character varying(5),
    "partDContractNumberAprId" character varying(5),
    "partDContractNumberMayId" character varying(5),
    "partDContractNumberJunId" character varying(5),
    "partDContractNumberJulId" character varying(5),
    "partDContractNumberAugId" character varying(5),
    "partDContractNumberSeptId" character varying(5),
    "partDContractNumberOctId" character varying(5),
    "partDContractNumberNovId" character varying(5),
    "partDContractNumberDecId" character varying(5),
    "partDPbpNumberJanId" character varying(3),
    "partDPbpNumberFebId" character varying(3),
    "partDPbpNumberMarId" character varying(3),
    "partDPbpNumberAprId" character varying(3),
    "partDPbpNumberMayId" character varying(3),
    "partDPbpNumberJunId" character varying(3),
    "partDPbpNumberJulId" character varying(3),
    "partDPbpNumberAugId" character varying(3),
    "partDPbpNumberSeptId" character varying(3),
    "partDPbpNumberOctId" character varying(3),
    "partDPbpNumberNovId" character varying(3),
    "partDPbpNumberDecId" character varying(3),
    "partDSegmentNumberJanId" character varying(3),
    "partDSegmentNumberFebId" character varying(3),
    "partDSegmentNumberMarId" character varying(3),
    "partDSegmentNumberAprId" character varying(3),
    "partDSegmentNumberMayId" character varying(3),
    "partDSegmentNumberJunId" character varying(3),
    "partDSegmentNumberJulId" character varying(3),
    "partDSegmentNumberAugId" character varying(3),
    "partDSegmentNumberSeptId" character varying(3),
    "partDSegmentNumberOctId" character varying(3),
    "partDSegmentNumberNovId" character varying(3),
    "partDSegmentNumberDecId" character varying(3),
    "partDRetireeDrugSubsidyJanInd" character(1),
    "partDRetireeDrugSubsidyFebInd" character(1),
    "partDRetireeDrugSubsidyMarInd" character(1),
    "partDRetireeDrugSubsidyAprInd" character(1),
    "partDRetireeDrugSubsidyMayInd" character(1),
    "partDRetireeDrugSubsidyJunInd" character(1),
    "partDRetireeDrugSubsidyJulInd" character(1),
    "partDRetireeDrugSubsidyAugInd" character(1),
    "partDRetireeDrugSubsidySeptInd" character(1),
    "partDRetireeDrugSubsidyOctInd" character(1),
    "partDRetireeDrugSubsidyNovInd" character(1),
    "partDRetireeDrugSubsidyDecInd" character(1),
    "medicaidDualEligibilityJanCode" character varying(2),
    "medicaidDualEligibilityFebCode" character varying(2),
    "medicaidDualEligibilityMarCode" character varying(2),
    "medicaidDualEligibilityAprCode" character varying(2),
    "medicaidDualEligibilityMayCode" character varying(2),
    "medicaidDualEligibilityJunCode" character varying(2),
    "medicaidDualEligibilityJulCode" character varying(2),
    "medicaidDualEligibilityAugCode" character varying(2),
    "medicaidDualEligibilitySeptCode" character varying(2),
    "medicaidDualEligibilityOctCode" character varying(2),
    "medicaidDualEligibilityNovCode" character varying(2),
    "medicaidDualEligibilityDecCode" character varying(2),
    "partDLowIncomeCostShareGroupJanCode" character varying(2),
    "partDLowIncomeCostShareGroupFebCode" character varying(2),
    "partDLowIncomeCostShareGroupMarCode" character varying(2),
    "partDLowIncomeCostShareGroupAprCode" character varying(2),
    "partDLowIncomeCostShareGroupMayCode" character varying(2),
    "partDLowIncomeCostShareGroupJunCode" character varying(2),
    "partDLowIncomeCostShareGroupJulCode" character varying(2),
    "partDLowIncomeCostShareGroupAugCode" character varying(2),
    "partDLowIncomeCostShareGroupSeptCode" character varying(2),
    "partDLowIncomeCostShareGroupOctCode" character varying(2),
    "partDLowIncomeCostShareGroupNovCode" character varying(2),
    "partDLowIncomeCostShareGroupDecCode" character varying(2),
    "mbiHash" character varying(64),
    lastupdated timestamp with time zone,
    "derivedMailingAddress1" character varying(40),
    "derivedMailingAddress2" character varying(40),
    "derivedMailingAddress3" character varying(40),
    "derivedMailingAddress4" character varying(40),
    "derivedMailingAddress5" character varying(40),
    "derivedMailingAddress6" character varying(40),
    "derivedCityName" character varying(100),
    "derivedStateCode" character varying(2),
    "derivedZipCode" character varying(9),
    "mbiEffectiveDate" date,
    "mbiObsoleteDate" date,
    "beneLinkKey" numeric(38,0)
);


ALTER TABLE public."Beneficiaries" OWNER TO svc_fhir_etl;

--
-- Name: BeneficiariesHistory; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."BeneficiariesHistory" (
    "beneficiaryHistoryId" bigint NOT NULL,
    "beneficiaryId" character varying(15) NOT NULL,
    "birthDate" date NOT NULL,
    hicn character varying(64) NOT NULL,
    sex character(1) NOT NULL,
    "hicnUnhashed" character varying(11),
    "medicareBeneficiaryId" character varying(11),
    "mbiHash" character varying(64),
    lastupdated timestamp with time zone,
    "mbiEffectiveDate" date,
    "mbiObsoleteDate" date
);


ALTER TABLE public."BeneficiariesHistory" OWNER TO svc_fhir_etl;

--
-- Name: BeneficiariesHistoryInvalidBeneficiaries; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."BeneficiariesHistoryInvalidBeneficiaries" (
    "beneficiaryHistoryId" bigint NOT NULL,
    "beneficiaryId" character varying(15),
    "birthDate" date NOT NULL,
    hicn character varying(64) NOT NULL,
    sex character(1) NOT NULL,
    "hicnUnhashed" character varying(11),
    "medicareBeneficiaryId" character varying(11)
);


ALTER TABLE public."BeneficiariesHistoryInvalidBeneficiaries" OWNER TO svc_fhir_etl;

--
-- Name: BeneficiaryMonthly; Type: TABLE; Schema: public; Owner: svc_bfd_pipeline_0
--

CREATE TABLE public."BeneficiaryMonthly" (
    "parentBeneficiary" character varying(255) NOT NULL,
    "yearMonth" date NOT NULL,
    "fipsStateCntyCode" character varying(5),
    "medicareStatusCode" character varying(2),
    "entitlementBuyInInd" character(1),
    "hmoIndicatorInd" character(1),
    "partCContractNumberId" character varying(5),
    "partCPbpNumberId" character varying(3),
    "partCPlanTypeCode" character varying(3),
    "partDContractNumberId" character varying(5),
    "partDPbpNumberId" character varying(3),
    "partDSegmentNumberId" character varying(3),
    "partDRetireeDrugSubsidyInd" character(1),
    "medicaidDualEligibilityCode" character varying(2),
    "partDLowIncomeCostShareGroupCode" character varying(2)
);


ALTER TABLE public."BeneficiaryMonthly" OWNER TO svc_bfd_pipeline_0;

--
-- Name: CarrierClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."CarrierClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "allowedChargeAmount" numeric(12,2) NOT NULL,
    "anesthesiaUnitCount" numeric NOT NULL,
    "beneficiaryPartBDeductAmount" numeric(12,2) NOT NULL,
    "beneficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "betosCode" character varying(3),
    "cliaLabNumber" character varying(10),
    "cmsServiceTypeCode" character(1) NOT NULL,
    "coinsuranceAmount" numeric(12,2) NOT NULL,
    "diagnosisCode" character varying(7),
    "diagnosisCodeVersion" character(1),
    "firstExpenseDate" date,
    "hcpcsCode" character varying(5),
    "hcpcsInitialModifierCode" character varying(5),
    "hcpcsSecondModifierCode" character varying(5),
    "hctHgbTestResult" numeric(4,1) NOT NULL,
    "hctHgbTestTypeCode" character varying(2),
    "hpsaScarcityCode" character(1),
    "lastExpenseDate" date,
    "linePricingLocalityCode" character varying(2) NOT NULL,
    "mtusCode" character(1),
    "mtusCount" numeric NOT NULL,
    "nationalDrugCode" character varying(11),
    "organizationNpi" character varying(10),
    "paymentAmount" numeric(12,2) NOT NULL,
    "paymentCode" character(1),
    "performingPhysicianNpi" character varying(12),
    "performingPhysicianUpin" character varying(12),
    "performingProviderIdNumber" character varying(15) NOT NULL,
    "placeOfServiceCode" character varying(2) NOT NULL,
    "primaryPayerCode" character(1),
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "processingIndicatorCode" character varying(2),
    "providerParticipatingIndCode" character(1),
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "providerSpecialityCode" character varying(3),
    "providerStateCode" character varying(2),
    "providerTaxNumber" character varying(10) NOT NULL,
    "providerTypeCode" character(1) NOT NULL,
    "providerZipCode" character varying(9),
    "reducedPaymentPhysicianAsstCode" character(1) NOT NULL,
    "rxNumber" character varying(30),
    "serviceCount" numeric NOT NULL,
    "serviceDeductibleCode" character(1),
    "submittedChargeAmount" numeric(12,2) NOT NULL
);


ALTER TABLE public."CarrierClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: CarrierClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."CarrierClaims" (
    "claimId" character varying(15) NOT NULL,
    "allowedChargeAmount" numeric(12,2) NOT NULL,
    "beneficiaryId" character varying(15) NOT NULL,
    "beneficiaryPartBDeductAmount" numeric(12,2) NOT NULL,
    "beneficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "carrierNumber" character varying(5) NOT NULL,
    "claimDispositionCode" character varying(2) NOT NULL,
    "claimEntryCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimTypeCode" character varying(2) NOT NULL,
    "clinicalTrialNumber" character varying(8),
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "hcpcsYearCode" character(1),
    "nearLineRecordIdCode" character(1) NOT NULL,
    "paymentAmount" numeric(12,2) NOT NULL,
    "paymentDenialCode" character varying(2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "providerAssignmentIndicator" character(1),
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "referringPhysicianNpi" character varying(12),
    "referringPhysicianUpin" character varying(12),
    "referringProviderIdNumber" character varying(14) NOT NULL,
    "submittedChargeAmount" numeric(12,2) NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "claimCarrierControlNumber" character varying(23)
);


ALTER TABLE public."CarrierClaims" OWNER TO svc_fhir_etl;

--
-- Name: DMEClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."DMEClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "allowedChargeAmount" numeric(12,2) NOT NULL,
    "beneficiaryPartBDeductAmount" numeric(12,2) NOT NULL,
    "beneficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "betosCode" character varying(3),
    "cmsServiceTypeCode" character(1) NOT NULL,
    "coinsuranceAmount" numeric(12,2) NOT NULL,
    "diagnosisCode" character varying(7),
    "diagnosisCodeVersion" character(1),
    "firstExpenseDate" date,
    "hcpcsCode" character varying(5),
    "hcpcsFourthModifierCode" character varying(5),
    "hcpcsInitialModifierCode" character varying(5),
    "hcpcsSecondModifierCode" character varying(5),
    "hcpcsThirdModifierCode" character varying(5),
    "hctHgbTestResult" numeric(3,1) NOT NULL,
    "hctHgbTestTypeCode" character varying(2),
    "lastExpenseDate" date,
    "mtusCode" character(1),
    "mtusCount" numeric NOT NULL,
    "nationalDrugCode" character varying(11),
    "paymentAmount" numeric(12,2) NOT NULL,
    "paymentCode" character(1),
    "placeOfServiceCode" character varying(2) NOT NULL,
    "pricingStateCode" character varying(2),
    "primaryPayerAllowedChargeAmount" numeric(12,2) NOT NULL,
    "primaryPayerCode" character(1),
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "processingIndicatorCode" character varying(2),
    "providerBillingNumber" character varying(10),
    "providerNPI" character varying(12),
    "providerParticipatingIndCode" character(1),
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "providerSpecialityCode" character varying(3),
    "providerStateCode" character varying(2) NOT NULL,
    "providerTaxNumber" character varying(10) NOT NULL,
    "purchasePriceAmount" numeric(12,2) NOT NULL,
    "screenSavingsAmount" numeric(12,2),
    "serviceCount" numeric NOT NULL,
    "serviceDeductibleCode" character(1),
    "submittedChargeAmount" numeric(12,2) NOT NULL,
    "supplierTypeCode" character(1)
);


ALTER TABLE public."DMEClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: DMEClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."DMEClaims" (
    "claimId" character varying(15) NOT NULL,
    "allowedChargeAmount" numeric(12,2) NOT NULL,
    "beneficiaryId" character varying(15) NOT NULL,
    "beneficiaryPartBDeductAmount" numeric(12,2) NOT NULL,
    "beneficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "carrierNumber" character varying(5) NOT NULL,
    "claimDispositionCode" character varying(2) NOT NULL,
    "claimEntryCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimTypeCode" character varying(2) NOT NULL,
    "clinicalTrialNumber" character varying(8),
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "hcpcsYearCode" character(1),
    "nearLineRecordIdCode" character(1) NOT NULL,
    "paymentAmount" numeric(12,2) NOT NULL,
    "paymentDenialCode" character varying(2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "providerAssignmentIndicator" character(1) NOT NULL,
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "referringPhysicianNpi" character varying(12),
    "referringPhysicianUpin" character varying(12),
    "submittedChargeAmount" numeric(12,2) NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "claimCarrierControlNumber" character varying(23)
);


ALTER TABLE public."DMEClaims" OWNER TO svc_fhir_etl;

--
-- Name: HHAClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."HHAClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "apcOrHippsCode" character varying(5),
    "deductibleCoinsuranceCd" character(1),
    "hcpcsCode" character varying(5),
    "hcpcsInitialModifierCode" character varying(5),
    "hcpcsSecondModifierCode" character varying(5),
    "nationalDrugCodeQualifierCode" character varying(2),
    "nationalDrugCodeQuantity" numeric,
    "nonCoveredChargeAmount" numeric(12,2) NOT NULL,
    "paymentAmount" numeric(12,2) NOT NULL,
    "paymentMethodCode" character varying(2),
    "rateAmount" numeric(12,2) NOT NULL,
    "revCntr1stAnsiCd" character varying(5),
    "revenueCenterCode" character varying(4) NOT NULL,
    "revenueCenterDate" date,
    "revenueCenterRenderingPhysicianNPI" character varying(12),
    "revenueCenterRenderingPhysicianUPIN" character varying(12),
    "statusCode" character varying(2),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "unitCount" numeric NOT NULL
);


ALTER TABLE public."HHAClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: HHAClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."HHAClaims" (
    "claimId" character varying(15) NOT NULL,
    "attendingPhysicianNpi" character varying(10),
    "attendingPhysicianUpin" character varying(9),
    "beneficiaryId" character varying(15) NOT NULL,
    "careStartDate" date,
    "claimFacilityTypeCode" character(1) NOT NULL,
    "claimFrequencyCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimLUPACode" character(1),
    "claimNonPaymentReasonCode" character varying(2),
    "claimPrimaryPayerCode" character(1),
    "claimReferralCode" character(1),
    "claimServiceClassificationTypeCode" character(1) NOT NULL,
    "claimTypeCode" character varying(2) NOT NULL,
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis13Code" character varying(7),
    "diagnosis13CodeVersion" character(1),
    "diagnosis14Code" character varying(7),
    "diagnosis14CodeVersion" character(1),
    "diagnosis15Code" character varying(7),
    "diagnosis15CodeVersion" character(1),
    "diagnosis16Code" character varying(7),
    "diagnosis16CodeVersion" character(1),
    "diagnosis17Code" character varying(7),
    "diagnosis17CodeVersion" character(1),
    "diagnosis18Code" character varying(7),
    "diagnosis18CodeVersion" character(1),
    "diagnosis19Code" character varying(7),
    "diagnosis19CodeVersion" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis20Code" character varying(7),
    "diagnosis20CodeVersion" character(1),
    "diagnosis21Code" character varying(7),
    "diagnosis21CodeVersion" character(1),
    "diagnosis22Code" character varying(7),
    "diagnosis22CodeVersion" character(1),
    "diagnosis23Code" character varying(7),
    "diagnosis23CodeVersion" character(1),
    "diagnosis24Code" character varying(7),
    "diagnosis24CodeVersion" character(1),
    "diagnosis25Code" character varying(7),
    "diagnosis25CodeVersion" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosisExternal10Code" character varying(7),
    "diagnosisExternal10CodeVersion" character(1),
    "diagnosisExternal11Code" character varying(7),
    "diagnosisExternal11CodeVersion" character(1),
    "diagnosisExternal12Code" character varying(7),
    "diagnosisExternal12CodeVersion" character(1),
    "diagnosisExternal1Code" character varying(7),
    "diagnosisExternal1CodeVersion" character(1),
    "diagnosisExternal2Code" character varying(7),
    "diagnosisExternal2CodeVersion" character(1),
    "diagnosisExternal3Code" character varying(7),
    "diagnosisExternal3CodeVersion" character(1),
    "diagnosisExternal4Code" character varying(7),
    "diagnosisExternal4CodeVersion" character(1),
    "diagnosisExternal5Code" character varying(7),
    "diagnosisExternal5CodeVersion" character(1),
    "diagnosisExternal6Code" character varying(7),
    "diagnosisExternal6CodeVersion" character(1),
    "diagnosisExternal7Code" character varying(7),
    "diagnosisExternal7CodeVersion" character(1),
    "diagnosisExternal8Code" character varying(7),
    "diagnosisExternal8CodeVersion" character(1),
    "diagnosisExternal9Code" character varying(7),
    "diagnosisExternal9CodeVersion" character(1),
    "diagnosisExternalFirstCode" character varying(7),
    "diagnosisExternalFirstCodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "fiscalIntermediaryClaimProcessDate" date,
    "fiscalIntermediaryNumber" character varying(5),
    "nearLineRecordIdCode" character(1) NOT NULL,
    "organizationNpi" character varying(10),
    "patientDischargeStatusCode" character varying(2) NOT NULL,
    "paymentAmount" numeric(12,2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "prospectivePaymentCode" character(1) NOT NULL,
    "providerNumber" character varying(9) NOT NULL,
    "providerStateCode" character varying(2) NOT NULL,
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "totalVisitCount" numeric(4,0) NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "fiDocumentClaimControlNumber" character varying(23),
    "fiOriginalClaimControlNumber" character varying(23)
);


ALTER TABLE public."HHAClaims" OWNER TO svc_fhir_etl;

--
-- Name: HospiceClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."HospiceClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "benficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "deductibleCoinsuranceCd" character(1),
    "hcpcsCode" character varying(5),
    "hcpcsInitialModifierCode" character varying(5),
    "hcpcsSecondModifierCode" character varying(5),
    "nationalDrugCodeQualifierCode" character varying(2),
    "nationalDrugCodeQuantity" numeric,
    "nonCoveredChargeAmount" numeric(12,2),
    "paymentAmount" numeric(12,2) NOT NULL,
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "rateAmount" numeric(12,2) NOT NULL,
    "revenueCenterCode" character varying(4) NOT NULL,
    "revenueCenterDate" date,
    "revenueCenterRenderingPhysicianNPI" character varying(12),
    "revenueCenterRenderingPhysicianUPIN" character varying(12),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "unitCount" numeric NOT NULL
);


ALTER TABLE public."HospiceClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: HospiceClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."HospiceClaims" (
    "claimId" character varying(15) NOT NULL,
    "attendingPhysicianNpi" character varying(10),
    "attendingPhysicianUpin" character varying(9),
    "beneficiaryDischargeDate" date,
    "beneficiaryId" character varying(15) NOT NULL,
    "claimFacilityTypeCode" character(1) NOT NULL,
    "claimFrequencyCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimHospiceStartDate" date,
    "claimNonPaymentReasonCode" character varying(2),
    "claimPrimaryPayerCode" character(1),
    "claimServiceClassificationTypeCode" character(1) NOT NULL,
    "claimTypeCode" character varying(2) NOT NULL,
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis13Code" character varying(7),
    "diagnosis13CodeVersion" character(1),
    "diagnosis14Code" character varying(7),
    "diagnosis14CodeVersion" character(1),
    "diagnosis15Code" character varying(7),
    "diagnosis15CodeVersion" character(1),
    "diagnosis16Code" character varying(7),
    "diagnosis16CodeVersion" character(1),
    "diagnosis17Code" character varying(7),
    "diagnosis17CodeVersion" character(1),
    "diagnosis18Code" character varying(7),
    "diagnosis18CodeVersion" character(1),
    "diagnosis19Code" character varying(7),
    "diagnosis19CodeVersion" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis20Code" character varying(7),
    "diagnosis20CodeVersion" character(1),
    "diagnosis21Code" character varying(7),
    "diagnosis21CodeVersion" character(1),
    "diagnosis22Code" character varying(7),
    "diagnosis22CodeVersion" character(1),
    "diagnosis23Code" character varying(7),
    "diagnosis23CodeVersion" character(1),
    "diagnosis24Code" character varying(7),
    "diagnosis24CodeVersion" character(1),
    "diagnosis25Code" character varying(7),
    "diagnosis25CodeVersion" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosisExternal10Code" character varying(7),
    "diagnosisExternal10CodeVersion" character(1),
    "diagnosisExternal11Code" character varying(7),
    "diagnosisExternal11CodeVersion" character(1),
    "diagnosisExternal12Code" character varying(7),
    "diagnosisExternal12CodeVersion" character(1),
    "diagnosisExternal1Code" character varying(7),
    "diagnosisExternal1CodeVersion" character(1),
    "diagnosisExternal2Code" character varying(7),
    "diagnosisExternal2CodeVersion" character(1),
    "diagnosisExternal3Code" character varying(7),
    "diagnosisExternal3CodeVersion" character(1),
    "diagnosisExternal4Code" character varying(7),
    "diagnosisExternal4CodeVersion" character(1),
    "diagnosisExternal5Code" character varying(7),
    "diagnosisExternal5CodeVersion" character(1),
    "diagnosisExternal6Code" character varying(7),
    "diagnosisExternal6CodeVersion" character(1),
    "diagnosisExternal7Code" character varying(7),
    "diagnosisExternal7CodeVersion" character(1),
    "diagnosisExternal8Code" character varying(7),
    "diagnosisExternal8CodeVersion" character(1),
    "diagnosisExternal9Code" character varying(7),
    "diagnosisExternal9CodeVersion" character(1),
    "diagnosisExternalFirstCode" character varying(7),
    "diagnosisExternalFirstCodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "fiscalIntermediaryClaimProcessDate" date,
    "fiscalIntermediaryNumber" character varying(5),
    "hospicePeriodCount" numeric(2,0),
    "nearLineRecordIdCode" character(1) NOT NULL,
    "organizationNpi" character varying(10),
    "patientDischargeStatusCode" character varying(2) NOT NULL,
    "patientStatusCd" character(1),
    "paymentAmount" numeric(12,2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "providerNumber" character varying(9) NOT NULL,
    "providerStateCode" character varying(2) NOT NULL,
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "utilizationDayCount" numeric NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "fiDocumentClaimControlNumber" character varying(23),
    "fiOriginalClaimControlNumber" character varying(23)
);


ALTER TABLE public."HospiceClaims" OWNER TO svc_fhir_etl;

--
-- Name: InpatientClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."InpatientClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "deductibleCoinsuranceCd" character(1),
    "hcpcsCode" character varying(5),
    "nationalDrugCodeQualifierCode" character varying(2),
    "nationalDrugCodeQuantity" numeric,
    "nonCoveredChargeAmount" numeric(12,2) NOT NULL,
    "rateAmount" numeric(12,2) NOT NULL,
    "revenueCenter" character varying(4) NOT NULL,
    "revenueCenterRenderingPhysicianNPI" character varying(12),
    "revenueCenterRenderingPhysicianUPIN" character varying(12),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "unitCount" numeric NOT NULL
);


ALTER TABLE public."InpatientClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: InpatientClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."InpatientClaims" (
    "claimId" character varying(15) NOT NULL,
    "admissionTypeCd" character(1) NOT NULL,
    "attendingPhysicianNpi" character varying(10),
    "attendingPhysicianUpin" character varying(9),
    "beneficiaryDischargeDate" date,
    "beneficiaryId" character varying(15) NOT NULL,
    "bloodDeductibleLiabilityAmount" numeric(12,2) NOT NULL,
    "bloodPintsFurnishedQty" numeric NOT NULL,
    "claimAdmissionDate" date,
    "claimFacilityTypeCode" character(1) NOT NULL,
    "claimFrequencyCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimNonPaymentReasonCode" character varying(2),
    "claimPPSCapitalDisproportionateShareAmt" numeric(12,2),
    "claimPPSCapitalDrgWeightNumber" numeric(7,4),
    "claimPPSCapitalExceptionAmount" numeric(12,2),
    "claimPPSCapitalFSPAmount" numeric(12,2),
    "claimPPSCapitalIMEAmount" numeric(12,2),
    "claimPPSCapitalOutlierAmount" numeric(12,2),
    "claimPPSOldCapitalHoldHarmlessAmount" numeric(12,2),
    "claimPrimaryPayerCode" character(1),
    "claimQueryCode" character(1) NOT NULL,
    "claimServiceClassificationTypeCode" character(1) NOT NULL,
    "claimTotalPPSCapitalAmount" numeric(12,2),
    "claimTypeCode" character varying(2) NOT NULL,
    "coinsuranceDayCount" numeric NOT NULL,
    "coveredCareThoughDate" date,
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "deductibleAmount" numeric(12,2) NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis10PresentOnAdmissionCode" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis11PresentOnAdmissionCode" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis12PresentOnAdmissionCode" character(1),
    "diagnosis13Code" character varying(7),
    "diagnosis13CodeVersion" character(1),
    "diagnosis13PresentOnAdmissionCode" character(1),
    "diagnosis14Code" character varying(7),
    "diagnosis14CodeVersion" character(1),
    "diagnosis14PresentOnAdmissionCode" character(1),
    "diagnosis15Code" character varying(7),
    "diagnosis15CodeVersion" character(1),
    "diagnosis15PresentOnAdmissionCode" character(1),
    "diagnosis16Code" character varying(7),
    "diagnosis16CodeVersion" character(1),
    "diagnosis16PresentOnAdmissionCode" character(1),
    "diagnosis17Code" character varying(7),
    "diagnosis17CodeVersion" character(1),
    "diagnosis17PresentOnAdmissionCode" character(1),
    "diagnosis18Code" character varying(7),
    "diagnosis18CodeVersion" character(1),
    "diagnosis18PresentOnAdmissionCode" character(1),
    "diagnosis19Code" character varying(7),
    "diagnosis19CodeVersion" character(1),
    "diagnosis19PresentOnAdmissionCode" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis1PresentOnAdmissionCode" character(1),
    "diagnosis20Code" character varying(7),
    "diagnosis20CodeVersion" character(1),
    "diagnosis20PresentOnAdmissionCode" character(1),
    "diagnosis21Code" character varying(7),
    "diagnosis21CodeVersion" character(1),
    "diagnosis21PresentOnAdmissionCode" character(1),
    "diagnosis22Code" character varying(7),
    "diagnosis22CodeVersion" character(1),
    "diagnosis22PresentOnAdmissionCode" character(1),
    "diagnosis23Code" character varying(7),
    "diagnosis23CodeVersion" character(1),
    "diagnosis23PresentOnAdmissionCode" character(1),
    "diagnosis24Code" character varying(7),
    "diagnosis24CodeVersion" character(1),
    "diagnosis24PresentOnAdmissionCode" character(1),
    "diagnosis25Code" character varying(7),
    "diagnosis25CodeVersion" character(1),
    "diagnosis25PresentOnAdmissionCode" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis2PresentOnAdmissionCode" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis3PresentOnAdmissionCode" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis4PresentOnAdmissionCode" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis5PresentOnAdmissionCode" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis6PresentOnAdmissionCode" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis7PresentOnAdmissionCode" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis8PresentOnAdmissionCode" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosis9PresentOnAdmissionCode" character(1),
    "diagnosisAdmittingCode" character varying(7),
    "diagnosisAdmittingCodeVersion" character(1),
    "diagnosisExternal10Code" character varying(7),
    "diagnosisExternal10CodeVersion" character(1),
    "diagnosisExternal10PresentOnAdmissionCode" character(1),
    "diagnosisExternal11Code" character varying(7),
    "diagnosisExternal11CodeVersion" character(1),
    "diagnosisExternal11PresentOnAdmissionCode" character(1),
    "diagnosisExternal12Code" character varying(7),
    "diagnosisExternal12CodeVersion" character(1),
    "diagnosisExternal12PresentOnAdmissionCode" character(1),
    "diagnosisExternal1Code" character varying(7),
    "diagnosisExternal1CodeVersion" character(1),
    "diagnosisExternal1PresentOnAdmissionCode" character(1),
    "diagnosisExternal2Code" character varying(7),
    "diagnosisExternal2CodeVersion" character(1),
    "diagnosisExternal2PresentOnAdmissionCode" character(1),
    "diagnosisExternal3Code" character varying(7),
    "diagnosisExternal3CodeVersion" character(1),
    "diagnosisExternal3PresentOnAdmissionCode" character(1),
    "diagnosisExternal4Code" character varying(7),
    "diagnosisExternal4CodeVersion" character(1),
    "diagnosisExternal4PresentOnAdmissionCode" character(1),
    "diagnosisExternal5Code" character varying(7),
    "diagnosisExternal5CodeVersion" character(1),
    "diagnosisExternal5PresentOnAdmissionCode" character(1),
    "diagnosisExternal6Code" character varying(7),
    "diagnosisExternal6CodeVersion" character(1),
    "diagnosisExternal6PresentOnAdmissionCode" character(1),
    "diagnosisExternal7Code" character varying(7),
    "diagnosisExternal7CodeVersion" character(1),
    "diagnosisExternal7PresentOnAdmissionCode" character(1),
    "diagnosisExternal8Code" character varying(7),
    "diagnosisExternal8CodeVersion" character(1),
    "diagnosisExternal8PresentOnAdmissionCode" character(1),
    "diagnosisExternal9Code" character varying(7),
    "diagnosisExternal9CodeVersion" character(1),
    "diagnosisExternal9PresentOnAdmissionCode" character(1),
    "diagnosisExternalFirstCode" character varying(7),
    "diagnosisExternalFirstCodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "diagnosisRelatedGroupCd" character varying(3),
    "diagnosisRelatedGroupOutlierStayCd" character(1) NOT NULL,
    "disproportionateShareAmount" numeric(12,2),
    "drgOutlierApprovedPaymentAmount" numeric(12,2),
    "fiscalIntermediaryClaimActionCode" character(1),
    "fiscalIntermediaryClaimProcessDate" date,
    "fiscalIntermediaryNumber" character varying(5),
    "indirectMedicalEducationAmount" numeric(12,2),
    "lifetimeReservedDaysUsedCount" numeric,
    "mcoPaidSw" character(1),
    "medicareBenefitsExhaustedDate" date,
    "nearLineRecordIdCode" character(1) NOT NULL,
    "nonUtilizationDayCount" numeric NOT NULL,
    "noncoveredCharge" numeric(12,2) NOT NULL,
    "noncoveredStayFromDate" date,
    "noncoveredStayThroughDate" date,
    "operatingPhysicianNpi" character varying(10),
    "operatingPhysicianUpin" character varying(9),
    "organizationNpi" character varying(10),
    "otherPhysicianNpi" character varying(10),
    "otherPhysicianUpin" character varying(9),
    "partACoinsuranceLiabilityAmount" numeric(12,2) NOT NULL,
    "passThruPerDiemAmount" numeric(12,2) NOT NULL,
    "patientDischargeStatusCode" character varying(2) NOT NULL,
    "patientStatusCd" character(1),
    "paymentAmount" numeric(12,2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "procedure10Code" character varying(7),
    "procedure10CodeVersion" character(1),
    "procedure10Date" date,
    "procedure11Code" character varying(7),
    "procedure11CodeVersion" character(1),
    "procedure11Date" date,
    "procedure12Code" character varying(7),
    "procedure12CodeVersion" character(1),
    "procedure12Date" date,
    "procedure13Code" character varying(7),
    "procedure13CodeVersion" character(1),
    "procedure13Date" date,
    "procedure14Code" character varying(7),
    "procedure14CodeVersion" character(1),
    "procedure14Date" date,
    "procedure15Code" character varying(7),
    "procedure15CodeVersion" character(1),
    "procedure15Date" date,
    "procedure16Code" character varying(7),
    "procedure16CodeVersion" character(1),
    "procedure16Date" date,
    "procedure17Code" character varying(7),
    "procedure17CodeVersion" character(1),
    "procedure17Date" date,
    "procedure18Code" character varying(7),
    "procedure18CodeVersion" character(1),
    "procedure18Date" date,
    "procedure19Code" character varying(7),
    "procedure19CodeVersion" character(1),
    "procedure19Date" date,
    "procedure1Code" character varying(7),
    "procedure1CodeVersion" character(1),
    "procedure1Date" date,
    "procedure20Code" character varying(7),
    "procedure20CodeVersion" character(1),
    "procedure20Date" date,
    "procedure21Code" character varying(7),
    "procedure21CodeVersion" character(1),
    "procedure21Date" date,
    "procedure22Code" character varying(7),
    "procedure22CodeVersion" character(1),
    "procedure22Date" date,
    "procedure23Code" character varying(7),
    "procedure23CodeVersion" character(1),
    "procedure23Date" date,
    "procedure24Code" character varying(7),
    "procedure24CodeVersion" character(1),
    "procedure24Date" date,
    "procedure25Code" character varying(7),
    "procedure25CodeVersion" character(1),
    "procedure25Date" date,
    "procedure2Code" character varying(7),
    "procedure2CodeVersion" character(1),
    "procedure2Date" date,
    "procedure3Code" character varying(7),
    "procedure3CodeVersion" character(1),
    "procedure3Date" date,
    "procedure4Code" character varying(7),
    "procedure4CodeVersion" character(1),
    "procedure4Date" date,
    "procedure5Code" character varying(7),
    "procedure5CodeVersion" character(1),
    "procedure5Date" date,
    "procedure6Code" character varying(7),
    "procedure6CodeVersion" character(1),
    "procedure6Date" date,
    "procedure7Code" character varying(7),
    "procedure7CodeVersion" character(1),
    "procedure7Date" date,
    "procedure8Code" character varying(7),
    "procedure8CodeVersion" character(1),
    "procedure8Date" date,
    "procedure9Code" character varying(7),
    "procedure9CodeVersion" character(1),
    "procedure9Date" date,
    "professionalComponentCharge" numeric(12,2) NOT NULL,
    "prospectivePaymentCode" character(1),
    "providerNumber" character varying(9) NOT NULL,
    "providerStateCode" character varying(2) NOT NULL,
    "sourceAdmissionCd" character(1),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "totalDeductionAmount" numeric(12,2) NOT NULL,
    "utilizationDayCount" numeric NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "claimUncompensatedCareAmount" numeric(38,2),
    "fiDocumentClaimControlNumber" character varying(23),
    "fiOriginalClaimControlNumber" character varying(23)
);


ALTER TABLE public."InpatientClaims" OWNER TO svc_fhir_etl;

--
-- Name: LoadedBatches; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."LoadedBatches" (
    "loadedBatchId" bigint NOT NULL,
    "loadedFileId" bigint NOT NULL,
    beneficiaries character varying(20000) NOT NULL,
    created timestamp with time zone NOT NULL
);


ALTER TABLE public."LoadedBatches" OWNER TO svc_fhir_etl;

--
-- Name: LoadedFiles; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."LoadedFiles" (
    "loadedFileId" bigint NOT NULL,
    "rifType" character varying(48) NOT NULL,
    created timestamp with time zone NOT NULL
);


ALTER TABLE public."LoadedFiles" OWNER TO svc_fhir_etl;

--
-- Name: MedicareBeneficiaryIdHistory; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."MedicareBeneficiaryIdHistory" (
    "medicareBeneficiaryIdKey" numeric NOT NULL,
    "beneficiaryId" character varying(15),
    "claimAccountNumber" character varying(9),
    "beneficiaryIdCode" character varying(2),
    "mbiSequenceNumber" numeric,
    "medicareBeneficiaryId" character varying(11),
    "mbiEffectiveDate" date,
    "mbiEndDate" date,
    "mbiEffectiveReasonCode" character varying(5),
    "mbiEndReasonCode" character varying(5),
    "mbiCardRequestDate" date,
    "mbiAddUser" character varying(30),
    "mbiAddDate" timestamp without time zone,
    "mbiUpdateUser" character varying(30),
    "mbiUpdateDate" timestamp without time zone,
    "mbiCrntRecIndId" numeric,
    lastupdated timestamp with time zone
);


ALTER TABLE public."MedicareBeneficiaryIdHistory" OWNER TO svc_fhir_etl;

--
-- Name: MedicareBeneficiaryIdHistoryInvalidBeneficiaries; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries" (
    "medicareBeneficiaryIdKey" numeric NOT NULL,
    "beneficiaryId" character varying(15),
    "claimAccountNumber" character varying(9),
    "beneficiaryIdCode" character varying(2),
    "mbiSequenceNumber" numeric,
    "medicareBeneficiaryId" character varying(11),
    "mbiEffectiveDate" date,
    "mbiEndDate" date,
    "mbiEffectiveReasonCode" character varying(5),
    "mbiEndReasonCode" character varying(5),
    "mbiCardRequestDate" date,
    "mbiAddUser" character varying(30),
    "mbiAddDate" timestamp without time zone,
    "mbiUpdateUser" character varying(30),
    "mbiUpdateDate" timestamp without time zone,
    "mbiCrntRecIndId" numeric
);


ALTER TABLE public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries" OWNER TO svc_fhir_etl;

--
-- Name: OutpatientClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."OutpatientClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "apcOrHippsCode" character varying(5),
    "benficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "bloodDeductibleAmount" numeric(12,2) NOT NULL,
    "cashDeductibleAmount" numeric(12,2) NOT NULL,
    "discountCode" character(1),
    "firstMspPaidAmount" numeric(12,2) NOT NULL,
    "hcpcsCode" character varying(5),
    "hcpcsInitialModifierCode" character varying(5),
    "hcpcsSecondModifierCode" character varying(5),
    "nationalDrugCode" character varying(24),
    "nationalDrugCodeQualifierCode" character varying(2),
    "nationalDrugCodeQuantity" numeric,
    "nonCoveredChargeAmount" numeric(12,2) NOT NULL,
    "obligationToAcceptAsFullPaymentCode" character(1),
    "packagingCode" character(1),
    "patientResponsibilityAmount" numeric(12,2) NOT NULL,
    "paymentAmount" numeric(12,2) NOT NULL,
    "paymentMethodCode" character varying(2),
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "rateAmount" numeric(12,2) NOT NULL,
    "reducedCoinsuranceAmount" numeric(12,2) NOT NULL,
    "revCntr1stAnsiCd" character varying(5),
    "revCntr2ndAnsiCd" character varying(5),
    "revCntr3rdAnsiCd" character varying(5),
    "revCntr4thAnsiCd" character varying(5),
    "revenueCenterCode" character varying(4) NOT NULL,
    "revenueCenterDate" date,
    "revenueCenterRenderingPhysicianNPI" character varying(12),
    "revenueCenterRenderingPhysicianUPIN" character varying(12),
    "secondMspPaidAmount" numeric(12,2) NOT NULL,
    "statusCode" character varying(2),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "unitCount" numeric NOT NULL,
    "wageAdjustedCoinsuranceAmount" numeric(12,2) NOT NULL
);


ALTER TABLE public."OutpatientClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: OutpatientClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."OutpatientClaims" (
    "claimId" character varying(15) NOT NULL,
    "attendingPhysicianNpi" character varying(10),
    "attendingPhysicianUpin" character varying(9),
    "beneficiaryId" character varying(15) NOT NULL,
    "beneficiaryPaymentAmount" numeric(12,2) NOT NULL,
    "bloodDeductibleLiabilityAmount" numeric(12,2) NOT NULL,
    "claimFacilityTypeCode" character(1) NOT NULL,
    "claimFrequencyCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimNonPaymentReasonCode" character varying(2),
    "claimPrimaryPayerCode" character(1),
    "claimQueryCode" character(1) NOT NULL,
    "claimServiceClassificationTypeCode" character(1) NOT NULL,
    "claimTypeCode" character varying(2) NOT NULL,
    "coinsuranceAmount" numeric(12,2) NOT NULL,
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "deductibleAmount" numeric(12,2) NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis13Code" character varying(7),
    "diagnosis13CodeVersion" character(1),
    "diagnosis14Code" character varying(7),
    "diagnosis14CodeVersion" character(1),
    "diagnosis15Code" character varying(7),
    "diagnosis15CodeVersion" character(1),
    "diagnosis16Code" character varying(7),
    "diagnosis16CodeVersion" character(1),
    "diagnosis17Code" character varying(7),
    "diagnosis17CodeVersion" character(1),
    "diagnosis18Code" character varying(7),
    "diagnosis18CodeVersion" character(1),
    "diagnosis19Code" character varying(7),
    "diagnosis19CodeVersion" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis20Code" character varying(7),
    "diagnosis20CodeVersion" character(1),
    "diagnosis21Code" character varying(7),
    "diagnosis21CodeVersion" character(1),
    "diagnosis22Code" character varying(7),
    "diagnosis22CodeVersion" character(1),
    "diagnosis23Code" character varying(7),
    "diagnosis23CodeVersion" character(1),
    "diagnosis24Code" character varying(7),
    "diagnosis24CodeVersion" character(1),
    "diagnosis25Code" character varying(7),
    "diagnosis25CodeVersion" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosisAdmission1Code" character varying(7),
    "diagnosisAdmission1CodeVersion" character(1),
    "diagnosisAdmission2Code" character varying(7),
    "diagnosisAdmission2CodeVersion" character(1),
    "diagnosisAdmission3Code" character varying(7),
    "diagnosisAdmission3CodeVersion" character(1),
    "diagnosisExternal10Code" character varying(7),
    "diagnosisExternal10CodeVersion" character(1),
    "diagnosisExternal11Code" character varying(7),
    "diagnosisExternal11CodeVersion" character(1),
    "diagnosisExternal12Code" character varying(7),
    "diagnosisExternal12CodeVersion" character(1),
    "diagnosisExternal1Code" character varying(7),
    "diagnosisExternal1CodeVersion" character(1),
    "diagnosisExternal2Code" character varying(7),
    "diagnosisExternal2CodeVersion" character(1),
    "diagnosisExternal3Code" character varying(7),
    "diagnosisExternal3CodeVersion" character(1),
    "diagnosisExternal4Code" character varying(7),
    "diagnosisExternal4CodeVersion" character(1),
    "diagnosisExternal5Code" character varying(7),
    "diagnosisExternal5CodeVersion" character(1),
    "diagnosisExternal6Code" character varying(7),
    "diagnosisExternal6CodeVersion" character(1),
    "diagnosisExternal7Code" character varying(7),
    "diagnosisExternal7CodeVersion" character(1),
    "diagnosisExternal8Code" character varying(7),
    "diagnosisExternal8CodeVersion" character(1),
    "diagnosisExternal9Code" character varying(7),
    "diagnosisExternal9CodeVersion" character(1),
    "diagnosisExternalFirstCode" character varying(7),
    "diagnosisExternalFirstCodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "fiscalIntermediaryClaimProcessDate" date,
    "fiscalIntermediaryNumber" character varying(5),
    "mcoPaidSw" character(1),
    "nearLineRecordIdCode" character(1) NOT NULL,
    "operatingPhysicianNpi" character varying(10),
    "operatingPhysicianUpin" character varying(9),
    "organizationNpi" character varying(10),
    "otherPhysicianNpi" character varying(10),
    "otherPhysicianUpin" character varying(9),
    "patientDischargeStatusCode" character varying(2),
    "paymentAmount" numeric(12,2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "procedure10Code" character varying(7),
    "procedure10CodeVersion" character(1),
    "procedure10Date" date,
    "procedure11Code" character varying(7),
    "procedure11CodeVersion" character(1),
    "procedure11Date" date,
    "procedure12Code" character varying(7),
    "procedure12CodeVersion" character(1),
    "procedure12Date" date,
    "procedure13Code" character varying(7),
    "procedure13CodeVersion" character(1),
    "procedure13Date" date,
    "procedure14Code" character varying(7),
    "procedure14CodeVersion" character(1),
    "procedure14Date" date,
    "procedure15Code" character varying(7),
    "procedure15CodeVersion" character(1),
    "procedure15Date" date,
    "procedure16Code" character varying(7),
    "procedure16CodeVersion" character(1),
    "procedure16Date" date,
    "procedure17Code" character varying(7),
    "procedure17CodeVersion" character(1),
    "procedure17Date" date,
    "procedure18Code" character varying(7),
    "procedure18CodeVersion" character(1),
    "procedure18Date" date,
    "procedure19Code" character varying(7),
    "procedure19CodeVersion" character(1),
    "procedure19Date" date,
    "procedure1Code" character varying(7),
    "procedure1CodeVersion" character(1),
    "procedure1Date" date,
    "procedure20Code" character varying(7),
    "procedure20CodeVersion" character(1),
    "procedure20Date" date,
    "procedure21Code" character varying(7),
    "procedure21CodeVersion" character(1),
    "procedure21Date" date,
    "procedure22Code" character varying(7),
    "procedure22CodeVersion" character(1),
    "procedure22Date" date,
    "procedure23Code" character varying(7),
    "procedure23CodeVersion" character(1),
    "procedure23Date" date,
    "procedure24Code" character varying(7),
    "procedure24CodeVersion" character(1),
    "procedure24Date" date,
    "procedure25Code" character varying(7),
    "procedure25CodeVersion" character(1),
    "procedure25Date" date,
    "procedure2Code" character varying(7),
    "procedure2CodeVersion" character(1),
    "procedure2Date" date,
    "procedure3Code" character varying(7),
    "procedure3CodeVersion" character(1),
    "procedure3Date" date,
    "procedure4Code" character varying(7),
    "procedure4CodeVersion" character(1),
    "procedure4Date" date,
    "procedure5Code" character varying(7),
    "procedure5CodeVersion" character(1),
    "procedure5Date" date,
    "procedure6Code" character varying(7),
    "procedure6CodeVersion" character(1),
    "procedure6Date" date,
    "procedure7Code" character varying(7),
    "procedure7CodeVersion" character(1),
    "procedure7Date" date,
    "procedure8Code" character varying(7),
    "procedure8CodeVersion" character(1),
    "procedure8Date" date,
    "procedure9Code" character varying(7),
    "procedure9CodeVersion" character(1),
    "procedure9Date" date,
    "professionalComponentCharge" numeric(12,2) NOT NULL,
    "providerNumber" character varying(9) NOT NULL,
    "providerPaymentAmount" numeric(12,2) NOT NULL,
    "providerStateCode" character varying(2) NOT NULL,
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "fiDocumentClaimControlNumber" character varying(23),
    "fiOriginalClaimControlNumber" character varying(23)
);


ALTER TABLE public."OutpatientClaims" OWNER TO svc_fhir_etl;

--
-- Name: PartDEvents; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."PartDEvents" (
    "eventId" character varying(15) NOT NULL,
    "adjustmentDeletionCode" character(1),
    "beneficiaryId" character varying(15) NOT NULL,
    "brandGenericCode" character(1),
    "catastrophicCoverageCode" character(1),
    "claimGroupId" numeric(12,0) NOT NULL,
    "compoundCode" integer NOT NULL,
    "daysSupply" numeric NOT NULL,
    "dispenseAsWrittenProductSelectionCode" character(1) NOT NULL,
    "dispensingStatusCode" character(1),
    "drugCoverageStatusCode" character(1) NOT NULL,
    "fillNumber" numeric NOT NULL,
    "gapDiscountAmount" numeric(8,2) NOT NULL,
    "grossCostAboveOutOfPocketThreshold" numeric(8,2) NOT NULL,
    "grossCostBelowOutOfPocketThreshold" numeric(8,2) NOT NULL,
    "lowIncomeSubsidyPaidAmount" numeric(8,2) NOT NULL,
    "nationalDrugCode" character varying(19) NOT NULL,
    "nonstandardFormatCode" character(1),
    "otherTrueOutOfPocketPaidAmount" numeric(8,2) NOT NULL,
    "partDPlanCoveredPaidAmount" numeric(8,2) NOT NULL,
    "partDPlanNonCoveredPaidAmount" numeric(8,2) NOT NULL,
    "patientLiabilityReductionOtherPaidAmount" numeric(8,2) NOT NULL,
    "patientPaidAmount" numeric(8,2) NOT NULL,
    "patientResidenceCode" character varying(2) NOT NULL,
    "paymentDate" date,
    "pharmacyTypeCode" character varying(2) NOT NULL,
    "planBenefitPackageId" character varying(3) NOT NULL,
    "planContractId" character varying(5) NOT NULL,
    "prescriberId" character varying(15) NOT NULL,
    "prescriberIdQualifierCode" character varying(2) NOT NULL,
    "prescriptionFillDate" date NOT NULL,
    "prescriptionOriginationCode" character(1),
    "prescriptionReferenceNumber" numeric(12,0) NOT NULL,
    "pricingExceptionCode" character(1),
    "quantityDispensed" numeric(10,3) NOT NULL,
    "serviceProviderId" character varying(15) NOT NULL,
    "serviceProviderIdQualiferCode" character varying(2) NOT NULL,
    "submissionClarificationCode" character varying(2),
    "totalPrescriptionCost" numeric(8,2) NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone
);


ALTER TABLE public."PartDEvents" OWNER TO svc_fhir_etl;

--
-- Name: SNFClaimLines; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."SNFClaimLines" (
    "lineNumber" numeric NOT NULL,
    "parentClaim" character varying(15) NOT NULL,
    "deductibleCoinsuranceCd" character(1),
    "hcpcsCode" character varying(5),
    "nationalDrugCodeQualifierCode" character varying(2),
    "nationalDrugCodeQuantity" numeric,
    "nonCoveredChargeAmount" numeric(12,2) NOT NULL,
    "rateAmount" numeric(12,2) NOT NULL,
    "revenueCenter" character varying(4) NOT NULL,
    "revenueCenterRenderingPhysicianNPI" character varying(12),
    "revenueCenterRenderingPhysicianUPIN" character varying(12),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "unitCount" integer NOT NULL
);


ALTER TABLE public."SNFClaimLines" OWNER TO svc_fhir_etl;

--
-- Name: SNFClaims; Type: TABLE; Schema: public; Owner: svc_fhir_etl
--

CREATE TABLE public."SNFClaims" (
    "claimId" character varying(15) NOT NULL,
    "admissionTypeCd" character(1) NOT NULL,
    "attendingPhysicianNpi" character varying(10),
    "attendingPhysicianUpin" character varying(9),
    "beneficiaryDischargeDate" date,
    "beneficiaryId" character varying(15) NOT NULL,
    "bloodDeductibleLiabilityAmount" numeric(12,2) NOT NULL,
    "bloodPintsFurnishedQty" numeric NOT NULL,
    "claimAdmissionDate" date,
    "claimFacilityTypeCode" character(1) NOT NULL,
    "claimFrequencyCode" character(1) NOT NULL,
    "claimGroupId" numeric(12,0) NOT NULL,
    "claimNonPaymentReasonCode" character varying(2),
    "claimPPSCapitalDisproportionateShareAmt" numeric(12,2),
    "claimPPSCapitalExceptionAmount" numeric(12,2),
    "claimPPSCapitalFSPAmount" numeric(12,2),
    "claimPPSCapitalIMEAmount" numeric(12,2),
    "claimPPSCapitalOutlierAmount" numeric(12,2),
    "claimPPSOldCapitalHoldHarmlessAmount" numeric(12,2),
    "claimPrimaryPayerCode" character(1),
    "claimQueryCode" character(1) NOT NULL,
    "claimServiceClassificationTypeCode" character(1) NOT NULL,
    "claimTypeCode" character varying(2) NOT NULL,
    "coinsuranceDayCount" numeric NOT NULL,
    "coveredCareThroughDate" date,
    "dateFrom" date NOT NULL,
    "dateThrough" date NOT NULL,
    "deductibleAmount" numeric(12,2) NOT NULL,
    "diagnosis10Code" character varying(7),
    "diagnosis10CodeVersion" character(1),
    "diagnosis11Code" character varying(7),
    "diagnosis11CodeVersion" character(1),
    "diagnosis12Code" character varying(7),
    "diagnosis12CodeVersion" character(1),
    "diagnosis13Code" character varying(7),
    "diagnosis13CodeVersion" character(1),
    "diagnosis14Code" character varying(7),
    "diagnosis14CodeVersion" character(1),
    "diagnosis15Code" character varying(7),
    "diagnosis15CodeVersion" character(1),
    "diagnosis16Code" character varying(7),
    "diagnosis16CodeVersion" character(1),
    "diagnosis17Code" character varying(7),
    "diagnosis17CodeVersion" character(1),
    "diagnosis18Code" character varying(7),
    "diagnosis18CodeVersion" character(1),
    "diagnosis19Code" character varying(7),
    "diagnosis19CodeVersion" character(1),
    "diagnosis1Code" character varying(7),
    "diagnosis1CodeVersion" character(1),
    "diagnosis20Code" character varying(7),
    "diagnosis20CodeVersion" character(1),
    "diagnosis21Code" character varying(7),
    "diagnosis21CodeVersion" character(1),
    "diagnosis22Code" character varying(7),
    "diagnosis22CodeVersion" character(1),
    "diagnosis23Code" character varying(7),
    "diagnosis23CodeVersion" character(1),
    "diagnosis24Code" character varying(7),
    "diagnosis24CodeVersion" character(1),
    "diagnosis25Code" character varying(7),
    "diagnosis25CodeVersion" character(1),
    "diagnosis2Code" character varying(7),
    "diagnosis2CodeVersion" character(1),
    "diagnosis3Code" character varying(7),
    "diagnosis3CodeVersion" character(1),
    "diagnosis4Code" character varying(7),
    "diagnosis4CodeVersion" character(1),
    "diagnosis5Code" character varying(7),
    "diagnosis5CodeVersion" character(1),
    "diagnosis6Code" character varying(7),
    "diagnosis6CodeVersion" character(1),
    "diagnosis7Code" character varying(7),
    "diagnosis7CodeVersion" character(1),
    "diagnosis8Code" character varying(7),
    "diagnosis8CodeVersion" character(1),
    "diagnosis9Code" character varying(7),
    "diagnosis9CodeVersion" character(1),
    "diagnosisAdmittingCode" character varying(7),
    "diagnosisAdmittingCodeVersion" character(1),
    "diagnosisExternal10Code" character varying(7),
    "diagnosisExternal10CodeVersion" character(1),
    "diagnosisExternal11Code" character varying(7),
    "diagnosisExternal11CodeVersion" character(1),
    "diagnosisExternal12Code" character varying(7),
    "diagnosisExternal12CodeVersion" character(1),
    "diagnosisExternal1Code" character varying(7),
    "diagnosisExternal1CodeVersion" character(1),
    "diagnosisExternal2Code" character varying(7),
    "diagnosisExternal2CodeVersion" character(1),
    "diagnosisExternal3Code" character varying(7),
    "diagnosisExternal3CodeVersion" character(1),
    "diagnosisExternal4Code" character varying(7),
    "diagnosisExternal4CodeVersion" character(1),
    "diagnosisExternal5Code" character varying(7),
    "diagnosisExternal5CodeVersion" character(1),
    "diagnosisExternal6Code" character varying(7),
    "diagnosisExternal6CodeVersion" character(1),
    "diagnosisExternal7Code" character varying(7),
    "diagnosisExternal7CodeVersion" character(1),
    "diagnosisExternal8Code" character varying(7),
    "diagnosisExternal8CodeVersion" character(1),
    "diagnosisExternal9Code" character varying(7),
    "diagnosisExternal9CodeVersion" character(1),
    "diagnosisExternalFirstCode" character varying(7),
    "diagnosisExternalFirstCodeVersion" character(1),
    "diagnosisPrincipalCode" character varying(7),
    "diagnosisPrincipalCodeVersion" character(1),
    "diagnosisRelatedGroupCd" character varying(3),
    "fiscalIntermediaryClaimActionCode" character(1),
    "fiscalIntermediaryClaimProcessDate" date,
    "fiscalIntermediaryNumber" character varying(5),
    "mcoPaidSw" character(1),
    "medicareBenefitsExhaustedDate" date,
    "nearLineRecordIdCode" character(1) NOT NULL,
    "nonUtilizationDayCount" numeric NOT NULL,
    "noncoveredCharge" numeric(12,2) NOT NULL,
    "noncoveredStayFromDate" date,
    "noncoveredStayThroughDate" date,
    "operatingPhysicianNpi" character varying(10),
    "operatingPhysicianUpin" character varying(9),
    "organizationNpi" character varying(10),
    "otherPhysicianNpi" character varying(10),
    "otherPhysicianUpin" character varying(9),
    "partACoinsuranceLiabilityAmount" numeric(12,2) NOT NULL,
    "patientDischargeStatusCode" character varying(2) NOT NULL,
    "patientStatusCd" character(1),
    "paymentAmount" numeric(12,2) NOT NULL,
    "primaryPayerPaidAmount" numeric(12,2) NOT NULL,
    "procedure10Code" character varying(7),
    "procedure10CodeVersion" character(1),
    "procedure10Date" date,
    "procedure11Code" character varying(7),
    "procedure11CodeVersion" character(1),
    "procedure11Date" date,
    "procedure12Code" character varying(7),
    "procedure12CodeVersion" character(1),
    "procedure12Date" date,
    "procedure13Code" character varying(7),
    "procedure13CodeVersion" character(1),
    "procedure13Date" date,
    "procedure14Code" character varying(7),
    "procedure14CodeVersion" character(1),
    "procedure14Date" date,
    "procedure15Code" character varying(7),
    "procedure15CodeVersion" character(1),
    "procedure15Date" date,
    "procedure16Code" character varying(7),
    "procedure16CodeVersion" character(1),
    "procedure16Date" date,
    "procedure17Code" character varying(7),
    "procedure17CodeVersion" character(1),
    "procedure17Date" date,
    "procedure18Code" character varying(7),
    "procedure18CodeVersion" character(1),
    "procedure18Date" date,
    "procedure19Code" character varying(7),
    "procedure19CodeVersion" character(1),
    "procedure19Date" date,
    "procedure1Code" character varying(7),
    "procedure1CodeVersion" character(1),
    "procedure1Date" date,
    "procedure20Code" character varying(7),
    "procedure20CodeVersion" character(1),
    "procedure20Date" date,
    "procedure21Code" character varying(7),
    "procedure21CodeVersion" character(1),
    "procedure21Date" date,
    "procedure22Code" character varying(7),
    "procedure22CodeVersion" character(1),
    "procedure22Date" date,
    "procedure23Code" character varying(7),
    "procedure23CodeVersion" character(1),
    "procedure23Date" date,
    "procedure24Code" character varying(7),
    "procedure24CodeVersion" character(1),
    "procedure24Date" date,
    "procedure25Code" character varying(7),
    "procedure25CodeVersion" character(1),
    "procedure25Date" date,
    "procedure2Code" character varying(7),
    "procedure2CodeVersion" character(1),
    "procedure2Date" date,
    "procedure3Code" character varying(7),
    "procedure3CodeVersion" character(1),
    "procedure3Date" date,
    "procedure4Code" character varying(7),
    "procedure4CodeVersion" character(1),
    "procedure4Date" date,
    "procedure5Code" character varying(7),
    "procedure5CodeVersion" character(1),
    "procedure5Date" date,
    "procedure6Code" character varying(7),
    "procedure6CodeVersion" character(1),
    "procedure6Date" date,
    "procedure7Code" character varying(7),
    "procedure7CodeVersion" character(1),
    "procedure7Date" date,
    "procedure8Code" character varying(7),
    "procedure8CodeVersion" character(1),
    "procedure8Date" date,
    "procedure9Code" character varying(7),
    "procedure9CodeVersion" character(1),
    "procedure9Date" date,
    "prospectivePaymentCode" character(1),
    "providerNumber" character varying(9) NOT NULL,
    "providerStateCode" character varying(2) NOT NULL,
    "qualifiedStayFromDate" date,
    "qualifiedStayThroughDate" date,
    "sourceAdmissionCd" character(1),
    "totalChargeAmount" numeric(12,2) NOT NULL,
    "totalDeductionAmount" numeric(12,2) NOT NULL,
    "utilizationDayCount" numeric NOT NULL,
    "weeklyProcessDate" date NOT NULL,
    "finalAction" character(1) NOT NULL,
    lastupdated timestamp with time zone,
    "fiDocumentClaimControlNumber" character varying(23),
    "fiOriginalClaimControlNumber" character varying(23)
);


ALTER TABLE public."SNFClaims" OWNER TO svc_fhir_etl;


--
-- Name: beneficiaryhistory_beneficiaryhistoryid_seq; Type: SEQUENCE; Schema: public; Owner: svc_fhir_etl
--

CREATE SEQUENCE public.beneficiaryhistory_beneficiaryhistoryid_seq
    START WITH 625300601
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.beneficiaryhistory_beneficiaryhistoryid_seq OWNER TO svc_fhir_etl;

--
-- Name: beneficiaryhistorytemp_beneficiaryhistoryid_seq; Type: SEQUENCE; Schema: public; Owner: svc_fhir_etl
--

CREATE SEQUENCE public.beneficiaryhistorytemp_beneficiaryhistoryid_seq
    START WITH 53275601
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.beneficiaryhistorytemp_beneficiaryhistoryid_seq OWNER TO svc_fhir_etl;


--
-- Name: loadedbatches_loadedbatchid_seq; Type: SEQUENCE; Schema: public; Owner: svc_fhir_etl
--

CREATE SEQUENCE public.loadedbatches_loadedbatchid_seq
    START WITH 1
    INCREMENT BY 20
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    CYCLE;


ALTER TABLE public.loadedbatches_loadedbatchid_seq OWNER TO svc_fhir_etl;

--
-- Name: loadedfiles_loadedfileid_seq; Type: SEQUENCE; Schema: public; Owner: svc_fhir_etl
--

CREATE SEQUENCE public.loadedfiles_loadedfileid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    CYCLE;


ALTER TABLE public.loadedfiles_loadedfileid_seq OWNER TO svc_fhir_etl;


--
-- Name: BeneficiariesHistoryInvalidBeneficiaries BeneficiariesHistoryInvalidBeneficiaries_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."BeneficiariesHistoryInvalidBeneficiaries"
    ADD CONSTRAINT "BeneficiariesHistoryInvalidBeneficiaries_pkey" PRIMARY KEY ("beneficiaryHistoryId");


--
-- Name: BeneficiariesHistory BeneficiariesHistory_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."BeneficiariesHistory"
    ADD CONSTRAINT "BeneficiariesHistory_pkey" PRIMARY KEY ("beneficiaryHistoryId");


--
-- Name: Beneficiaries Beneficiaries_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."Beneficiaries"
    ADD CONSTRAINT "Beneficiaries_pkey" PRIMARY KEY ("beneficiaryId");


--
-- Name: beneficiary_monthly_audit BeneficiaryMonthlyAudit_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_bfd_pipeline_0
--

ALTER TABLE ONLY public.beneficiary_monthly_audit
    ADD CONSTRAINT "BeneficiaryMonthlyAudit_pkey" PRIMARY KEY ("parentBeneficiary", "yearMonth");


--
-- Name: BeneficiaryMonthly BeneficiaryMonthly_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_bfd_pipeline_0
--

ALTER TABLE ONLY public."BeneficiaryMonthly"
    ADD CONSTRAINT "BeneficiaryMonthly_pkey" PRIMARY KEY ("parentBeneficiary", "yearMonth");


--
-- Name: CarrierClaimLines CarrierClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."CarrierClaimLines"
    ADD CONSTRAINT "CarrierClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: CarrierClaims CarrierClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."CarrierClaims"
    ADD CONSTRAINT "CarrierClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: DMEClaimLines DMEClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."DMEClaimLines"
    ADD CONSTRAINT "DMEClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: DMEClaims DMEClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."DMEClaims"
    ADD CONSTRAINT "DMEClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: HHAClaimLines HHAClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HHAClaimLines"
    ADD CONSTRAINT "HHAClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: HHAClaims HHAClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HHAClaims"
    ADD CONSTRAINT "HHAClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: HospiceClaimLines HospiceClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HospiceClaimLines"
    ADD CONSTRAINT "HospiceClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: HospiceClaims HospiceClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HospiceClaims"
    ADD CONSTRAINT "HospiceClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: InpatientClaimLines InpatientClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."InpatientClaimLines"
    ADD CONSTRAINT "InpatientClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: InpatientClaims InpatientClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."InpatientClaims"
    ADD CONSTRAINT "InpatientClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: LoadedBatches LoadedBatches_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."LoadedBatches"
    ADD CONSTRAINT "LoadedBatches_pkey" PRIMARY KEY ("loadedBatchId");


--
-- Name: LoadedFiles LoadedFiles_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."LoadedFiles"
    ADD CONSTRAINT "LoadedFiles_pkey" PRIMARY KEY ("loadedFileId");


--
-- Name: MedicareBeneficiaryIdHistoryInvalidBeneficiaries MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries"
    ADD CONSTRAINT "MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey" PRIMARY KEY ("medicareBeneficiaryIdKey");


--
-- Name: MedicareBeneficiaryIdHistory MedicareBeneficiaryIdHistory_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."MedicareBeneficiaryIdHistory"
    ADD CONSTRAINT "MedicareBeneficiaryIdHistory_pkey" PRIMARY KEY ("medicareBeneficiaryIdKey");


--
-- Name: OutpatientClaimLines OutpatientClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."OutpatientClaimLines"
    ADD CONSTRAINT "OutpatientClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: OutpatientClaims OutpatientClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."OutpatientClaims"
    ADD CONSTRAINT "OutpatientClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: PartDEvents PartDEvents_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."PartDEvents"
    ADD CONSTRAINT "PartDEvents_pkey" PRIMARY KEY ("eventId");


--
-- Name: SNFClaimLines SNFClaimLines_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."SNFClaimLines"
    ADD CONSTRAINT "SNFClaimLines_pkey" PRIMARY KEY ("parentClaim", "lineNumber");


--
-- Name: SNFClaims SNFClaims_pkey; Type: CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."SNFClaims"
    ADD CONSTRAINT "SNFClaims_pkey" PRIMARY KEY ("claimId");


--
-- Name: BeneficiariesHistory_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "BeneficiariesHistory_beneficiaryId_idx" ON public."BeneficiariesHistory" USING btree ("beneficiaryId");


--
-- Name: BeneficiariesHistory_hicn_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "BeneficiariesHistory_hicn_idx" ON public."BeneficiariesHistory" USING btree (hicn);


--
-- Name: Beneficiaries_hicn_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_hicn_idx" ON public."Beneficiaries" USING btree (hicn);


--
-- Name: Beneficiaries_history_mbi_hash_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_history_mbi_hash_idx" ON public."BeneficiariesHistory" USING btree ("mbiHash");


--
-- Name: Beneficiaries_mbi_hash_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_mbi_hash_idx" ON public."Beneficiaries" USING btree ("mbiHash");


--
-- Name: Beneficiaries_partd_contract_number_apr_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_apr_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberAprId");


--
-- Name: Beneficiaries_partd_contract_number_aug_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_aug_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberAugId");


--
-- Name: Beneficiaries_partd_contract_number_dec_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_dec_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberDecId");


--
-- Name: Beneficiaries_partd_contract_number_feb_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_feb_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberFebId");


--
-- Name: Beneficiaries_partd_contract_number_jan_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_jan_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberJanId");


--
-- Name: Beneficiaries_partd_contract_number_jul_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_jul_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberJulId");


--
-- Name: Beneficiaries_partd_contract_number_jun_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_jun_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberJunId");


--
-- Name: Beneficiaries_partd_contract_number_mar_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_mar_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberMarId");


--
-- Name: Beneficiaries_partd_contract_number_may_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_may_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberMayId");


--
-- Name: Beneficiaries_partd_contract_number_nov_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_nov_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberNovId");


--
-- Name: Beneficiaries_partd_contract_number_oct_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_oct_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberOctId");


--
-- Name: Beneficiaries_partd_contract_number_sept_id_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "Beneficiaries_partd_contract_number_sept_id_idx" ON public."Beneficiaries" USING btree ("partDContractNumberSeptId");


--
-- Name: BeneficiaryMonthly_partDContractNumberId_yearmonth_idx; Type: INDEX; Schema: public; Owner: svc_bfd_pipeline_0
--

CREATE INDEX "BeneficiaryMonthly_partDContractNumberId_yearmonth_idx" ON public."BeneficiaryMonthly" USING btree ("partDContractNumberId", "yearMonth");


--
-- Name: CarrierClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "CarrierClaims_beneficiaryId_idx" ON public."CarrierClaims" USING btree ("beneficiaryId");


--
-- Name: DMEClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "DMEClaims_beneficiaryId_idx" ON public."DMEClaims" USING btree ("beneficiaryId");


--
-- Name: HHAClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "HHAClaims_beneficiaryId_idx" ON public."HHAClaims" USING btree ("beneficiaryId");


--
-- Name: HospiceClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "HospiceClaims_beneficiaryId_idx" ON public."HospiceClaims" USING btree ("beneficiaryId");


--
-- Name: InpatientClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "InpatientClaims_beneficiaryId_idx" ON public."InpatientClaims" USING btree ("beneficiaryId");


--
-- Name: LoadedBatches_created_index; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "LoadedBatches_created_index" ON public."LoadedBatches" USING btree (created);


--
-- Name: MedicareBeneficiaryIdHistory_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "MedicareBeneficiaryIdHistory_beneficiaryId_idx" ON public."MedicareBeneficiaryIdHistory" USING btree ("beneficiaryId");


--
-- Name: OutpatientClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "OutpatientClaims_beneficiaryId_idx" ON public."OutpatientClaims" USING btree ("beneficiaryId");


--
-- Name: PartDEvents_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "PartDEvents_beneficiaryId_idx" ON public."PartDEvents" USING btree ("beneficiaryId");


--
-- Name: SNFClaims_beneficiaryId_idx; Type: INDEX; Schema: public; Owner: svc_fhir_etl
--

CREATE INDEX "SNFClaims_beneficiaryId_idx" ON public."SNFClaims" USING btree ("beneficiaryId");


--
-- Name: BeneficiariesHistory BeneficiariesHistory_beneficiaryId_to_Beneficiary; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."BeneficiariesHistory"
    ADD CONSTRAINT "BeneficiariesHistory_beneficiaryId_to_Beneficiary" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: BeneficiaryMonthly BeneficiaryMonthly_parentBeneficiary_to_Beneficiary; Type: FK CONSTRAINT; Schema: public; Owner: svc_bfd_pipeline_0
--

ALTER TABLE ONLY public."BeneficiaryMonthly"
    ADD CONSTRAINT "BeneficiaryMonthly_parentBeneficiary_to_Beneficiary" FOREIGN KEY ("parentBeneficiary") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: CarrierClaimLines CarrierClaimLines_parentClaim_to_CarrierClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."CarrierClaimLines"
    ADD CONSTRAINT "CarrierClaimLines_parentClaim_to_CarrierClaims" FOREIGN KEY ("parentClaim") REFERENCES public."CarrierClaims"("claimId");


--
-- Name: CarrierClaims CarrierClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."CarrierClaims"
    ADD CONSTRAINT "CarrierClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: DMEClaimLines DMEClaimLines_parentClaim_to_DMEClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."DMEClaimLines"
    ADD CONSTRAINT "DMEClaimLines_parentClaim_to_DMEClaims" FOREIGN KEY ("parentClaim") REFERENCES public."DMEClaims"("claimId");


--
-- Name: DMEClaims DMEClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."DMEClaims"
    ADD CONSTRAINT "DMEClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: HHAClaimLines HHAClaimLines_parentClaim_to_HHAClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HHAClaimLines"
    ADD CONSTRAINT "HHAClaimLines_parentClaim_to_HHAClaims" FOREIGN KEY ("parentClaim") REFERENCES public."HHAClaims"("claimId");


--
-- Name: HHAClaims HHAClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HHAClaims"
    ADD CONSTRAINT "HHAClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: HospiceClaimLines HospiceClaimLines_parentClaim_to_HospiceClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HospiceClaimLines"
    ADD CONSTRAINT "HospiceClaimLines_parentClaim_to_HospiceClaims" FOREIGN KEY ("parentClaim") REFERENCES public."HospiceClaims"("claimId");


--
-- Name: HospiceClaims HospiceClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."HospiceClaims"
    ADD CONSTRAINT "HospiceClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: InpatientClaimLines InpatientClaimLines_parentClaim_to_InpatientClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."InpatientClaimLines"
    ADD CONSTRAINT "InpatientClaimLines_parentClaim_to_InpatientClaims" FOREIGN KEY ("parentClaim") REFERENCES public."InpatientClaims"("claimId");


--
-- Name: InpatientClaims InpatientClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."InpatientClaims"
    ADD CONSTRAINT "InpatientClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: MedicareBeneficiaryIdHistory MedicareBeneficiaryIdHistory_beneficiaryId_to_Beneficiary; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."MedicareBeneficiaryIdHistory"
    ADD CONSTRAINT "MedicareBeneficiaryIdHistory_beneficiaryId_to_Beneficiary" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: OutpatientClaimLines OutpatientClaimLines_parentClaim_to_OutpatientClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."OutpatientClaimLines"
    ADD CONSTRAINT "OutpatientClaimLines_parentClaim_to_OutpatientClaims" FOREIGN KEY ("parentClaim") REFERENCES public."OutpatientClaims"("claimId");


--
-- Name: OutpatientClaims OutpatientClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."OutpatientClaims"
    ADD CONSTRAINT "OutpatientClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: PartDEvents PartDEvents_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."PartDEvents"
    ADD CONSTRAINT "PartDEvents_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: SNFClaimLines SNFClaimLines_parentClaim_to_SNFClaims; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."SNFClaimLines"
    ADD CONSTRAINT "SNFClaimLines_parentClaim_to_SNFClaims" FOREIGN KEY ("parentClaim") REFERENCES public."SNFClaims"("claimId");


--
-- Name: SNFClaims SNFClaims_beneficiaryId_to_Beneficiaries; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."SNFClaims"
    ADD CONSTRAINT "SNFClaims_beneficiaryId_to_Beneficiaries" FOREIGN KEY ("beneficiaryId") REFERENCES public."Beneficiaries"("beneficiaryId");


--
-- Name: LoadedBatches loadedBatches_loadedFileId; Type: FK CONSTRAINT; Schema: public; Owner: svc_fhir_etl
--

ALTER TABLE ONLY public."LoadedBatches"
    ADD CONSTRAINT "loadedBatches_loadedFileId" FOREIGN KEY ("loadedFileId") REFERENCES public."LoadedFiles"("loadedFileId");
