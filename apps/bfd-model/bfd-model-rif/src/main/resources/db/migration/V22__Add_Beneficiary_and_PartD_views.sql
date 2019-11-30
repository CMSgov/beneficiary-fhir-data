-- Turns out, Diesel (our ORM), currently only supports tables with up to 128 columns. The default
-- is actually 32 columns; more requires turning on optional features.
-- References:
-- * <https://github.com/diesel-rs/diesel/issues/359>
-- * <https://github.com/diesel-rs/diesel/blob/v1.4.2/diesel/Cargo.toml>
--
-- To workaround that, we break up the CCW's RIF data tables into smaller, more normalized views.
-- It's a decent idea, in general, so I'm not _too_ upset about it.

-- Conventions used here:
-- 1. Don't go nuts with normalization: the primary goal here is to get max(columns-per-table)
--    down, not to build the most beautiful schema ever. Specifically, we're shooting for at most
--    64 columns in each view.
-- 2. Move back towards CCW field names, as much as possible and reasonable.
--    * <https://www2.ccwdata.org/web/guest/data-dictionaries>

-- Return beneficiary demographic data.
CREATE OR REPLACE VIEW benes AS
  SELECT
    "beneficiaryId" AS BENE_ID,
    "nameSurname" AS BENE_SRNM_NAME,
    "nameMiddleInitial" AS BENE_MDL_NAME,
    "nameGiven" AS BENE_GVN_NAME,
    "birthDate" AS BENE_BIRTH_DT,
    "ageOfBeneficiary" AS AGE,
    "validDateOfDeathSw" AS V_DOD_SW,
    race AS BENE_RACE_CD,
    "rtiRaceCode" AS RTI_RACE_CD,
    sex AS BENE_SEX_IDENT_CD,
    "beneficiaryDateOfDeath" AS DEATH_DT,
    "stateCode" AS STATE_CODE,
    "countyCode" AS BENE_COUNTY_CD,
    "postalCode" AS BENE_ZIP_CD,
    "sampleMedicareGroupIndicator" AS SAMPLE_GROUP,
    "enhancedMedicareSampleIndicator" AS EFIVEPCT,
    "sourceOfEnrollmentData" AS ENRL_SRC,
    "beneEnrollmentReferenceYear" AS RFRNC_YR,
    "medicareEnrollmentStatusCode" AS BENE_MDCR_STATUS_CD,
    "partATerminationCode" AS BENE_PTA_TRMNTN_CD,
    "partBTerminationCode" AS BENE_PTB_TRMNTN_CD,
    "currentBeneficiaryIdCode" AS CRNT_BIC,
    "entitlementCodeCurrent" AS BENE_ENTLMT_RSN_CURR,
    "entitlementCodeOriginal" AS BENE_ENTLMT_RSN_ORIG,
    "endStageRenalDiseaseCode" AS BENE_ESRD_IND,
    "partAMonthsCount" AS A_MO_CNT,
    "partBMonthsCount" AS B_MO_CNT,
    "stateBuyInCoverageCount" AS BUYIN_MO_CNT,
    "hmoCoverageCount" AS HMO_MO_CNT,
    "monthsRetireeDrugSubsidyCoverage" AS RDS_MO_CNT,
    "medicareCoverageStartDate" AS COVSTART,
    "monthsOfDualEligibility" AS DUAL_MO_CNT,
    "partDMonthsCount" AS PLAN_CVRG_MO_CNT
  FROM "Beneficiaries";

