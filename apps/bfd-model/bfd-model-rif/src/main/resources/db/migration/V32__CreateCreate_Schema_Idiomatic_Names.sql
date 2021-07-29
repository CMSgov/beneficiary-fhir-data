/*
 * This migration has two primary goals:
 *
 * First, we want to convert the beneficiaryId, claimId and claimGroupId columns to `bigint`, rather than
 * their current `varchar(11)` types. Per the PostgreSQL documentation, a `varchar(11)` column requires 16 bytes
 * per record on disk [1], and a `bigint` column only requires 8 bytes. Accordingly, this change roughly halves
 * the size of our indexes using those columns.
 *
 * Second, we're _finally_ moving to a more idiomatic (and user-friendly) naming scheme for these tables based
 * (almost entirely) on CCW field names.
 *
 * In addition to those primary goals, this schema migration is also being used as a good opportunity to
 * address all of these other less major schema changes:
 * 
 * * Use better defined Postgres data types (i.e., NUMERIC(3,0) to SMALLINT)
 * * Minor changes to column ordering to facilitate potential indexing
 * * Group together like data elements.
 *
 * In all table definitions, there is a commented identifier to the right of each table column;
 * this represents the previous schema name for that data column.
 *
 * Overall, it is expected that these migrations will be in steps:
 *
 * 1. Create the new tables in the same schema as the current database schema.
 * 2. Migrate current data to new tables (i.e., Insert-Select)
 * 3. apply indeces as needed; based on some of the known queries that have proven probabmatic,
 *    final disposition of all indeces will not be defined in this file.
 * 3. Deploy a new version of the application that only uses those new tables.
 * 4. Remove the original tables.
 * 
 * This script is for Step 1.
 *
 * NOTE: Steps 1 and 2 may be combined as a single step depending on final strategic paln.
 *
 * [1]: https://www.postgresql.org/docs/10/datatype-character.html
 * [2]: https://www.postgresql.org/docs/10/datatype-numeric.html
 */
