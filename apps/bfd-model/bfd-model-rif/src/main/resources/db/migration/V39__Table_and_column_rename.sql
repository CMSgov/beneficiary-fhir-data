--
-- NOTES:
--   1. when you rename a table, indexes/constraints will trickle down to contraint & index directives,
--      BUT not contraint or index names themselves
--   2. don't try to rename a column that already has the name (i.e., "hicn" ${logic.rename-to} hicn)
--   3. optionally rename contraint and/or index names (i.e., remove camelCase)
--
-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file prior to performing table or column rename.
-- SCRIPT './bfd_schema_pre.txt';
--
-- Drop foreign key constraints
--
ALTER TABLE public."BeneficiariesHistory"
    DROP CONSTRAINT "BeneficiariesHistory_beneficiaryId_to_Beneficiary";

ALTER TABLE public."BeneficiaryMonthly"
    DROP CONSTRAINT "BeneficiaryMonthly_parentBeneficiary_to_Beneficiary";

ALTER TABLE public."CarrierClaimLines"
    DROP CONSTRAINT "CarrierClaimLines_parentClaim_to_CarrierClaims";

ALTER TABLE public."CarrierClaims"
    DROP CONSTRAINT "CarrierClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."DMEClaimLines"
    DROP CONSTRAINT "DMEClaimLines_parentClaim_to_DMEClaims";

ALTER TABLE public."DMEClaims"
    DROP CONSTRAINT "DMEClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."HHAClaimLines"
    DROP CONSTRAINT "HHAClaimLines_parentClaim_to_HHAClaims";

ALTER TABLE public."HHAClaims"
    DROP CONSTRAINT "HHAClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."HospiceClaimLines"
    DROP CONSTRAINT "HospiceClaimLines_parentClaim_to_HospiceClaims";

ALTER TABLE public."HospiceClaims"
    DROP CONSTRAINT "HospiceClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."InpatientClaimLines"
    DROP CONSTRAINT "InpatientClaimLines_parentClaim_to_InpatientClaims";

ALTER TABLE public."InpatientClaims"
    DROP CONSTRAINT "InpatientClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."MedicareBeneficiaryIdHistory"
    DROP CONSTRAINT "MedicareBeneficiaryIdHistory_beneficiaryId_to_Beneficiary";

ALTER TABLE public."OutpatientClaimLines"
    DROP CONSTRAINT "OutpatientClaimLines_parentClaim_to_OutpatientClaims";

ALTER TABLE public."OutpatientClaims"
    DROP CONSTRAINT "OutpatientClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."PartDEvents"
    DROP CONSTRAINT "PartDEvents_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."SNFClaimLines"
    DROP CONSTRAINT "SNFClaimLines_parentClaim_to_SNFClaims";

ALTER TABLE public."SNFClaims"
    DROP CONSTRAINT "SNFClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."LoadedBatches"
    DROP CONSTRAINT "loadedBatches_loadedFileId";
--
-- Drop primary key constraints; hsql does not support rename constraint
--
ALTER TABLE public."BeneficiariesHistoryInvalidBeneficiaries"
    DROP CONSTRAINT "BeneficiariesHistoryInvalidBeneficiaries_pkey";

ALTER TABLE public."BeneficiariesHistory"
    DROP CONSTRAINT "BeneficiariesHistory_pkey";

ALTER TABLE public."Beneficiaries"
    DROP CONSTRAINT "Beneficiaries_pkey";

ALTER TABLE public."BeneficiaryMonthly"
    DROP CONSTRAINT "BeneficiaryMonthly_pkey";

ALTER TABLE public."CarrierClaimLines"
    DROP CONSTRAINT "CarrierClaimLines_pkey";

ALTER TABLE public."CarrierClaims"
    DROP CONSTRAINT "CarrierClaims_pkey";

ALTER TABLE public."DMEClaimLines"
    DROP CONSTRAINT "DMEClaimLines_pkey";

ALTER TABLE public."DMEClaims"
    DROP CONSTRAINT "DMEClaims_pkey";

ALTER TABLE public."HHAClaimLines"
    DROP CONSTRAINT "HHAClaimLines_pkey";

ALTER TABLE public."HHAClaims"
    DROP CONSTRAINT "HHAClaims_pkey";

ALTER TABLE public."HospiceClaimLines"
    DROP CONSTRAINT "HospiceClaimLines_pkey";

ALTER TABLE public."HospiceClaims"
    DROP CONSTRAINT "HospiceClaims_pkey";

ALTER TABLE public."InpatientClaimLines"
    DROP CONSTRAINT "InpatientClaimLines_pkey";

ALTER TABLE public."InpatientClaims"
    DROP CONSTRAINT "InpatientClaims_pkey";

-- FIX THIS
/*
ALTER TABLE public."LoadedBatches"
    DROP CONSTRAINT "LoadedBatches_pkey";

ALTER TABLE public."LoadedFiles"
    DROP CONSTRAINT "LoadedFiles_pkey";
*/
ALTER TABLE public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries"
    DROP CONSTRAINT "MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey";

ALTER TABLE public."MedicareBeneficiaryIdHistory"
    DROP CONSTRAINT "MedicareBeneficiaryIdHistory_pkey";

ALTER TABLE public."OutpatientClaimLines"
    DROP CONSTRAINT "OutpatientClaimLines_pkey";

ALTER TABLE public."OutpatientClaims"
    DROP CONSTRAINT "OutpatientClaims_pkey";

ALTER TABLE public."PartDEvents"
    DROP CONSTRAINT "PartDEvents_pkey";

ALTER TABLE public."SNFClaimLines"
    DROP CONSTRAINT "SNFClaimLines_pkey";

ALTER TABLE public."SNFClaims"
    DROP CONSTRAINT "SNFClaims_pkey";