-- Return the HICNs for beneficiaries.
CREATE OR REPLACE VIEW benes_hicns AS
  SELECT
    DISTINCT ON (BENE_ID, BENE_CRNT_HIC_NUM)
    *
  FROM (
    SELECT
      "beneficiaryId" AS BENE_ID,
      hicn AS BENE_CRNT_HIC_NUM_HASH,
      "hicnUnhashed" AS BENE_CRNT_HIC_NUM,
      TRUE AS HICN_LATEST  
    FROM "Beneficiaries"
    UNION
    SELECT
      "beneficiaryId" AS BENE_ID,
      hicn AS BENE_CRNT_HIC_NUM_HASH,
      "hicnUnhashed" AS BENE_CRNT_HIC_NUM,
      FALSE AS HICN_LATEST  
    FROM "BeneficiariesHistory"
  ) AS hicns_union
  ORDER BY
    BENE_ID,
    BENE_CRNT_HIC_NUM,
    -- Return the row where HICN_LATEST=TRUE first, which will be the row selected by the
    -- DISTINCT ON clause.
    HICN_LATEST DESC;

-- Return the MBIs for beneficiaries.
CREATE OR REPLACE VIEW benes_mbis AS
  SELECT
    DISTINCT ON (BENE_ID, MBI_NUM)
    *
  FROM (
    SELECT
      "beneficiaryId" AS BENE_ID,
      "medicareBeneficiaryId" AS MBI_NUM,
      TRUE AS MBI_LATEST  
    FROM "Beneficiaries"
    UNION
    SELECT
      "beneficiaryId" AS BENE_ID,
      "medicareBeneficiaryId" AS MBI_NUM,
      FALSE AS MBI_LATEST  
    FROM "BeneficiariesHistory"
  ) AS mbis_union
  ORDER BY
    BENE_ID,
    MBI_NUM,
    -- Return the row where MBI_LATEST=TRUE first, which will be the row selected by the
    -- DISTINCT ON clause.
    MBI_LATEST DESC;
  SELECT
    "beneficiaryId" AS BENE_ID,
    "medicareBeneficiaryId" AS MBI_NUM
  FROM "Beneficiaries";