${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table beneficiaries (
    bene_id                                  bigint not null,                          -- beneficiaryId
    bene_birth_dt                            date not null,                            -- birthDate
    bene_esrd_ind                            character(1),                             -- endStageRenalDiseaseCode
    bene_entlmt_rsn_curr                     character(1),                             -- entitlementCodeCurrent
    bene_entlmt_rsn_orig                     character(1),                             -- entitlementCodeOriginal
    bene_mdcr_status_cd                      character varying(2),                     -- medicareEnrollmentStatusCode
    bene_gvn_name                            character varying(15) not null,           -- nameGiven
    bene_mdl_name                            character(1),                             -- nameMiddleInitial
    bene_srnm_name                           character varying(24) not null,           -- nameSurname
    state_code                               character varying(2) not null,            -- stateCode
    bene_county_cd                           character varying(10) not null,           -- countyCode
    bene_zip_cd                              character varying(9) not null,            -- postalCode
    age                                      smallint,                                 -- ageOfBeneficiary
    bene_race_cd                             character(1),                             -- race
    rti_race_cd                              character(1),                             -- rtiRaceCode
    bene_sex_ident_cd                        character(1) not null,                    -- sex
    bene_crnt_hic_num                        character varying(64) not null,           -- hicn
    hicn_unhashed                            character varying(11),                    -- hicnUnhashed
    mbi_num                                  character varying(11),                    -- medicareBeneficiaryId
    mbi_hash                                 character varying(64),                    -- mbiHash
    mbi_efctv_bgn_dt                         date,                                     -- mbiEffectiveDate
    mbi_efctv_end_dt                         date,                                     -- mbiObsoleteDate
    death_dt                                 date,                                     -- beneficiaryDateOfDeath
    v_dod_sw                                 character(1),                             -- validDateOfDeathSw
    rfrnc_yr                                 smallint,                                 -- beneEnrollmentReferenceYear
    bene_pta_trmntn_cd                       character(1),                             -- partATerminationCode
    bene_ptb_trmntn_cd                       character(1),                             -- partBTerminationCode
    a_mo_cnt                                 smallint,                                 -- partAMonthsCount
    b_mo_cnt                                 smallint,                                 -- partBMonthsCount
    buyin_mo_cnt                             smallint,                                 -- stateBuyInCoverageCount
    hmo_mo_cnt                               smallint,                                 -- hmoCoverageCount
    rds_mo_cnt                               smallint,                                 -- monthsRetireeDrugSubsidyCoverage
    enrl_src                                 character varying(3),                     -- sourceOfEnrollmentData
    sample_group                             character varying(2),                     -- sampleMedicareGroupIndicator
    efivepct                                 character(1),                             -- enhancedMedicareSampleIndicator
    crnt_bic                                 character varying(2),                     -- currentBeneficiaryIdCode
    covstart                                 date,                                     -- medicareCoverageStartDate
    dual_mo_cnt                              smallint,                                 -- monthsOfDualEligibility
    fips_state_cnty_jan_cd                   character varying(5),                     -- fipsStateCntyJanCode
    fips_state_cnty_feb_cd                   character varying(5),                     -- fipsStateCntyFebCode
    fips_state_cnty_mar_cd                   character varying(5),                     -- fipsStateCntyMarCode
    fips_state_cnty_apr_cd                   character varying(5),                     -- fipsStateCntyAprCode
    fips_state_cnty_may_cd                   character varying(5),                     -- fipsStateCntyMayCode
    fips_state_cnty_jun_cd                   character varying(5),                     -- fipsStateCntyJunCode
    fips_state_cnty_jul_cd                   character varying(5),                     -- fipsStateCntyJulCode
    fips_state_cnty_aug_cd                   character varying(5),                     -- fipsStateCntyAugCode
    fips_state_cnty_sept_cd                  character varying(5),                     -- fipsStateCntySeptCode
    fips_state_cnty_oct_cd                   character varying(5),                     -- fipsStateCntyOctCode
    fips_state_cnty_nov_cd                   character varying(5),                     -- fipsStateCntyNovCode
    fips_state_cnty_dec_cd                   character varying(5),                     -- fipsStateCntyDecCode
    mdcr_stus_jan_cd                         character varying(2),                     -- medicareStatusJanCode
    mdcr_stus_feb_cd                         character varying(2),                     -- medicareStatusFebCode
    mdcr_stus_mar_cd                         character varying(2),                     -- medicareStatusMarCode
    mdcr_stus_apr_cd                         character varying(2),                     -- medicareStatusAprCode
    mdcr_stus_may_cd                         character varying(2),                     -- medicareStatusMayCode
    mdcr_stus_jun_cd                         character varying(2),                     -- medicareStatusJunCode
    mdcr_stus_jul_cd                         character varying(2),                     -- medicareStatusJulCode
    mdcr_stus_aug_cd                         character varying(2),                     -- medicareStatusAugCode
    mdcr_stus_sept_cd                        character varying(2),                     -- medicareStatusSeptCode
    mdcr_stus_oct_cd                         character varying(2),                     -- medicareStatusOctCode
    mdcr_stus_nov_cd                         character varying(2),                     -- medicareStatusNovCode
    mdcr_stus_dec_cd                         character varying(2),                     -- medicareStatusDecCode
    plan_cvrg_mo_cnt                         smallint,                                 -- partDMonthsCount
    mdcr_entlmt_buyin_1_ind                  character(1),                             -- entitlementBuyInJanInd
    mdcr_entlmt_buyin_2_ind                  character(1),                             -- entitlementBuyInFebInd
    mdcr_entlmt_buyin_3_ind                  character(1),                             -- entitlementBuyInMarInd
    mdcr_entlmt_buyin_4_ind                  character(1),                             -- entitlementBuyInAprInd
    mdcr_entlmt_buyin_5_ind                  character(1),                             -- entitlementBuyInMayInd
    mdcr_entlmt_buyin_6_ind                  character(1),                             -- entitlementBuyInJunInd
    mdcr_entlmt_buyin_7_ind                  character(1),                             -- entitlementBuyInJulInd
    mdcr_entlmt_buyin_8_ind                  character(1),                             -- entitlementBuyInAugInd
    mdcr_entlmt_buyin_9_ind                  character(1),                             -- entitlementBuyInSeptInd
    mdcr_entlmt_buyin_10_ind                 character(1),                             -- entitlementBuyInOctInd
    mdcr_entlmt_buyin_11_ind                 character(1),                             -- entitlementBuyInNovInd
    mdcr_entlmt_buyin_12_ind                 character(1),                             -- entitlementBuyInDecInd
    hmo_1_ind                                character(1),                             -- hmoIndicatorJanInd
    hmo_2_ind                                character(1),                             -- hmoIndicatorFebInd
    hmo_3_ind                                character(1),                             -- hmoIndicatorMarInd
    hmo_4_ind                                character(1),                             -- hmoIndicatorAprInd
    hmo_5_ind                                character(1),                             -- hmoIndicatorMayInd
    hmo_6_ind                                character(1),                             -- hmoIndicatorJunInd
    hmo_7_ind                                character(1),                             -- hmoIndicatorJulInd
    hmo_8_ind                                character(1),                             -- hmoIndicatorAugInd
    hmo_9_ind                                character(1),                             -- hmoIndicatorSeptInd
    hmo_10_ind                               character(1),                             -- hmoIndicatorOctInd
    hmo_11_ind                               character(1),                             -- hmoIndicatorNovInd
    hmo_12_ind                               character(1),                             -- hmoIndicatorDecInd
    ptc_cntrct_jan_id                        character varying(5),                     -- partCContractNumberJanId
    ptc_cntrct_feb_id                        character varying(5),                     -- partCContractNumberFebId
    ptc_cntrct_mar_id                        character varying(5),                     -- partCContractNumberMarId
    ptc_cntrct_apr_id                        character varying(5),                     -- partCContractNumberAprId
    ptc_cntrct_may_id                        character varying(5),                     -- partCContractNumberMayId
    ptc_cntrct_jun_id                        character varying(5),                     -- partCContractNumberJunId
    ptc_cntrct_jul_id                        character varying(5),                     -- partCContractNumberJulId
    ptc_cntrct_aug_id                        character varying(5),                     -- partCContractNumberAugId
    ptc_cntrct_sept_id                       character varying(5),                     -- partCContractNumberSeptId
    ptc_cntrct_oct_id                        character varying(5),                     -- partCContractNumberOctId
    ptc_cntrct_nov_id                        character varying(5),                     -- partCContractNumberNovId
    ptc_cntrct_dec_id                        character varying(5),                     -- partCContractNumberDecId
    ptc_pbp_jan_id                           character varying(3),                     -- partCPbpNumberJanId
    ptc_pbp_feb_id                           character varying(3),                     -- partCPbpNumberFebId
    ptc_pbp_mar_id                           character varying(3),                     -- partCPbpNumberMarId
    ptc_pbp_apr_id                           character varying(3),                     -- partCPbpNumberAprId
    ptc_pbp_may_id                           character varying(3),                     -- partCPbpNumberMayId
    ptc_pbp_jun_id                           character varying(3),                     -- partCPbpNumberJunId
    ptc_pbp_jul_id                           character varying(3),                     -- partCPbpNumberJulId
    ptc_pbp_aug_id                           character varying(3),                     -- partCPbpNumberAugId
    ptc_pbp_sept_id                          character varying(3),                     -- partCPbpNumberSeptId
    ptc_pbp_oct_id                           character varying(3),                     -- partCPbpNumberOctId
    ptc_pbp_nov_id                           character varying(3),                     -- partCPbpNumberNovId
    ptc_pbp_dec_id                           character varying(3),                     -- partCPbpNumberDecId
    ptc_plan_type_jan_cd                     character varying(3),                     -- partCPlanTypeJanCode
    ptc_plan_type_feb_cd                     character varying(3),                     -- partCPlanTypeFebCode
    ptc_plan_type_mar_cd                     character varying(3),                     -- partCPlanTypeMarCode
    ptc_plan_type_apr_cd                     character varying(3),                     -- partCPlanTypeAprCode
    ptc_plan_type_may_cd                     character varying(3),                     -- partCPlanTypeMayCode
    ptc_plan_type_jun_cd                     character varying(3),                     -- partCPlanTypeJunCode
    ptc_plan_type_jul_cd                     character varying(3),                     -- partCPlanTypeJulCode
    ptc_plan_type_aug_cd                     character varying(3),                     -- partCPlanTypeAugCode
    ptc_plan_type_sept_cd                    character varying(3),                     -- partCPlanTypeSeptCode
    ptc_plan_type_oct_cd                     character varying(3),                     -- partCPlanTypeOctCode
    ptc_plan_type_nov_cd                     character varying(3),                     -- partCPlanTypeNovCode
    ptc_plan_type_dec_cd                     character varying(3),                     -- partCPlanTypeDecCode
    ptd_cntrct_jan_id                        character varying(5),                     -- partDContractNumberJanId
    ptd_cntrct_feb_id                        character varying(5),                     -- partDContractNumberFebId
    ptd_cntrct_mar_id                        character varying(5),                     -- partDContractNumberMarId
    ptd_cntrct_apr_id                        character varying(5),                     -- partDContractNumberAprId
    ptd_cntrct_may_id                        character varying(5),                     -- partDContractNumberMayId
    ptd_cntrct_jun_id                        character varying(5),                     -- partDContractNumberJunId
    ptd_cntrct_jul_id                        character varying(5),                     -- partDContractNumberJulId
    ptd_cntrct_aug_id                        character varying(5),                     -- partDContractNumberAugId
    ptd_cntrct_sept_id                       character varying(5),                     -- partDContractNumberSeptId
    ptd_cntrct_oct_id                        character varying(5),                     -- partDContractNumberOctId
    ptd_cntrct_nov_id                        character varying(5),                     -- partDContractNumberNovId
    ptd_cntrct_dec_id                        character varying(5),                     -- partDContractNumberDecId
    ptd_pbp_jan_id                           character varying(3),                     -- partDPbpNumberJanId
    ptd_pbp_feb_id                           character varying(3),                     -- partDPbpNumberFebId
    ptd_pbp_mar_id                           character varying(3),                     -- partDPbpNumberMarId
    ptd_pbp_apr_id                           character varying(3),                     -- partDPbpNumberAprId
    ptd_pbp_may_id                           character varying(3),                     -- partDPbpNumberMayId
    ptd_pbp_jun_id                           character varying(3),                     -- partDPbpNumberJunId
    ptd_pbp_jul_id                           character varying(3),                     -- partDPbpNumberJulId
    ptd_pbp_aug_id                           character varying(3),                     -- partDPbpNumberAugId
    ptd_pbp_sept_id                          character varying(3),                     -- partDPbpNumberSeptId
    ptd_pbp_oct_id                           character varying(3),                     -- partDPbpNumberOctId
    ptd_pbp_nov_id                           character varying(3),                     -- partDPbpNumberNovId
    ptd_pbp_dec_id                           character varying(3),                     -- partDPbpNumberDecId
    ptd_sgmt_jan_id                          character varying(3),                     -- partDSegmentNumberJanId
    ptd_sgmt_feb_id                          character varying(3),                     -- partDSegmentNumberFebId
    ptd_sgmt_mar_id                          character varying(3),                     -- partDSegmentNumberMarId
    ptd_sgmt_apr_id                          character varying(3),                     -- partDSegmentNumberAprId
    ptd_sgmt_may_id                          character varying(3),                     -- partDSegmentNumberMayId
    ptd_sgmt_jun_id                          character varying(3),                     -- partDSegmentNumberJunId
    ptd_sgmt_jul_id                          character varying(3),                     -- partDSegmentNumberJulId
    ptd_sgmt_aug_id                          character varying(3),                     -- partDSegmentNumberAugId
    ptd_sgmt_sept_id                         character varying(3),                     -- partDSegmentNumberSeptId
    ptd_sgmt_oct_id                          character varying(3),                     -- partDSegmentNumberOctId
    ptd_sgmt_nov_id                          character varying(3),                     -- partDSegmentNumberNovId
    ptd_sgmt_dec_id                          character varying(3),                     -- partDSegmentNumberDecId
    rds_jan_ind                              character(1),                             -- partDRetireeDrugSubsidyJanInd
    rds_feb_ind                              character(1),                             -- partDRetireeDrugSubsidyFebInd
    rds_mar_ind                              character(1),                             -- partDRetireeDrugSubsidyMarInd
    rds_apr_ind                              character(1),                             -- partDRetireeDrugSubsidyAprInd
    rds_may_ind                              character(1),                             -- partDRetireeDrugSubsidyMayInd
    rds_jun_ind                              character(1),                             -- partDRetireeDrugSubsidyJunInd
    rds_jul_ind                              character(1),                             -- partDRetireeDrugSubsidyJulInd
    rds_aug_ind                              character(1),                             -- partDRetireeDrugSubsidyAugInd
    rds_sept_ind                             character(1),                             -- partDRetireeDrugSubsidySeptInd
    rds_oct_ind                              character(1),                             -- partDRetireeDrugSubsidyOctInd
    rds_nov_ind                              character(1),                             -- partDRetireeDrugSubsidyNovInd
    rds_dec_ind                              character(1),                             -- partDRetireeDrugSubsidyDecInd
    meta_dual_elgbl_stus_jan_cd              character varying(2),                     -- medicaidDualEligibilityJanCode
    meta_dual_elgbl_stus_feb_cd              character varying(2),                     -- medicaidDualEligibilityFebCode
    meta_dual_elgbl_stus_mar_cd              character varying(2),                     -- medicaidDualEligibilityMarCode
    meta_dual_elgbl_stus_apr_cd              character varying(2),                     -- medicaidDualEligibilityAprCode
    meta_dual_elgbl_stus_may_cd              character varying(2),                     -- medicaidDualEligibilityMayCode
    meta_dual_elgbl_stus_jun_cd              character varying(2),                     -- medicaidDualEligibilityJunCode
    meta_dual_elgbl_stus_jul_cd              character varying(2),                     -- medicaidDualEligibilityJulCode
    meta_dual_elgbl_stus_aug_cd              character varying(2),                     -- medicaidDualEligibilityAugCode
    meta_dual_elgbl_stus_sept_cd             character varying(2),                     -- medicaidDualEligibilitySeptCode
    meta_dual_elgbl_stus_oct_cd              character varying(2),                     -- medicaidDualEligibilityOctCode
    meta_dual_elgbl_stus_nov_cd              character varying(2),                     -- medicaidDualEligibilityNovCode
    meta_dual_elgbl_stus_dec_cd              character varying(2),                     -- medicaidDualEligibilityDecCode
    cst_shr_grp_jan_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupJanCode
    cst_shr_grp_feb_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupFebCode
    cst_shr_grp_mar_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupMarCode
    cst_shr_grp_apr_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupAprCode
    cst_shr_grp_may_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupMayCode
    cst_shr_grp_jun_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupJunCode
    cst_shr_grp_jul_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupJulCode
    cst_shr_grp_aug_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupAugCode
    cst_shr_grp_sept_cd                      character varying(2),                     -- partDLowIncomeCostShareGroupSeptCode
    cst_shr_grp_oct_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupOctCode
    cst_shr_grp_nov_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupNovCode
    cst_shr_grp_dec_cd                       character varying(2),                     -- partDLowIncomeCostShareGroupDecCode
    drvd_line_1_adr                          character varying(40),                    -- derivedMailingAddress1
    drvd_line_2_adr                          character varying(40),                    -- derivedMailingAddress2
    drvd_line_3_adr                          character varying(40),                    -- derivedMailingAddress3
    drvd_line_4_adr                          character varying(40),                    -- derivedMailingAddress4
    drvd_line_5_adr                          character varying(40),                    -- derivedMailingAddress5
    drvd_line_6_adr                          character varying(40),                    -- derivedMailingAddress6
    city_name                                character varying(100),                   -- derivedCityName
    state_cd                                 character varying(2),                     -- derivedStateCode
    state_cnty_zip_cd                        character varying(9),                     -- derivedZipCode
    bene_link_key                            bigint,                                   -- beneLinkKey
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint beneficiaries_pkey primary key (bene_id)
)


create table beneficiaries_history (
    beneficiary_history_id                   bigint not null,                          -- beneficiaryHistoryId
    bene_id                                  bigint not null,                          -- beneficiaryId
    bene_crnt_hic_num                        character varying(64) not null,           -- hicn
    hicn_unhashed                            character varying(11),                    -- hicnUnhashed
    mbi_num                                  character varying(11),                    -- medicareBeneficiaryId
    mbi_hash                                 character varying(64),                    -- mbiHash
    mbi_efctv_bgn_dt                         date,                                     -- mbiEffectiveDate
    mbi_efctv_end_dt                         date,                                     -- mbiObsoleteDate
    bene_sex_ident_cd                        character(1) not null,                    -- sex
    bene_birth_dt                            date not null,                            -- birthDate
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint beneficiaries_history_pkey primary key (beneficiary_history_id)
)


create table beneficiaries_history_invalid_beneficiaries (
    beneficiary_history_id                   bigint not null,                          -- beneficiaryHistoryId
    bene_id                                  bigint,                                   -- beneficiaryId
    bene_crnt_hic_num                        character varying(64) not null,           -- hicn
    hicn_unhashed                            character varying(11),                    -- hicnUnhashed
    mbi_num                                  character varying(11),                    -- medicareBeneficiaryId
    bene_sex_ident_cd                        character(1) not null,                    -- sex
    bene_birth_dt                            date not null,                            -- birthDate
    constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (beneficiary_history_id)
)


create table beneficiary_monthly (
    bene_id                                  bigint not null,                          -- parentBeneficiary
    year_month                               date not null,                            -- yearMonth
    partd_contract_number_id                 character varying(5),                     -- partDContractNumberId
    partc_contract_number_id                 character varying(5),                     -- partCContractNumberId
    fips_state_cnty_code                     character varying(5),                     -- fipsStateCntyCode
    medicare_status_code                     character varying(2),                     -- medicareStatusCode
    entitlement_buy_in_ind                   character(1),                             -- entitlementBuyInInd
    hmo_indicator_ind                        character(1),                             -- hmoIndicatorInd
    partc_pbp_number_id                      character varying(3),                     -- partCPbpNumberId
    partc_plan_type_code                     character varying(3),                     -- partCPlanTypeCode   
    partd_pbp_number_id                      character varying(3),                     -- partDPbpNumberId
    partd_segment_number_id                  character varying(3),                     -- partDSegmentNumberId
    partd_retiree_drug_subsidy_ind           character(1),                             -- partDRetireeDrugSubsidyInd
    partd_low_income_cost_share_group_code   character varying(2),                     -- partDLowIncomeCostShareGroupCode
    medicaid_dual_eligibility_code           character varying(2),                     -- medicaidDualEligibilityCode
    constraint beneficiary_monthly_pkey primary key (bene_id, year_month)
)


create table loaded_batches (
    loaded_batch_id                          bigint not null,                          -- loadedBatchId
    loaded_file_id                           bigint not null,                          -- loadedFileId
    beneficiaries                            character varying(20000) not null,        -- beneficiaries
    created                                  timestamp with time zone not null,        -- created
    constraint loaded_batches_pkey primary key (loaded_batch_id)
)


create table loaded_files (
    loaded_file_id                           bigint not null,                          -- loadedFileId
    rif_type                                 character varying(48) not null,           -- rifType
    created                                  timestamp with time zone not null,        -- created
    constraint loaded_files_pkey primary key (loaded_file_id)
)


create table medicare_beneficiaryid_history (
    medicare_beneficiaryid_key               bigint not null,                          -- medicareBeneficiaryIdKey
    bene_id                                  bigint,                                   -- beneficiaryId
    bene_clm_acnt_num                        character varying(9),                     -- claimAccountNumber
    bene_ident_cd                            character varying(2),                     -- beneficiaryIdCode
    bene_crnt_rec_ind_id                     integer,                                  -- mbiCrntRecIndId
    mbi_sqnc_num                             smallint,                                 -- mbiSequenceNumber
    mbi_num                                  character varying(11),                    -- medicareBeneficiaryId
    mbi_efctv_bgn_dt                         date,                                     -- mbiEffectiveDate
    mbi_efctv_end_dt                         date,                                     -- mbiEndDate
    mbi_bgn_rsn_cd                           character varying(5),                     -- mbiEffectiveReasonCode
    mbi_end_rsn_cd                           character varying(5),                     -- mbiEndReasonCode
    mbi_card_rqst_dt                         date,                                     -- mbiCardRequestDate
    creat_user_id                            character varying(30),                    -- mbiAddUser
    creat_ts                                 timestamp without time zone,              -- mbiAddDate
    updt_user_id                             character varying(30),                    -- mbiUpdateUser
    updt_ts                                  timestamp without time zone,              -- mbiUpdateDate
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint medicare_beneficiaryid_history_pkey primary key (medicare_beneficiaryid_key)
)


create table medicare_beneficiaryid_history_invalid_beneficiaries (
    medicare_beneficiaryid_key               bigint not null,                          -- medicareBeneficiaryIdKey
    bene_id                                  bigint,                                   -- beneficiaryId
    bene_clm_acnt_num                        character varying(9),                     -- claimAccountNumber
    bene_ident_cd                            character varying(2),                     -- beneficiaryIdCode
    bene_crnt_rec_ind_id                     integer,                                  -- mbiCrntRecIndId
    mbi_sqnc_num                             smallint,                                 -- mbiSequenceNumber
    mbi_num                                  character varying(11),                    -- medicareBeneficiaryId
    mbi_efctv_bgn_dt                         date,                                     -- mbiEffectiveDate
    mbi_efctv_end_dt                         date,                                     -- mbiEndDate
    mbi_bgn_rsn_cd                           character varying(5),                     -- mbiEffectiveReasonCode
    mbi_end_rsn_cd                           character varying(5),                     -- mbiEndReasonCode
    mbi_card_rqst_dt                         date,                                     -- mbiCardRequestDate
    creat_user_id                            character varying(30),                    -- mbiAddUser
    creat_ts                                 timestamp without time zone,              -- mbiAddDate
    updt_user_id                             character varying(30),                    -- mbiUpdateUser
    updt_ts                                  timestamp without time zone,              -- mbiUpdateDate
    constraint medicare_beneficiaryid_history_invalid_beneficiaries_pkey primary key (medicare_beneficiaryid_key)
)


create table carrier_claims (
    clm_id                                   bigint not null,                          -- claimId
	bene_id                                  bigint not null,                          -- beneficiaryId
	clm_grp_id                               bigint not null,                          -- claimGroupId
	clm_clncl_tril_num                       character varying(8),                     -- clinicalTrialNumber
	clm_disp_cd                              character varying(2) not null,            -- claimDispositionCode
	clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
	clm_from_dt                              date not null,                            -- dateFrom
	clm_thru_dt                              date not null,                            -- dateThrough
	carr_clm_cntl_num                        character varying(23),                    -- claimCarrierControlNumber
	carr_clm_entry_cd                        character(1) not null,                    -- claimEntryCode
	carr_clm_hcpcs_yr_cd                     character(1),                             -- hcpcsYearCode
	carr_clm_pmt_dnl_cd                      character varying(2) not null,            -- paymentDenialCode
	carr_clm_prvdr_asgnmt_ind_sw             character(1),                             -- providerAssignmentIndicator
	carr_clm_rfrng_pin_num                   character varying(14) not null,           -- referringProviderIdNumber
	carr_num                                 character varying(5) not null,            -- carrierNumber
	final_action                             character(1) not null,                    -- finalAction
	nch_clm_alowd_amt                        numeric(10,2) not null,                   -- allowedChargeAmount
    nch_clm_sbmtd_chrg_amt                   numeric(10,2) not null,                   -- submittedChargeAmount
	nch_clm_bene_pmt_amt                     numeric(10,2) not null,                   -- beneficiaryPaymentAmount
	nch_clm_bene_ptb_ddctbl_amt              numeric(10,2) not null,                   -- beneficiaryPartBDeductAmount
	nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
	nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
	nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
	nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
	prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
	prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
	rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount
	rfr_physn_npi                            character varying(12),                    -- referringPhysicianNpi
	rfr_physn_upin                           character varying(12),                    -- referringPhysicianUpin
	icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
	icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
	icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
	icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
	icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
	icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
	icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
	icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
	icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
	icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
	icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
	icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
	icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
	icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
	icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
	icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
	icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
	icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
	icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
	icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
	icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
	icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
	icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
	icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
	last_updated                             timestamp with time zone,                 -- lastupdated
    constraint carrier_claims_pkey primary key (clm_id)
)


create table carrier_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber
    line_pmt_amt                             numeric(10,2) not null,                   -- paymentAmount
    line_1st_expns_dt                        date,                                     -- firstExpenseDate
    line_alowd_chrg_amt                      numeric(10,2) not null,                   -- allowedChargeAmount
    line_bene_pmt_amt                        numeric(10,2) not null,                   -- beneficiaryPaymentAmount
    line_bene_prmry_pyr_cd                   character(1),                             -- primaryPayerCode
    line_bene_ptb_ddctbl_amt                 numeric(10,2) not null,                   -- beneficiaryPartBDeductAmount
    line_cms_type_srvc_cd                    character(1) not null,                    -- cmsServiceTypeCode
    line_coinsrnc_amt                        numeric(10,2) not null,                   -- coinsuranceAmount
    line_hct_hgb_rslt_num                    numeric(4,1) not null,                    -- hctHgbTestResult
    line_hct_hgb_type_cd                     character varying(2),                     -- hctHgbTestTypeCode
    line_icd_dgns_cd                         character varying(7),                     -- diagnosisCode
    line_icd_dgns_vrsn_cd                    character(1),                             -- diagnosisCodeVersion
    line_last_expns_dt                       date,                                     -- lastExpenseDate
    line_ndc_cd                              character varying(11),                    -- nationalDrugCode
    line_place_of_srvc_cd                    character varying(2) not null,            -- placeOfServiceCode
    line_pmt_80_100_cd                       character(1),                             -- paymentCode
    line_prcsg_ind_cd                        character varying(2),                     -- processingIndicatorCode
    line_sbmtd_chrg_amt                      numeric(10,2) not null,                   -- submittedChargeAmount
    line_service_deductible                  character(1),                             -- serviceDeductibleCode
    line_srvc_cnt                            smallint not null,                        -- serviceCount
    dmerc_line_mtus_cd                       character(1),                             -- mtusCode
    betos_cd                                 character varying(3),                     -- betosCode
    carr_line_ansthsa_unit_cnt               smallint not null,                        -- anesthesiaUnitCount
    carr_line_clia_lab_num                   character varying(10),                    -- cliaLabNumber
    carr_line_prcng_lclty_cd                 character varying(2) not null,            -- linePricingLocalityCode
    carr_line_prvdr_type_cd                  character(1) not null,                    -- providerTypeCode
    carr_line_rdcd_pmt_phys_astn_c           character(1) not null,                    -- reducedPaymentPhysicianAsstCode
    carr_line_rx_num                         character varying(30),                    -- rxNumber
    carr_prfrng_pin_num                      character varying(15) not null,           -- performingProviderIdNumber
    dmerc_line_mtus_cnt                      smallint not null,                        -- mtusCount
    hcpcs_1st_mdfr_cd                        character varying(5),                     -- hcpcsInitialModifierCode
    hcpcs_2nd_mdfr_cd                        character varying(5),                     -- hcpcsSecondModifierCode
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    hpsa_scrcty_ind_cd                       character(1),                             -- hpsaScarcityCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    org_npi_num                              character varying(10),                    -- organizationNpi
    prf_physn_npi                            character varying(12),                    -- performingPhysicianNpi
    prf_physn_upin                           character varying(12),                    -- performingPhysicianUpin
    prtcptng_ind_cd                          character(1),                             -- providerParticipatingIndCode
    prvdr_spclty                             character varying(3),                     -- providerSpecialityCode
    prvdr_state_cd                           character varying(2),                     -- providerStateCode
    prvdr_zip                                character varying(9),                     -- providerZipCode
    prvdr_tax_num                            character varying(10) not null,           -- providerTaxNumber  
    rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount

    constraint carrier_claim_lines_pkey primary key (parent_claim, clm_line_num)
)