--
-- Rename tables and table columns
--
--      psql: alter table public.beneficiaries rename column "beneficiaryId" to bene_id;
--      hsql: alter table public.beneficiaries alter column  "beneficiaryId" rename to bene_id;
--
--      ${logic.alter-rename-column}
--          psql: "rename column"
--          hsql: "alter column"
--
--      ${logic.rename-to}
--          psql: "to"
--          hsql: "rename to"
--
-- Beneficiaries to beneficiaries
-- 
alter table public."Beneficiaries" rename to beneficiaries;
alter table public.beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.beneficiaries ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries ${logic.alter-rename-column} "endStageRenalDiseaseCode" ${logic.rename-to} bene_esrd_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementCodeCurrent" ${logic.rename-to} bene_entlmt_rsn_curr;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementCodeOriginal" ${logic.rename-to} bene_entlmt_rsn_orig;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareEnrollmentStatusCode" ${logic.rename-to} bene_mdcr_status_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "nameGiven" ${logic.rename-to} bene_gvn_name;
alter table public.beneficiaries ${logic.alter-rename-column} "nameMiddleInitial" ${logic.rename-to} bene_mdl_name;
alter table public.beneficiaries ${logic.alter-rename-column} "nameSurname" ${logic.rename-to} bene_srnm_name;
alter table public.beneficiaries ${logic.alter-rename-column} "stateCode" ${logic.rename-to} state_code;
alter table public.beneficiaries ${logic.alter-rename-column} "countyCode" ${logic.rename-to} bene_county_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "postalCode" ${logic.rename-to} bene_zip_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "ageOfBeneficiary" ${logic.rename-to} age;
alter table public.beneficiaries ${logic.alter-rename-column} "race" ${logic.rename-to} bene_race_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "rtiRaceCode" ${logic.rename-to} rti_race_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries ${logic.alter-rename-column} "mbiHash" ${logic.rename-to} mbi_hash;
alter table public.beneficiaries ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} efctv_bgn_dt;
alter table public.beneficiaries ${logic.alter-rename-column} "mbiObsoleteDate" ${logic.rename-to} efctv_end_dt;
alter table public.beneficiaries ${logic.alter-rename-column} "beneficiaryDateOfDeath" ${logic.rename-to} death_dt;
alter table public.beneficiaries ${logic.alter-rename-column} "validDateOfDeathSw" ${logic.rename-to} v_dod_sw;
alter table public.beneficiaries ${logic.alter-rename-column} "beneEnrollmentReferenceYear" ${logic.rename-to} rfrnc_yr;
alter table public.beneficiaries ${logic.alter-rename-column} "partATerminationCode" ${logic.rename-to} bene_pta_trmntn_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partBTerminationCode" ${logic.rename-to} bene_ptb_trmntn_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partAMonthsCount" ${logic.rename-to} a_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "partBMonthsCount" ${logic.rename-to} b_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "stateBuyInCoverageCount" ${logic.rename-to} buyin_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoCoverageCount" ${logic.rename-to} hmo_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "monthsRetireeDrugSubsidyCoverage" ${logic.rename-to} rds_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "sourceOfEnrollmentData" ${logic.rename-to} enrl_src;
alter table public.beneficiaries ${logic.alter-rename-column} "sampleMedicareGroupIndicator" ${logic.rename-to} sample_group;
alter table public.beneficiaries ${logic.alter-rename-column} "enhancedMedicareSampleIndicator" ${logic.rename-to} efivepct;
alter table public.beneficiaries ${logic.alter-rename-column} "currentBeneficiaryIdCode" ${logic.rename-to} crnt_bic;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareCoverageStartDate" ${logic.rename-to} covstart;
alter table public.beneficiaries ${logic.alter-rename-column} "monthsOfDualEligibility" ${logic.rename-to} dual_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyJanCode" ${logic.rename-to} fips_state_cnty_jan_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyFebCode" ${logic.rename-to} fips_state_cnty_feb_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyMarCode" ${logic.rename-to} fips_state_cnty_mar_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyAprCode" ${logic.rename-to} fips_state_cnty_apr_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyMayCode" ${logic.rename-to} fips_state_cnty_may_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyJunCode" ${logic.rename-to} fips_state_cnty_jun_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyJulCode" ${logic.rename-to} fips_state_cnty_jul_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyAugCode" ${logic.rename-to} fips_state_cnty_aug_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntySeptCode" ${logic.rename-to} fips_state_cnty_sept_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyOctCode" ${logic.rename-to} fips_state_cnty_oct_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyNovCode" ${logic.rename-to} fips_state_cnty_nov_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "fipsStateCntyDecCode" ${logic.rename-to} fips_state_cnty_dec_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusJanCode" ${logic.rename-to} mdcr_stus_jan_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusFebCode" ${logic.rename-to} mdcr_stus_feb_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusMarCode" ${logic.rename-to} mdcr_stus_mar_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusAprCode" ${logic.rename-to} mdcr_stus_apr_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusMayCode" ${logic.rename-to} mdcr_stus_may_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusJunCode" ${logic.rename-to} mdcr_stus_jun_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusJulCode" ${logic.rename-to} mdcr_stus_jul_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusAugCode" ${logic.rename-to} mdcr_stus_aug_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusSeptCode" ${logic.rename-to} mdcr_stus_sept_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusOctCode" ${logic.rename-to} mdcr_stus_oct_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusNovCode" ${logic.rename-to} mdcr_stus_nov_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicareStatusDecCode" ${logic.rename-to} mdcr_stus_dec_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDMonthsCount" ${logic.rename-to} plan_cvrg_mo_cnt;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInJanInd" ${logic.rename-to} mdcr_entlmt_buyin_1_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInFebInd" ${logic.rename-to} mdcr_entlmt_buyin_2_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInMarInd" ${logic.rename-to} mdcr_entlmt_buyin_3_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInAprInd" ${logic.rename-to} mdcr_entlmt_buyin_4_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInMayInd" ${logic.rename-to} mdcr_entlmt_buyin_5_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInJunInd" ${logic.rename-to} mdcr_entlmt_buyin_6_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInJulInd" ${logic.rename-to} mdcr_entlmt_buyin_7_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInAugInd" ${logic.rename-to} mdcr_entlmt_buyin_8_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInSeptInd" ${logic.rename-to} mdcr_entlmt_buyin_9_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInOctInd" ${logic.rename-to} mdcr_entlmt_buyin_10_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInNovInd" ${logic.rename-to} mdcr_entlmt_buyin_11_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "entitlementBuyInDecInd" ${logic.rename-to} mdcr_entlmt_buyin_12_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorJanInd" ${logic.rename-to} hmo_1_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorFebInd" ${logic.rename-to} hmo_2_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorMarInd" ${logic.rename-to} hmo_3_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorAprInd" ${logic.rename-to} hmo_4_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorMayInd" ${logic.rename-to} hmo_5_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorJunInd" ${logic.rename-to} hmo_6_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorJulInd" ${logic.rename-to} hmo_7_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorAugInd" ${logic.rename-to} hmo_8_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorSeptInd" ${logic.rename-to} hmo_9_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorOctInd" ${logic.rename-to} hmo_10_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorNovInd" ${logic.rename-to} hmo_11_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "hmoIndicatorDecInd" ${logic.rename-to} hmo_12_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberJanId" ${logic.rename-to} ptc_cntrct_jan_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberFebId" ${logic.rename-to} ptc_cntrct_feb_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberMarId" ${logic.rename-to} ptc_cntrct_mar_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberAprId" ${logic.rename-to} ptc_cntrct_apr_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberMayId" ${logic.rename-to} ptc_cntrct_may_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberJunId" ${logic.rename-to} ptc_cntrct_jun_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberJulId" ${logic.rename-to} ptc_cntrct_jul_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberAugId" ${logic.rename-to} ptc_cntrct_aug_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberSeptId" ${logic.rename-to} ptc_cntrct_sept_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberOctId" ${logic.rename-to} ptc_cntrct_oct_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberNovId" ${logic.rename-to} ptc_cntrct_nov_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCContractNumberDecId" ${logic.rename-to} ptc_cntrct_dec_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberJanId" ${logic.rename-to} ptc_pbp_jan_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberFebId" ${logic.rename-to} ptc_pbp_feb_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberMarId" ${logic.rename-to} ptc_pbp_mar_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberAprId" ${logic.rename-to} ptc_pbp_apr_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberMayId" ${logic.rename-to} ptc_pbp_may_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberJunId" ${logic.rename-to} ptc_pbp_jun_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberJulId" ${logic.rename-to} ptc_pbp_jul_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberAugId" ${logic.rename-to} ptc_pbp_aug_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberSeptId" ${logic.rename-to} ptc_pbp_sept_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberOctId" ${logic.rename-to} ptc_pbp_oct_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberNovId" ${logic.rename-to} ptc_pbp_nov_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPbpNumberDecId" ${logic.rename-to} ptc_pbp_dec_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeJanCode" ${logic.rename-to} ptc_plan_type_jan_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeFebCode" ${logic.rename-to} ptc_plan_type_feb_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeMarCode" ${logic.rename-to} ptc_plan_type_mar_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeAprCode" ${logic.rename-to} ptc_plan_type_apr_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeMayCode" ${logic.rename-to} ptc_plan_type_may_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeJunCode" ${logic.rename-to} ptc_plan_type_jun_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeJulCode" ${logic.rename-to} ptc_plan_type_jul_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeAugCode" ${logic.rename-to} ptc_plan_type_aug_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeSeptCode" ${logic.rename-to} ptc_plan_type_sept_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeOctCode" ${logic.rename-to} ptc_plan_type_oct_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeNovCode" ${logic.rename-to} ptc_plan_type_nov_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partCPlanTypeDecCode" ${logic.rename-to} ptc_plan_type_dec_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberJanId" ${logic.rename-to} ptd_cntrct_jan_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberFebId" ${logic.rename-to} ptd_cntrct_feb_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberMarId" ${logic.rename-to} ptd_cntrct_mar_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberAprId" ${logic.rename-to} ptd_cntrct_apr_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberMayId" ${logic.rename-to} ptd_cntrct_may_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberJunId" ${logic.rename-to} ptd_cntrct_jun_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberJulId" ${logic.rename-to} ptd_cntrct_jul_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberAugId" ${logic.rename-to} ptd_cntrct_aug_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberSeptId" ${logic.rename-to} ptd_cntrct_sept_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberOctId" ${logic.rename-to} ptd_cntrct_oct_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberNovId" ${logic.rename-to} ptd_cntrct_nov_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDContractNumberDecId" ${logic.rename-to} ptd_cntrct_dec_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberJanId" ${logic.rename-to} ptd_pbp_jan_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberFebId" ${logic.rename-to} ptd_pbp_feb_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberMarId" ${logic.rename-to} ptd_pbp_mar_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberAprId" ${logic.rename-to} ptd_pbp_apr_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberMayId" ${logic.rename-to} ptd_pbp_may_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberJunId" ${logic.rename-to} ptd_pbp_jun_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberJulId" ${logic.rename-to} ptd_pbp_jul_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberAugId" ${logic.rename-to} ptd_pbp_aug_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberSeptId" ${logic.rename-to} ptd_pbp_sept_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberOctId" ${logic.rename-to} ptd_pbp_oct_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberNovId" ${logic.rename-to} ptd_pbp_nov_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDPbpNumberDecId" ${logic.rename-to} ptd_pbp_dec_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberJanId" ${logic.rename-to} ptd_sgmt_jan_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberFebId" ${logic.rename-to} ptd_sgmt_feb_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberMarId" ${logic.rename-to} ptd_sgmt_mar_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberAprId" ${logic.rename-to} ptd_sgmt_apr_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberMayId" ${logic.rename-to} ptd_sgmt_may_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberJunId" ${logic.rename-to} ptd_sgmt_jun_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberJulId" ${logic.rename-to} ptd_sgmt_jul_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberAugId" ${logic.rename-to} ptd_sgmt_aug_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberSeptId" ${logic.rename-to} ptd_sgmt_sept_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberOctId" ${logic.rename-to} ptd_sgmt_oct_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberNovId" ${logic.rename-to} ptd_sgmt_nov_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDSegmentNumberDecId" ${logic.rename-to} ptd_sgmt_dec_id;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyJanInd" ${logic.rename-to} rds_jan_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyFebInd" ${logic.rename-to} rds_feb_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyMarInd" ${logic.rename-to} rds_mar_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyAprInd" ${logic.rename-to} rds_apr_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyMayInd" ${logic.rename-to} rds_may_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyJunInd" ${logic.rename-to} rds_jun_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyJulInd" ${logic.rename-to} rds_jul_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyAugInd" ${logic.rename-to} rds_aug_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidySeptInd" ${logic.rename-to} rds_sept_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyOctInd" ${logic.rename-to} rds_oct_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyNovInd" ${logic.rename-to} rds_nov_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "partDRetireeDrugSubsidyDecInd" ${logic.rename-to} rds_dec_ind;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityJanCode" ${logic.rename-to} meta_dual_elgbl_stus_jan_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityFebCode" ${logic.rename-to} meta_dual_elgbl_stus_feb_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityMarCode" ${logic.rename-to} meta_dual_elgbl_stus_mar_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityAprCode" ${logic.rename-to} meta_dual_elgbl_stus_apr_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityMayCode" ${logic.rename-to} meta_dual_elgbl_stus_may_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityJunCode" ${logic.rename-to} meta_dual_elgbl_stus_jun_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityJulCode" ${logic.rename-to} meta_dual_elgbl_stus_jul_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityAugCode" ${logic.rename-to} meta_dual_elgbl_stus_aug_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilitySeptCode" ${logic.rename-to} meta_dual_elgbl_stus_sept_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityOctCode" ${logic.rename-to} meta_dual_elgbl_stus_oct_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityNovCode" ${logic.rename-to} meta_dual_elgbl_stus_nov_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "medicaidDualEligibilityDecCode" ${logic.rename-to} meta_dual_elgbl_stus_dec_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupJanCode" ${logic.rename-to} cst_shr_grp_jan_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupFebCode" ${logic.rename-to} cst_shr_grp_feb_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupMarCode" ${logic.rename-to} cst_shr_grp_mar_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupAprCode" ${logic.rename-to} cst_shr_grp_apr_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupMayCode" ${logic.rename-to} cst_shr_grp_may_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupJunCode" ${logic.rename-to} cst_shr_grp_jun_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupJulCode" ${logic.rename-to} cst_shr_grp_jul_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupAugCode" ${logic.rename-to} cst_shr_grp_aug_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupSeptCode" ${logic.rename-to} cst_shr_grp_sept_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupOctCode" ${logic.rename-to} cst_shr_grp_oct_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupNovCode" ${logic.rename-to} cst_shr_grp_nov_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "partDLowIncomeCostShareGroupDecCode" ${logic.rename-to} cst_shr_grp_dec_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedMailingAddress1" ${logic.rename-to} drvd_line_1_adr;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedMailingAddress2" ${logic.rename-to} drvd_line_2_adr;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedMailingAddress3" ${logic.rename-to} drvd_line_3_adr;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedMailingAddress4" ${logic.rename-to} drvd_line_4_adr;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedMailingAddress5" ${logic.rename-to} drvd_line_5_adr;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedMailingAddress6" ${logic.rename-to} drvd_line_6_adr;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedCityName" ${logic.rename-to} city_name;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedStateCode" ${logic.rename-to} state_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "derivedZipCode" ${logic.rename-to} state_cnty_zip_cd;
alter table public.beneficiaries ${logic.alter-rename-column} "beneLinkKey" ${logic.rename-to} bene_link_key;
--
-- CarrierClaims to carrier_claims
--
alter table public."CarrierClaims" rename to carrier_claims;
alter table public.carrier_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.carrier_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.carrier_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.carrier_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.carrier_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.carrier_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.carrier_claims ${logic.alter-rename-column} "clinicalTrialNumber" ${logic.rename-to} clm_clncl_tril_num;
alter table public.carrier_claims ${logic.alter-rename-column} "claimDispositionCode" ${logic.rename-to} clm_disp_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "claimCarrierControlNumber" ${logic.rename-to} carr_clm_cntl_num;
alter table public.carrier_claims ${logic.alter-rename-column} "claimEntryCode" ${logic.rename-to} carr_clm_entry_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "hcpcsYearCode" ${logic.rename-to} carr_clm_hcpcs_yr_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "paymentDenialCode" ${logic.rename-to} carr_clm_pmt_dnl_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "providerAssignmentIndicator" ${logic.rename-to} carr_clm_prvdr_asgnmt_ind_sw;
alter table public.carrier_claims ${logic.alter-rename-column} "referringProviderIdNumber" ${logic.rename-to} carr_clm_rfrng_pin_num;
alter table public.carrier_claims ${logic.alter-rename-column} "carrierNumber" ${logic.rename-to} carr_num;
alter table public.carrier_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.carrier_claims ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} nch_carr_clm_alowd_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} nch_carr_clm_sbmtd_chrg_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} nch_clm_bene_pmt_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} nch_clm_prvdr_pmt_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} carr_clm_cash_ddctbl_apld_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} carr_clm_prmry_pyr_pd_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "referringPhysicianNpi" ${logic.rename-to} rfr_physn_npi;
alter table public.carrier_claims ${logic.alter-rename-column} "referringPhysicianUpin" ${logic.rename-to} rfr_physn_upin;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
--
-- CarrierClaimLines to carrier_claim_lines
--
alter table public."CarrierClaimLines" rename to carrier_claim_lines;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} line_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} line_nch_pmt_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "firstExpenseDate" ${logic.rename-to} line_1st_expns_dt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} line_alowd_chrg_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} line_bene_pmt_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "primaryPayerCode" ${logic.rename-to} line_bene_prmry_pyr_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} line_bene_prmry_pyr_pd_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} line_bene_ptb_ddctbl_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "cmsServiceTypeCode" ${logic.rename-to} line_cms_type_srvc_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "coinsuranceAmount" ${logic.rename-to} line_coinsrnc_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hctHgbTestResult" ${logic.rename-to} line_hct_hgb_rslt_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hctHgbTestTypeCode" ${logic.rename-to} line_hct_hgb_type_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "diagnosisCode" ${logic.rename-to} line_icd_dgns_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "diagnosisCodeVersion" ${logic.rename-to} line_icd_dgns_vrsn_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "lastExpenseDate" ${logic.rename-to} line_last_expns_dt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} line_ndc_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "placeOfServiceCode" ${logic.rename-to} line_place_of_srvc_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "paymentCode" ${logic.rename-to} line_pmt_80_100_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "processingIndicatorCode" ${logic.rename-to} line_prcsg_ind_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} line_sbmtd_chrg_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "serviceDeductibleCode" ${logic.rename-to} line_service_deductible;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "serviceCount" ${logic.rename-to} line_srvc_cnt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "mtusCode" ${logic.rename-to} carr_line_mtus_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "mtusCount" ${logic.rename-to} carr_line_mtus_cnt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "betosCode" ${logic.rename-to} betos_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "anesthesiaUnitCount" ${logic.rename-to} carr_line_ansthsa_unit_cnt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "cliaLabNumber" ${logic.rename-to} carr_line_clia_lab_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "linePricingLocalityCode" ${logic.rename-to} carr_line_prcng_lclty_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerTypeCode" ${logic.rename-to} carr_line_prvdr_type_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "reducedPaymentPhysicianAsstCode" ${logic.rename-to} carr_line_rdcd_pmt_phys_astn_c;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "rxNumber" ${logic.rename-to} carr_line_rx_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "performingProviderIdNumber" ${logic.rename-to} carr_prfrng_pin_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hpsaScarcityCode" ${logic.rename-to} hpsa_scrcty_ind_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "performingPhysicianNpi" ${logic.rename-to} prf_physn_npi;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "performingPhysicianUpin" ${logic.rename-to} prf_physn_upin;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerParticipatingIndCode" ${logic.rename-to} prtcptng_ind_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerSpecialityCode" ${logic.rename-to} prvdr_spclty;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerZipCode" ${logic.rename-to} prvdr_zip;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerTaxNumber" ${logic.rename-to} tax_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} line_prvdr_pmt_amt;
--
-- DMEClaims to dme_claims
--
alter table public."DMEClaims" rename to dme_claims;
alter table public.dme_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.dme_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.dme_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.dme_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.dme_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.dme_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.dme_claims ${logic.alter-rename-column} "claimDispositionCode" ${logic.rename-to} clm_disp_cd;
alter table public.dme_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.dme_claims ${logic.alter-rename-column} "clinicalTrialNumber" ${logic.rename-to} clm_clncl_tril_num;
alter table public.dme_claims ${logic.alter-rename-column} "carrierNumber" ${logic.rename-to} carr_num;
alter table public.dme_claims ${logic.alter-rename-column} "claimCarrierControlNumber" ${logic.rename-to} carr_clm_cntl_num;
alter table public.dme_claims ${logic.alter-rename-column} "claimEntryCode" ${logic.rename-to} carr_clm_entry_cd;
alter table public.dme_claims ${logic.alter-rename-column} "providerAssignmentIndicator" ${logic.rename-to} carr_clm_prvdr_asgnmt_ind_sw;
alter table public.dme_claims ${logic.alter-rename-column} "hcpcsYearCode" ${logic.rename-to} carr_clm_hcpcs_yr_cd;
alter table public.dme_claims ${logic.alter-rename-column} "paymentDenialCode" ${logic.rename-to} carr_clm_pmt_dnl_cd;
alter table public.dme_claims ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} nch_carr_clm_alowd_amt;
alter table public.dme_claims ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} nch_carr_clm_sbmtd_chrg_amt;
alter table public.dme_claims ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} carr_clm_cash_ddctbl_apld_amt;
alter table public.dme_claims ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} nch_clm_bene_pmt_amt;
alter table public.dme_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.dme_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.dme_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.dme_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} carr_clm_prmry_pyr_pd_amt;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.dme_claims ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} nch_clm_prvdr_pmt_amt;
alter table public.dme_claims ${logic.alter-rename-column} "referringPhysicianNpi" ${logic.rename-to} rfr_physn_npi;
alter table public.dme_claims ${logic.alter-rename-column} "referringPhysicianUpin" ${logic.rename-to} rfr_physn_upin;
alter table public.dme_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
--
-- DMEClaimLines to dme_claim_lines
--
alter table public."DMEClaimLines" rename to dme_claim_lines;
alter table public.dme_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.dme_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} line_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} line_nch_pmt_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} line_sbmtd_chrg_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} line_alowd_chrg_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} line_bene_ptb_ddctbl_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} line_bene_pmt_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} line_ndc_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "cmsServiceTypeCode" ${logic.rename-to} line_cms_type_srvc_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "coinsuranceAmount" ${logic.rename-to} line_coinsrnc_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "diagnosisCode" ${logic.rename-to} line_icd_dgns_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "diagnosisCodeVersion" ${logic.rename-to} line_icd_dgns_vrsn_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "firstExpenseDate" ${logic.rename-to} line_1st_expns_dt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hctHgbTestResult" ${logic.rename-to} line_hct_hgb_rslt_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hctHgbTestTypeCode" ${logic.rename-to} line_hct_hgb_type_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "lastExpenseDate" ${logic.rename-to} line_last_expns_dt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "paymentCode" ${logic.rename-to} line_pmt_80_100_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "placeOfServiceCode" ${logic.rename-to} line_place_of_srvc_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "primaryPayerAllowedChargeAmount" ${logic.rename-to} line_prmry_alowd_chrg_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "primaryPayerCode" ${logic.rename-to} line_bene_prmry_pyr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "processingIndicatorCode" ${logic.rename-to} line_prcsg_ind_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "purchasePriceAmount" ${logic.rename-to} line_dme_prchs_price_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "serviceCount" ${logic.rename-to} line_srvc_cnt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "serviceDeductibleCode" ${logic.rename-to} line_service_deductible;
alter table public.dme_claim_lines ${logic.alter-rename-column} "betosCode" ${logic.rename-to} betos_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsFourthModifierCode" ${logic.rename-to} hcpcs_4th_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsThirdModifierCode" ${logic.rename-to} hcpcs_3rd_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "mtusCode" ${logic.rename-to} dmerc_line_mtus_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "mtusCount" ${logic.rename-to} dmerc_line_mtus_cnt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "pricingStateCode" ${logic.rename-to} dmerc_line_prcng_state_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "screenSavingsAmount" ${logic.rename-to} dmerc_line_scrn_svgs_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "supplierTypeCode" ${logic.rename-to} dmerc_line_supplr_type_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} line_bene_prmry_pyr_pd_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerBillingNumber" ${logic.rename-to} prvdr_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerNPI" ${logic.rename-to} prvdr_npi;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerSpecialityCode" ${logic.rename-to} prvdr_spclty;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerTaxNumber" ${logic.rename-to} tax_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerParticipatingIndCode" ${logic.rename-to} prtcptng_ind_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} line_prvdr_pmt_amt;
--
-- HHAClaims to hha_claims
--
alter table public."HHAClaims" rename to hha_claims;
alter table public.hha_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.hha_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.hha_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.hha_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.hha_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.hha_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.hha_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.hha_claims ${logic.alter-rename-column} "careStartDate" ${logic.rename-to} clm_admsn_dt;
alter table public.hha_claims ${logic.alter-rename-column} "claimFacilityTypeCode" ${logic.rename-to} clm_fac_type_cd;
alter table public.hha_claims ${logic.alter-rename-column} "claimFrequencyCode" ${logic.rename-to} clm_freq_cd;
alter table public.hha_claims ${logic.alter-rename-column} "claimLUPACode" ${logic.rename-to} clm_hha_lupa_ind_cd;
alter table public.hha_claims ${logic.alter-rename-column} "claimReferralCode" ${logic.rename-to} clm_hha_rfrl_cd;
alter table public.hha_claims ${logic.alter-rename-column} "totalVisitCount" ${logic.rename-to} clm_hha_tot_visit_cnt;
alter table public.hha_claims ${logic.alter-rename-column} "claimNonPaymentReasonCode" ${logic.rename-to} clm_mdcr_non_pmt_rsn_cd;
alter table public.hha_claims ${logic.alter-rename-column} "prospectivePaymentCode" ${logic.rename-to} clm_pps_ind_cd;
alter table public.hha_claims ${logic.alter-rename-column} "claimServiceClassificationTypeCode" ${logic.rename-to} clm_srvc_clsfctn_type_cd;
alter table public.hha_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimProcessDate" ${logic.rename-to} fi_clm_proc_dt;
alter table public.hha_claims ${logic.alter-rename-column} "fiDocumentClaimControlNumber" ${logic.rename-to} fi_doc_clm_cntl_num;
alter table public.hha_claims ${logic.alter-rename-column} "fiscalIntermediaryNumber" ${logic.rename-to} fi_num;
alter table public.hha_claims ${logic.alter-rename-column} "fiOriginalClaimControlNumber" ${logic.rename-to} fi_orig_clm_cntl_num;
alter table public.hha_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternalFirstCode" ${logic.rename-to} fst_dgns_e_cd;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternalFirstCodeVersion" ${logic.rename-to} fst_dgns_e_vrsn_cd;
alter table public.hha_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.hha_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.hha_claims ${logic.alter-rename-column} "claimPrimaryPayerCode" ${logic.rename-to} nch_prmry_pyr_cd;
alter table public.hha_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} nch_prmry_pyr_clm_pd_amt;
alter table public.hha_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.hha_claims ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.hha_claims ${logic.alter-rename-column} "providerNumber" ${logic.rename-to} prvdr_num;
alter table public.hha_claims ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.hha_claims ${logic.alter-rename-column} "patientDischargeStatusCode" ${logic.rename-to} ptnt_dschrg_stus_cd;
alter table public.hha_claims ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} clm_tot_chrg_amt;
alter table public.hha_claims ${logic.alter-rename-column} "attendingPhysicianNpi" ${logic.rename-to} at_physn_npi;
alter table public.hha_claims ${logic.alter-rename-column} "attendingPhysicianUpin" ${logic.rename-to} at_physn_upin;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis13Code" ${logic.rename-to} icd_dgns_cd13;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis14Code" ${logic.rename-to} icd_dgns_cd14;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis15Code" ${logic.rename-to} icd_dgns_cd15;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis16Code" ${logic.rename-to} icd_dgns_cd16;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis17Code" ${logic.rename-to} icd_dgns_cd17;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis18Code" ${logic.rename-to} icd_dgns_cd18;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis19Code" ${logic.rename-to} icd_dgns_cd19;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis20Code" ${logic.rename-to} icd_dgns_cd20;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis21Code" ${logic.rename-to} icd_dgns_cd21;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis22Code" ${logic.rename-to} icd_dgns_cd22;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis23Code" ${logic.rename-to} icd_dgns_cd23;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis24Code" ${logic.rename-to} icd_dgns_cd24;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis25Code" ${logic.rename-to} icd_dgns_cd25;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal1Code" ${logic.rename-to} icd_dgns_e_cd1;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal2Code" ${logic.rename-to} icd_dgns_e_cd2;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal3Code" ${logic.rename-to} icd_dgns_e_cd3;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal4Code" ${logic.rename-to} icd_dgns_e_cd4;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal5Code" ${logic.rename-to} icd_dgns_e_cd5;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal6Code" ${logic.rename-to} icd_dgns_e_cd6;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal7Code" ${logic.rename-to} icd_dgns_e_cd7;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal8Code" ${logic.rename-to} icd_dgns_e_cd8;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal9Code" ${logic.rename-to} icd_dgns_e_cd9;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal10Code" ${logic.rename-to} icd_dgns_e_cd10;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal11Code" ${logic.rename-to} icd_dgns_e_cd11;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal12Code" ${logic.rename-to} icd_dgns_e_cd12;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal1CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd1;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal2CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd2;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal3CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd3;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal4CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd4;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal5CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd5;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal6CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd6;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal7CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd7;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal8CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd8;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal9CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd9;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal10CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd10;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal11CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd11;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosisExternal12CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd12;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis13CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd13;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis14CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd14;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis15CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd15;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis16CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd16;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis17CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd17;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis18CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd18;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis19CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd19;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis20CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd20;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis21CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd21;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis22CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd22;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis23CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd23;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis24CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd24;
alter table public.hha_claims ${logic.alter-rename-column} "diagnosis25CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd25;
--
-- HHAClaimLines to hha_claim_lines
--
alter table public."HHAClaimLines" rename to hha_claim_lines;
alter table public.hha_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.hha_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} clm_line_num;
alter table public.hha_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianNPI" ${logic.rename-to} rndrng_physn_npi;
alter table public.hha_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianUPIN" ${logic.rename-to} rndrng_physn_upin;
alter table public.hha_claim_lines ${logic.alter-rename-column} "revenueCenterCode" ${logic.rename-to} rev_cntr;
alter table public.hha_claim_lines ${logic.alter-rename-column} "revenueCenterDate" ${logic.rename-to} rev_cntr_dt;
alter table public.hha_claim_lines ${logic.alter-rename-column} "apcOrHippsCode" ${logic.rename-to} rev_cntr_apc_hipps_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "deductibleCoinsuranceCd" ${logic.rename-to} rev_cntr_ddctbl_coinsrnc_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQualifierCode" ${logic.rename-to} rev_cntr_ndc_qty_qlfr_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQuantity" ${logic.rename-to} rev_cntr_ndc_qty;
alter table public.hha_claim_lines ${logic.alter-rename-column} "nonCoveredChargeAmount" ${logic.rename-to} rev_cntr_ncvrd_chrg_amt;
alter table public.hha_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} rev_cntr_pmt_amt_amt;
alter table public.hha_claim_lines ${logic.alter-rename-column} "paymentMethodCode" ${logic.rename-to} rev_cntr_pmt_mthd_ind_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "rateAmount" ${logic.rename-to} rev_cntr_rate_amt;
alter table public.hha_claim_lines ${logic.alter-rename-column} "revCntr1stAnsiCd" ${logic.rename-to} rev_cntr_1st_ansi_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "statusCode" ${logic.rename-to} rev_cntr_stus_ind_cd;
alter table public.hha_claim_lines ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} rev_cntr_tot_chrg_amt;
alter table public.hha_claim_lines ${logic.alter-rename-column} "unitCount" ${logic.rename-to} rev_cntr_unit_cnt;
--
-- HospiceClaims to hospice_claims
--
alter table public."HospiceClaims" rename to hospice_claims;
alter table public.hospice_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.hospice_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.hospice_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.hospice_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.hospice_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.hospice_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.hospice_claims ${logic.alter-rename-column} "claimFacilityTypeCode" ${logic.rename-to} clm_fac_type_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "claimFrequencyCode" ${logic.rename-to} clm_freq_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "claimHospiceStartDate" ${logic.rename-to} clm_hospc_start_dt_id;
alter table public.hospice_claims ${logic.alter-rename-column} "claimNonPaymentReasonCode" ${logic.rename-to} clm_mdcr_non_pmt_rsn_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.hospice_claims ${logic.alter-rename-column} "claimServiceClassificationTypeCode" ${logic.rename-to} clm_srvc_clsfctn_type_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "utilizationDayCount" ${logic.rename-to} clm_utlztn_day_cnt;
alter table public.hospice_claims ${logic.alter-rename-column} "attendingPhysicianNpi" ${logic.rename-to} at_physn_npi;
alter table public.hospice_claims ${logic.alter-rename-column} "attendingPhysicianUpin" ${logic.rename-to} at_physn_upin;
alter table public.hospice_claims ${logic.alter-rename-column} "hospicePeriodCount" ${logic.rename-to} bene_hospc_prd_cnt;
alter table public.hospice_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimProcessDate" ${logic.rename-to} fi_clm_proc_dt;
alter table public.hospice_claims ${logic.alter-rename-column} "fiDocumentClaimControlNumber" ${logic.rename-to} fi_doc_clm_cntl_num;
alter table public.hospice_claims ${logic.alter-rename-column} "fiscalIntermediaryNumber" ${logic.rename-to} fi_num;
alter table public.hospice_claims ${logic.alter-rename-column} "fiOriginalClaimControlNumber" ${logic.rename-to} fi_orig_clm_cntl_num;
alter table public.hospice_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternalFirstCode" ${logic.rename-to} fst_dgns_e_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternalFirstCodeVersion" ${logic.rename-to} fst_dgns_e_vrsn_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "beneficiaryDischargeDate" ${logic.rename-to} nch_bene_dschrg_dt;
alter table public.hospice_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "claimPrimaryPayerCode" ${logic.rename-to} nch_prmry_pyr_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} nch_prmry_pyr_clm_pd_amt;
alter table public.hospice_claims ${logic.alter-rename-column} "patientStatusCd" ${logic.rename-to} nch_ptnt_status_ind_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.hospice_claims ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "providerNumber" ${logic.rename-to} prvdr_num;
alter table public.hospice_claims ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "patientDischargeStatusCode" ${logic.rename-to} ptnt_dschrg_stus_cd;
alter table public.hospice_claims ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} clm_tot_chrg_amt;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis13Code" ${logic.rename-to} icd_dgns_cd13;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis14Code" ${logic.rename-to} icd_dgns_cd14;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis15Code" ${logic.rename-to} icd_dgns_cd15;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis16Code" ${logic.rename-to} icd_dgns_cd16;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis17Code" ${logic.rename-to} icd_dgns_cd17;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis18Code" ${logic.rename-to} icd_dgns_cd18;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis19Code" ${logic.rename-to} icd_dgns_cd19;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis20Code" ${logic.rename-to} icd_dgns_cd20;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis21Code" ${logic.rename-to} icd_dgns_cd21;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis22Code" ${logic.rename-to} icd_dgns_cd22;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis23Code" ${logic.rename-to} icd_dgns_cd23;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis24Code" ${logic.rename-to} icd_dgns_cd24;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis25Code" ${logic.rename-to} icd_dgns_cd25;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal1Code" ${logic.rename-to} icd_dgns_e_cd1;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal2Code" ${logic.rename-to} icd_dgns_e_cd2;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal3Code" ${logic.rename-to} icd_dgns_e_cd3;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal4Code" ${logic.rename-to} icd_dgns_e_cd4;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal5Code" ${logic.rename-to} icd_dgns_e_cd5;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal6Code" ${logic.rename-to} icd_dgns_e_cd6;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal7Code" ${logic.rename-to} icd_dgns_e_cd7;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal8Code" ${logic.rename-to} icd_dgns_e_cd8;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal9Code" ${logic.rename-to} icd_dgns_e_cd9;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal10Code" ${logic.rename-to} icd_dgns_e_cd10;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal11Code" ${logic.rename-to} icd_dgns_e_cd11;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal12Code" ${logic.rename-to} icd_dgns_e_cd12;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal1CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd1;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal2CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd2;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal3CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd3;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal4CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd4;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal5CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd5;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal6CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd6;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal7CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd7;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal8CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd8;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal9CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd9;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal10CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd10;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal11CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd11;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosisExternal12CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd12;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis13CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd13;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis14CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd14;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis15CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd15;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis16CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd16;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis17CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd17;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis18CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd18;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis19CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd19;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis20CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd20;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis21CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd21;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis22CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd22;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis23CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd23;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis24CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd24;
alter table public.hospice_claims ${logic.alter-rename-column} "diagnosis25CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd25;
--
-- HospiceClaimLines to hospice_claim_lines
--
alter table public."HospiceClaimLines" rename to hospice_claim_lines;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} clm_line_num;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "revenueCenterCode" ${logic.rename-to} rev_cntr;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} rev_cntr_pmt_amt_amt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "revenueCenterDate" ${logic.rename-to} rev_cntr_dt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} rev_cntr_tot_chrg_amt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "unitCount" ${logic.rename-to} rev_cntr_unit_cnt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "benficiaryPaymentAmount" ${logic.rename-to} rev_cntr_bene_pmt_amt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "deductibleCoinsuranceCd" ${logic.rename-to} rev_cntr_ddctbl_coinsrnc_cd;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQualifierCode" ${logic.rename-to} rev_cntr_ndc_qty_qlfr_cd;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQuantity" ${logic.rename-to} rev_cntr_ndc_qty;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "nonCoveredChargeAmount" ${logic.rename-to} rev_cntr_ncvrd_chrg_amt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} rev_cntr_prvdr_pmt_amt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "rateAmount" ${logic.rename-to} rev_cntr_rate_amt;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianNPI" ${logic.rename-to} rndrng_physn_npi;
alter table public.hospice_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianUPIN" ${logic.rename-to} rndrng_physn_upin;
--
-- InpatientClaims to inpatient_claims
--
alter table public."InpatientClaims" rename to inpatient_claims;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.inpatient_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.inpatient_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.inpatient_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimAdmissionDate" ${logic.rename-to} clm_admsn_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisRelatedGroupCd" ${logic.rename-to} clm_drg_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisRelatedGroupOutlierStayCd" ${logic.rename-to} clm_drg_outlier_stay_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimFacilityTypeCode" ${logic.rename-to} clm_fac_type_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimFrequencyCode" ${logic.rename-to} clm_freq_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "admissionTypeCd" ${logic.rename-to} clm_ip_admsn_type_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "mcoPaidSw" ${logic.rename-to} clm_mco_pd_sw;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimNonPaymentReasonCode" ${logic.rename-to} clm_mdcr_non_pmt_rsn_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "nonUtilizationDayCount" ${logic.rename-to} clm_non_utlztn_days_cnt;
alter table public.inpatient_claims ${logic.alter-rename-column} "passThruPerDiemAmount" ${logic.rename-to} clm_pass_thru_per_diem_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSCapitalDrgWeightNumber" ${logic.rename-to} clm_pps_cptl_drg_wt_num;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSCapitalDisproportionateShareAmt" ${logic.rename-to} clm_pps_cptl_dsprprtnt_shr_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSCapitalExceptionAmount" ${logic.rename-to} clm_pps_cptl_excptn_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSCapitalFSPAmount" ${logic.rename-to} clm_pps_cptl_fsp_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSCapitalIMEAmount" ${logic.rename-to} clm_pps_cptl_ime_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSCapitalOutlierAmount" ${logic.rename-to} clm_pps_cptl_outlier_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "prospectivePaymentCode" ${logic.rename-to} clm_pps_ind_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPPSOldCapitalHoldHarmlessAmount" ${logic.rename-to} clm_pps_old_cptl_hld_hrmls_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "sourceAdmissionCd" ${logic.rename-to} clm_src_ip_admsn_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimServiceClassificationTypeCode" ${logic.rename-to} clm_srvc_clsfctn_type_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimTotalPPSCapitalAmount" ${logic.rename-to} clm_tot_pps_cptl_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimUncompensatedCareAmount" ${logic.rename-to} clm_uncompd_care_pmt_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "utilizationDayCount" ${logic.rename-to} clm_utlztn_day_cnt;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisAdmittingCode" ${logic.rename-to} admtg_dgns_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisAdmittingCodeVersion" ${logic.rename-to} admtg_dgns_vrsn_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "attendingPhysicianNpi" ${logic.rename-to} at_physn_npi;
alter table public.inpatient_claims ${logic.alter-rename-column} "attendingPhysicianUpin" ${logic.rename-to} at_physn_upin;
alter table public.inpatient_claims ${logic.alter-rename-column} "lifetimeReservedDaysUsedCount" ${logic.rename-to} bene_lrd_used_cnt;
alter table public.inpatient_claims ${logic.alter-rename-column} "coinsuranceDayCount" ${logic.rename-to} bene_tot_coinsrnc_days_cnt;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimQueryCode" ${logic.rename-to} claim_query_code;
alter table public.inpatient_claims ${logic.alter-rename-column} "disproportionateShareAmount" ${logic.rename-to} dsh_op_clm_val_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimActionCode" ${logic.rename-to} fi_clm_actn_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimProcessDate" ${logic.rename-to} fi_clm_proc_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "fiDocumentClaimControlNumber" ${logic.rename-to} fi_doc_clm_cntl_num;
alter table public.inpatient_claims ${logic.alter-rename-column} "fiscalIntermediaryNumber" ${logic.rename-to} fi_num;
alter table public.inpatient_claims ${logic.alter-rename-column} "fiOriginalClaimControlNumber" ${logic.rename-to} fi_orig_clm_cntl_num;
alter table public.inpatient_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternalFirstCode" ${logic.rename-to} fst_dgns_e_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternalFirstCodeVersion" ${logic.rename-to} fst_dgns_e_vrsn_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "indirectMedicalEducationAmount" ${logic.rename-to} ime_op_clm_val_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "coveredCareThoughDate" ${logic.rename-to} nch_actv_or_cvrd_lvl_care_thru;
alter table public.inpatient_claims ${logic.alter-rename-column} "bloodDeductibleLiabilityAmount" ${logic.rename-to} nch_bene_blood_ddctbl_lblty_am;
alter table public.inpatient_claims ${logic.alter-rename-column} "beneficiaryDischargeDate" ${logic.rename-to} nch_bene_dschrg_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "deductibleAmount" ${logic.rename-to} nch_bene_ip_ddctbl_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "medicareBenefitsExhaustedDate" ${logic.rename-to} nch_bene_mdcr_bnfts_exhtd_dt_i;
alter table public.inpatient_claims ${logic.alter-rename-column} "partACoinsuranceLiabilityAmount" ${logic.rename-to} nch_bene_pta_coinsrnc_lblty_am;
alter table public.inpatient_claims ${logic.alter-rename-column} "bloodPintsFurnishedQty" ${logic.rename-to} nch_blood_pnts_frnshd_qty;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "drgOutlierApprovedPaymentAmount" ${logic.rename-to} nch_drg_outlier_aprvd_pmt_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "noncoveredCharge" ${logic.rename-to} nch_ip_ncvrd_chrg_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "totalDeductionAmount" ${logic.rename-to} nch_ip_tot_ddctn_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "claimPrimaryPayerCode" ${logic.rename-to} nch_prmry_pyr_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} nch_prmry_pyr_clm_pd_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "professionalComponentCharge" ${logic.rename-to} nch_profnl_cmpnt_chrg_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "patientStatusCd" ${logic.rename-to} nch_ptnt_status_ind_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "noncoveredStayFromDate" ${logic.rename-to} nch_vrfd_ncvrd_stay_from_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "noncoveredStayThroughDate" ${logic.rename-to} nch_vrfd_ncvrd_stay_thru_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.inpatient_claims ${logic.alter-rename-column} "operatingPhysicianNpi" ${logic.rename-to} op_physn_npi;
alter table public.inpatient_claims ${logic.alter-rename-column} "operatingPhysicianUpin" ${logic.rename-to} op_physn_upin;
alter table public.inpatient_claims ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.inpatient_claims ${logic.alter-rename-column} "otherPhysicianNpi" ${logic.rename-to} ot_physn_npi;
alter table public.inpatient_claims ${logic.alter-rename-column} "otherPhysicianUpin" ${logic.rename-to} ot_physn_upin;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "providerNumber" ${logic.rename-to} prvdr_num;
alter table public.inpatient_claims ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "patientDischargeStatusCode" ${logic.rename-to} ptnt_dschrg_stus_cd;
alter table public.inpatient_claims ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} clm_tot_chrg_amt;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal1PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw1;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal2PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw2;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal3PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw3;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal4PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw4;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal5PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw5;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal6PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw6;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal7PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw7;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal8PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw8;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal9PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw9;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal10PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw10;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal11PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw11;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal12PresentOnAdmissionCode" ${logic.rename-to} clm_e_poa_ind_sw12;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis1PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw1;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis2PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw2;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis3PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw3;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis4PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw4;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis5PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw5;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis6PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw6;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis7PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw7;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis8PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw8;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis9PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw9;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis10PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw10;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis11PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw11;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis12PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw12;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis13PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw13;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis14PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw14;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis15PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw15;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis16PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw16;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis17PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw17;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis18PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw18;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis19PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw19;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis20PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw20;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis21PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw21;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis22PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw22;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis23PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw23;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis24PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw24;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis25PresentOnAdmissionCode" ${logic.rename-to} clm_poa_ind_sw25;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis13Code" ${logic.rename-to} icd_dgns_cd13;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis14Code" ${logic.rename-to} icd_dgns_cd14;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis15Code" ${logic.rename-to} icd_dgns_cd15;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis16Code" ${logic.rename-to} icd_dgns_cd16;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis17Code" ${logic.rename-to} icd_dgns_cd17;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis18Code" ${logic.rename-to} icd_dgns_cd18;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis19Code" ${logic.rename-to} icd_dgns_cd19;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis20Code" ${logic.rename-to} icd_dgns_cd20;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis21Code" ${logic.rename-to} icd_dgns_cd21;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis22Code" ${logic.rename-to} icd_dgns_cd22;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis23Code" ${logic.rename-to} icd_dgns_cd23;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis24Code" ${logic.rename-to} icd_dgns_cd24;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis25Code" ${logic.rename-to} icd_dgns_cd25;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal1Code" ${logic.rename-to} icd_dgns_e_cd1;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal2Code" ${logic.rename-to} icd_dgns_e_cd2;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal3Code" ${logic.rename-to} icd_dgns_e_cd3;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal4Code" ${logic.rename-to} icd_dgns_e_cd4;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal5Code" ${logic.rename-to} icd_dgns_e_cd5;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal6Code" ${logic.rename-to} icd_dgns_e_cd6;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal7Code" ${logic.rename-to} icd_dgns_e_cd7;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal8Code" ${logic.rename-to} icd_dgns_e_cd8;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal9Code" ${logic.rename-to} icd_dgns_e_cd9;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal10Code" ${logic.rename-to} icd_dgns_e_cd10;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal11Code" ${logic.rename-to} icd_dgns_e_cd11;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal12Code" ${logic.rename-to} icd_dgns_e_cd12;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal1CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd1;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal2CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd2;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal3CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd3;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal4CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd4;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal5CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd5;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal6CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd6;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal7CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd7;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal8CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd8;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal9CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd9;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal10CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd10;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal11CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd11;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosisExternal12CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd12;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis13CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd13;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis14CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd14;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis15CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd15;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis16CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd16;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis17CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd17;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis18CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd18;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis19CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd19;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis20CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd20;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis21CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd21;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis22CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd22;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis23CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd23;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis24CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd24;
alter table public.inpatient_claims ${logic.alter-rename-column} "diagnosis25CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd25;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure1Code" ${logic.rename-to} icd_prcdr_cd1;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure2Code" ${logic.rename-to} icd_prcdr_cd2;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure3Code" ${logic.rename-to} icd_prcdr_cd3;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure4Code" ${logic.rename-to} icd_prcdr_cd4;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure5Code" ${logic.rename-to} icd_prcdr_cd5;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure6Code" ${logic.rename-to} icd_prcdr_cd6;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure7Code" ${logic.rename-to} icd_prcdr_cd7;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure8Code" ${logic.rename-to} icd_prcdr_cd8;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure9Code" ${logic.rename-to} icd_prcdr_cd9;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure10Code" ${logic.rename-to} icd_prcdr_cd10;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure11Code" ${logic.rename-to} icd_prcdr_cd11;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure12Code" ${logic.rename-to} icd_prcdr_cd12;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure13Code" ${logic.rename-to} icd_prcdr_cd13;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure14Code" ${logic.rename-to} icd_prcdr_cd14;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure15Code" ${logic.rename-to} icd_prcdr_cd15;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure16Code" ${logic.rename-to} icd_prcdr_cd16;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure17Code" ${logic.rename-to} icd_prcdr_cd17;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure18Code" ${logic.rename-to} icd_prcdr_cd18;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure19Code" ${logic.rename-to} icd_prcdr_cd19;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure20Code" ${logic.rename-to} icd_prcdr_cd20;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure21Code" ${logic.rename-to} icd_prcdr_cd21;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure22Code" ${logic.rename-to} icd_prcdr_cd22;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure23Code" ${logic.rename-to} icd_prcdr_cd23;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure24Code" ${logic.rename-to} icd_prcdr_cd24;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure25Code" ${logic.rename-to} icd_prcdr_cd25;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure1CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd1;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure2CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd2;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure3CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd3;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure4CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd4;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure5CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd5;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure6CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd6;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure7CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd7;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure8CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd8;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure9CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd9;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure10CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd10;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure11CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd11;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure12CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd12;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure13CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd13;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure14CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd14;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure15CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd15;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure16CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd16;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure17CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd17;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure18CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd18;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure19CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd19;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure20CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd20;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure21CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd21;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure22CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd22;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure23CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd23;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure24CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd24;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure25CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd25;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure1Date" ${logic.rename-to} prcdr_dt1;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure2Date" ${logic.rename-to} prcdr_dt2;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure3Date" ${logic.rename-to} prcdr_dt3;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure4Date" ${logic.rename-to} prcdr_dt4;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure5Date" ${logic.rename-to} prcdr_dt5;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure6Date" ${logic.rename-to} prcdr_dt6;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure7Date" ${logic.rename-to} prcdr_dt7;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure8Date" ${logic.rename-to} prcdr_dt8;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure9Date" ${logic.rename-to} prcdr_dt9;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure10Date" ${logic.rename-to} prcdr_dt10;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure11Date" ${logic.rename-to} prcdr_dt11;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure12Date" ${logic.rename-to} prcdr_dt12;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure13Date" ${logic.rename-to} prcdr_dt13;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure14Date" ${logic.rename-to} prcdr_dt14;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure15Date" ${logic.rename-to} prcdr_dt15;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure16Date" ${logic.rename-to} prcdr_dt16;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure17Date" ${logic.rename-to} prcdr_dt17;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure18Date" ${logic.rename-to} prcdr_dt18;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure19Date" ${logic.rename-to} prcdr_dt19;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure20Date" ${logic.rename-to} prcdr_dt20;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure21Date" ${logic.rename-to} prcdr_dt21;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure22Date" ${logic.rename-to} prcdr_dt22;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure23Date" ${logic.rename-to} prcdr_dt23;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure24Date" ${logic.rename-to} prcdr_dt24;
alter table public.inpatient_claims ${logic.alter-rename-column} "procedure25Date" ${logic.rename-to} prcdr_dt25;
--
-- InpatientClaimLines to inpatient_claim_lines
--
alter table public."InpatientClaimLines" rename to inpatient_claim_lines;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} clm_line_num;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "deductibleCoinsuranceCd" ${logic.rename-to} rev_cntr_ddctbl_coinsrnc_cd;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQualifierCode" ${logic.rename-to} rev_cntr_ndc_qty_qlfr_cd;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQuantity" ${logic.rename-to} rev_cntr_ndc_qty;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "nonCoveredChargeAmount" ${logic.rename-to} rev_cntr_ncvrd_chrg_amt;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "rateAmount" ${logic.rename-to} rev_cntr_rate_amt;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "revenueCenter" ${logic.rename-to} rev_cntr;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} rev_cntr_tot_chrg_amt;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "unitCount" ${logic.rename-to} rev_cntr_unit_cnt;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianNPI" ${logic.rename-to} rndrng_physn_npi;
alter table public.inpatient_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianUPIN" ${logic.rename-to} rndrng_physn_upin;
--
-- OutpatientClaims to outpatient_claims
--
alter table public."OutpatientClaims" rename to outpatient_claims;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.outpatient_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.outpatient_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.outpatient_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.outpatient_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimFacilityTypeCode" ${logic.rename-to} clm_fac_type_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimFrequencyCode" ${logic.rename-to} clm_freq_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "mcoPaidSw" ${logic.rename-to} clm_mco_pd_sw;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimNonPaymentReasonCode" ${logic.rename-to} clm_mdcr_non_pmt_rsn_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimServiceClassificationTypeCode" ${logic.rename-to} clm_srvc_clsfctn_type_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "attendingPhysicianNpi" ${logic.rename-to} at_physn_npi;
alter table public.outpatient_claims ${logic.alter-rename-column} "attendingPhysicianUpin" ${logic.rename-to} at_physn_upin;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimQueryCode" ${logic.rename-to} claim_query_code;
alter table public.outpatient_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimProcessDate" ${logic.rename-to} fi_clm_proc_dt;
alter table public.outpatient_claims ${logic.alter-rename-column} "fiDocumentClaimControlNumber" ${logic.rename-to} fi_doc_clm_cntl_num;
alter table public.outpatient_claims ${logic.alter-rename-column} "fiscalIntermediaryNumber" ${logic.rename-to} fi_num;
alter table public.outpatient_claims ${logic.alter-rename-column} "fiOriginalClaimControlNumber" ${logic.rename-to} fi_orig_clm_cntl_num;
alter table public.outpatient_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternalFirstCode" ${logic.rename-to} fst_dgns_e_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternalFirstCodeVersion" ${logic.rename-to} fst_dgns_e_vrsn_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} clm_op_bene_pmt_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "coinsuranceAmount" ${logic.rename-to} nch_bene_ptb_coinsrnc_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "bloodDeductibleLiabilityAmount" ${logic.rename-to} nch_bene_blood_ddctbl_lblty_am;
alter table public.outpatient_claims ${logic.alter-rename-column} "deductibleAmount" ${logic.rename-to} nch_bene_ptb_ddctbl_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "claimPrimaryPayerCode" ${logic.rename-to} nch_prmry_pyr_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} nch_prmry_pyr_clm_pd_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "professionalComponentCharge" ${logic.rename-to} nch_profnl_cmpnt_chrg_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.outpatient_claims ${logic.alter-rename-column} "operatingPhysicianNpi" ${logic.rename-to} op_physn_npi;
alter table public.outpatient_claims ${logic.alter-rename-column} "operatingPhysicianUpin" ${logic.rename-to} op_physn_upin;
alter table public.outpatient_claims ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.outpatient_claims ${logic.alter-rename-column} "otherPhysicianNpi" ${logic.rename-to} ot_physn_npi;
alter table public.outpatient_claims ${logic.alter-rename-column} "otherPhysicianUpin" ${logic.rename-to} ot_physn_upin;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "providerNumber" ${logic.rename-to} prvdr_num;
alter table public.outpatient_claims ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "patientDischargeStatusCode" ${logic.rename-to} ptnt_dschrg_stus_cd;
alter table public.outpatient_claims ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} clm_op_prvdr_pmt_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} clm_tot_chrg_amt;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisAdmission1Code" ${logic.rename-to} rsn_visit_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisAdmission1CodeVersion" ${logic.rename-to} rsn_visit_vrsn_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisAdmission2Code" ${logic.rename-to} rsn_visit_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisAdmission2CodeVersion" ${logic.rename-to} rsn_visit_vrsn_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisAdmission3Code" ${logic.rename-to} rsn_visit_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisAdmission3CodeVersion" ${logic.rename-to} rsn_visit_vrsn_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis13Code" ${logic.rename-to} icd_dgns_cd13;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis14Code" ${logic.rename-to} icd_dgns_cd14;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis15Code" ${logic.rename-to} icd_dgns_cd15;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis16Code" ${logic.rename-to} icd_dgns_cd16;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis17Code" ${logic.rename-to} icd_dgns_cd17;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis18Code" ${logic.rename-to} icd_dgns_cd18;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis19Code" ${logic.rename-to} icd_dgns_cd19;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis20Code" ${logic.rename-to} icd_dgns_cd20;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis21Code" ${logic.rename-to} icd_dgns_cd21;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis22Code" ${logic.rename-to} icd_dgns_cd22;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis23Code" ${logic.rename-to} icd_dgns_cd23;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis24Code" ${logic.rename-to} icd_dgns_cd24;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis25Code" ${logic.rename-to} icd_dgns_cd25;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal1Code" ${logic.rename-to} icd_dgns_e_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal2Code" ${logic.rename-to} icd_dgns_e_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal3Code" ${logic.rename-to} icd_dgns_e_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal4Code" ${logic.rename-to} icd_dgns_e_cd4;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal5Code" ${logic.rename-to} icd_dgns_e_cd5;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal6Code" ${logic.rename-to} icd_dgns_e_cd6;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal7Code" ${logic.rename-to} icd_dgns_e_cd7;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal8Code" ${logic.rename-to} icd_dgns_e_cd8;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal9Code" ${logic.rename-to} icd_dgns_e_cd9;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal10Code" ${logic.rename-to} icd_dgns_e_cd10;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal11Code" ${logic.rename-to} icd_dgns_e_cd11;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal12Code" ${logic.rename-to} icd_dgns_e_cd12;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal1CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal2CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal3CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal4CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd4;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal5CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd5;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal6CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd6;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal7CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd7;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal8CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd8;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal9CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd9;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal10CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd10;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal11CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd11;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosisExternal12CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd12;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis13CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd13;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis14CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd14;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis15CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd15;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis16CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd16;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis17CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd17;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis18CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd18;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis19CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd19;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis20CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd20;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis21CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd21;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis22CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd22;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis23CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd23;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis24CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd24;
alter table public.outpatient_claims ${logic.alter-rename-column} "diagnosis25CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd25;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure1Code" ${logic.rename-to} icd_prcdr_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure2Code" ${logic.rename-to} icd_prcdr_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure3Code" ${logic.rename-to} icd_prcdr_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure4Code" ${logic.rename-to} icd_prcdr_cd4;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure5Code" ${logic.rename-to} icd_prcdr_cd5;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure6Code" ${logic.rename-to} icd_prcdr_cd6;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure7Code" ${logic.rename-to} icd_prcdr_cd7;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure8Code" ${logic.rename-to} icd_prcdr_cd8;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure9Code" ${logic.rename-to} icd_prcdr_cd9;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure10Code" ${logic.rename-to} icd_prcdr_cd10;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure11Code" ${logic.rename-to} icd_prcdr_cd11;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure12Code" ${logic.rename-to} icd_prcdr_cd12;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure13Code" ${logic.rename-to} icd_prcdr_cd13;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure14Code" ${logic.rename-to} icd_prcdr_cd14;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure15Code" ${logic.rename-to} icd_prcdr_cd15;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure16Code" ${logic.rename-to} icd_prcdr_cd16;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure17Code" ${logic.rename-to} icd_prcdr_cd17;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure18Code" ${logic.rename-to} icd_prcdr_cd18;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure19Code" ${logic.rename-to} icd_prcdr_cd19;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure20Code" ${logic.rename-to} icd_prcdr_cd20;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure21Code" ${logic.rename-to} icd_prcdr_cd21;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure22Code" ${logic.rename-to} icd_prcdr_cd22;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure23Code" ${logic.rename-to} icd_prcdr_cd23;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure24Code" ${logic.rename-to} icd_prcdr_cd24;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure25Code" ${logic.rename-to} icd_prcdr_cd25;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure1CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd1;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure2CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd2;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure3CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd3;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure4CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd4;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure5CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd5;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure6CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd6;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure7CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd7;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure8CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd8;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure9CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd9;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure10CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd10;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure11CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd11;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure12CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd12;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure13CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd13;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure14CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd14;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure15CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd15;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure16CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd16;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure17CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd17;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure18CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd18;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure19CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd19;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure20CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd20;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure21CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd21;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure22CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd22;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure23CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd23;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure24CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd24;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure25CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd25;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure1Date" ${logic.rename-to} prcdr_dt1;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure2Date" ${logic.rename-to} prcdr_dt2;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure3Date" ${logic.rename-to} prcdr_dt3;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure4Date" ${logic.rename-to} prcdr_dt4;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure5Date" ${logic.rename-to} prcdr_dt5;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure6Date" ${logic.rename-to} prcdr_dt6;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure7Date" ${logic.rename-to} prcdr_dt7;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure8Date" ${logic.rename-to} prcdr_dt8;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure9Date" ${logic.rename-to} prcdr_dt9;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure10Date" ${logic.rename-to} prcdr_dt10;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure11Date" ${logic.rename-to} prcdr_dt11;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure12Date" ${logic.rename-to} prcdr_dt12;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure13Date" ${logic.rename-to} prcdr_dt13;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure14Date" ${logic.rename-to} prcdr_dt14;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure15Date" ${logic.rename-to} prcdr_dt15;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure16Date" ${logic.rename-to} prcdr_dt16;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure17Date" ${logic.rename-to} prcdr_dt17;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure18Date" ${logic.rename-to} prcdr_dt18;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure19Date" ${logic.rename-to} prcdr_dt19;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure20Date" ${logic.rename-to} prcdr_dt20;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure21Date" ${logic.rename-to} prcdr_dt21;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure22Date" ${logic.rename-to} prcdr_dt22;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure23Date" ${logic.rename-to} prcdr_dt23;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure24Date" ${logic.rename-to} prcdr_dt24;
alter table public.outpatient_claims ${logic.alter-rename-column} "procedure25Date" ${logic.rename-to} prcdr_dt25;
--
-- OutpatientClaimLines to outpatient_claim_lines
--
alter table public."OutpatientClaimLines" rename to outpatient_claim_lines;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} clm_line_num;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} rev_cntr_ide_ndc_upc_num;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianNPI" ${logic.rename-to} rndrng_physn_npi;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianUPIN" ${logic.rename-to} rndrng_physn_upin;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revenueCenterCode" ${logic.rename-to} rev_cntr;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revenueCenterDate" ${logic.rename-to} rev_cntr_dt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} rev_cntr_pmt_amt_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "apcOrHippsCode" ${logic.rename-to} rev_cntr_apc_hipps_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "benficiaryPaymentAmount" ${logic.rename-to} rev_cntr_bene_pmt_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "bloodDeductibleAmount" ${logic.rename-to} rev_cntr_blood_ddctbl_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "cashDeductibleAmount" ${logic.rename-to} rev_cntr_cash_ddctbl_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "discountCode" ${logic.rename-to} rev_cntr_dscnt_ind_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "firstMspPaidAmount" ${logic.rename-to} rev_cntr_1st_msp_pd_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQualifierCode" ${logic.rename-to} rev_cntr_ndc_qty_qlfr_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQuantity" ${logic.rename-to} rev_cntr_ndc_qty;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "nonCoveredChargeAmount" ${logic.rename-to} rev_cntr_ncvrd_chrg_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "obligationToAcceptAsFullPaymentCode" ${logic.rename-to} rev_cntr_otaf_pmt_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "packagingCode" ${logic.rename-to} rev_cntr_packg_ind_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "patientResponsibilityAmount" ${logic.rename-to} rev_cntr_ptnt_rspnsblty_pmt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "paymentMethodCode" ${logic.rename-to} rev_cntr_pmt_mthd_ind_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} rev_cntr_prvdr_pmt_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "rateAmount" ${logic.rename-to} rev_cntr_rate_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "reducedCoinsuranceAmount" ${logic.rename-to} rev_cntr_rdcd_coinsrnc_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revCntr1stAnsiCd" ${logic.rename-to} rev_cntr_1st_ansi_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revCntr2ndAnsiCd" ${logic.rename-to} rev_cntr_2nd_ansi_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revCntr3rdAnsiCd" ${logic.rename-to} rev_cntr_3rd_ansi_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "revCntr4thAnsiCd" ${logic.rename-to} rev_cntr_4th_ansi_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "secondMspPaidAmount" ${logic.rename-to} rev_cntr_2nd_msp_pd_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "statusCode" ${logic.rename-to} rev_cntr_stus_ind_cd;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} rev_cntr_tot_chrg_amt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "unitCount" ${logic.rename-to} rev_cntr_unit_cnt;
alter table public.outpatient_claim_lines ${logic.alter-rename-column} "wageAdjustedCoinsuranceAmount" ${logic.rename-to} rev_cntr_coinsrnc_wge_adjstd_c;
--
-- PartDEvents to partd_events
--
alter table public."PartDEvents" rename to partd_events;
alter table public.partd_events ${logic.alter-rename-column} "eventId" ${logic.rename-to} pde_id;
alter table public.partd_events ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.partd_events ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.partd_events ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.partd_events ${logic.alter-rename-column} "adjustmentDeletionCode" ${logic.rename-to} adjstmt_dltn_cd;
alter table public.partd_events ${logic.alter-rename-column} "brandGenericCode" ${logic.rename-to} brnd_gnrc_cd;
alter table public.partd_events ${logic.alter-rename-column} "compoundCode" ${logic.rename-to} cmpnd_cd;
alter table public.partd_events ${logic.alter-rename-column} "catastrophicCoverageCode" ${logic.rename-to} ctstrphc_cvrg_cd;
alter table public.partd_events ${logic.alter-rename-column} "partDPlanCoveredPaidAmount" ${logic.rename-to} cvrd_d_plan_pd_amt;
alter table public.partd_events ${logic.alter-rename-column} "dispenseAsWrittenProductSelectionCode" ${logic.rename-to} daw_prod_slctn_cd;
alter table public.partd_events ${logic.alter-rename-column} "daysSupply" ${logic.rename-to} days_suply_num;
alter table public.partd_events ${logic.alter-rename-column} "drugCoverageStatusCode" ${logic.rename-to} drug_cvrg_stus_cd;
alter table public.partd_events ${logic.alter-rename-column} "dispensingStatusCode" ${logic.rename-to} dspnsng_stus_cd;
alter table public.partd_events ${logic.alter-rename-column} "fillNumber" ${logic.rename-to} fill_num;
alter table public.partd_events ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.partd_events ${logic.alter-rename-column} "grossCostAboveOutOfPocketThreshold" ${logic.rename-to} gdc_abv_oopt_amt;
alter table public.partd_events ${logic.alter-rename-column} "grossCostBelowOutOfPocketThreshold" ${logic.rename-to} gdc_blw_oopt_amt;
alter table public.partd_events ${logic.alter-rename-column} "lowIncomeSubsidyPaidAmount" ${logic.rename-to} lics_amt;
alter table public.partd_events ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} prod_srvc_id;
alter table public.partd_events ${logic.alter-rename-column} "partDPlanNonCoveredPaidAmount" ${logic.rename-to} ncvrd_plan_pd_amt;
alter table public.partd_events ${logic.alter-rename-column} "nonstandardFormatCode" ${logic.rename-to} nstd_frmt_cd;
alter table public.partd_events ${logic.alter-rename-column} "otherTrueOutOfPocketPaidAmount" ${logic.rename-to} othr_troop_amt;
alter table public.partd_events ${logic.alter-rename-column} "paymentDate" ${logic.rename-to} pd_dt;
alter table public.partd_events ${logic.alter-rename-column} "pharmacyTypeCode" ${logic.rename-to} phrmcy_srvc_type_cd;
alter table public.partd_events ${logic.alter-rename-column} "planContractId" ${logic.rename-to} plan_cntrct_rec_id;
alter table public.partd_events ${logic.alter-rename-column} "planBenefitPackageId" ${logic.rename-to} plan_pbp_rec_num;
alter table public.partd_events ${logic.alter-rename-column} "patientLiabilityReductionOtherPaidAmount" ${logic.rename-to} plro_amt;
alter table public.partd_events ${logic.alter-rename-column} "pricingExceptionCode" ${logic.rename-to} prcng_excptn_cd;
alter table public.partd_events ${logic.alter-rename-column} "prescriberId" ${logic.rename-to} prscrbr_id;
alter table public.partd_events ${logic.alter-rename-column} "prescriberIdQualifierCode" ${logic.rename-to} prscrbr_id_qlfyr_cd;
alter table public.partd_events ${logic.alter-rename-column} "patientPaidAmount" ${logic.rename-to} ptnt_pay_amt;
alter table public.partd_events ${logic.alter-rename-column} "patientResidenceCode" ${logic.rename-to} ptnt_rsdnc_cd;
alter table public.partd_events ${logic.alter-rename-column} "quantityDispensed" ${logic.rename-to} qty_dspnsd_num;
alter table public.partd_events ${logic.alter-rename-column} "gapDiscountAmount" ${logic.rename-to} rptd_gap_dscnt_num;
alter table public.partd_events ${logic.alter-rename-column} "prescriptionOriginationCode" ${logic.rename-to} rx_orgn_cd;
alter table public.partd_events ${logic.alter-rename-column} "prescriptionReferenceNumber" ${logic.rename-to} rx_srvc_rfrnc_num;
alter table public.partd_events ${logic.alter-rename-column} "prescriptionFillDate" ${logic.rename-to} srvc_dt;
alter table public.partd_events ${logic.alter-rename-column} "serviceProviderId" ${logic.rename-to} srvc_prvdr_id;
alter table public.partd_events ${logic.alter-rename-column} "serviceProviderIdQualiferCode" ${logic.rename-to} srvc_prvdr_id_qlfyr_cd;
alter table public.partd_events ${logic.alter-rename-column} "submissionClarificationCode" ${logic.rename-to} submsn_clr_cd;
alter table public.partd_events ${logic.alter-rename-column} "totalPrescriptionCost" ${logic.rename-to} tot_rx_cst_amt;
--
-- SNFClaims to snf_claims
--
alter table public."SNFClaims" rename to snf_claims;
alter table public.snf_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.snf_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.snf_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.snf_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.snf_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.snf_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.snf_claims ${logic.alter-rename-column} "claimAdmissionDate" ${logic.rename-to} clm_admsn_dt;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisRelatedGroupCd" ${logic.rename-to} clm_drg_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimFacilityTypeCode" ${logic.rename-to} clm_fac_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimFrequencyCode" ${logic.rename-to} clm_freq_cd;
alter table public.snf_claims ${logic.alter-rename-column} "admissionTypeCd" ${logic.rename-to} clm_ip_admsn_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "mcoPaidSw" ${logic.rename-to} clm_mco_pd_sw;
alter table public.snf_claims ${logic.alter-rename-column} "claimNonPaymentReasonCode" ${logic.rename-to} clm_mdcr_non_pmt_rsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "nonUtilizationDayCount" ${logic.rename-to} clm_non_utlztn_days_cnt;
alter table public.snf_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalDisproportionateShareAmt" ${logic.rename-to} clm_pps_cptl_dsprprtnt_shr_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalExceptionAmount" ${logic.rename-to} clm_pps_cptl_excptn_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalFSPAmount" ${logic.rename-to} clm_pps_cptl_fsp_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalIMEAmount" ${logic.rename-to} clm_pps_cptl_ime_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalOutlierAmount" ${logic.rename-to} clm_pps_cptl_outlier_amt;
alter table public.snf_claims ${logic.alter-rename-column} "prospectivePaymentCode" ${logic.rename-to} clm_pps_ind_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSOldCapitalHoldHarmlessAmount" ${logic.rename-to} clm_pps_old_cptl_hld_hrmls_amt;
alter table public.snf_claims ${logic.alter-rename-column} "sourceAdmissionCd" ${logic.rename-to} clm_src_ip_admsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimServiceClassificationTypeCode" ${logic.rename-to} clm_srvc_clsfctn_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "utilizationDayCount" ${logic.rename-to} clm_utlztn_day_cnt;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisAdmittingCode" ${logic.rename-to} admtg_dgns_cd;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisAdmittingCodeVersion" ${logic.rename-to} admtg_dgns_vrsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "attendingPhysicianNpi" ${logic.rename-to} at_physn_npi;
alter table public.snf_claims ${logic.alter-rename-column} "attendingPhysicianUpin" ${logic.rename-to} at_physn_upin;
alter table public.snf_claims ${logic.alter-rename-column} "coinsuranceDayCount" ${logic.rename-to} bene_tot_coinsrnc_days_cnt;
alter table public.snf_claims ${logic.alter-rename-column} "claimQueryCode" ${logic.rename-to} claim_query_code;
alter table public.snf_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimActionCode" ${logic.rename-to} fi_clm_actn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimProcessDate" ${logic.rename-to} fi_clm_proc_dt;
alter table public.snf_claims ${logic.alter-rename-column} "fiDocumentClaimControlNumber" ${logic.rename-to} fi_doc_clm_cntl_num;
alter table public.snf_claims ${logic.alter-rename-column} "fiscalIntermediaryNumber" ${logic.rename-to} fi_num;
alter table public.snf_claims ${logic.alter-rename-column} "fiOriginalClaimControlNumber" ${logic.rename-to} fi_orig_clm_cntl_num;
alter table public.snf_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternalFirstCode" ${logic.rename-to} fst_dgns_e_cd;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternalFirstCodeVersion" ${logic.rename-to} fst_dgns_e_vrsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "coveredCareThroughDate" ${logic.rename-to} nch_actv_or_cvrd_lvl_care_thru;
alter table public.snf_claims ${logic.alter-rename-column} "bloodDeductibleLiabilityAmount" ${logic.rename-to} nch_bene_blood_ddctbl_lblty_am;
alter table public.snf_claims ${logic.alter-rename-column} "beneficiaryDischargeDate" ${logic.rename-to} nch_bene_dschrg_dt;
alter table public.snf_claims ${logic.alter-rename-column} "deductibleAmount" ${logic.rename-to} nch_bene_ip_ddctbl_amt;
alter table public.snf_claims ${logic.alter-rename-column} "medicareBenefitsExhaustedDate" ${logic.rename-to} nch_bene_mdcr_bnfts_exhtd_dt_i;
alter table public.snf_claims ${logic.alter-rename-column} "partACoinsuranceLiabilityAmount" ${logic.rename-to} nch_bene_pta_coinsrnc_lblty_am;
alter table public.snf_claims ${logic.alter-rename-column} "bloodPintsFurnishedQty" ${logic.rename-to} nch_blood_pnts_frnshd_qty;
alter table public.snf_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "noncoveredCharge" ${logic.rename-to} nch_ip_ncvrd_chrg_amt;
alter table public.snf_claims ${logic.alter-rename-column} "totalDeductionAmount" ${logic.rename-to} nch_ip_tot_ddctn_amt;
alter table public.snf_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimPrimaryPayerCode" ${logic.rename-to} nch_prmry_pyr_cd;
alter table public.snf_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} nch_prmry_pyr_clm_pd_amt;
alter table public.snf_claims ${logic.alter-rename-column} "patientStatusCd" ${logic.rename-to} nch_ptnt_status_ind_cd;
alter table public.snf_claims ${logic.alter-rename-column} "qualifiedStayFromDate" ${logic.rename-to} nch_qlfyd_stay_from_dt;
alter table public.snf_claims ${logic.alter-rename-column} "qualifiedStayThroughDate" ${logic.rename-to} nch_qlfyd_stay_thru_dt;
alter table public.snf_claims ${logic.alter-rename-column} "noncoveredStayFromDate" ${logic.rename-to} nch_vrfd_ncvrd_stay_from_dt;
alter table public.snf_claims ${logic.alter-rename-column} "noncoveredStayThroughDate" ${logic.rename-to} nch_vrfd_ncvrd_stay_thru_dt;
alter table public.snf_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.snf_claims ${logic.alter-rename-column} "operatingPhysicianNpi" ${logic.rename-to} op_physn_npi;
alter table public.snf_claims ${logic.alter-rename-column} "operatingPhysicianUpin" ${logic.rename-to} op_physn_upin;
alter table public.snf_claims ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.snf_claims ${logic.alter-rename-column} "otherPhysicianNpi" ${logic.rename-to} ot_physn_npi;
alter table public.snf_claims ${logic.alter-rename-column} "otherPhysicianUpin" ${logic.rename-to} ot_physn_upin;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "providerNumber" ${logic.rename-to} prvdr_num;
alter table public.snf_claims ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.snf_claims ${logic.alter-rename-column} "patientDischargeStatusCode" ${logic.rename-to} ptnt_dschrg_stus_cd;
alter table public.snf_claims ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} clm_tot_chrg_amt;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis13Code" ${logic.rename-to} icd_dgns_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis14Code" ${logic.rename-to} icd_dgns_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis15Code" ${logic.rename-to} icd_dgns_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis16Code" ${logic.rename-to} icd_dgns_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis17Code" ${logic.rename-to} icd_dgns_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis18Code" ${logic.rename-to} icd_dgns_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis19Code" ${logic.rename-to} icd_dgns_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis20Code" ${logic.rename-to} icd_dgns_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis21Code" ${logic.rename-to} icd_dgns_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis22Code" ${logic.rename-to} icd_dgns_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis23Code" ${logic.rename-to} icd_dgns_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis24Code" ${logic.rename-to} icd_dgns_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis25Code" ${logic.rename-to} icd_dgns_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal1Code" ${logic.rename-to} icd_dgns_e_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal2Code" ${logic.rename-to} icd_dgns_e_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal3Code" ${logic.rename-to} icd_dgns_e_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal4Code" ${logic.rename-to} icd_dgns_e_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal5Code" ${logic.rename-to} icd_dgns_e_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal6Code" ${logic.rename-to} icd_dgns_e_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal7Code" ${logic.rename-to} icd_dgns_e_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal8Code" ${logic.rename-to} icd_dgns_e_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal9Code" ${logic.rename-to} icd_dgns_e_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal10Code" ${logic.rename-to} icd_dgns_e_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal11Code" ${logic.rename-to} icd_dgns_e_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal12Code" ${logic.rename-to} icd_dgns_e_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal1CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal2CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal3CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal4CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal5CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal6CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal7CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal8CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal9CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal10CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal11CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal12CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis13CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis14CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis15CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis16CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis17CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis18CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis19CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis20CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis21CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis22CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis23CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis24CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis25CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "procedure1Code" ${logic.rename-to} icd_prcdr_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "procedure2Code" ${logic.rename-to} icd_prcdr_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "procedure3Code" ${logic.rename-to} icd_prcdr_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "procedure4Code" ${logic.rename-to} icd_prcdr_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "procedure5Code" ${logic.rename-to} icd_prcdr_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "procedure6Code" ${logic.rename-to} icd_prcdr_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "procedure7Code" ${logic.rename-to} icd_prcdr_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "procedure8Code" ${logic.rename-to} icd_prcdr_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "procedure9Code" ${logic.rename-to} icd_prcdr_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "procedure10Code" ${logic.rename-to} icd_prcdr_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "procedure11Code" ${logic.rename-to} icd_prcdr_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "procedure12Code" ${logic.rename-to} icd_prcdr_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "procedure13Code" ${logic.rename-to} icd_prcdr_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "procedure14Code" ${logic.rename-to} icd_prcdr_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "procedure15Code" ${logic.rename-to} icd_prcdr_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "procedure16Code" ${logic.rename-to} icd_prcdr_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "procedure17Code" ${logic.rename-to} icd_prcdr_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "procedure18Code" ${logic.rename-to} icd_prcdr_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "procedure19Code" ${logic.rename-to} icd_prcdr_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "procedure20Code" ${logic.rename-to} icd_prcdr_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "procedure21Code" ${logic.rename-to} icd_prcdr_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "procedure22Code" ${logic.rename-to} icd_prcdr_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "procedure23Code" ${logic.rename-to} icd_prcdr_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "procedure24Code" ${logic.rename-to} icd_prcdr_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "procedure25Code" ${logic.rename-to} icd_prcdr_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "procedure1CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "procedure2CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "procedure3CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "procedure4CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "procedure5CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "procedure6CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "procedure7CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "procedure8CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "procedure9CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "procedure10CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "procedure11CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "procedure12CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "procedure13CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "procedure14CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "procedure15CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "procedure16CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "procedure17CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "procedure18CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "procedure19CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "procedure20CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "procedure21CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "procedure22CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "procedure23CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "procedure24CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "procedure25CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "procedure1Date" ${logic.rename-to} prcdr_dt1;
alter table public.snf_claims ${logic.alter-rename-column} "procedure2Date" ${logic.rename-to} prcdr_dt2;
alter table public.snf_claims ${logic.alter-rename-column} "procedure3Date" ${logic.rename-to} prcdr_dt3;
alter table public.snf_claims ${logic.alter-rename-column} "procedure4Date" ${logic.rename-to} prcdr_dt4;
alter table public.snf_claims ${logic.alter-rename-column} "procedure5Date" ${logic.rename-to} prcdr_dt5;
alter table public.snf_claims ${logic.alter-rename-column} "procedure6Date" ${logic.rename-to} prcdr_dt6;
alter table public.snf_claims ${logic.alter-rename-column} "procedure7Date" ${logic.rename-to} prcdr_dt7;
alter table public.snf_claims ${logic.alter-rename-column} "procedure8Date" ${logic.rename-to} prcdr_dt8;
alter table public.snf_claims ${logic.alter-rename-column} "procedure9Date" ${logic.rename-to} prcdr_dt9;
alter table public.snf_claims ${logic.alter-rename-column} "procedure10Date" ${logic.rename-to} prcdr_dt10;
alter table public.snf_claims ${logic.alter-rename-column} "procedure11Date" ${logic.rename-to} prcdr_dt11;
alter table public.snf_claims ${logic.alter-rename-column} "procedure12Date" ${logic.rename-to} prcdr_dt12;
alter table public.snf_claims ${logic.alter-rename-column} "procedure13Date" ${logic.rename-to} prcdr_dt13;
alter table public.snf_claims ${logic.alter-rename-column} "procedure14Date" ${logic.rename-to} prcdr_dt14;
alter table public.snf_claims ${logic.alter-rename-column} "procedure15Date" ${logic.rename-to} prcdr_dt15;
alter table public.snf_claims ${logic.alter-rename-column} "procedure16Date" ${logic.rename-to} prcdr_dt16;
alter table public.snf_claims ${logic.alter-rename-column} "procedure17Date" ${logic.rename-to} prcdr_dt17;
alter table public.snf_claims ${logic.alter-rename-column} "procedure18Date" ${logic.rename-to} prcdr_dt18;
alter table public.snf_claims ${logic.alter-rename-column} "procedure19Date" ${logic.rename-to} prcdr_dt19;
alter table public.snf_claims ${logic.alter-rename-column} "procedure20Date" ${logic.rename-to} prcdr_dt20;
alter table public.snf_claims ${logic.alter-rename-column} "procedure21Date" ${logic.rename-to} prcdr_dt21;
alter table public.snf_claims ${logic.alter-rename-column} "procedure22Date" ${logic.rename-to} prcdr_dt22;
alter table public.snf_claims ${logic.alter-rename-column} "procedure23Date" ${logic.rename-to} prcdr_dt23;
alter table public.snf_claims ${logic.alter-rename-column} "procedure24Date" ${logic.rename-to} prcdr_dt24;
alter table public.snf_claims ${logic.alter-rename-column} "procedure25Date" ${logic.rename-to} prcdr_dt25;
--
-- SNFClaimLines to snf_claim_lines
--
alter table public."SNFClaimLines" rename to snf_claim_lines;
alter table public.snf_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.snf_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} clm_line_num;
alter table public.snf_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.snf_claim_lines ${logic.alter-rename-column} "revenueCenter" ${logic.rename-to} rev_cntr;
alter table public.snf_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQualifierCode" ${logic.rename-to} rev_cntr_ndc_qty_qlfr_cd;
alter table public.snf_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQuantity" ${logic.rename-to} rev_cntr_ndc_qty;
alter table public.snf_claim_lines ${logic.alter-rename-column} "nonCoveredChargeAmount" ${logic.rename-to} rev_cntr_ncvrd_chrg_amt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "rateAmount" ${logic.rename-to} rev_cntr_rate_amt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} rev_cntr_tot_chrg_amt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "deductibleCoinsuranceCd" ${logic.rename-to} rev_cntr_ddctbl_coinsrnc_cd;
alter table public.snf_claim_lines ${logic.alter-rename-column} "unitCount" ${logic.rename-to} rev_cntr_unit_cnt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianNPI" ${logic.rename-to} rndrng_physn_npi;
alter table public.snf_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianUPIN" ${logic.rename-to} rndrng_physn_upin;
--
-- BeneficiariesHistory to beneficiaries_history
--
alter table public."BeneficiariesHistory" rename to beneficiaries_history;
alter table public.beneficiaries_history ${logic.alter-rename-column} "beneficiaryHistoryId" ${logic.rename-to} bene_history_id;
alter table public.beneficiaries_history ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries_history ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries_history ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries_history ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries_history ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiHash" ${logic.rename-to} mbi_hash;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} efctv_bgn_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiObsoleteDate" ${logic.rename-to} efctv_end_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
--
-- BeneficiariesHistoryInvalidBeneficiaries to beneficiaries_history_invalid_beneficiaries
--
alter table public."BeneficiariesHistoryInvalidBeneficiaries" rename to beneficiaries_history_invalid_beneficiaries;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryHistoryId" ${logic.rename-to} bene_history_id;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;
--
-- BeneficiaryMonthly to beneficiary_monthly
--
alter table public."BeneficiaryMonthly" rename to beneficiary_monthly;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "yearMonth" ${logic.rename-to} year_month;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "parentBeneficiary" ${logic.rename-to} bene_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDContractNumberId" ${logic.rename-to} partd_contract_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCContractNumberId" ${logic.rename-to} partc_contract_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "fipsStateCntyCode" ${logic.rename-to} fips_state_cnty_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "medicareStatusCode" ${logic.rename-to} medicare_status_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "entitlementBuyInInd" ${logic.rename-to} entitlement_buy_in_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "hmoIndicatorInd" ${logic.rename-to} hmo_indicator_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCPbpNumberId" ${logic.rename-to} partc_pbp_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCPlanTypeCode" ${logic.rename-to} partc_plan_type_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDPbpNumberId" ${logic.rename-to} partd_pbp_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDSegmentNumberId" ${logic.rename-to} partd_segment_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDRetireeDrugSubsidyInd" ${logic.rename-to} partd_retiree_drug_subsidy_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDLowIncomeCostShareGroupCode" ${logic.rename-to} partd_low_income_cost_share_group_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "medicaidDualEligibilityCode" ${logic.rename-to} medicaid_dual_eligibility_code;
--
-- LoadedFiles to loaded_files
--
-- We have a bit of a funky condition between psql and hsql; for both loaded_files and loaded_batches
-- there is a column called "created". For psql there is no need to do a rename; in fact if we tried
-- to do something like:
--
--      psql: alter table public.loaded_files rename column "created" to created
--
-- we'd get an error. So in theory, maybe we don't even need to do a rename for that type of condition.
-- However, in hsql, if we don't do a rename, we end up with a column called "created" (ltierally,
-- meaning the double-quotes are an integral part of the column name). So for hsql we do need to
-- perform the rename so we can rid the column name of the double-quotes.
--
--      ${logic.hsql-only-alter}
--          psql: "--"
--          hsql: "alter"
--
alter table public."LoadedFiles" rename to loaded_files;
alter table public.loaded_files ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_fileid;
alter table public.loaded_files ${logic.alter-rename-column} "rifType" ${logic.rename-to} rif_type;
${logic.hsql-only-alter} table public.loaded_files ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- LoadedBatches to loaded_batches
--
alter table public."LoadedBatches" rename to loaded_batches;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedBatchId" ${logic.rename-to} loaded_batchid;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_fileid;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "beneficiaries" ${logic.rename-to} beneficiaries;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- MedicareBeneficiaryIdHistory to medicare_beneficiaryid_history
--
alter table public."MedicareBeneficiaryIdHistory" rename to medicare_beneficiaryid_history;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "medicareBeneficiaryIdKey" ${logic.rename-to} bene_mbi_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "claimAccountNumber" ${logic.rename-to} bene_clm_acnt_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "beneficiaryIdCode" ${logic.rename-to} bene_ident_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiCrntRecIndId" ${logic.rename-to} bene_crnt_rec_ind_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiSequenceNumber" ${logic.rename-to} mbi_sqnc_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} mbi_efctv_bgn_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEndDate" ${logic.rename-to} mbi_efctv_end_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEffectiveReasonCode" ${logic.rename-to} mbi_bgn_rsn_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEndReasonCode" ${logic.rename-to} mbi_end_rsn_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiCardRequestDate" ${logic.rename-to} mbi_card_rqst_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiAddUser" ${logic.rename-to} creat_user_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiAddDate" ${logic.rename-to} creat_ts;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiUpdateUser" ${logic.rename-to} updt_user_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiUpdateDate" ${logic.rename-to} updt_ts;
--
-- MedicareBeneficiaryIdHistoryInvalidBeneficiaries to medicare_beneficiaryid_history_invalid_beneficiaries
--
alter table public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries" rename to medicare_beneficiaryid_history_invalid_beneficiaries;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryIdKey" ${logic.rename-to} bene_mbi_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "claimAccountNumber" ${logic.rename-to} bene_clm_acnt_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryIdCode" ${logic.rename-to} bene_ident_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiCrntRecIndId" ${logic.rename-to} bene_crnt_rec_ind_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiSequenceNumber" ${logic.rename-to} mbi_sqnc_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} mbi_efctv_bgn_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEndDate" ${logic.rename-to} mbi_efctv_end_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEffectiveReasonCode" ${logic.rename-to} mbi_bgn_rsn_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEndReasonCode" ${logic.rename-to} mbi_end_rsn_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiCardRequestDate" ${logic.rename-to} mbi_card_rqst_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiAddUser" ${logic.rename-to} creat_user_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiAddDate" ${logic.rename-to} creat_ts;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiUpdateUser" ${logic.rename-to} updt_user_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiUpdateDate" ${logic.rename-to} updt_ts;
--
-- take care of primary jeys
--
ALTER TABLE public.beneficiaries
    ADD CONSTRAINT beneficiaries_pkey PRIMARY KEY (bene_id);
  
