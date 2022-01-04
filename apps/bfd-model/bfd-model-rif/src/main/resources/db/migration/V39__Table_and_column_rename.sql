--
-- NOTES:
--   1. when you rename a table, indexes/constraints will trickle down to contraint & index directives,
--      BUT do not modify constraint or index names themselves
--   2. don't try to rename a column that already has the name (i.e., "hicn" ${logic.rename-to} hicn)
--   3. optionally rename contraint and/or index names (i.e., remove camelCase)
--
-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file prior to performing table or column rename.
-- SCRIPT './bfd_schema_pre.txt';
--
-- Drop foreign key constraints to Beneficiary
--
ALTER TABLE public."BeneficiariesHistory"
    drop constraint "BeneficiariesHistory_beneficiaryId_to_Beneficiary";

ALTER TABLE public."BeneficiaryMonthly"
    drop constraint "BeneficiaryMonthly_parentBeneficiary_to_Beneficiary";

ALTER TABLE public."CarrierClaimLines"
    drop constraint "CarrierClaimLines_parentClaim_to_CarrierClaims";

ALTER TABLE public."CarrierClaims"
    drop constraint "CarrierClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."DMEClaimLines"
    drop constraint "DMEClaimLines_parentClaim_to_DMEClaims";

ALTER TABLE public."DMEClaims"
    drop constraint "DMEClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."HHAClaimLines"
    drop constraint "HHAClaimLines_parentClaim_to_HHAClaims";

ALTER TABLE public."HHAClaims"
    drop constraint "HHAClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."HospiceClaimLines"
    drop constraint "HospiceClaimLines_parentClaim_to_HospiceClaims";

ALTER TABLE public."HospiceClaims"
    drop constraint "HospiceClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."InpatientClaimLines"
    drop constraint "InpatientClaimLines_parentClaim_to_InpatientClaims";

ALTER TABLE public."InpatientClaims"
    drop constraint "InpatientClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."MedicareBeneficiaryIdHistory"
    drop constraint "MedicareBeneficiaryIdHistory_beneficiaryId_to_Beneficiary";

ALTER TABLE public."OutpatientClaimLines"
    drop constraint "OutpatientClaimLines_parentClaim_to_OutpatientClaims";

ALTER TABLE public."OutpatientClaims"
    drop constraint "OutpatientClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."PartDEvents"
    drop constraint "PartDEvents_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."SNFClaimLines"
    drop constraint "SNFClaimLines_parentClaim_to_SNFClaims";

ALTER TABLE public."SNFClaims"
    drop constraint "SNFClaims_beneficiaryId_to_Beneficiaries";

ALTER TABLE public."LoadedBatches"
    drop constraint "loadedBatches_loadedFileId";
--
-- Drop primary key constraints; hsql does not support rename constraint
--
--      hsql: alter table public."BeneficiariesHistoryInvalidBeneficiaries" drop constraint "BeneficiariesHistoryInvalidBeneficiaries_pkey";
--
--      ${logic.hsql-only-alter}
--          psql: "-- alter"
--          hsql: "alter"
--
${logic.hsql-only-alter} table public."BeneficiariesHistoryInvalidBeneficiaries" drop constraint "BeneficiariesHistoryInvalidBeneficiaries_pkey";
${logic.hsql-only-alter} table public."BeneficiariesHistory" drop constraint "BeneficiariesHistory_pkey";
${logic.hsql-only-alter} table public."Beneficiaries" drop constraint "Beneficiaries_pkey";
${logic.hsql-only-alter} table public."BeneficiaryMonthly" drop constraint "BeneficiaryMonthly_pkey";
${logic.hsql-only-alter} table public."CarrierClaimLines" drop constraint "CarrierClaimLines_pkey";
${logic.hsql-only-alter} table public."CarrierClaims" drop constraint "CarrierClaims_pkey";
${logic.hsql-only-alter} table public."DMEClaimLines" drop constraint "DMEClaimLines_pkey";
${logic.hsql-only-alter} table public."DMEClaims" drop constraint "DMEClaims_pkey";
${logic.hsql-only-alter} table public."HHAClaimLines" drop constraint "HHAClaimLines_pkey";
${logic.hsql-only-alter} table public."HHAClaims" drop constraint "HHAClaims_pkey";
${logic.hsql-only-alter} table public."HospiceClaimLines" drop constraint "HospiceClaimLines_pkey";
${logic.hsql-only-alter} table public."HospiceClaims" drop constraint "HospiceClaims_pkey";
${logic.hsql-only-alter} table public."InpatientClaimLines" drop constraint "InpatientClaimLines_pkey";
${logic.hsql-only-alter} table public."InpatientClaims" drop constraint "InpatientClaims_pkey";
${logic.hsql-only-alter} table public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries" drop constraint "MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey";
${logic.hsql-only-alter} table public."MedicareBeneficiaryIdHistory" drop constraint "MedicareBeneficiaryIdHistory_pkey";
${logic.hsql-only-alter} table public."OutpatientClaimLines" drop constraint "OutpatientClaimLines_pkey";
${logic.hsql-only-alter} table public."OutpatientClaims" drop constraint "OutpatientClaims_pkey";
${logic.hsql-only-alter} table public."PartDEvents" drop constraint "PartDEvents_pkey";
${logic.hsql-only-alter} table public."SNFClaimLines" drop constraint "SNFClaimLines_pkey";
${logic.hsql-only-alter} table public."SNFClaims" drop constraint "SNFClaims_pkey";
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

${logic.alter-rename-index} public."Beneficiaries_pkey" rename to beneficiaries_pkey;

${logic.hsql-only-alter} table public.beneficiaries add constraint beneficiaries_pkey primary key (bene_id);

ALTER INDEX "Beneficiaries_hicn_idx" RENAME TO beneficiaries_hicn_idx;
ALTER INDEX "Beneficiaries_mbi_hash_idx" RENAME TO beneficiaries_mbi_hash_idx;

-- CHECK THIS - why do we even have these???
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