create table dme_claims (
    clm_id                                   bigint not null,                          -- claimId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    clm_disp_cd                              character varying(2) not null,            -- claimDispositionCode
    clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
    clm_clncl_tril_num                       character varying(8),                     -- clinicalTrialNumber
    clm_from_dt                              date not null,                            -- dateFrom
    clm_thru_dt                              date not null,                            -- dateThrough
    carr_num                                 character varying(5) not null,            -- carrierNumber
    carr_clm_cntl_num                        character varying(23),                    -- claimCarrierControlNumber
    carr_clm_entry_cd                        character(1) not null,                    -- claimEntryCode
    carr_clm_prvdr_asgnmt_ind_sw             character(1) not null,                    -- providerAssignmentIndicator
    carr_clm_hcpcs_yr_cd                     character(1),                             -- hcpcsYearCode
    carr_clm_pmt_dnl_cd                      character varying(2) not null,            -- paymentDenialCode
    alowd_chrg_amt                           numeric(10,2) not null,                   -- allowedChargeAmount
    sbmtd_chrg_amt                           numeric(10,2) not null,                   -- submittedChargeAmount
    bene_ptb_ddctbl_amt                      numeric(10,2) not null,                   -- beneficiaryPartBDeductAmount
    bene_pmt_amt                             numeric(10,2) not null,                   -- beneficiaryPaymentAmount
    nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
    nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
    nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
    prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
    rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount
    rfr_physn_npi                            character varying(12),                    -- referringPhysicianNpi
    rfr_physn_upin                           character varying(12),                    -- referringPhysicianUpin
    final_action                             character(1) not null,                    -- finalAction
    icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
    icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
    icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
    icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
    icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
    icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
    icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
    icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
    icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
    icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
    icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
    icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
    icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
    icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
    icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
    icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
    icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
    icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
    icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
    icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
    icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
    icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
    icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
    icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint dme_claims_pkey primary key (clm_id)
)


create table dme_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber
    line_pmt_amt                             numeric(10,2) not null,                   -- paymentAmount
    line_sbmtd_chrg_amt                      numeric(10,2) not null,                   -- submittedChargeAmount
    line_alowd_chrg_amt                      numeric(10,2) not null,                   -- allowedChargeAmount
    line_bene_ptb_ddctbl_amt                 numeric(10,2) not null,                   -- beneficiaryPartBDeductAmount
    line_bene_pmt_amt                        numeric(10,2) not null,                   -- beneficiaryPaymentAmount
    line_ndc_cd                              character varying(11),                    -- nationalDrugCode
    line_cms_type_srvc_cd                    character(1) not null,                    -- cmsServiceTypeCode
    line_coinsrnc_amt                        numeric(10,2) not null,                   -- coinsuranceAmount
    line_icd_dgns_cd                         character varying(7),                     -- diagnosisCode
    line_icd_dgns_vrsn_cd                    character(1),                             -- diagnosisCodeVersion
    line_1st_expns_dt                        date,                                     -- firstExpenseDate
    line_hct_hgb_rslt_num                    numeric(3,1) not null,                    -- hctHgbTestResult
    line_hct_hgb_type_cd                     character varying(2),                     -- hctHgbTestTypeCode
    line_last_expns_dt                       date,                                     -- lastExpenseDate
    line_pmt_80_100_cd                       character(1),                             -- paymentCode
    line_place_of_srvc_cd                    character varying(2) not null,            -- placeOfServiceCode
    line_prmry_alowd_chrg_amt                numeric(10,2) not null,                   -- primaryPayerAllowedChargeAmount
    line_bene_prmry_pyr_cd                   character(1),                             -- primaryPayerCode
    line_prcsg_ind_cd                        character varying(2),                     -- processingIndicatorCode
    line_dme_prchs_price_amt                 numeric(10,2) not null,                   -- purchasePriceAmount
    line_srvc_cnt                            smallint not null,                        -- serviceCount
    line_service_deductible                  character(1),                             -- serviceDeductibleCode
    betos_cd                                 character varying(3),                     -- betosCode
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    hcpcs_4th_mdfr_cd                        character varying(5),                     -- hcpcsFourthModifierCode
    hcpcs_1st_mdfr_cd                        character varying(5),                     -- hcpcsInitialModifierCode
    hcpcs_2nd_mdfr_cd                        character varying(5),                     -- hcpcsSecondModifierCode
    hcpcs_3rd_mdfr_cd                        character varying(5),                     -- hcpcsThirdModifierCode
    dmerc_line_mtus_cd                       character(1),                             -- mtusCode
    dmerc_line_mtus_cnt                      smallint not null,                        -- mtusCount
    dmerc_line_prcng_state_cd                character varying(2),                     -- pricingStateCode
    dmerc_line_scrn_svgs_amt                 numeric(10,2),                            -- screenSavingsAmount
    dmerc_line_supplr_type_cd                character(1),                             -- supplierTypeCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    prvdr_num                                character varying(10),                    -- providerBillingNumber
    prvdr_npi                                character varying(12),                    -- providerNPI
    prvdr_spclty                             character varying(3),                     -- providerSpecialityCode
    prvdr_state_cd                           character varying(2) not null,            -- providerStateCode
    prvdr_tax_num                            character varying(10) not null,           -- providerTaxNumber   
    prtcptng_ind_cd                          character(1),                             -- providerParticipatingIndCode
    rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount
    constraint dme_claim_lines_pkey primary key (parent_claim, clm_line_num)
)


create table hha_claims (
    clm_id                                   bigint not null,                          -- claimId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
    clm_from_dt                              date not null,                            -- dateFrom
    clm_thru_dt                              date not null,                            -- dateThrough
    clm_admsn_dt                             date,                                     -- careStartDate
    clm_fac_type_cd                          character(1) not null,                    -- claimFacilityTypeCode
    clm_freq_cd                              character(1) not null,                    -- claimFrequencyCode
    clm_hha_lupa_ind_cd                      character(1),                             -- claimLUPACode
    clm_hha_rfrl_cd                          character(1),                             -- claimReferralCode
    clm_hha_tot_visit_cnt                    smallint not null,                        -- totalVisitCount
    clm_mdcr_non_pmt_rsn_cd                  character varying(2),                     -- claimNonPaymentReasonCode
    clm_pps_ind_cd                           character(1) not null,                    -- prospectivePaymentCode
    clm_srvc_clsfctn_type_cd                 character(1) not null,                    -- claimServiceClassificationTypeCode
    fi_clm_proc_dt                           date,                                     -- fiscalIntermediaryClaimProcessDate
    fi_doc_clm_cntl_num                      character varying(23),                    -- fiDocumentClaimControlNumber
    fi_num                                   character varying(5),                     -- fiscalIntermediaryNumber
    fi_orig_clm_cntl_num                     character varying(23),                    -- fiOriginalClaimControlNumber
    final_action                             character(1) not null,                    -- finalAction
    fst_dgns_e_cd                            character varying(7),                     -- diagnosisExternalFirstCode
    fst_dgns_e_vrsn_cd                       character(1),                             -- diagnosisExternalFirstCodeVersion
    nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
    nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
    nch_prmry_pyr_cd                         character(1),                             -- claimPrimaryPayerCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
    org_npi_num                              character varying(10),                    -- organizationNpi
    prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
    prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
    prvdr_num                                character varying(9) not null,            -- providerNumber
    prvdr_state_cd                           character varying(2) not null,            -- providerStateCode
    ptnt_dschrg_stus_cd                      character varying(2) not null,            -- patientDischargeStatusCode
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    at_physn_npi                             character varying(10),                    -- attendingPhysicianNpi
    at_physn_upin                            character varying(9),                     -- attendingPhysicianUpin
    icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
    icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
    icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
    icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
    icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
    icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
    icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
    icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
    icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
    icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
    icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
    icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
    icd_dgns_cd13                            character varying(7),                     -- diagnosis13Code
    icd_dgns_cd14                            character varying(7),                     -- diagnosis14Code
    icd_dgns_cd15                            character varying(7),                     -- diagnosis15Code
    icd_dgns_cd16                            character varying(7),                     -- diagnosis16Code
    icd_dgns_cd17                            character varying(7),                     -- diagnosis17Code
    icd_dgns_cd18                            character varying(7),                     -- diagnosis18Code
    icd_dgns_cd19                            character varying(7),                     -- diagnosis19Code
    icd_dgns_cd20                            character varying(7),                     -- diagnosis20Code
    icd_dgns_cd21                            character varying(7),                     -- diagnosis21Code
    icd_dgns_cd22                            character varying(7),                     -- diagnosis22Code
    icd_dgns_cd23                            character varying(7),                     -- diagnosis23Code
    icd_dgns_cd24                            character varying(7),                     -- diagnosis24Code
    icd_dgns_cd25                            character varying(7),                     -- diagnosis25Code
    icd_dgns_e_cd1                           character varying(7),                     -- diagnosisExternal1Code
    icd_dgns_e_cd2                           character varying(7),                     -- diagnosisExternal2Code
    icd_dgns_e_cd3                           character varying(7),                     -- diagnosisExternal3Code
    icd_dgns_e_cd4                           character varying(7),                     -- diagnosisExternal4Code
    icd_dgns_e_cd5                           character varying(7),                     -- diagnosisExternal5Code
    icd_dgns_e_cd6                           character varying(7),                     -- diagnosisExternal6Code
    icd_dgns_e_cd7                           character varying(7),                     -- diagnosisExternal7Code
    icd_dgns_e_cd8                           character varying(7),                     -- diagnosisExternal8Code
    icd_dgns_e_cd9                           character varying(7),                     -- diagnosisExternal9Code
    icd_dgns_e_cd10                          character varying(7),                     -- diagnosisExternal10Code
    icd_dgns_e_cd11                          character varying(7),                     -- diagnosisExternal11Code
    icd_dgns_e_cd12                          character varying(7),                     -- diagnosisExternal12Code
    icd_dgns_e_vrsn_cd1                      character(1),                             -- diagnosisExternal1CodeVersion
    icd_dgns_e_vrsn_cd2                      character(1),                             -- diagnosisExternal2CodeVersion
    icd_dgns_e_vrsn_cd3                      character(1),                             -- diagnosisExternal3CodeVersion
    icd_dgns_e_vrsn_cd4                      character(1),                             -- diagnosisExternal4CodeVersion
    icd_dgns_e_vrsn_cd5                      character(1),                             -- diagnosisExternal5CodeVersion
    icd_dgns_e_vrsn_cd6                      character(1),                             -- diagnosisExternal6CodeVersion
    icd_dgns_e_vrsn_cd7                      character(1),                             -- diagnosisExternal7CodeVersion
    icd_dgns_e_vrsn_cd8                      character(1),                             -- diagnosisExternal8CodeVersion
    icd_dgns_e_vrsn_cd9                      character(1),                             -- diagnosisExternal9CodeVersion
    icd_dgns_e_vrsn_cd10                     character(1),                             -- diagnosisExternal10CodeVersion
    icd_dgns_e_vrsn_cd11                     character(1),                             -- diagnosisExternal11CodeVersion
    icd_dgns_e_vrsn_cd12                     character(1),                             -- diagnosisExternal12CodeVersion
    icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
    icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
    icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
    icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
    icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
    icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
    icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
    icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
    icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
    icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
    icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
    icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
    icd_dgns_vrsn_cd13                       character(1),                             -- diagnosis13CodeVersion
    icd_dgns_vrsn_cd14                       character(1),                             -- diagnosis14CodeVersion
    icd_dgns_vrsn_cd15                       character(1),                             -- diagnosis15CodeVersion
    icd_dgns_vrsn_cd16                       character(1),                             -- diagnosis16CodeVersion
    icd_dgns_vrsn_cd17                       character(1),                             -- diagnosis17CodeVersion
    icd_dgns_vrsn_cd18                       character(1),                             -- diagnosis18CodeVersion
    icd_dgns_vrsn_cd19                       character(1),                             -- diagnosis19CodeVersion
    icd_dgns_vrsn_cd20                       character(1),                             -- diagnosis20CodeVersion
    icd_dgns_vrsn_cd21                       character(1),                             -- diagnosis21CodeVersion
    icd_dgns_vrsn_cd22                       character(1),                             -- diagnosis22CodeVersion
    icd_dgns_vrsn_cd23                       character(1),                             -- diagnosis23CodeVersion
    icd_dgns_vrsn_cd24                       character(1),                             -- diagnosis24CodeVersion
    icd_dgns_vrsn_cd25                       character(1),                             -- diagnosis25CodeVersion
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint hha_claims_pkey primary key (clm_id)
)