ALTER TABLE public.beneficiaries_history
    ADD CONSTRAINT beneficiaries_history_pkey PRIMARY KEY (bene_history_id);
    
ALTER TABLE public.beneficiaries_history_invalid_beneficiaries
    ADD CONSTRAINT beneficiaries_history_invalid_beneficiaries_pkey PRIMARY KEY (bene_history_id);
    
ALTER TABLE public.beneficiary_monthly
    ADD CONSTRAINT beneficiary_monthly_pkey PRIMARY KEY (bene_id, year_month);
    
ALTER TABLE public.carrier_claim_lines
    ADD CONSTRAINT carrier_claim_lines_pkey PRIMARY KEY (clm_id, line_num);
    
ALTER TABLE public.carrier_claims
    ADD CONSTRAINT carrier_claims_pkey PRIMARY KEY (clm_id);
    
ALTER TABLE public.dme_claim_lines
    ADD CONSTRAINT dme_claim_lines_pkey PRIMARY KEY (clm_id, line_num);
    
ALTER TABLE public.dme_claims
    ADD CONSTRAINT dme_claims_pkey PRIMARY KEY (clm_id);
    
ALTER TABLE public.hha_claim_lines
    ADD CONSTRAINT hha_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
    
ALTER TABLE public.hha_claims
    ADD CONSTRAINT hha_claims_pkey PRIMARY KEY (clm_id);
    