-- Return the month-specific data for beneficiaries.
CREATE OR REPLACE VIEW benes_monthly AS
  SELECT
    "beneficiaryId" AS BENE_ID,
    "beneEnrollmentReferenceYear" AS RFRNC_YR,
    unnest(array[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]) AS RFRNC_MO,
    unnest(array["fipsStateCntyJanCode", "fipsStateCntyFebCode", "fipsStateCntyMarCode", "fipsStateCntyAprCode", "fipsStateCntyMayCode", "fipsStateCntyJunCode", "fipsStateCntyJulCode", "fipsStateCntyAugCode", "fipsStateCntySeptCode", "fipsStateCntyOctCode", "fipsStateCntyNovCode", "fipsStateCntyDecCode"]) AS FIPS_STATE_CNTY_CD,
    unnest(array["medicareStatusJanCode", "medicareStatusFebCode", "medicareStatusMarCode", "medicareStatusAprCode", "medicareStatusMayCode", "medicareStatusJunCode", "medicareStatusJulCode", "medicareStatusAugCode", "medicareStatusSeptCode", "medicareStatusOctCode", "medicareStatusNovCode", "medicareStatusDecCode"]) AS MDCR_STUS_CD,
    unnest(array["entitlementBuyInJanInd", "entitlementBuyInFebInd", "entitlementBuyInMarInd", "entitlementBuyInAprInd", "entitlementBuyInMayInd", "entitlementBuyInJunInd", "entitlementBuyInJulInd", "entitlementBuyInAugInd", "entitlementBuyInSeptInd", "entitlementBuyInOctInd", "entitlementBuyInNovInd", "entitlementBuyInDecInd"]) AS MDCR_ENTLMT_BUYIN_IND,
    unnest(array["hmoIndicatorJanInd", "hmoIndicatorFebInd", "hmoIndicatorMarInd", "hmoIndicatorAprInd", "hmoIndicatorMayInd", "hmoIndicatorJunInd", "hmoIndicatorJulInd", "hmoIndicatorAugInd", "hmoIndicatorSeptInd", "hmoIndicatorOctInd", "hmoIndicatorNovInd", "hmoIndicatorDecInd"]) AS HMO_IND,
    unnest(array["partCContractNumberJanId", "partCContractNumberFebId", "partCContractNumberMarId", "partCContractNumberAprId", "partCContractNumberMayId", "partCContractNumberJunId", "partCContractNumberJulId", "partCContractNumberAugId", "partCContractNumberSeptId", "partCContractNumberOctId", "partCContractNumberNovId", "partCContractNumberDecId"]) AS PTC_CNTRCT_ID,
    unnest(array["partCPbpNumberJanId", "partCPbpNumberFebId", "partCPbpNumberMarId", "partCPbpNumberAprId", "partCPbpNumberMayId", "partCPbpNumberJunId", "partCPbpNumberJulId", "partCPbpNumberAugId", "partCPbpNumberSeptId", "partCPbpNumberOctId", "partCPbpNumberNovId", "partCPbpNumberDecId"]) AS PTC_PBP_ID,
    unnest(array["partCPlanTypeJanCode", "partCPlanTypeFebCode", "partCPlanTypeMarCode", "partCPlanTypeAprCode", "partCPlanTypeMayCode", "partCPlanTypeJunCode", "partCPlanTypeJulCode", "partCPlanTypeAugCode", "partCPlanTypeSeptCode", "partCPlanTypeOctCode", "partCPlanTypeNovCode", "partCPlanTypeDecCode"]) AS PTC_PLAN_TYPE_CD,
    unnest(array["partDContractNumberJanId", "partDContractNumberFebId", "partDContractNumberMarId", "partDContractNumberAprId", "partDContractNumberMayId", "partDContractNumberJunId", "partDContractNumberJulId", "partDContractNumberAugId", "partDContractNumberSeptId", "partDContractNumberOctId", "partDContractNumberNovId", "partDContractNumberDecId"]) AS PTD_CNTRCT_ID,
    unnest(array["partDPbpNumberJanId", "partDPbpNumberFebId", "partDPbpNumberMarId", "partDPbpNumberAprId", "partDPbpNumberMayId", "partDPbpNumberJunId", "partDPbpNumberJulId", "partDPbpNumberAugId", "partDPbpNumberSeptId", "partDPbpNumberOctId", "partDPbpNumberNovId", "partDPbpNumberDecId"]) AS PTD_PBP_ID,
    unnest(array["partDSegmentNumberJanId", "partDSegmentNumberFebId", "partDSegmentNumberMarId", "partDSegmentNumberAprId", "partDSegmentNumberMayId", "partDSegmentNumberJunId", "partDSegmentNumberJulId", "partDSegmentNumberAugId", "partDSegmentNumberSeptId", "partDSegmentNumberOctId", "partDSegmentNumberNovId", "partDSegmentNumberDecId"]) AS PTD_SGMT_ID,
    unnest(array["partDRetireeDrugSubsidyJanInd", "partDRetireeDrugSubsidyFebInd", "partDRetireeDrugSubsidyMarInd", "partDRetireeDrugSubsidyAprInd", "partDRetireeDrugSubsidyMayInd", "partDRetireeDrugSubsidyJunInd", "partDRetireeDrugSubsidyJulInd", "partDRetireeDrugSubsidyAugInd", "partDRetireeDrugSubsidySeptInd", "partDRetireeDrugSubsidyOctInd", "partDRetireeDrugSubsidyNovInd", "partDRetireeDrugSubsidyDecInd"]) AS RDS_IND,
    unnest(array["medicaidDualEligibilityJanCode", "medicaidDualEligibilityFebCode", "medicaidDualEligibilityMarCode", "medicaidDualEligibilityAprCode", "medicaidDualEligibilityMayCode", "medicaidDualEligibilityJunCode", "medicaidDualEligibilityJulCode", "medicaidDualEligibilityAugCode", "medicaidDualEligibilitySeptCode", "medicaidDualEligibilityOctCode", "medicaidDualEligibilityNovCode", "medicaidDualEligibilityDecCode"]) AS META_DUAL_ELGBL_STUS_CD,
    unnest(array["partDLowIncomeCostShareGroupJanCode", "partDLowIncomeCostShareGroupFebCode", "partDLowIncomeCostShareGroupMarCode", "partDLowIncomeCostShareGroupAprCode", "partDLowIncomeCostShareGroupMayCode", "partDLowIncomeCostShareGroupJunCode", "partDLowIncomeCostShareGroupJulCode", "partDLowIncomeCostShareGroupAugCode", "partDLowIncomeCostShareGroupSeptCode", "partDLowIncomeCostShareGroupOctCode", "partDLowIncomeCostShareGroupNovCode", "partDLowIncomeCostShareGroupDecCode"]) AS CST_SHR_GRP_CD
  FROM "Beneficiaries";