create table hha_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber
    line_pmt_amt                             numeric(10,2) not null,                   -- paymentAmount
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    hcpcs_1st_mdfr_cd                        character varying(5),                     -- hcpcsInitialModifierCode
    hcpcs_2nd_mdfr_cd                        character varying(5),                     -- hcpcsSecondModifierCode
    rndrng_physn_npi                         character varying(12),                    -- revenueCenterRenderingPhysicianNPI
    rndrng_physn_upin                        character varying(12),                    -- revenueCenterRenderingPhysicianUPIN
    rev_cntr                                 character varying(4) not null,            -- revenueCenterCode
    rev_cntr_dt                              date,                                     -- revenueCenterDate
    rev_cntr_apc_hipps_cd                    character varying(5),                     -- apcOrHippsCode
    rev_cntr_ddctbl_coinsrnc_cd              character(1),                             -- deductibleCoinsuranceCd
    rev_cntr_ndc_qty_qlfr_cd                 character varying(2),                     -- nationalDrugCodeQualifierCode
    rev_cntr_ndc_qty                         smallint,                                 -- nationalDrugCodeQuantity
    rev_cntr_ncvrd_chrg_amt                  numeric(10,2) not null,                   -- nonCoveredChargeAmount
    rev_cntr_pmt_mthd_ind_cd                 character varying(2),                     -- paymentMethodCode
    rev_cntr_rate_amt                        numeric(10,2) not null,                   -- rateAmount
    rev_cntr_1st_ansi_cd                     character varying(5),                     -- revCntr1stAnsiCd
    rev_cntr_stus_ind_cd                     character varying(2),                     -- statusCode
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    rev_cntr_unit_cnt                        smallint not null,                        -- unitCount
    constraint hha_claim_lines_pkey primary key (parent_claim, clm_line_num)
)


create table hospice_claims (
    clm_id                                   bigint not null,                          -- claimId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    clm_fac_type_cd                          character(1) not null,                    -- claimFacilityTypeCode
    clm_freq_cd                              character(1) not null,                    -- claimFrequencyCode
    clm_from_dt                              date not null,                            -- dateFrom
    clm_thru_dt                              date not null,                            -- dateThrough
    clm_hospc_start_dt_id                    date,                                     -- claimHospiceStartDate
    clm_mdcr_non_pmt_rsn_cd                  character varying(2),                     -- claimNonPaymentReasonCode
    clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
    clm_srvc_clsfctn_type_cd                 character(1) not null,                    -- claimServiceClassificationTypeCode
    clm_utlztn_day_cnt                       smallint not null,                        -- utilizationDayCount
    at_physn_npi                             character varying(10),                    -- attendingPhysicianNpi
    at_physn_upin                            character varying(9),                     -- attendingPhysicianUpin
    bene_hospc_prd_cnt                       smallint,                                 -- hospicePeriodCount
    fi_clm_proc_dt                           date,                                     -- fiscalIntermediaryClaimProcessDate
    fi_doc_clm_cntl_num                      character varying(23),                    -- fiDocumentClaimControlNumber
    fi_num                                   character varying(5),                     -- fiscalIntermediaryNumber
    fi_orig_clm_cntl_num                     character varying(23),                    -- fiOriginalClaimControlNumber
    final_action                             character(1) not null,                    -- finalAction
    fst_dgns_e_cd                            character varying(7),                     -- diagnosisExternalFirstCode
    fst_dgns_e_vrsn_cd                       character(1),                             -- diagnosisExternalFirstCodeVersion
    nch_bene_dschrg_dt                       date,                                     -- beneficiaryDischargeDate
    nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
    nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
    nch_prmry_pyr_cd                         character(1),                             -- claimPrimaryPayerCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    nch_ptnt_status_ind_cd                   character(1),                             -- patientStatusCd
    nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
    org_npi_num                              character varying(10),                    -- organizationNpi
    prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
    prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
    prvdr_num                                character varying(9) not null,            -- providerNumber
    prvdr_state_cd                           character varying(2) not null,            -- providerStateCode
    ptnt_dschrg_stus_cd                      character varying(2) not null,            -- patientDischargeStatusCode
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
	icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
	icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
	icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
	icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
	icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
	icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
	icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
	icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
	icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
	icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
	icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
	icd_dgns_cd13                            character varying(7),                     -- diagnosis13Code
	icd_dgns_cd14                            character varying(7),                     -- diagnosis14Code
	icd_dgns_cd15                            character varying(7),                     -- diagnosis15Code
	icd_dgns_cd16                            character varying(7),                     -- diagnosis16Code
	icd_dgns_cd17                            character varying(7),                     -- diagnosis17Code
	icd_dgns_cd18                            character varying(7),                     -- diagnosis18Code
	icd_dgns_cd19                            character varying(7),                     -- diagnosis19Code
	icd_dgns_cd20                            character varying(7),                     -- diagnosis20Code
	icd_dgns_cd21                            character varying(7),                     -- diagnosis21Code
	icd_dgns_cd22                            character varying(7),                     -- diagnosis22Code
	icd_dgns_cd23                            character varying(7),                     -- diagnosis23Code
	icd_dgns_cd24                            character varying(7),                     -- diagnosis24Code
	icd_dgns_cd25                            character varying(7),                     -- diagnosis25Code
	icd_dgns_e_cd1                           character varying(7),                     -- diagnosisExternal1Code
	icd_dgns_e_cd2                           character varying(7),                     -- diagnosisExternal2Code
	icd_dgns_e_cd3                           character varying(7),                     -- diagnosisExternal3Code
	icd_dgns_e_cd4                           character varying(7),                     -- diagnosisExternal4Code
	icd_dgns_e_cd5                           character varying(7),                     -- diagnosisExternal5Code
	icd_dgns_e_cd6                           character varying(7),                     -- diagnosisExternal6Code
	icd_dgns_e_cd7                           character varying(7),                     -- diagnosisExternal7Code
	icd_dgns_e_cd8                           character varying(7),                     -- diagnosisExternal8Code
	icd_dgns_e_cd9                           character varying(7),                     -- diagnosisExternal9Code
	icd_dgns_e_cd10                          character varying(7),                     -- diagnosisExternal10Code
	icd_dgns_e_cd11                          character varying(7),                     -- diagnosisExternal11Code
	icd_dgns_e_cd12                          character varying(7),                     -- diagnosisExternal12Code
	icd_dgns_e_vrsn_cd1                      character(1),                             -- diagnosisExternal1CodeVersion
	icd_dgns_e_vrsn_cd2                      character(1),                             -- diagnosisExternal2CodeVersion
	icd_dgns_e_vrsn_cd3                      character(1),                             -- diagnosisExternal3CodeVersion
	icd_dgns_e_vrsn_cd4                      character(1),                             -- diagnosisExternal4CodeVersion
	icd_dgns_e_vrsn_cd5                      character(1),                             -- diagnosisExternal5CodeVersion
	icd_dgns_e_vrsn_cd6                      character(1),                             -- diagnosisExternal6CodeVersion
	icd_dgns_e_vrsn_cd7                      character(1),                             -- diagnosisExternal7CodeVersion
	icd_dgns_e_vrsn_cd8                      character(1),                             -- diagnosisExternal8CodeVersion
	icd_dgns_e_vrsn_cd9                      character(1),                             -- diagnosisExternal9CodeVersion
	icd_dgns_e_vrsn_cd10                     character(1),                             -- diagnosisExternal10CodeVersion
	icd_dgns_e_vrsn_cd11                     character(1),                             -- diagnosisExternal11CodeVersion
	icd_dgns_e_vrsn_cd12                     character(1),                             -- diagnosisExternal12CodeVersion
	icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
	icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
	icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
	icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
	icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
	icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
	icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
	icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
	icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
	icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
	icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
	icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
	icd_dgns_vrsn_cd13                       character(1),                             -- diagnosis13CodeVersion
	icd_dgns_vrsn_cd14                       character(1),                             -- diagnosis14CodeVersion
	icd_dgns_vrsn_cd15                       character(1),                             -- diagnosis15CodeVersion
	icd_dgns_vrsn_cd16                       character(1),                             -- diagnosis16CodeVersion
	icd_dgns_vrsn_cd17                       character(1),                             -- diagnosis17CodeVersion
	icd_dgns_vrsn_cd18                       character(1),                             -- diagnosis18CodeVersion
	icd_dgns_vrsn_cd19                       character(1),                             -- diagnosis19CodeVersion
	icd_dgns_vrsn_cd20                       character(1),                             -- diagnosis20CodeVersion
	icd_dgns_vrsn_cd21                       character(1),                             -- diagnosis21CodeVersion
	icd_dgns_vrsn_cd22                       character(1),                             -- diagnosis22CodeVersion
	icd_dgns_vrsn_cd23                       character(1),                             -- diagnosis23CodeVersion
	icd_dgns_vrsn_cd24                       character(1),                             -- diagnosis24CodeVersion
	icd_dgns_vrsn_cd25                       character(1),                             -- diagnosis25CodeVersion
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint hospice_claims_pkey primary key (clm_id)
)


create table hospice_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber
    line_pmt_amt                             numeric(10,2) not null,                   -- paymentAmount
    rev_cntr                                 character varying(4) not null,            -- revenueCenterCode
    rev_cntr_dt                              date,                                     -- revenueCenterDate
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    rev_cntr_unit_cnt                        smallint not null,                        -- unitCount
    rev_cntr_bene_pmt_amt                    numeric(10,2) not null,                   -- benficiaryPaymentAmount
    rev_cntr_ddctbl_coinsrnc_cd              character(1),                             -- deductibleCoinsuranceCd
    rev_cntr_ndc_qty_qlfr_cd                 character varying(2),                     -- nationalDrugCodeQualifierCode
    rev_cntr_ndc_qty                         smallint,                                 -- nationalDrugCodeQuantity
    rev_cntr_ncvrd_chrg_amt                  numeric(10,2),                            -- nonCoveredChargeAmount
    rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount
    rev_cntr_rate_amt                        numeric(10,2) not null,                   -- rateAmount
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    hcpcs_1st_mdfr_cd                        character varying(5),                     -- hcpcsInitialModifierCode
    hcpcs_2nd_mdfr_cd                        character varying(5),                     -- hcpcsSecondModifierCode
    rndrng_physn_npi                         character varying(12),                    -- revenueCenterRenderingPhysicianNPI
    rndrng_physn_upin                        character varying(12),                    -- revenueCenterRenderingPhysicianUPIN
    constraint hospice_claim_lines_pkey primary key (parent_claim, clm_line_num)
)