ALTER TABLE public.hospice_claim_lines
    ADD CONSTRAINT hospice_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
    
ALTER TABLE public.hospice_claims
    ADD CONSTRAINT hospice_claims_pkey PRIMARY KEY (clm_id);
    
ALTER TABLE public.inpatient_claim_lines
    ADD CONSTRAINT inpatient_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
    
ALTER TABLE public.inpatient_claims
    ADD CONSTRAINT npatient_claims_pkey PRIMARY KEY (clm_id);
/* 
ALTER TABLE public.loaded_batches
    ADD CONSTRAINT loaded_batches_pkey PRIMARY KEY (loaded_batchid);
    
ALTER TABLE public.loaded_files
    ADD CONSTRAINT loaded_files_pkey PRIMARY KEY (loaded_fileid);
*/   
ALTER TABLE public.medicare_beneficiaryid_history_invalid_beneficiaries
    ADD CONSTRAINT medicare_beneficiaryid_history_invalid_beneficiaries_pkey PRIMARY KEY (bene_mbi_id);
    
ALTER TABLE public.medicare_beneficiaryid_history
    ADD CONSTRAINT medicare_beneficiaryid_history_pkey PRIMARY KEY (bene_mbi_id);
    
ALTER TABLE public.outpatient_claim_lines
    ADD CONSTRAINT outpatient_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
    