-- First, get back to the original CCW/RIF column names.
CREATE OR REPLACE VIEW claims_partd AS
  SELECT
    "eventId" AS PDE_ID,
    "adjustmentDeletionCode" AS ADJSTMT_DLTN_CD,
    "beneficiaryId" AS BENE_ID,
    "brandGenericCode" AS BRND_GNRC_CD,
    "catastrophicCoverageCode" AS CTSTRPHC_CVRG_CD,
    "claimGroupId" AS CLM_GRP_ID,
    "compoundCode" AS CMPND_CD,
    "daysSupply" AS DAYS_SUPLY_NUM,
    "dispenseAsWrittenProductSelectionCode" AS DAW_PROD_SLCTN_CD,
    "dispensingStatusCode" AS DSPNSNG_STUS_CD,
    "drugCoverageStatusCode" AS DRUG_CVRG_STUS_CD,
    "fillNumber" AS FILL_NUM,
    "gapDiscountAmount" AS RPTD_GAP_DSCNT_NUM,
    "grossCostAboveOutOfPocketThreshold" AS GDC_ABV_OOPT_AMT,
    "grossCostBelowOutOfPocketThreshold" AS GDC_BLW_OOPT_AMT,
    "lowIncomeSubsidyPaidAmount" AS LICS_AMT,
    "nationalDrugCode" AS PROD_SRVC_ID,
    "nonstandardFormatCode" AS NSTD_FRMT_CD,
    "otherTrueOutOfPocketPaidAmount" AS OTHR_TROOP_AMT,
    "partDPlanCoveredPaidAmount" AS CVRD_D_PLAN_PD_AMT,
    "partDPlanNonCoveredPaidAmount" AS NCVRD_PLAN_PD_AMT,
    "patientLiabilityReductionOtherPaidAmount" AS PLRO_AMT,
    "patientPaidAmount" AS PTNT_PAY_AMT,
    "patientResidenceCode" AS PTNT_RSDNC_CD,
    "paymentDate" AS PD_DT,
    "pharmacyTypeCode" AS PHRMCY_SRVC_TYPE_CD,
    "planBenefitPackageId" AS PLAN_PBP_REC_NUM,
    "planContractId" AS PLAN_CNTRCT_REC_ID,
    "prescriberId" AS PRSCRBR_ID,
    "prescriberIdQualifierCode" AS PRSCRBR_ID_QLFYR_CD,
    "prescriptionFillDate" AS SRVC_DT,
    "prescriptionOriginationCode" AS RX_ORGN_CD,
    "prescriptionReferenceNumber" AS RX_SRVC_RFRNC_NUM,
    "pricingExceptionCode" AS PRCNG_EXCPTN_CD,
    "quantityDispensed" AS QTY_DSPNSD_NUM,
    "serviceProviderId" AS SRVC_PRVDR_ID,
    "serviceProviderIdQualiferCode" AS SRVC_PRVDR_ID_QLFYR_CD,
    "submissionClarificationCode" AS SUBMSN_CLR_CD,
    "totalPrescriptionCost" AS TOT_RX_CST_AMT,
    "finalAction" AS FINAL_ACTION
  FROM "PartDEvents";