create table inpatient_claims (
	clm_id                                   bigint not null,                          -- claimId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    clm_admsn_dt                             date,                                     -- claimAdmissionDate
    clm_drg_cd                               character varying(3),                     -- diagnosisRelatedGroupCd
    clm_drg_outlier_stay_cd                  character(1) not null,                    -- diagnosisRelatedGroupOutlierStayCd
    clm_fac_type_cd                          character(1) not null,                    -- claimFacilityTypeCode
    clm_freq_cd                              character(1) not null,                    -- claimFrequencyCode
    clm_from_dt                              date not null,                            -- dateFrom
	clm_thru_dt                              date not null,                            -- dateThrough
	clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
    clm_ip_admsn_type_cd                     character(1) not null,                    -- admissionTypeCd
    clm_mco_pd_sw                            character(1),                             -- mcoPaidSw
    clm_mdcr_non_pmt_rsn_cd                  character varying(2),                     -- claimNonPaymentReasonCode
    clm_non_utlztn_days_cnt                  smallint not null,                        -- nonUtilizationDayCount
    clm_pass_thru_per_diem_amt               numeric(10,2) not null,                   -- passThruPerDiemAmount
    clm_pps_cptl_drg_wt_num                  numeric(7,4),                             -- claimPPSCapitalDrgWeightNumber
    clm_pps_cptl_dsprprtnt_shr_amt           numeric(10,2),                            -- claimPPSCapitalDisproportionateShareAmt
    clm_pps_cptl_excptn_amt                  numeric(10,2),                            -- claimPPSCapitalExceptionAmount
    clm_pps_cptl_fsp_amt                     numeric(10,2),                            -- claimPPSCapitalFSPAmount
    clm_pps_cptl_ime_amt                     numeric(10,2),                            -- claimPPSCapitalIMEAmount
    clm_pps_cptl_outlier_amt                 numeric(10,2),                            -- claimPPSCapitalOutlierAmount
    clm_pps_ind_cd                           character(1),                             -- prospectivePaymentCode
    clm_pps_old_cptl_hld_hrmls_amt           numeric(10,2),                            -- claimPPSOldCapitalHoldHarmlessAmount
    clm_src_ip_admsn_cd                      character(1),                             -- sourceAdmissionCd
    clm_srvc_clsfctn_type_cd                 character(1) not null,                    -- claimServiceClassificationTypeCode
    clm_tot_pps_cptl_amt                     numeric(10,2),                            -- claimTotalPPSCapitalAmount
    clm_uncompd_care_pmt_amt                 numeric(10,2),                            -- claimUncompensatedCareAmount
    clm_utlztn_day_cnt                       smallint not null,                        -- utilizationDayCount
    admtg_dgns_cd                            character varying(7),                     -- diagnosisAdmittingCode
    admtg_dgns_vrsn_cd                       character(1),                             -- diagnosisAdmittingCodeVersion
    at_physn_npi                             character varying(10),                    -- attendingPhysicianNpi
    at_physn_upin                            character varying(9),                     -- attendingPhysicianUpin
    bene_lrd_used_cnt                        numeric,                                  -- lifetimeReservedDaysUsedCount
    bene_tot_coinsrnc_days_cnt               smallint not null,                        -- coinsuranceDayCount
    claim_query_code                         character(1) not null,                    -- claimQueryCode
    dsh_op_clm_val_amt                       numeric(10,2),                            -- disproportionateShareAmount
    fi_clm_actn_cd                           character(1),                             -- fiscalIntermediaryClaimActionCode
    fi_clm_proc_dt                           date,                                     -- fiscalIntermediaryClaimProcessDate
    fi_doc_clm_cntl_num                      character varying(23),                    -- fiDocumentClaimControlNumber
    fi_num                                   character varying(5),                     -- fiscalIntermediaryNumber
    fi_orig_clm_cntl_num                     character varying(23),                    -- fiOriginalClaimControlNumber
    final_action                             character(1) not null,                    -- finalAction
    fst_dgns_e_cd                            character varying(7),                     -- diagnosisExternalFirstCode
    fst_dgns_e_vrsn_cd                       character(1),                             -- diagnosisExternalFirstCodeVersion
    ime_op_clm_val_amt                       numeric(10,2),                            -- indirectMedicalEducationAmount
    nch_actv_or_cvrd_lvl_care_thru           date,                                     -- coveredCareThoughDate
    nch_bene_blood_ddctbl_lblty_am           numeric(10,2) not null,                   -- bloodDeductibleLiabilityAmount
    nch_bene_dschrg_dt                       date,                                     -- beneficiaryDischargeDate
    nch_bene_ip_ddctbl_amt                   numeric(10,2) not null,                   -- deductibleAmount
    nch_bene_mdcr_bnfts_exhtd_dt_i           date,                                     -- medicareBenefitsExhaustedDate
    nch_bene_pta_coinsrnc_lblty_am           numeric(10,2) not null,                   -- partACoinsuranceLiabilityAmount
    nch_blood_pnts_frnshd_qty                smallint not null,                        -- bloodPintsFurnishedQty
    nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
    nch_drg_outlier_aprvd_pmt_amt            numeric(10,2),                            -- drgOutlierApprovedPaymentAmount
    nch_ip_ncvrd_chrg_amt                    numeric(10,2) not null,                   -- noncoveredCharge
    nch_ip_tot_ddctn_amt                     numeric(10,2) not null,                   -- totalDeductionAmount
    nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
    nch_prmry_pyr_cd                         character(1),                             -- claimPrimaryPayerCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    nch_profnl_cmpnt_chrg_amt                numeric(10,2) not null,                   -- professionalComponentCharge
    nch_ptnt_status_ind_cd                   character(1),                             -- patientStatusCd
    nch_vrfd_ncvrd_stay_from_dt              date,                                     -- noncoveredStayFromDate
    nch_vrfd_ncvrd_stay_thru_dt              date,                                     -- noncoveredStayThroughDate
    nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
    op_physn_npi                             character varying(10),                    -- operatingPhysicianNpi
    op_physn_upin                            character varying(9),                     -- operatingPhysicianUpin
    org_npi_num                              character varying(10),                    -- organizationNpi
    ot_physn_npi                             character varying(10),                    -- otherPhysicianNpi
    ot_physn_upin                            character varying(9),                     -- otherPhysicianUpin
    prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
    prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
    prvdr_num                                character varying(9) not null,            -- providerNumber
    prvdr_state_cd                           character varying(2) not null,            -- providerStateCode
    ptnt_dschrg_stus_cd                      character varying(2) not null,            -- patientDischargeStatusCode
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
	clm_e_poa_ind_sw1                        character(1),                             -- diagnosisExternal1PresentOnAdmissionCode
	clm_e_poa_ind_sw2                        character(1),                             -- diagnosisExternal2PresentOnAdmissionCode
	clm_e_poa_ind_sw3                        character(1),                             -- diagnosisExternal3PresentOnAdmissionCode
	clm_e_poa_ind_sw4                        character(1),                             -- diagnosisExternal4PresentOnAdmissionCode
	clm_e_poa_ind_sw5                        character(1),                             -- diagnosisExternal5PresentOnAdmissionCode
	clm_e_poa_ind_sw6                        character(1),                             -- diagnosisExternal6PresentOnAdmissionCode
	clm_e_poa_ind_sw7                        character(1),                             -- diagnosisExternal7PresentOnAdmissionCode
	clm_e_poa_ind_sw8                        character(1),                             -- diagnosisExternal8PresentOnAdmissionCode
	clm_e_poa_ind_sw9                        character(1),                             -- diagnosisExternal9PresentOnAdmissionCode     
	clm_e_poa_ind_sw10                       character(1),                             -- diagnosisExternal10PresentOnAdmissionCode
	clm_e_poa_ind_sw11                       character(1),                             -- diagnosisExternal11PresentOnAdmissionCode
	clm_e_poa_ind_sw12                       character(1),                             -- diagnosisExternal12PresentOnAdmissionCode
	clm_poa_ind_sw1                          character(1),                             -- diagnosis1PresentOnAdmissionCode
	clm_poa_ind_sw2                          character(1),                             -- diagnosis2PresentOnAdmissionCode
	clm_poa_ind_sw3                          character(1),                             -- diagnosis3PresentOnAdmissionCode
	clm_poa_ind_sw4                          character(1),                             -- diagnosis4PresentOnAdmissionCode
	clm_poa_ind_sw5                          character(1),                             -- diagnosis5PresentOnAdmissionCode
	clm_poa_ind_sw6                          character(1),                             -- diagnosis6PresentOnAdmissionCode
	clm_poa_ind_sw7                          character(1),                             -- diagnosis7PresentOnAdmissionCode
	clm_poa_ind_sw8                          character(1),                             -- diagnosis8PresentOnAdmissionCode
	clm_poa_ind_sw9                          character(1),                             -- diagnosis9PresentOnAdmissionCode
	clm_poa_ind_sw10                         character(1),                             -- diagnosis10PresentOnAdmissionCode
	clm_poa_ind_sw11                         character(1),                             -- diagnosis11PresentOnAdmissionCode
	clm_poa_ind_sw12                         character(1),                             -- diagnosis12PresentOnAdmissionCode
	clm_poa_ind_sw13                         character(1),                             -- diagnosis13PresentOnAdmissionCode
	clm_poa_ind_sw14                         character(1),                             -- diagnosis14PresentOnAdmissionCode
	clm_poa_ind_sw15                         character(1),                             -- diagnosis15PresentOnAdmissionCode
	clm_poa_ind_sw16                         character(1),                             -- diagnosis16PresentOnAdmissionCode
	clm_poa_ind_sw17                         character(1),                             -- diagnosis17PresentOnAdmissionCode
	clm_poa_ind_sw18                         character(1),                             -- diagnosis18PresentOnAdmissionCode
	clm_poa_ind_sw19                         character(1),                             -- diagnosis19PresentOnAdmissionCode
	clm_poa_ind_sw20                         character(1),                             -- diagnosis20PresentOnAdmissionCode
	clm_poa_ind_sw21                         character(1),                             -- diagnosis21PresentOnAdmissionCode
	clm_poa_ind_sw22                         character(1),                             -- diagnosis22PresentOnAdmissionCode
	clm_poa_ind_sw23                         character(1),                             -- diagnosis23PresentOnAdmissionCode
	clm_poa_ind_sw24                         character(1),                             -- diagnosis24PresentOnAdmissionCode
	clm_poa_ind_sw25                         character(1),                             -- diagnosis25PresentOnAdmissionCode
	icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
	icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
	icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
	icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
	icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
	icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
	icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
	icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
	icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
	icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
	icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
	icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
	icd_dgns_cd13                            character varying(7),                     -- diagnosis13Code
	icd_dgns_cd14                            character varying(7),                     -- diagnosis14Code
	icd_dgns_cd15                            character varying(7),                     -- diagnosis15Code
	icd_dgns_cd16                            character varying(7),                     -- diagnosis16Code
	icd_dgns_cd17                            character varying(7),                     -- diagnosis17Code
	icd_dgns_cd18                            character varying(7),                     -- diagnosis18Code
	icd_dgns_cd19                            character varying(7),                     -- diagnosis19Code
	icd_dgns_cd20                            character varying(7),                     -- diagnosis20Code
	icd_dgns_cd21                            character varying(7),                     -- diagnosis21Code
	icd_dgns_cd22                            character varying(7),                     -- diagnosis22Code
	icd_dgns_cd23                            character varying(7),                     -- diagnosis23Code
	icd_dgns_cd24                            character varying(7),                     -- diagnosis24Code
	icd_dgns_cd25                            character varying(7),                     -- diagnosis25Code
	icd_dgns_e_cd1                           character varying(7),                     -- diagnosisExternal1Code
	icd_dgns_e_cd2                           character varying(7),                     -- diagnosisExternal2Code
	icd_dgns_e_cd3                           character varying(7),                     -- diagnosisExternal3Code
	icd_dgns_e_cd4                           character varying(7),                     -- diagnosisExternal4Code
	icd_dgns_e_cd5                           character varying(7),                     -- diagnosisExternal5Code
	icd_dgns_e_cd6                           character varying(7),                     -- diagnosisExternal6Code
	icd_dgns_e_cd7                           character varying(7),                     -- diagnosisExternal7Code
	icd_dgns_e_cd8                           character varying(7),                     -- diagnosisExternal8Code
	icd_dgns_e_cd9                           character varying(7),                     -- diagnosisExternal9Code
	icd_dgns_e_cd10                          character varying(7),                     -- diagnosisExternal10Code
	icd_dgns_e_cd11                          character varying(7),                     -- diagnosisExternal11Code
	icd_dgns_e_cd12                          character varying(7),                     -- diagnosisExternal12Code
	icd_dgns_e_vrsn_cd1                      character(1),                             -- diagnosisExternal1CodeVersion
	icd_dgns_e_vrsn_cd2                      character(1),                             -- diagnosisExternal2CodeVersion
	icd_dgns_e_vrsn_cd3                      character(1),                             -- diagnosisExternal3CodeVersion
	icd_dgns_e_vrsn_cd4                      character(1),                             -- diagnosisExternal4CodeVersion
	icd_dgns_e_vrsn_cd5                      character(1),                             -- diagnosisExternal5CodeVersion
	icd_dgns_e_vrsn_cd6                      character(1),                             -- diagnosisExternal6CodeVersion
	icd_dgns_e_vrsn_cd7                      character(1),                             -- diagnosisExternal7CodeVersion
	icd_dgns_e_vrsn_cd8                      character(1),                             -- diagnosisExternal8CodeVersion
	icd_dgns_e_vrsn_cd9                      character(1),                             -- diagnosisExternal9CodeVersion
	icd_dgns_e_vrsn_cd10                     character(1),                             -- diagnosisExternal10CodeVersion
	icd_dgns_e_vrsn_cd11                     character(1),                             -- diagnosisExternal11CodeVersion
	icd_dgns_e_vrsn_cd12                     character(1),                             -- diagnosisExternal12CodeVersion
	icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
	icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
	icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
	icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
	icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
	icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
	icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
	icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
	icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
	icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
	icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
	icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
	icd_dgns_vrsn_cd13                       character(1),                             -- diagnosis13CodeVersion
	icd_dgns_vrsn_cd14                       character(1),                             -- diagnosis14CodeVersion
	icd_dgns_vrsn_cd15                       character(1),                             -- diagnosis15CodeVersion
	icd_dgns_vrsn_cd16                       character(1),                             -- diagnosis16CodeVersion
	icd_dgns_vrsn_cd17                       character(1),                             -- diagnosis17CodeVersion
	icd_dgns_vrsn_cd18                       character(1),                             -- diagnosis18CodeVersion
	icd_dgns_vrsn_cd19                       character(1),                             -- diagnosis19CodeVersion
	icd_dgns_vrsn_cd20                       character(1),                             -- diagnosis20CodeVersion
	icd_dgns_vrsn_cd21                       character(1),                             -- diagnosis21CodeVersion
	icd_dgns_vrsn_cd22                       character(1),                             -- diagnosis22CodeVersion
	icd_dgns_vrsn_cd23                       character(1),                             -- diagnosis23CodeVersion
	icd_dgns_vrsn_cd24                       character(1),                             -- diagnosis24CodeVersion
	icd_dgns_vrsn_cd25                       character(1),                             -- diagnosis25CodeVersion
	icd_prcdr_cd1                            character varying(7),                     -- procedure1Code
	icd_prcdr_cd2                            character varying(7),                     -- procedure2Code
	icd_prcdr_cd3                            character varying(7),                     -- procedure3Code
	icd_prcdr_cd4                            character varying(7),                     -- procedure4Code
	icd_prcdr_cd5                            character varying(7),                     -- procedure5Code
	icd_prcdr_cd6                            character varying(7),                     -- procedure6Code
	icd_prcdr_cd7                            character varying(7),                     -- procedure7Code
	icd_prcdr_cd8                            character varying(7),                     -- procedure8Code
	icd_prcdr_cd9                            character varying(7),                     -- procedure9Code
	icd_prcdr_cd10                           character varying(7),                     -- procedure10Code
	icd_prcdr_cd11                           character varying(7),                     -- procedure11Code
	icd_prcdr_cd12                           character varying(7),                     -- procedure12Code
	icd_prcdr_cd13                           character varying(7),                     -- procedure13Code
	icd_prcdr_cd14                           character varying(7),                     -- procedure14Code
	icd_prcdr_cd15                           character varying(7),                     -- procedure15Code
	icd_prcdr_cd16                           character varying(7),                     -- procedure16Code
	icd_prcdr_cd17                           character varying(7),                     -- procedure17Code
	icd_prcdr_cd18                           character varying(7),                     -- procedure18Code
	icd_prcdr_cd19                           character varying(7),                     -- procedure19Code
	icd_prcdr_cd20                           character varying(7),                     -- procedure20Code
	icd_prcdr_cd21                           character varying(7),                     -- procedure21Code
	icd_prcdr_cd22                           character varying(7),                     -- procedure22Code
	icd_prcdr_cd23                           character varying(7),                     -- procedure23Code
	icd_prcdr_cd24                           character varying(7),                     -- procedure24Code
	icd_prcdr_cd25                           character varying(7),                     -- procedure25Code
	icd_prcdr_vrsn_cd1                       character(1),                             -- procedure1CodeVersion
	icd_prcdr_vrsn_cd2                       character(1),                             -- procedure2CodeVersion
	icd_prcdr_vrsn_cd3                       character(1),                             -- procedure3CodeVersion
	icd_prcdr_vrsn_cd4                       character(1),                             -- procedure4CodeVersion
	icd_prcdr_vrsn_cd5                       character(1),                             -- procedure5CodeVersion
	icd_prcdr_vrsn_cd6                       character(1),                             -- procedure6CodeVersion
	icd_prcdr_vrsn_cd7                       character(1),                             -- procedure7CodeVersion
	icd_prcdr_vrsn_cd8                       character(1),                             -- procedure8CodeVersion
	icd_prcdr_vrsn_cd9                       character(1),                             -- procedure9CodeVersion
	icd_prcdr_vrsn_cd10                      character(1),                             -- procedure10CodeVersion
	icd_prcdr_vrsn_cd11                      character(1),                             -- procedure11CodeVersion
	icd_prcdr_vrsn_cd12                      character(1),                             -- procedure12CodeVersion
	icd_prcdr_vrsn_cd13                      character(1),                             -- procedure13CodeVersion
	icd_prcdr_vrsn_cd14                      character(1),                             -- procedure14CodeVersion
	icd_prcdr_vrsn_cd15                      character(1),                             -- procedure15CodeVersion
	icd_prcdr_vrsn_cd16                      character(1),                             -- procedure16CodeVersion
	icd_prcdr_vrsn_cd17                      character(1),                             -- procedure17CodeVersion
	icd_prcdr_vrsn_cd18                      character(1),                             -- procedure18CodeVersion
	icd_prcdr_vrsn_cd19                      character(1),                             -- procedure19CodeVersion
	icd_prcdr_vrsn_cd20                      character(1),                             -- procedure20CodeVersion
	icd_prcdr_vrsn_cd21                      character(1),                             -- procedure21CodeVersion
	icd_prcdr_vrsn_cd22                      character(1),                             -- procedure22CodeVersion
	icd_prcdr_vrsn_cd23                      character(1),                             -- procedure23CodeVersion
	icd_prcdr_vrsn_cd24                      character(1),                             -- procedure24CodeVersion
	icd_prcdr_vrsn_cd25                      character(1),                             -- procedure25CodeVersion
	prcdr_dt1                                date,                                     -- procedure1Date
	prcdr_dt2                                date,                                     -- procedure2Date
	prcdr_dt3                                date,                                     -- procedure3Date
	prcdr_dt4                                date,                                     -- procedure4Date
	prcdr_dt5                                date,                                     -- procedure5Date
	prcdr_dt6                                date,                                     -- procedure6Date
	prcdr_dt7                                date,                                     -- procedure7Date
	prcdr_dt8                                date,                                     -- procedure8Date
	prcdr_dt9                                date,                                     -- procedure9Date
	prcdr_dt10                               date,                                     -- procedure10Date
	prcdr_dt11                               date,                                     -- procedure11Date
	prcdr_dt12                               date,                                     -- procedure12Date
	prcdr_dt13                               date,                                     -- procedure13Date
	prcdr_dt14                               date,                                     -- procedure14Date
	prcdr_dt15                               date,                                     -- procedure15Date
	prcdr_dt16                               date,                                     -- procedure16Date
	prcdr_dt17                               date,                                     -- procedure17Date
	prcdr_dt18                               date,                                     -- procedure18Date
	prcdr_dt19                               date,                                     -- procedure19Date
	prcdr_dt20                               date,                                     -- procedure20Date
	prcdr_dt21                               date,                                     -- procedure21Date
	prcdr_dt22                               date,                                     -- procedure22Date
	prcdr_dt23                               date,                                     -- procedure23Date
	prcdr_dt24                               date,                                     -- procedure24Date
	prcdr_dt25                               date,                                     -- procedure25Date
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint inpatient_claims_pkey primary key (clm_id)
)