ALTER TABLE public.outpatient_claims
    ADD CONSTRAINT outpatient_claims_pkey PRIMARY KEY (clm_id);
    
ALTER TABLE public.partd_events
    ADD CONSTRAINT ppartd_events_pkey PRIMARY KEY (pde_id);
    
ALTER TABLE public.snf_claim_lines
    ADD CONSTRAINT snf_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
    
ALTER TABLE public.snf_claims
    ADD CONSTRAINT snf_claims_pkey PRIMARY KEY (clm_id);
--
-- rename indexes (index names are limited to 64 chars)
--
ALTER INDEX "BeneficiariesHistory_beneficiaryId_idx" RENAME TO beneficiaries_history_beneid_idx;
ALTER INDEX "BeneficiariesHistory_hicn_idx" RENAME TO beneficiaries_history_hicn_idx;
ALTER INDEX "Beneficiaries_history_mbi_hash_idx" RENAME TO beneficiaries_history_mbi_hash_idx;
ALTER INDEX "Beneficiaries_hicn_idx" RENAME TO beneficiaries_hicn_idx;
ALTER INDEX "Beneficiaries_mbi_hash_idx" RENAME TO beneficiaries_mbi_hash_idx; 
ALTER INDEX "BeneficiaryMonthly_partDContractNumId_yearMonth_parentBene_idx" RENAME TO beneficiary_monthly_year_month_partd_contract_beneid_idx;
ALTER INDEX "BeneficiaryMonthly_partDContractNumberId_yearmonth_idx" RENAME TO beneficiary_monthly_partd_contract_number_year_month_idx;
ALTER INDEX "CarrierClaims_beneficiaryId_idx" RENAME TO carrier_claims_beneid_idx;
ALTER INDEX "DMEClaims_beneficiaryId_idx" RENAME TO dme_claims_beneid_idx;
ALTER INDEX "HHAClaims_beneficiaryId_idx" RENAME TO hha_claims_beneid_idx;
ALTER INDEX "HospiceClaims_beneficiaryId_idx" RENAME TO hospice_claims_beneid_idx;
ALTER INDEX "InpatientClaims_beneficiaryId_idx" RENAME TO inpatient_claims_beneid_idx;
ALTER INDEX "LoadedBatches_created_index" RENAME TO loaded_batches_created_idx;
ALTER INDEX "MedicareBeneficiaryIdHistory_beneficiaryId_idx" RENAME TO medicare_beneficiaryid_history_beneid_idx;
ALTER INDEX "OutpatientClaims_beneficiaryId_idx" RENAME TO outpatient_claims_beneid_idx;
ALTER INDEX "PartDEvents_beneficiaryId_idx" RENAME TO partd_events_beneid_idx;
ALTER INDEX "SNFClaims_beneficiaryId_idx" RENAME TO snf_claims_beneid_idx;