create table inpatient_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber
    rev_cntr_ddctbl_coinsrnc_cd              character(1),                             -- deductibleCoinsuranceCd
    rev_cntr_ndc_qty_qlfr_cd                 character varying(2),                     -- nationalDrugCodeQualifierCode
    rev_cntr_ndc_qty                         smallint,                                 -- nationalDrugCodeQuantity
    rev_cntr_ncvrd_chrg_amt                  numeric(10,2) not null,                   -- nonCoveredChargeAmount
    rev_cntr_rate_amt                        numeric(10,2) not null,                   -- rateAmount
    rev_cntr                                 character varying(4) not null,            -- revenueCenter
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    rev_cntr_unit_cnt                        smallint not null ,                       -- unitCount
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    rndrng_physn_npi                         character varying(12),                    -- revenueCenterRenderingPhysicianNPI
    rndrng_physn_upin                        character varying(12),                    -- revenueCenterRenderingPhysicianUPIN
    constraint inpatient_claim_lines_pkey primary key (parent_claim, clm_line_num)
)


create table outpatient_claims (
    clm_id                                   bigint not null,                          -- claimId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    clm_fac_type_cd                          character(1) not null,                    -- claimFacilityTypeCode
    clm_freq_cd                              character(1) not null,                    -- claimFrequencyCode
    clm_from_dt                              date not null,                            -- dateFrom
    clm_thru_dt                              date not null,                            -- dateThrough
    clm_mco_pd_sw                            character(1),                             -- mcoPaidSw
    clm_mdcr_non_pmt_rsn_cd                  character varying(2),                     -- claimNonPaymentReasonCode
    clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
    clm_srvc_clsfctn_type_cd                 character(1) not null,                    -- claimServiceClassificationTypeCode
    at_physn_npi                             character varying(10),                    -- attendingPhysicianNpi
    at_physn_upin                            character varying(9),                     -- attendingPhysicianUpin
    claim_query_code                         character(1) not null,                    -- claimQueryCode
    fi_clm_proc_dt                           date,                                     -- fiscalIntermediaryClaimProcessDate
    fi_doc_clm_cntl_num                      character varying(23),                    -- fiDocumentClaimControlNumber
    fi_num                                   character varying(5),                     -- fiscalIntermediaryNumber
    fi_orig_clm_cntl_num                     character varying(23),                    -- fiOriginalClaimControlNumber
    final_action                             character(1) not null,                    -- finalAction
    fst_dgns_e_cd                            character varying(7),                     -- diagnosisExternalFirstCode
    fst_dgns_e_vrsn_cd                       character(1),                             -- diagnosisExternalFirstCodeVersion
    line_bene_pmt_amt                        numeric(10,2) not null,                   -- beneficiaryPaymentAmount
    line_coinsrnc_amt                        numeric(10,2) not null,                   -- coinsuranceAmount
    nch_bene_blood_ddctbl_lblty_am           numeric(10,2) not null,                   -- bloodDeductibleLiabilityAmount
    nch_bene_ip_ddctbl_amt                   numeric(10,2) not null,                   -- deductibleAmount
    nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
    nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
    nch_prmry_pyr_cd                         character(1),                             -- claimPrimaryPayerCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    nch_profnl_cmpnt_chrg_amt                numeric(10,2) not null,                   -- professionalComponentCharge
    nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
    op_physn_npi                             character varying(10),                    -- operatingPhysicianNpi
    op_physn_upin                            character varying(9),                     -- operatingPhysicianUpin
    org_npi_num                              character varying(10),                    -- organizationNpi
    ot_physn_npi                             character varying(10),                    -- otherPhysicianNpi
    ot_physn_upin                            character varying(9),                     -- otherPhysicianUpin
    prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
    prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
    prvdr_num                                character varying(9) not null,            -- providerNumber
    prvdr_state_cd                           character varying(2) not null,            -- providerStateCode
    ptnt_dschrg_stus_cd                      character varying(2),                     -- patientDischargeStatusCode
    rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    rsn_visit_cd1                            character varying(7),                     -- diagnosisAdmission1Code
    rsn_visit_vrsn_cd1                       character(1),                             -- diagnosisAdmission1CodeVersion
    rsn_visit_cd2                            character varying(7),                     -- diagnosisAdmission2Code
    rsn_visit_vrsn_cd2                       character(1),                             -- diagnosisAdmission2CodeVersion
    rsn_visit_cd3                            character varying(7),                     -- diagnosisAdmission3Code
    rsn_visit_vrsn_cd3                       character(1),                             -- diagnosisAdmission3CodeVersion
    icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
    icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
    icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
    icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
    icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
    icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
    icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
    icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
    icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
    icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
    icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
    icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
    icd_dgns_cd13                            character varying(7),                     -- diagnosis13Code
    icd_dgns_cd14                            character varying(7),                     -- diagnosis14Code
    icd_dgns_cd15                            character varying(7),                     -- diagnosis15Code
    icd_dgns_cd16                            character varying(7),                     -- diagnosis16Code
    icd_dgns_cd17                            character varying(7),                     -- diagnosis17Code
    icd_dgns_cd18                            character varying(7),                     -- diagnosis18Code
    icd_dgns_cd19                            character varying(7),                     -- diagnosis19Code
    icd_dgns_cd20                            character varying(7),                     -- diagnosis20Code
    icd_dgns_cd21                            character varying(7),                     -- diagnosis21Code
    icd_dgns_cd22                            character varying(7),                     -- diagnosis22Code
    icd_dgns_cd23                            character varying(7),                     -- diagnosis23Code
    icd_dgns_cd24                            character varying(7),                     -- diagnosis24Code
    icd_dgns_cd25                            character varying(7),                     -- diagnosis25Code
    icd_dgns_e_cd1                           character varying(7),                     -- diagnosisExternal1Code
    icd_dgns_e_cd2                           character varying(7),                     -- diagnosisExternal2Code
    icd_dgns_e_cd3                           character varying(7),                     -- diagnosisExternal3Code
    icd_dgns_e_cd4                           character varying(7),                     -- diagnosisExternal4Code
    icd_dgns_e_cd5                           character varying(7),                     -- diagnosisExternal5Code
    icd_dgns_e_cd6                           character varying(7),                     -- diagnosisExternal6Code
    icd_dgns_e_cd7                           character varying(7),                     -- diagnosisExternal7Code
    icd_dgns_e_cd8                           character varying(7),                     -- diagnosisExternal8Code
    icd_dgns_e_cd9                           character varying(7),                     -- diagnosisExternal9Code
    icd_dgns_e_cd10                          character varying(7),                     -- diagnosisExternal10Code
    icd_dgns_e_cd11                          character varying(7),                     -- diagnosisExternal11Code
    icd_dgns_e_cd12                          character varying(7),                     -- diagnosisExternal12Code
    icd_dgns_e_vrsn_cd1                      character(1),                             -- diagnosisExternal1CodeVersion
    icd_dgns_e_vrsn_cd2                      character(1),                             -- diagnosisExternal2CodeVersion
    icd_dgns_e_vrsn_cd3                      character(1),                             -- diagnosisExternal3CodeVersion
    icd_dgns_e_vrsn_cd4                      character(1),                             -- diagnosisExternal4CodeVersion
    icd_dgns_e_vrsn_cd5                      character(1),                             -- diagnosisExternal5CodeVersion
    icd_dgns_e_vrsn_cd6                      character(1),                             -- diagnosisExternal6CodeVersion
    icd_dgns_e_vrsn_cd7                      character(1),                             -- diagnosisExternal7CodeVersion
    icd_dgns_e_vrsn_cd8                      character(1),                             -- diagnosisExternal8CodeVersion
    icd_dgns_e_vrsn_cd9                      character(1),                             -- diagnosisExternal9CodeVersion
    icd_dgns_e_vrsn_cd10                     character(1),                             -- diagnosisExternal10CodeVersion
    icd_dgns_e_vrsn_cd11                     character(1),                             -- diagnosisExternal11CodeVersion
    icd_dgns_e_vrsn_cd12                     character(1),                             -- diagnosisExternal12CodeVersion
    icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
    icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
    icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
    icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
    icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
    icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
    icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
    icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
    icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
    icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
    icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
    icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
    icd_dgns_vrsn_cd13                       character(1),                             -- diagnosis13CodeVersion
    icd_dgns_vrsn_cd14                       character(1),                             -- diagnosis14CodeVersion
    icd_dgns_vrsn_cd15                       character(1),                             -- diagnosis15CodeVersion
    icd_dgns_vrsn_cd16                       character(1),                             -- diagnosis16CodeVersion
    icd_dgns_vrsn_cd17                       character(1),                             -- diagnosis17CodeVersion
    icd_dgns_vrsn_cd18                       character(1),                             -- diagnosis18CodeVersion
    icd_dgns_vrsn_cd19                       character(1),                             -- diagnosis19CodeVersion
    icd_dgns_vrsn_cd20                       character(1),                             -- diagnosis20CodeVersion
    icd_dgns_vrsn_cd21                       character(1),                             -- diagnosis21CodeVersion
    icd_dgns_vrsn_cd22                       character(1),                             -- diagnosis22CodeVersion
    icd_dgns_vrsn_cd23                       character(1),                             -- diagnosis23CodeVersion
    icd_dgns_vrsn_cd24                       character(1),                             -- diagnosis24CodeVersion
    icd_dgns_vrsn_cd25                       character(1),                             -- diagnosis25CodeVersion
    icd_prcdr_cd1                            character varying(7),                     -- procedure1Code
    icd_prcdr_cd2                            character varying(7),                     -- procedure2Code
    icd_prcdr_cd3                            character varying(7),                     -- procedure3Code
    icd_prcdr_cd4                            character varying(7),                     -- procedure4Code
    icd_prcdr_cd5                            character varying(7),                     -- procedure5Code
    icd_prcdr_cd6                            character varying(7),                     -- procedure6Code
    icd_prcdr_cd7                            character varying(7),                     -- procedure7Code
    icd_prcdr_cd8                            character varying(7),                     -- procedure8Code
    icd_prcdr_cd9                            character varying(7),                     -- procedure9Code
    icd_prcdr_cd10                           character varying(7),                     -- procedure10Code
    icd_prcdr_cd11                           character varying(7),                     -- procedure11Code
    icd_prcdr_cd12                           character varying(7),                     -- procedure12Code
    icd_prcdr_cd13                           character varying(7),                     -- procedure13Code
    icd_prcdr_cd14                           character varying(7),                     -- procedure14Code
    icd_prcdr_cd15                           character varying(7),                     -- procedure15Code
    icd_prcdr_cd16                           character varying(7),                     -- procedure16Code
    icd_prcdr_cd17                           character varying(7),                     -- procedure17Code
    icd_prcdr_cd18                           character varying(7),                     -- procedure18Code
    icd_prcdr_cd19                           character varying(7),                     -- procedure19Code
    icd_prcdr_cd20                           character varying(7),                     -- procedure20Code
    icd_prcdr_cd21                           character varying(7),                     -- procedure21Code
    icd_prcdr_cd22                           character varying(7),                     -- procedure22Code
    icd_prcdr_cd23                           character varying(7),                     -- procedure23Code
    icd_prcdr_cd24                           character varying(7),                     -- procedure24Code
    icd_prcdr_cd25                           character varying(7),                     -- procedure25Code
    icd_prcdr_vrsn_cd1                       character(1),                             -- procedure1CodeVersion
    icd_prcdr_vrsn_cd2                       character(1),                             -- procedure2CodeVersion
    icd_prcdr_vrsn_cd3                       character(1),                             -- procedure3CodeVersion
    icd_prcdr_vrsn_cd4                       character(1),                             -- procedure4CodeVersion
    icd_prcdr_vrsn_cd5                       character(1),                             -- procedure5CodeVersion
    icd_prcdr_vrsn_cd6                       character(1),                             -- procedure6CodeVersion
    icd_prcdr_vrsn_cd7                       character(1),                             -- procedure7CodeVersion
    icd_prcdr_vrsn_cd8                       character(1),                             -- procedure8CodeVersion
    icd_prcdr_vrsn_cd9                       character(1),                             -- procedure9CodeVersion
    icd_prcdr_vrsn_cd10                      character(1),                             -- procedure10CodeVersion
    icd_prcdr_vrsn_cd11                      character(1),                             -- procedure11CodeVersion
    icd_prcdr_vrsn_cd12                      character(1),                             -- procedure12CodeVersion
    icd_prcdr_vrsn_cd13                      character(1),                             -- procedure13CodeVersion
    icd_prcdr_vrsn_cd14                      character(1),                             -- procedure14CodeVersion
    icd_prcdr_vrsn_cd15                      character(1),                             -- procedure15CodeVersion
    icd_prcdr_vrsn_cd16                      character(1),                             -- procedure16CodeVersion
    icd_prcdr_vrsn_cd17                      character(1),                             -- procedure17CodeVersion
    icd_prcdr_vrsn_cd18                      character(1),                             -- procedure18CodeVersion
    icd_prcdr_vrsn_cd19                      character(1),                             -- procedure19CodeVersion
    icd_prcdr_vrsn_cd20                      character(1),                             -- procedure20CodeVersion
    icd_prcdr_vrsn_cd21                      character(1),                             -- procedure21CodeVersion
    icd_prcdr_vrsn_cd22                      character(1),                             -- procedure22CodeVersion
    icd_prcdr_vrsn_cd23                      character(1),                             -- procedure23CodeVersion
    icd_prcdr_vrsn_cd24                      character(1),                             -- procedure24CodeVersion
    icd_prcdr_vrsn_cd25                      character(1),                             -- procedure25CodeVersion
    prcdr_dt1                                date,                                     -- procedure1Date
    prcdr_dt2                                date,                                     -- procedure2Date
    prcdr_dt3                                date,                                     -- procedure3Date
    prcdr_dt4                                date,                                     -- procedure4Date
    prcdr_dt5                                date,                                     -- procedure5Date
    prcdr_dt6                                date,                                     -- procedure6Date
    prcdr_dt7                                date,                                     -- procedure7Date
    prcdr_dt8                                date,                                     -- procedure8Date
    prcdr_dt9                                date,                                     -- procedure9Date
    prcdr_dt10                               date,                                     -- procedure10Date
    prcdr_dt11                               date,                                     -- procedure11Date
    prcdr_dt12                               date,                                     -- procedure12Date
    prcdr_dt13                               date,                                     -- procedure13Date
    prcdr_dt14                               date,                                     -- procedure14Date
    prcdr_dt15                               date,                                     -- procedure15Date
    prcdr_dt16                               date,                                     -- procedure16Date
    prcdr_dt17                               date,                                     -- procedure17Date
    prcdr_dt18                               date,                                     -- procedure18Date
    prcdr_dt19                               date,                                     -- procedure19Date
    prcdr_dt20                               date,                                     -- procedure20Date
    prcdr_dt21                               date,                                     -- procedure21Date
    prcdr_dt22                               date,                                     -- procedure22Date
    prcdr_dt23                               date,                                     -- procedure23Date
    prcdr_dt24                               date,                                     -- procedure24Date
    prcdr_dt25                               date,                                     -- procedure25Date    
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint outpatient_claims_pkey primary key (clm_id)
)