-- FIX THIS - why do we even have these???
ALTER INDEX "Beneficiaries_partd_contract_number_apr_id_idx" RENAME  TO beneficiaries_partd_contract_number_apr_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_aug_id_idx" RENAME  TO beneficiaries_partd_contract_number_aug_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_dec_id_idx" RENAME  TO beneficiaries_partd_contract_number_dec_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_feb_id_idx" RENAME  TO beneficiaries_partd_contract_number_feb_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jan_id_idx" RENAME  TO beneficiaries_partd_contract_number_jan_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jul_id_idx" RENAME  TO beneficiaries_partd_contract_number_jul_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jun_id_idx" RENAME  TO beneficiaries_partd_contract_number_jun_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_mar_id_idx" RENAME  TO beneficiaries_partd_contract_number_mar_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_may_id_idx" RENAME  TO beneficiaries_partd_contract_number_may_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_nov_id_idx" RENAME  TO beneficiaries_partd_contract_number_nov_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_oct_id_idx" RENAME  TO beneficiaries_partd_contract_number_oct_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_sept_id_idx" RENAME TO beneficiaries_partd_contract_number_sept_id_idx;
--
-- Add foreign key constraints
--
ALTER TABLE public.beneficiaries_history
    ADD CONSTRAINT beneficiaries_history_bene_id_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.beneficiary_monthly
    ADD CONSTRAINT beneficiary_monthly_beneid_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.carrier_claim_lines
    ADD CONSTRAINT carrier_claim_lines_clmid_to_carrier_claims FOREIGN KEY (clm_id) REFERENCES public.carrier_claims (clm_id);