create table outpatient_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber 
    line_ndc_cd                              character varying(24),                    -- nationalDrugCode
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    hcpcs_1st_mdfr_cd                        character varying(5),                     -- hcpcsInitialModifierCode
    hcpcs_2nd_mdfr_cd                        character varying(5),                     -- hcpcsSecondModifierCode
    rndrng_physn_npi                         character varying(12),                    -- revenueCenterRenderingPhysicianNPI
    rndrng_physn_upin                        character varying(12),                    -- revenueCenterRenderingPhysicianUPIN
    rev_cntr                                 character varying(4) not null,            -- revenueCenterCode
    rev_cntr_dt                              date,                                     -- revenueCenterDate
    rev_cntr_pmt_amt                         numeric(10,2) not null,                   -- paymentAmount
    rev_cntr_apc_hipps_cd                    character varying(5),                     -- apcOrHippsCode
    rev_cntr_bene_pmt_amt                    numeric(10,2) not null,                   -- benficiaryPaymentAmount
    rev_cntr_blood_ddctbl_amt                numeric(10,2) not null,                   -- bloodDeductibleAmount
    rev_cntr_cash_ddctbl_amt                 numeric(10,2) not null,                   -- cashDeductibleAmount
    rev_cntr_dscnt_ind_cd                    character(1),                             -- discountCode
    rev_cntr_1st_msp_pd_amt                  numeric(10,2) not null,                   -- firstMspPaidAmount
    rev_cntr_ndc_qty_qlfr_cd                 character varying(2),                     -- nationalDrugCodeQualifierCode
    rev_cntr_ndc_qty                         smallint,                                 -- nationalDrugCodeQuantity
    rev_cntr_ncvrd_chrg_amt                  numeric(10,2) not null,                   -- nonCoveredChargeAmount
    rev_cntr_otaf_pmt_cd                     character(1),                             -- obligationToAcceptAsFullPaymentCode
    rev_cntr_packg_ind_cd                    character(1),                             -- packagingCode
    rev_cntr_ptnt_rspnsblty_pmt              numeric(10,2) not null,                   -- patientResponsibilityAmount
    rev_cntr_pmt_mthd_ind_cd                 character varying(2),                     -- paymentMethodCode
    rev_cntr_prvdr_pmt_amt                   numeric(10,2) not null,                   -- providerPaymentAmount
    rev_cntr_rate_amt                        numeric(10,2) not null,                   -- rateAmount
    rev_cntr_rdcd_coinsrnc_amt               numeric(10,2) not null,                   -- reducedCoinsuranceAmount
    rev_cntr_1st_ansi_cd                     character varying(5),                     -- revCntr1stAnsiCd
    rev_cntr_2nd_ansi_cd                     character varying(5),                     -- revCntr2ndAnsiCd
    rev_cntr_3rd_ansi_cd                     character varying(5),                     -- revCntr3rdAnsiCd
    rev_cntr_4th_ansi_cd                     character varying(5),                     -- revCntr4thAnsiCd
    rev_cntr_2nd_msp_pd_amt                  numeric(10,2) not null,                   -- secondMspPaidAmount
    rev_cntr_stus_ind_cd                     character varying(2),                     -- statusCode
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    rev_cntr_unit_cnt                        smallint not null,                        -- unitCount
    rev_cntr_coinsrnc_wge_adjstd_amt         numeric(10,2) not null,                   -- wageAdjustedCoinsuranceAmount
    constraint outpatient_claim_lines_pkey primary key (parent_claim, clm_line_num)
)

create table partd_events (
    clm_id                                   bigint not null,                          -- eventId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    adjstmt_dltn_cd                          character(1),                             -- adjustmentDeletionCode
    brnd_gnrc_cd                             character(1),                             -- brandGenericCode
    cmpnd_cd                                 integer not null,                         -- compoundCode
    ctstrphc_cvrg_cd                         character(1),                             -- catastrophicCoverageCode
    cvrd_d_plan_pd_amt                       numeric(10,2) not null,                   -- partDPlanCoveredPaidAmount
    daw_prod_slctn_cd                        character(1) not null,                    -- dispenseAsWrittenProductSelectionCode
    days_suply_num                           smallint not null,                        -- daysSupply
    drug_cvrg_stus_cd                        character(1) not null,                    -- drugCoverageStatusCode
    dspnsng_stus_cd                          character(1),                             -- dispensingStatusCode
    fill_num                                 smallint not null,                        -- fillNumber
    final_action                             character(1) not null,                    -- finalAction
    gdc_abv_oopt_amt                         numeric(10,2) not null,                   -- grossCostAboveOutOfPocketThreshold
    gdc_blw_oopt_amt                         numeric(10,2) not null,                   -- grossCostBelowOutOfPocketThreshold
    lics_amt                                 numeric(10,2) not null,                   -- lowIncomeSubsidyPaidAmount
    line_ndc_cd                              character varying(19) not null,           -- nationalDrugCode
    ncvrd_plan_pd_amt                        numeric(10,2) not null,                   -- partDPlanNonCoveredPaidAmount
    nstd_frmt_cd                             character(1),                             -- nonstandardFormatCode
    othr_troop_amt                           numeric(10,2) not null,                   -- otherTrueOutOfPocketPaidAmount
    pd_dt                                    date,                                     -- paymentDate
    phrmcy_srvc_type_cd                      character varying(2) not null,            -- pharmacyTypeCode
    plan_cntrct_rec_id                       character varying(5) not null,            -- planContractId
    plan_pbp_rec_num                         character varying(3) not null,            -- planBenefitPackageId
    plro_amt                                 numeric(10,2) not null,                   -- patientLiabilityReductionOtherPaidAmount
    prcng_excptn_cd                          character(1),                             -- pricingExceptionCode
    prscrbr_id                               character varying(15) not null,           -- prescriberId
    prscrbr_id_qlfyr_cd                      character varying(2) not null,            -- prescriberIdQualifierCode
    ptnt_pay_amt                             numeric(10,2) not null,                   -- patientPaidAmount
    ptnt_rsdnc_cd                            character varying(2) not null,            -- patientResidenceCode
    qty_dspnsd_num                           numeric(10,3) not null,                   -- quantityDispensed
    rptd_gap_dscnt_num                       numeric(10,2) not null,                   -- gapDiscountAmount
    rx_orgn_cd                               character(1),                             -- prescriptionOriginationCode
    rx_srvc_rfrnc_num                        bigint not null,                          -- prescriptionReferenceNumber
    srvc_dt                                  date not null,                            -- prescriptionFillDate
    srvc_prvdr_id                            character varying(15) not null,           -- serviceProviderId
    srvc_prvdr_id_qlfyr_cd                   character varying(2) not null,            -- serviceProviderIdQualiferCode
    submsn_clr_cd                            character varying(2),                     -- submissionClarificationCode
    tot_rx_cst_amt                           numeric(10,2) not null,                   -- totalPrescriptionCost
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint partd_events_pkey primary key (clm_id)
)


create table snf_claims (
    clm_id                                   bigint not null,                          -- claimId
    bene_id                                  bigint not null,                          -- beneficiaryId
    clm_grp_id                               bigint not null,                          -- claimGroupId
    clm_admsn_dt                             date,                                     -- claimAdmissionDate
    clm_drg_cd                               character varying(3),                     -- diagnosisRelatedGroupCd
    clm_fac_type_cd                          character(1) not null,                    -- claimFacilityTypeCode
    clm_freq_cd                              character(1) not null,                    -- claimFrequencyCode
    clm_from_dt                              date not null,                            -- dateFrom
    clm_thru_dt                              date not null,                            -- dateThrough
    clm_ip_admsn_type_cd                     character(1) not null,                    -- admissionTypeCd
    clm_mco_pd_sw                            character(1),                             -- mcoPaidSw
    clm_mdcr_non_pmt_rsn_cd                  character varying(2),                     -- claimNonPaymentReasonCode
    clm_non_utlztn_days_cnt                  smallint not null,                        -- nonUtilizationDayCount
    clm_pmt_amt                              numeric(10,2) not null,                   -- paymentAmount
    clm_pps_cptl_dsprprtnt_shr_amt           numeric(10,2),                            -- claimPPSCapitalDisproportionateShareAmt
    clm_pps_cptl_excptn_amt                  numeric(10,2),                            -- claimPPSCapitalExceptionAmount
    clm_pps_cptl_fsp_amt                     numeric(10,2),                            -- claimPPSCapitalFSPAmount
    clm_pps_cptl_ime_amt                     numeric(10,2),                            -- claimPPSCapitalIMEAmount
    clm_pps_cptl_outlier_amt                 numeric(10,2),                            -- claimPPSCapitalOutlierAmount
    clm_pps_ind_cd                           character(1),                             -- prospectivePaymentCode
    clm_pps_old_cptl_hld_hrmls_amt           numeric(10,2),                            -- claimPPSOldCapitalHoldHarmlessAmount
    clm_src_ip_admsn_cd                      character(1),                             -- sourceAdmissionCd
    clm_srvc_clsfctn_type_cd                 character(1) not null,                    -- claimServiceClassificationTypeCode
    clm_utlztn_day_cnt                       smallint not null,                        -- utilizationDayCount
    admtg_dgns_cd                            character varying(7),                     -- diagnosisAdmittingCode
    admtg_dgns_vrsn_cd                       character(1),                             -- diagnosisAdmittingCodeVersion
    at_physn_npi                             character varying(10),                    -- attendingPhysicianNpi
    at_physn_upin                            character varying(9),                     -- attendingPhysicianUpin
    bene_tot_coinsrnc_days_cnt               smallint not null,                        -- coinsuranceDayCount
    claim_query_code                         character(1) not null,                    -- claimQueryCode
    fi_clm_actn_cd                           character(1),                             -- fiscalIntermediaryClaimActionCode
    fi_clm_proc_dt                           date,                                     -- fiscalIntermediaryClaimProcessDate
    fi_doc_clm_cntl_num                      character varying(23),                    -- fiDocumentClaimControlNumber
    fi_num                                   character varying(5),                     -- fiscalIntermediaryNumber
    fi_orig_clm_cntl_num                     character varying(23),                    -- fiOriginalClaimControlNumber
    final_action                             character(1) not null,                    -- finalAction
    fst_dgns_e_cd                            character varying(7),                     -- diagnosisExternalFirstCode
    fst_dgns_e_vrsn_cd                       character(1),                             -- diagnosisExternalFirstCodeVersion
    nch_actv_or_cvrd_lvl_care_thru           date,                                     -- coveredCareThroughDate
    nch_bene_blood_ddctbl_lblty_am           numeric(10,2) not null,                   -- bloodDeductibleLiabilityAmount
    nch_bene_dschrg_dt                       date,                                     -- beneficiaryDischargeDate
    nch_bene_ip_ddctbl_amt                   numeric(10,2) not null,                   -- deductibleAmount
    nch_bene_mdcr_bnfts_exhtd_dt_i           date,                                     -- medicareBenefitsExhaustedDate
    nch_bene_pta_coinsrnc_lblty_am           numeric(10,2) not null,                   -- partACoinsuranceLiabilityAmount
    nch_blood_pnts_frnshd_qty                smallint not null,                        -- bloodPintsFurnishedQty
    nch_clm_type_cd                          character varying(2) not null,            -- claimTypeCode
    nch_ip_ncvrd_chrg_amt                    numeric(10,2) not null,                   -- noncoveredCharge
    nch_ip_tot_ddctn_amt                     numeric(10,2) not null,                   -- totalDeductionAmount
    nch_near_line_rec_ident_cd               character(1) not null,                    -- nearLineRecordIdCode
    nch_prmry_pyr_cd                         character(1),                             -- claimPrimaryPayerCode
    nch_prmry_pyr_clm_pd_amt                 numeric(10,2) not null,                   -- primaryPayerPaidAmount
    nch_ptnt_status_ind_cd                   character(1),                             -- patientStatusCd
    nch_qlfyd_stay_from_dt                   date,                                     -- qualifiedStayFromDate
    nch_qlfyd_stay_thru_dt                   date,                                     -- qualifiedStayThroughDate
    nch_vrfd_ncvrd_stay_from_dt              date,                                     -- noncoveredStayFromDate
    nch_vrfd_ncvrd_stay_thru_dt              date,                                     -- noncoveredStayThroughDate
    nch_wkly_proc_dt                         date not null,                            -- weeklyProcessDate
    op_physn_npi                             character varying(10),                    -- operatingPhysicianNpi
    op_physn_upin                            character varying(9),                     -- operatingPhysicianUpin
    org_npi_num                              character varying(10),                    -- organizationNpi
    ot_physn_npi                             character varying(10),                    -- otherPhysicianNpi
    ot_physn_upin                            character varying(9),                     -- otherPhysicianUpin
    prncpal_dgns_cd                          character varying(7),                     -- diagnosisPrincipalCode
    prncpal_dgns_vrsn_cd                     character(1),                             -- diagnosisPrincipalCodeVersion
    prvdr_num                                character varying(9) not null,            -- providerNumber
    prvdr_state_cd                           character varying(2) not null,            -- providerStateCode
    ptnt_dschrg_stus_cd                      character varying(2) not null,            -- patientDischargeStatusCode
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    icd_dgns_cd1                             character varying(7),                     -- diagnosis1Code
    icd_dgns_cd2                             character varying(7),                     -- diagnosis2Code
    icd_dgns_cd3                             character varying(7),                     -- diagnosis3Code
    icd_dgns_cd4                             character varying(7),                     -- diagnosis4Code
    icd_dgns_cd5                             character varying(7),                     -- diagnosis5Code
    icd_dgns_cd6                             character varying(7),                     -- diagnosis6Code
    icd_dgns_cd7                             character varying(7),                     -- diagnosis7Code
    icd_dgns_cd8                             character varying(7),                     -- diagnosis8Code
    icd_dgns_cd9                             character varying(7),                     -- diagnosis9Code
    icd_dgns_cd10                            character varying(7),                     -- diagnosis10Code
    icd_dgns_cd11                            character varying(7),                     -- diagnosis11Code
    icd_dgns_cd12                            character varying(7),                     -- diagnosis12Code
    icd_dgns_cd13                            character varying(7),                     -- diagnosis13Code
    icd_dgns_cd14                            character varying(7),                     -- diagnosis14Code
    icd_dgns_cd15                            character varying(7),                     -- diagnosis15Code
    icd_dgns_cd16                            character varying(7),                     -- diagnosis16Code
    icd_dgns_cd17                            character varying(7),                     -- diagnosis17Code
    icd_dgns_cd18                            character varying(7),                     -- diagnosis18Code
    icd_dgns_cd19                            character varying(7),                     -- diagnosis19Code
    icd_dgns_cd20                            character varying(7),                     -- diagnosis20Code
    icd_dgns_cd21                            character varying(7),                     -- diagnosis21Code
    icd_dgns_cd22                            character varying(7),                     -- diagnosis22Code
    icd_dgns_cd23                            character varying(7),                     -- diagnosis23Code
    icd_dgns_cd24                            character varying(7),                     -- diagnosis24Code
    icd_dgns_cd25                            character varying(7),                     -- diagnosis25Code
    icd_dgns_e_cd1                           character varying(7),                     -- diagnosisExternal1Code
    icd_dgns_e_cd2                           character varying(7),                     -- diagnosisExternal2Code
    icd_dgns_e_cd3                           character varying(7),                     -- diagnosisExternal3Code
    icd_dgns_e_cd4                           character varying(7),                     -- diagnosisExternal4Code
    icd_dgns_e_cd5                           character varying(7),                     -- diagnosisExternal5Code
    icd_dgns_e_cd6                           character varying(7),                     -- diagnosisExternal6Code
    icd_dgns_e_cd7                           character varying(7),                     -- diagnosisExternal7Code
    icd_dgns_e_cd8                           character varying(7),                     -- diagnosisExternal8Code
    icd_dgns_e_cd9                           character varying(7),                     -- diagnosisExternal9Code
    icd_dgns_e_cd10                          character varying(7),                     -- diagnosisExternal10Code
    icd_dgns_e_cd11                          character varying(7),                     -- diagnosisExternal11Code
    icd_dgns_e_cd12                          character varying(7),                     -- diagnosisExternal12Code
    icd_dgns_e_vrsn_cd1                      character(1),                             -- diagnosisExternal1CodeVersion
    icd_dgns_e_vrsn_cd2                      character(1),                             -- diagnosisExternal2CodeVersion
    icd_dgns_e_vrsn_cd3                      character(1),                             -- diagnosisExternal3CodeVersion
    icd_dgns_e_vrsn_cd4                      character(1),                             -- diagnosisExternal4CodeVersion
    icd_dgns_e_vrsn_cd5                      character(1),                             -- diagnosisExternal5CodeVersion
    icd_dgns_e_vrsn_cd6                      character(1),                             -- diagnosisExternal6CodeVersion
    icd_dgns_e_vrsn_cd7                      character(1),                             -- diagnosisExternal7CodeVersion
    icd_dgns_e_vrsn_cd8                      character(1),                             -- diagnosisExternal8CodeVersion
    icd_dgns_e_vrsn_cd9                      character(1),                             -- diagnosisExternal9CodeVersion
    icd_dgns_e_vrsn_cd10                     character(1),                             -- diagnosisExternal10CodeVersion
    icd_dgns_e_vrsn_cd11                     character(1),                             -- diagnosisExternal11CodeVersion
    icd_dgns_e_vrsn_cd12                     character(1),                             -- diagnosisExternal12CodeVersion
    icd_dgns_vrsn_cd1                        character(1),                             -- diagnosis1CodeVersion
    icd_dgns_vrsn_cd2                        character(1),                             -- diagnosis2CodeVersion
    icd_dgns_vrsn_cd3                        character(1),                             -- diagnosis3CodeVersion
    icd_dgns_vrsn_cd4                        character(1),                             -- diagnosis4CodeVersion
    icd_dgns_vrsn_cd5                        character(1),                             -- diagnosis5CodeVersion
    icd_dgns_vrsn_cd6                        character(1),                             -- diagnosis6CodeVersion
    icd_dgns_vrsn_cd7                        character(1),                             -- diagnosis7CodeVersion
    icd_dgns_vrsn_cd8                        character(1),                             -- diagnosis8CodeVersion
    icd_dgns_vrsn_cd9                        character(1),                             -- diagnosis9CodeVersion
    icd_dgns_vrsn_cd10                       character(1),                             -- diagnosis10CodeVersion
    icd_dgns_vrsn_cd11                       character(1),                             -- diagnosis11CodeVersion
    icd_dgns_vrsn_cd12                       character(1),                             -- diagnosis12CodeVersion
    icd_dgns_vrsn_cd13                       character(1),                             -- diagnosis13CodeVersion
    icd_dgns_vrsn_cd14                       character(1),                             -- diagnosis14CodeVersion
    icd_dgns_vrsn_cd15                       character(1),                             -- diagnosis15CodeVersion
    icd_dgns_vrsn_cd16                       character(1),                             -- diagnosis16CodeVersion
    icd_dgns_vrsn_cd17                       character(1),                             -- diagnosis17CodeVersion
    icd_dgns_vrsn_cd18                       character(1),                             -- diagnosis18CodeVersion
    icd_dgns_vrsn_cd19                       character(1),                             -- diagnosis19CodeVersion
    icd_dgns_vrsn_cd20                       character(1),                             -- diagnosis20CodeVersion
    icd_dgns_vrsn_cd21                       character(1),                             -- diagnosis21CodeVersion
    icd_dgns_vrsn_cd22                       character(1),                             -- diagnosis22CodeVersion
    icd_dgns_vrsn_cd23                       character(1),                             -- diagnosis23CodeVersion
    icd_dgns_vrsn_cd24                       character(1),                             -- diagnosis24CodeVersion
    icd_dgns_vrsn_cd25                       character(1),                             -- diagnosis25CodeVersion
    icd_prcdr_cd1                            character varying(7),                     -- procedure1Code
    icd_prcdr_cd2                            character varying(7),                     -- procedure2Code
    icd_prcdr_cd3                            character varying(7),                     -- procedure3Code
    icd_prcdr_cd4                            character varying(7),                     -- procedure4Code
    icd_prcdr_cd5                            character varying(7),                     -- procedure5Code
    icd_prcdr_cd6                            character varying(7),                     -- procedure6Code
    icd_prcdr_cd7                            character varying(7),                     -- procedure7Code
    icd_prcdr_cd8                            character varying(7),                     -- procedure8Code
    icd_prcdr_cd9                            character varying(7),                     -- procedure9Code
    icd_prcdr_cd10                           character varying(7),                     -- procedure10Code
    icd_prcdr_cd11                           character varying(7),                     -- procedure11Code
    icd_prcdr_cd12                           character varying(7),                     -- procedure12Code
    icd_prcdr_cd13                           character varying(7),                     -- procedure13Code
    icd_prcdr_cd14                           character varying(7),                     -- procedure14Code
    icd_prcdr_cd15                           character varying(7),                     -- procedure15Code
    icd_prcdr_cd16                           character varying(7),                     -- procedure16Code
    icd_prcdr_cd17                           character varying(7),                     -- procedure17Code
    icd_prcdr_cd18                           character varying(7),                     -- procedure18Code
    icd_prcdr_cd19                           character varying(7),                     -- procedure19Code
    icd_prcdr_cd20                           character varying(7),                     -- procedure20Code
    icd_prcdr_cd21                           character varying(7),                     -- procedure21Code
    icd_prcdr_cd22                           character varying(7),                     -- procedure22Code
    icd_prcdr_cd23                           character varying(7),                     -- procedure23Code
    icd_prcdr_cd24                           character varying(7),                     -- procedure24Code
    icd_prcdr_cd25                           character varying(7),                     -- procedure25Code
    icd_prcdr_vrsn_cd1                       character(1),                             -- procedure1CodeVersion
    icd_prcdr_vrsn_cd2                       character(1),                             -- procedure2CodeVersion
    icd_prcdr_vrsn_cd3                       character(1),                             -- procedure3CodeVersion
    icd_prcdr_vrsn_cd4                       character(1),                             -- procedure4CodeVersion
    icd_prcdr_vrsn_cd5                       character(1),                             -- procedure5CodeVersion
    icd_prcdr_vrsn_cd6                       character(1),                             -- procedure6CodeVersion
    icd_prcdr_vrsn_cd7                       character(1),                             -- procedure7CodeVersion
    icd_prcdr_vrsn_cd8                       character(1),                             -- procedure8CodeVersion
    icd_prcdr_vrsn_cd9                       character(1),                             -- procedure9CodeVersion
    icd_prcdr_vrsn_cd10                      character(1),                             -- procedure10CodeVersion
    icd_prcdr_vrsn_cd11                      character(1),                             -- procedure11CodeVersion
    icd_prcdr_vrsn_cd12                      character(1),                             -- procedure12CodeVersion
    icd_prcdr_vrsn_cd13                      character(1),                             -- procedure13CodeVersion
    icd_prcdr_vrsn_cd14                      character(1),                             -- procedure14CodeVersion
    icd_prcdr_vrsn_cd15                      character(1),                             -- procedure15CodeVersion
    icd_prcdr_vrsn_cd16                      character(1),                             -- procedure16CodeVersion
    icd_prcdr_vrsn_cd17                      character(1),                             -- procedure17CodeVersion
    icd_prcdr_vrsn_cd18                      character(1),                             -- procedure18CodeVersion
    icd_prcdr_vrsn_cd19                      character(1),                             -- procedure19CodeVersion
    icd_prcdr_vrsn_cd20                      character(1),                             -- procedure20CodeVersion
    icd_prcdr_vrsn_cd21                      character(1),                             -- procedure21CodeVersion
    icd_prcdr_vrsn_cd22                      character(1),                             -- procedure22CodeVersion
    icd_prcdr_vrsn_cd23                      character(1),                             -- procedure23CodeVersion
    icd_prcdr_vrsn_cd24                      character(1),                             -- procedure24CodeVersion
    icd_prcdr_vrsn_cd25                      character(1),                             -- procedure25CodeVersion
    prcdr_dt1                                date,                                     -- procedure1Date
    prcdr_dt2                                date,                                     -- procedure2Date
    prcdr_dt3                                date,                                     -- procedure3Date
    prcdr_dt4                                date,                                     -- procedure4Date
    prcdr_dt5                                date,                                     -- procedure5Date
    prcdr_dt6                                date,                                     -- procedure6Date
    prcdr_dt7                                date,                                     -- procedure7Date
    prcdr_dt8                                date,                                     -- procedure8Date
    prcdr_dt9                                date,                                     -- procedure9Date
    prcdr_dt10                               date,                                     -- procedure10Date
    prcdr_dt11                               date,                                     -- procedure11Date
    prcdr_dt12                               date,                                     -- procedure12Date
    prcdr_dt13                               date,                                     -- procedure13Date
    prcdr_dt14                               date,                                     -- procedure14Date
    prcdr_dt15                               date,                                     -- procedure15Date
    prcdr_dt16                               date,                                     -- procedure16Date
    prcdr_dt17                               date,                                     -- procedure17Date
    prcdr_dt18                               date,                                     -- procedure18Date
    prcdr_dt19                               date,                                     -- procedure19Date
    prcdr_dt20                               date,                                     -- procedure20Date
    prcdr_dt21                               date,                                     -- procedure21Date
    prcdr_dt22                               date,                                     -- procedure22Date
    prcdr_dt23                               date,                                     -- procedure23Date
    prcdr_dt24                               date,                                     -- procedure24Date
    prcdr_dt25                               date,                                     -- procedure25Date    
    last_updated                             timestamp with time zone,                 -- lastupdated
    constraint snf_claims_pkey primary key (clm_id)
)


create table snf_claim_lines (
    parent_claim                             bigint not null,                          -- parentClaim
    clm_line_num                             smallint not null,                        -- lineNumber
    hcpcs_cd                                 character varying(5),                     -- hcpcsCode
    rev_cntr                                 character varying(4) not null,            -- revenueCenter
    rev_cntr_ndc_qty_qlfr_cd                 character varying(2),                     -- nationalDrugCodeQualifierCode
    rev_cntr_ndc_qty                         smallint,                                 -- nationalDrugCodeQuantity
    rev_cntr_ncvrd_chrg_amt                  numeric(10,2) not null,                   -- nonCoveredChargeAmount
    rev_cntr_rate_amt                        numeric(10,2) not null,                   -- rateAmount
    rev_cntr_tot_chrg_amt                    numeric(10,2) not null,                   -- totalChargeAmount
    rev_cntr_ddctbl_coinsrnc_cd              character(1),                             -- deductibleCoinsuranceCd
    rev_cntr_unit_cnt                        smallint not null ,                       -- unitCount
    rndrng_physn_npi                         character varying(12),                    -- revenueCenterRenderingPhysicianNPI
    rndrng_physn_upin                        character varying(12),                    -- revenueCenterRenderingPhysicianUPIN
    constraint snf_claim_lines_pkey primary key (parent_claim, clm_line_num)
)