ALTER TABLE public.carrier_claims
    ADD CONSTRAINT carrier_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.dme_claim_lines
    ADD CONSTRAINT dme_claim_lines_clmid_to_dme_claims FOREIGN KEY (clm_id) REFERENCES public.dme_claims (clm_id);

ALTER TABLE public.dme_claims
    ADD CONSTRAINT dme_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries(bene_id);

ALTER TABLE public.hha_claim_lines
    ADD CONSTRAINT hha_claim_lines_parent_claim_to_hha_claims FOREIGN KEY (clm_id) REFERENCES public.hha_claims (clm_id);

ALTER TABLE public.hha_claims
    ADD CONSTRAINT hha_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.hospice_claim_lines
    ADD CONSTRAINT hospice_claim_lines_parent_claim_to_hospice_claims FOREIGN KEY (clm_id) REFERENCES public.hospice_claims (clm_id);

ALTER TABLE public.hospice_claims
    ADD CONSTRAINT hospice_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.inpatient_claim_lines
    ADD CONSTRAINT inpatient_claim_lines_parent_claim_to_inpatient_claims FOREIGN KEY (clm_id) REFERENCES public.inpatient_claims (clm_id);

ALTER TABLE public.inpatient_claims
    ADD CONSTRAINT inpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.medicare_beneficiaryid_history
    ADD CONSTRAINT medicare_beneficiaryid_history_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.outpatient_claim_lines
    ADD CONSTRAINT outpatient_claim_lines_parent_claim_to_outpatient_claims FOREIGN KEY (clm_id) REFERENCES public.outpatient_claims (clm_id);

ALTER TABLE public.outpatient_claims
    ADD CONSTRAINT outpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.partd_events
    ADD CONSTRAINT partd_events_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.snf_claim_lines
    ADD CONSTRAINT snf_claim_lines_parent_claim_to_snf_claims FOREIGN KEY (clm_id) REFERENCES public.snf_claims (clm_id);

ALTER TABLE public.snf_claims
    ADD CONSTRAINT snf_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.loaded_batches
    ADD CONSTRAINT loaded_batches_loaded_fileid FOREIGN KEY (loaded_fileid) REFERENCES public.loaded_files (loaded_fileid);

-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file; helpful in tracking down misnamed table or columns in an hsql db.
-- SCRIPT './bfd_schema_post.txt';