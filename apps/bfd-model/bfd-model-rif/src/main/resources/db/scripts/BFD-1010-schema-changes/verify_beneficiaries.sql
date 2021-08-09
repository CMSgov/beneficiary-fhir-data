do $$
DECLARE
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_bene_id		bigint  := 0;
  v_tbl_name	varchar(40) := 'beneficiaries';

BEGIN
	for counter in 1..1000
	loop
		-- randomly select a "beneficiaryId" from original table
		SELECT cast("beneficiaryId" as bigint) into v_bene_id
		FROM public."Beneficiaries" TABLESAMPLE SYSTEM_ROWS(40)
		limit 1;

		select into curr
			bene_id as f_1,
			last_updated as f_2,
			bene_birth_dt as f_3,
			bene_esrd_ind as f_4,
			bene_entlmt_rsn_curr as f_5,
			bene_entlmt_rsn_orig as f_6,
			bene_mdcr_status_cd as f_7,
			bene_gvn_name as f_8,
			bene_mdl_name as f_9,
			bene_srnm_name as f_10,
			state_code as f_11,
			bene_county_cd as f_12,
			bene_zip_cd as f_13,
			age as f_14,
			bene_race_cd as f_15,
			rti_race_cd as f_16,
			bene_sex_ident_cd as f_17,
			bene_crnt_hic_num as f_18,
			hicn_unhashed as f_19,
			mbi_num as f_20,
			mbi_hash as f_21,
			mbi_efctv_bgn_dt as f_22,
			mbi_efctv_end_dt as f_23,
			death_dt as f_24,
			v_dod_sw as f_25,
			rfrnc_yr as f_26,
			bene_pta_trmntn_cd as f_27,
			bene_ptb_trmntn_cd as f_28,
			a_mo_cnt as f_29,
			b_mo_cnt as f_30,
			buyin_mo_cnt as f_31,
			hmo_mo_cnt as f_32,
			rds_mo_cnt as f_33,
			enrl_src as f_34,
			sample_group as f_35,
			efivepct as f_36,
			crnt_bic as f_37,
			covstart as f_38,
			dual_mo_cnt as f_39,
			fips_state_cnty_jan_cd as f_40,
			fips_state_cnty_feb_cd as f_41,
			fips_state_cnty_mar_cd as f_42,
			fips_state_cnty_apr_cd as f_43,
			fips_state_cnty_may_cd as f_44,
			fips_state_cnty_jun_cd as f_45,
			fips_state_cnty_jul_cd as f_46,
			fips_state_cnty_aug_cd as f_47,
			fips_state_cnty_sept_cd as f_48,
			fips_state_cnty_oct_cd as f_49,
			fips_state_cnty_nov_cd as f_50,
			fips_state_cnty_dec_cd as f_51,
			mdcr_stus_jan_cd as f_52,
			mdcr_stus_feb_cd as f_53,
			mdcr_stus_mar_cd as f_54,
			mdcr_stus_apr_cd as f_55,
			mdcr_stus_may_cd as f_56,
			mdcr_stus_jun_cd as f_57,
			mdcr_stus_jul_cd as f_58,
			mdcr_stus_aug_cd as f_59,
			mdcr_stus_sept_cd as f_60,
			mdcr_stus_oct_cd as f_61,
			mdcr_stus_nov_cd as f_62,
			mdcr_stus_dec_cd as f_63,
			plan_cvrg_mo_cnt as f_64,
			mdcr_entlmt_buyin_1_ind as f_65,
			mdcr_entlmt_buyin_2_ind as f_66,
			mdcr_entlmt_buyin_3_ind as f_67,
			mdcr_entlmt_buyin_4_ind as f_68,
			mdcr_entlmt_buyin_5_ind as f_69,
			mdcr_entlmt_buyin_6_ind as f_70,
			mdcr_entlmt_buyin_7_ind as f_71,
			mdcr_entlmt_buyin_8_ind as f_72,
			mdcr_entlmt_buyin_9_ind as f_73,
			mdcr_entlmt_buyin_10_ind as f_74,
			mdcr_entlmt_buyin_11_ind as f_75,
			mdcr_entlmt_buyin_12_ind as f_76,
			hmo_1_ind as f_77,
			hmo_2_ind as f_78,
			hmo_3_ind as f_79,
			hmo_4_ind as f_80,
			hmo_5_ind as f_81,
			hmo_6_ind as f_82,
			hmo_7_ind as f_83,
			hmo_8_ind as f_84,
			hmo_9_ind as f_85,
			hmo_10_ind as f_86,
			hmo_11_ind as f_87,
			hmo_12_ind as f_88,
			ptc_cntrct_jan_id as f_89,
			ptc_cntrct_feb_id as f_90,
			ptc_cntrct_mar_id as f_91,
			ptc_cntrct_apr_id as f_92,
			ptc_cntrct_may_id as f_93,
			ptc_cntrct_jun_id as f_94,
			ptc_cntrct_jul_id as f_95,
			ptc_cntrct_aug_id as f_96,
			ptc_cntrct_sept_id as f_97,
			ptc_cntrct_oct_id as f_98,
			ptc_cntrct_nov_id as f_99,
			ptc_cntrct_dec_id as f_100,
			ptc_pbp_jan_id as f_101,
			ptc_pbp_feb_id as f_102,
			ptc_pbp_mar_id as f_103,
			ptc_pbp_apr_id as f_104,
			ptc_pbp_may_id as f_105,
			ptc_pbp_jun_id as f_106,
			ptc_pbp_jul_id as f_107,
			ptc_pbp_aug_id as f_108,
			ptc_pbp_sept_id as f_109,
			ptc_pbp_oct_id as f_110,
			ptc_pbp_nov_id as f_111,
			ptc_pbp_dec_id as f_112,
			ptc_plan_type_jan_cd as f_113,
			ptc_plan_type_feb_cd as f_114,
			ptc_plan_type_mar_cd as f_115,
			ptc_plan_type_apr_cd as f_116,
			ptc_plan_type_may_cd as f_117,
			ptc_plan_type_jun_cd as f_118,
			ptc_plan_type_jul_cd as f_119,
			ptc_plan_type_aug_cd as f_120,
			ptc_plan_type_sept_cd as f_121,
			ptc_plan_type_oct_cd as f_122,
			ptc_plan_type_nov_cd as f_123,
			ptc_plan_type_dec_cd as f_124,
			ptd_cntrct_jan_id as f_125,
			ptd_cntrct_feb_id as f_126,
			ptd_cntrct_mar_id as f_127,
			ptd_cntrct_apr_id as f_128,
			ptd_cntrct_may_id as f_129,
			ptd_cntrct_jun_id as f_130,
			ptd_cntrct_jul_id as f_131,
			ptd_cntrct_aug_id as f_132,
			ptd_cntrct_sept_id as f_133,
			ptd_cntrct_oct_id as f_134,
			ptd_cntrct_nov_id as f_135,
			ptd_cntrct_dec_id as f_136,
			ptd_pbp_jan_id as f_137,
			ptd_pbp_feb_id as f_138,
			ptd_pbp_mar_id as f_139,
			ptd_pbp_apr_id as f_140,
			ptd_pbp_may_id as f_141,
			ptd_pbp_jun_id as f_142,
			ptd_pbp_jul_id as f_143,
			ptd_pbp_aug_id as f_144,
			ptd_pbp_sept_id as f_145,
			ptd_pbp_oct_id as f_146,
			ptd_pbp_nov_id as f_147,
			ptd_pbp_dec_id as f_148,
			ptd_sgmt_jan_id as f_149,
			ptd_sgmt_feb_id as f_150,
			ptd_sgmt_mar_id as f_151,
			ptd_sgmt_apr_id as f_152,
			ptd_sgmt_may_id as f_153,
			ptd_sgmt_jun_id as f_154,
			ptd_sgmt_jul_id as f_155,
			ptd_sgmt_aug_id as f_156,
			ptd_sgmt_sept_id as f_157,
			ptd_sgmt_oct_id as f_158,
			ptd_sgmt_nov_id as f_159,
			ptd_sgmt_dec_id as f_160,
			rds_jan_ind as f_161,
			rds_feb_ind as f_162,
			rds_mar_ind as f_163,
			rds_apr_ind as f_164,
			rds_may_ind as f_165,
			rds_jun_ind as f_166,
			rds_jul_ind as f_167,
			rds_aug_ind as f_168,
			rds_sept_ind as f_169,
			rds_oct_ind as f_170,
			rds_nov_ind as f_171,
			rds_dec_ind as f_172,
			meta_dual_elgbl_stus_jan_cd as f_173,
			meta_dual_elgbl_stus_feb_cd as f_174,
			meta_dual_elgbl_stus_mar_cd as f_175,
			meta_dual_elgbl_stus_apr_cd as f_176,
			meta_dual_elgbl_stus_may_cd as f_177,
			meta_dual_elgbl_stus_jun_cd as f_178,
			meta_dual_elgbl_stus_jul_cd as f_179,
			meta_dual_elgbl_stus_aug_cd as f_180,
			meta_dual_elgbl_stus_sept_cd as f_181,
			meta_dual_elgbl_stus_oct_cd as f_182,
			meta_dual_elgbl_stus_nov_cd as f_183,
			meta_dual_elgbl_stus_dec_cd as f_184,
			cst_shr_grp_jan_cd as f_185,
			cst_shr_grp_feb_cd as f_186,
			cst_shr_grp_mar_cd as f_187,
			cst_shr_grp_apr_cd as f_188,
			cst_shr_grp_may_cd as f_189,
			cst_shr_grp_jun_cd as f_190,
			cst_shr_grp_jul_cd as f_191,
			cst_shr_grp_aug_cd as f_192,
			cst_shr_grp_sept_cd as f_193,
			cst_shr_grp_oct_cd as f_194,
			cst_shr_grp_nov_cd as f_195,
			cst_shr_grp_dec_cd as f_196,
			drvd_line_1_adr as f_197,
			drvd_line_2_adr as f_198,
			drvd_line_3_adr as f_199,
			drvd_line_4_adr as f_200,
			drvd_line_5_adr as f_201,
			drvd_line_6_adr as f_202,
			city_name as f_203,
			state_cd as f_204,
			state_cnty_zip_cd as f_205,
			bene_link_key as f_206
		from
			public.beneficiaries
		WHERE
			bene_id = v_bene_id;
		

		SELECT INTO orig
			cast("beneficiaryId" as bigint) as f_1,
			"lastupdated" as f_2,
			"birthDate" as f_3,
			"endStageRenalDiseaseCode" as f_4,
			"entitlementCodeCurrent" as f_5,
			"entitlementCodeOriginal" as f_6,
			"medicareEnrollmentStatusCode" as f_7,
			"nameGiven" as f_8,
			"nameMiddleInitial" as f_9,
			"nameSurname" as f_10,
			"stateCode" as f_11,
			"countyCode" as f_12,
			"postalCode" as f_13,
			"ageOfBeneficiary" as f_14,
			"race" as f_15,
			"rtiRaceCode" as f_16,
			"sex" as f_17,
			"hicn" as f_18,
			"hicnUnhashed" as f_19,
			"medicareBeneficiaryId" as f_20,
			"mbiHash" as f_21,
			"mbiEffectiveDate" as f_22,
			"mbiObsoleteDate" as f_23,
			"beneficiaryDateOfDeath" as f_24,
			"validDateOfDeathSw" as f_25,
			"beneEnrollmentReferenceYear" as f_26,
			"partATerminationCode" as f_27,
			"partBTerminationCode" as f_28,
			"partAMonthsCount" as f_29,
			"partBMonthsCount" as f_30,
			"stateBuyInCoverageCount" as f_31,
			"hmoCoverageCount" as f_32,
			"monthsRetireeDrugSubsidyCoverage" as f_33,
			"sourceOfEnrollmentData" as f_34,
			"sampleMedicareGroupIndicator" as f_35,
			"enhancedMedicareSampleIndicator" as f_36,
			"currentBeneficiaryIdCode" as f_37,
			"medicareCoverageStartDate" as f_38,
			"monthsOfDualEligibility" as f_39,
			"fipsStateCntyJanCode" as f_40,
			"fipsStateCntyFebCode" as f_41,
			"fipsStateCntyMarCode" as f_42,
			"fipsStateCntyAprCode" as f_43,
			"fipsStateCntyMayCode" as f_44,
			"fipsStateCntyJunCode" as f_45,
			"fipsStateCntyJulCode" as f_46,
			"fipsStateCntyAugCode" as f_47,
			"fipsStateCntySeptCode" as f_48,
			"fipsStateCntyOctCode" as f_49,
			"fipsStateCntyNovCode" as f_50,
			"fipsStateCntyDecCode" as f_51,
			"medicareStatusJanCode" as f_52,
			"medicareStatusFebCode" as f_53,
			"medicareStatusMarCode" as f_54,
			"medicareStatusAprCode" as f_55,
			"medicareStatusMayCode" as f_56,
			"medicareStatusJunCode" as f_57,
			"medicareStatusJulCode" as f_58,
			"medicareStatusAugCode" as f_59,
			"medicareStatusSeptCode" as f_60,
			"medicareStatusOctCode" as f_61,
			"medicareStatusNovCode" as f_62,
			"medicareStatusDecCode" as f_63,
			"partDMonthsCount" as f_64,
			"entitlementBuyInJanInd" as f_65,
			"entitlementBuyInFebInd" as f_66,
			"entitlementBuyInMarInd" as f_67,
			"entitlementBuyInAprInd" as f_68,
			"entitlementBuyInMayInd" as f_69,
			"entitlementBuyInJunInd" as f_70,
			"entitlementBuyInJulInd" as f_71,
			"entitlementBuyInAugInd" as f_72,
			"entitlementBuyInSeptInd" as f_73,
			"entitlementBuyInOctInd" as f_74,
			"entitlementBuyInNovInd" as f_75,
			"entitlementBuyInDecInd" as f_76,
			"hmoIndicatorJanInd" as f_77,
			"hmoIndicatorFebInd" as f_78,
			"hmoIndicatorMarInd" as f_79,
			"hmoIndicatorAprInd" as f_80,
			"hmoIndicatorMayInd" as f_81,
			"hmoIndicatorJunInd" as f_82,
			"hmoIndicatorJulInd" as f_83,
			"hmoIndicatorAugInd" as f_84,
			"hmoIndicatorSeptInd" as f_85,
			"hmoIndicatorOctInd" as f_86,
			"hmoIndicatorNovInd" as f_87,
			"hmoIndicatorDecInd" as f_88,
			"partCContractNumberJanId" as f_89,
			"partCContractNumberFebId" as f_90,
			"partCContractNumberMarId" as f_91,
			"partCContractNumberAprId" as f_92,
			"partCContractNumberMayId" as f_93,
			"partCContractNumberJunId" as f_94,
			"partCContractNumberJulId" as f_95,
			"partCContractNumberAugId" as f_96,
			"partCContractNumberSeptId" as f_97,
			"partCContractNumberOctId" as f_98,
			"partCContractNumberNovId" as f_99,
			"partCContractNumberDecId" as f_100,
			"partCPbpNumberJanId" as f_101,
			"partCPbpNumberFebId" as f_102,
			"partCPbpNumberMarId" as f_103,
			"partCPbpNumberAprId" as f_104,
			"partCPbpNumberMayId" as f_105,
			"partCPbpNumberJunId" as f_106,
			"partCPbpNumberJulId" as f_107,
			"partCPbpNumberAugId" as f_108,
			"partCPbpNumberSeptId" as f_109,
			"partCPbpNumberOctId" as f_110,
			"partCPbpNumberNovId" as f_111,
			"partCPbpNumberDecId" as f_112,
			"partCPlanTypeJanCode" as f_113,
			"partCPlanTypeFebCode" as f_114,
			"partCPlanTypeMarCode" as f_115,
			"partCPlanTypeAprCode" as f_116,
			"partCPlanTypeMayCode" as f_117,
			"partCPlanTypeJunCode" as f_118,
			"partCPlanTypeJulCode" as f_119,
			"partCPlanTypeAugCode" as f_120,
			"partCPlanTypeSeptCode" as f_121,
			"partCPlanTypeOctCode" as f_122,
			"partCPlanTypeNovCode" as f_123,
			"partCPlanTypeDecCode" as f_124,
			"partDContractNumberJanId" as f_125,
			"partDContractNumberFebId" as f_126,
			"partDContractNumberMarId" as f_127,
			"partDContractNumberAprId" as f_128,
			"partDContractNumberMayId" as f_129,
			"partDContractNumberJunId" as f_130,
			"partDContractNumberJulId" as f_131,
			"partDContractNumberAugId" as f_132,
			"partDContractNumberSeptId" as f_133,
			"partDContractNumberOctId" as f_134,
			"partDContractNumberNovId" as f_135,
			"partDContractNumberDecId" as f_136,
			"partDPbpNumberJanId" as f_137,
			"partDPbpNumberFebId" as f_138,
			"partDPbpNumberMarId" as f_139,
			"partDPbpNumberAprId" as f_140,
			"partDPbpNumberMayId" as f_141,
			"partDPbpNumberJunId" as f_142,
			"partDPbpNumberJulId" as f_143,
			"partDPbpNumberAugId" as f_144,
			"partDPbpNumberSeptId" as f_145,
			"partDPbpNumberOctId" as f_146,
			"partDPbpNumberNovId" as f_147,
			"partDPbpNumberDecId" as f_148,
			"partDSegmentNumberJanId" as f_149,
			"partDSegmentNumberFebId" as f_150,
			"partDSegmentNumberMarId" as f_151,
			"partDSegmentNumberAprId" as f_152,
			"partDSegmentNumberMayId" as f_153,
			"partDSegmentNumberJunId" as f_154,
			"partDSegmentNumberJulId" as f_155,
			"partDSegmentNumberAugId" as f_156,
			"partDSegmentNumberSeptId" as f_157,
			"partDSegmentNumberOctId" as f_158,
			"partDSegmentNumberNovId" as f_159,
			"partDSegmentNumberDecId" as f_160,
			"partDRetireeDrugSubsidyJanInd" as f_161,
			"partDRetireeDrugSubsidyFebInd" as f_162,
			"partDRetireeDrugSubsidyMarInd" as f_163,
			"partDRetireeDrugSubsidyAprInd" as f_164,
			"partDRetireeDrugSubsidyMayInd" as f_165,
			"partDRetireeDrugSubsidyJunInd" as f_166,
			"partDRetireeDrugSubsidyJulInd" as f_167,
			"partDRetireeDrugSubsidyAugInd" as f_168,
			"partDRetireeDrugSubsidySeptInd" as f_169,
			"partDRetireeDrugSubsidyOctInd" as f_170,
			"partDRetireeDrugSubsidyNovInd" as f_171,
			"partDRetireeDrugSubsidyDecInd" as f_172,
			"medicaidDualEligibilityJanCode" as f_173,
			"medicaidDualEligibilityFebCode" as f_174,
			"medicaidDualEligibilityMarCode" as f_175,
			"medicaidDualEligibilityAprCode" as f_176,
			"medicaidDualEligibilityMayCode" as f_177,
			"medicaidDualEligibilityJunCode" as f_178,
			"medicaidDualEligibilityJulCode" as f_179,
			"medicaidDualEligibilityAugCode" as f_180,
			"medicaidDualEligibilitySeptCode" as f_181,
			"medicaidDualEligibilityOctCode" as f_182,
			"medicaidDualEligibilityNovCode" as f_183,
			"medicaidDualEligibilityDecCode" as f_184,
			"partDLowIncomeCostShareGroupJanCode" as f_185,
			"partDLowIncomeCostShareGroupFebCode" as f_186,
			"partDLowIncomeCostShareGroupMarCode" as f_187,
			"partDLowIncomeCostShareGroupAprCode" as f_188,
			"partDLowIncomeCostShareGroupMayCode" as f_189,
			"partDLowIncomeCostShareGroupJunCode" as f_190,
			"partDLowIncomeCostShareGroupJulCode" as f_191,
			"partDLowIncomeCostShareGroupAugCode" as f_192,
			"partDLowIncomeCostShareGroupSeptCode" as f_193,
			"partDLowIncomeCostShareGroupOctCode" as f_194,
			"partDLowIncomeCostShareGroupNovCode" as f_195,
			"partDLowIncomeCostShareGroupDecCode" as f_196,
			"derivedMailingAddress1" as f_197,
			"derivedMailingAddress2" as f_198,
			"derivedMailingAddress3" as f_199,
			"derivedMailingAddress4" as f_200,
			"derivedMailingAddress5" as f_201,
			"derivedMailingAddress6" as f_202,
			"derivedCityName" as f_203,
			"derivedStateCode" as f_204,
			"derivedZipCode" as f_205,
			cast("beneLinkKey" as bigint) as f_206
		from
			public."Beneficiaries"
		WHERE
			"beneficiaryId" = v_bene_id::text;
		
		if curr.f_1 <> orig.f_1 then err_cnt := err_cnt + 1; end if;
		if curr.f_2 <> orig.f_2 then err_cnt := err_cnt + 1; end if;
		if curr.f_3 <> orig.f_3 then err_cnt := err_cnt + 1; end if;
		if curr.f_4 <> orig.f_4 then err_cnt := err_cnt + 1; end if;
		if curr.f_5 <> orig.f_5 then err_cnt := err_cnt + 1; end if;
		if curr.f_6 <> orig.f_6 then err_cnt := err_cnt + 1; end if;
		if curr.f_7 <> orig.f_7 then err_cnt := err_cnt + 1; end if;
		if curr.f_8 <> orig.f_8 then err_cnt := err_cnt + 1; end if;
		if curr.f_9 <> orig.f_9 then err_cnt := err_cnt + 1; end if;
		if curr.f_10 <> orig.f_10 then err_cnt := err_cnt + 1; end if;
		if curr.f_11 <> orig.f_11 then err_cnt := err_cnt + 1; end if;
		if curr.f_12 <> orig.f_12 then err_cnt := err_cnt + 1; end if;
		if curr.f_13 <> orig.f_13 then err_cnt := err_cnt + 1; end if;
		if curr.f_14 <> orig.f_14 then err_cnt := err_cnt + 1; end if;
		if curr.f_15 <> orig.f_15 then err_cnt := err_cnt + 1; end if;
		if curr.f_16 <> orig.f_16 then err_cnt := err_cnt + 1; end if;
		if curr.f_17 <> orig.f_17 then err_cnt := err_cnt + 1; end if;
		if curr.f_18 <> orig.f_18 then err_cnt := err_cnt + 1; end if;
		if curr.f_19 <> orig.f_19 then err_cnt := err_cnt + 1; end if;
		if curr.f_20 <> orig.f_20 then err_cnt := err_cnt + 1; end if;
		if curr.f_21 <> orig.f_21 then err_cnt := err_cnt + 1; end if;
		if curr.f_22 <> orig.f_22 then err_cnt := err_cnt + 1; end if;
		if curr.f_23 <> orig.f_23 then err_cnt := err_cnt + 1; end if;
		if curr.f_24 <> orig.f_24 then err_cnt := err_cnt + 1; end if;
		if curr.f_25 <> orig.f_25 then err_cnt := err_cnt + 1; end if;
		if curr.f_26 <> orig.f_26 then err_cnt := err_cnt + 1; end if;
		if curr.f_27 <> orig.f_27 then err_cnt := err_cnt + 1; end if;
		if curr.f_28 <> orig.f_28 then err_cnt := err_cnt + 1; end if;
		if curr.f_29 <> orig.f_29 then err_cnt := err_cnt + 1; end if;
		if curr.f_30 <> orig.f_30 then err_cnt := err_cnt + 1; end if;
		if curr.f_31 <> orig.f_31 then err_cnt := err_cnt + 1; end if;
		if curr.f_32 <> orig.f_32 then err_cnt := err_cnt + 1; end if;
		if curr.f_33 <> orig.f_33 then err_cnt := err_cnt + 1; end if;
		if curr.f_34 <> orig.f_34 then err_cnt := err_cnt + 1; end if;
		if curr.f_35 <> orig.f_35 then err_cnt := err_cnt + 1; end if;
		if curr.f_36 <> orig.f_36 then err_cnt := err_cnt + 1; end if;
		if curr.f_37 <> orig.f_37 then err_cnt := err_cnt + 1; end if;
		if curr.f_38 <> orig.f_38 then err_cnt := err_cnt + 1; end if;
		if curr.f_39 <> orig.f_39 then err_cnt := err_cnt + 1; end if;
		if curr.f_40 <> orig.f_40 then err_cnt := err_cnt + 1; end if;
		if curr.f_41 <> orig.f_41 then err_cnt := err_cnt + 1; end if;
		if curr.f_42 <> orig.f_42 then err_cnt := err_cnt + 1; end if;
		if curr.f_43 <> orig.f_43 then err_cnt := err_cnt + 1; end if;
		if curr.f_44 <> orig.f_44 then err_cnt := err_cnt + 1; end if;
		if curr.f_45 <> orig.f_45 then err_cnt := err_cnt + 1; end if;
		if curr.f_46 <> orig.f_46 then err_cnt := err_cnt + 1; end if;
		if curr.f_47 <> orig.f_47 then err_cnt := err_cnt + 1; end if;
		if curr.f_48 <> orig.f_48 then err_cnt := err_cnt + 1; end if;
		if curr.f_49 <> orig.f_49 then err_cnt := err_cnt + 1; end if;
		if curr.f_50 <> orig.f_50 then err_cnt := err_cnt + 1; end if;
		if curr.f_51 <> orig.f_51 then err_cnt := err_cnt + 1; end if;
		if curr.f_52 <> orig.f_52 then err_cnt := err_cnt + 1; end if;
		if curr.f_53 <> orig.f_53 then err_cnt := err_cnt + 1; end if;
		if curr.f_54 <> orig.f_54 then err_cnt := err_cnt + 1; end if;
		if curr.f_55 <> orig.f_55 then err_cnt := err_cnt + 1; end if;
		if curr.f_56 <> orig.f_56 then err_cnt := err_cnt + 1; end if;
		if curr.f_57 <> orig.f_57 then err_cnt := err_cnt + 1; end if;
		if curr.f_58 <> orig.f_58 then err_cnt := err_cnt + 1; end if;
		if curr.f_59 <> orig.f_59 then err_cnt := err_cnt + 1; end if;
		if curr.f_60 <> orig.f_60 then err_cnt := err_cnt + 1; end if;
		if curr.f_61 <> orig.f_61 then err_cnt := err_cnt + 1; end if;
		if curr.f_62 <> orig.f_62 then err_cnt := err_cnt + 1; end if;
		if curr.f_63 <> orig.f_63 then err_cnt := err_cnt + 1; end if;
		if curr.f_64 <> orig.f_64 then err_cnt := err_cnt + 1; end if;
		if curr.f_65 <> orig.f_65 then err_cnt := err_cnt + 1; end if;
		if curr.f_66 <> orig.f_66 then err_cnt := err_cnt + 1; end if;
		if curr.f_67 <> orig.f_67 then err_cnt := err_cnt + 1; end if;
		if curr.f_68 <> orig.f_68 then err_cnt := err_cnt + 1; end if;
		if curr.f_69 <> orig.f_69 then err_cnt := err_cnt + 1; end if;
		if curr.f_70 <> orig.f_70 then err_cnt := err_cnt + 1; end if;
		if curr.f_71 <> orig.f_71 then err_cnt := err_cnt + 1; end if;
		if curr.f_72 <> orig.f_72 then err_cnt := err_cnt + 1; end if;
		if curr.f_73 <> orig.f_73 then err_cnt := err_cnt + 1; end if;
		if curr.f_74 <> orig.f_74 then err_cnt := err_cnt + 1; end if;
		if curr.f_75 <> orig.f_75 then err_cnt := err_cnt + 1; end if;
		if curr.f_76 <> orig.f_76 then err_cnt := err_cnt + 1; end if;
		if curr.f_77 <> orig.f_77 then err_cnt := err_cnt + 1; end if;
		if curr.f_78 <> orig.f_78 then err_cnt := err_cnt + 1; end if;
		if curr.f_79 <> orig.f_79 then err_cnt := err_cnt + 1; end if;
		if curr.f_80 <> orig.f_80 then err_cnt := err_cnt + 1; end if;
		if curr.f_81 <> orig.f_81 then err_cnt := err_cnt + 1; end if;
		if curr.f_82 <> orig.f_82 then err_cnt := err_cnt + 1; end if;
		if curr.f_83 <> orig.f_83 then err_cnt := err_cnt + 1; end if;
		if curr.f_84 <> orig.f_84 then err_cnt := err_cnt + 1; end if;
		if curr.f_85 <> orig.f_85 then err_cnt := err_cnt + 1; end if;
		if curr.f_86 <> orig.f_86 then err_cnt := err_cnt + 1; end if;
		if curr.f_87 <> orig.f_87 then err_cnt := err_cnt + 1; end if;
		if curr.f_88 <> orig.f_88 then err_cnt := err_cnt + 1; end if;
		if curr.f_89 <> orig.f_89 then err_cnt := err_cnt + 1; end if;
		if curr.f_90 <> orig.f_90 then err_cnt := err_cnt + 1; end if;
		if curr.f_91 <> orig.f_91 then err_cnt := err_cnt + 1; end if;
		if curr.f_92 <> orig.f_92 then err_cnt := err_cnt + 1; end if;
		if curr.f_93 <> orig.f_93 then err_cnt := err_cnt + 1; end if;
		if curr.f_94 <> orig.f_94 then err_cnt := err_cnt + 1; end if;
		if curr.f_95 <> orig.f_95 then err_cnt := err_cnt + 1; end if;
		if curr.f_96 <> orig.f_96 then err_cnt := err_cnt + 1; end if;
		if curr.f_97 <> orig.f_97 then err_cnt := err_cnt + 1; end if;
		if curr.f_98 <> orig.f_98 then err_cnt := err_cnt + 1; end if;
		if curr.f_99 <> orig.f_99 then err_cnt := err_cnt + 1; end if;
		if curr.f_100 <> orig.f_100 then err_cnt := err_cnt + 1; end if;
		if curr.f_101 <> orig.f_101 then err_cnt := err_cnt + 1; end if;
		if curr.f_102 <> orig.f_102 then err_cnt := err_cnt + 1; end if;
		if curr.f_103 <> orig.f_103 then err_cnt := err_cnt + 1; end if;
		if curr.f_104 <> orig.f_104 then err_cnt := err_cnt + 1; end if;
		if curr.f_105 <> orig.f_105 then err_cnt := err_cnt + 1; end if;
		if curr.f_106 <> orig.f_106 then err_cnt := err_cnt + 1; end if;
		if curr.f_107 <> orig.f_107 then err_cnt := err_cnt + 1; end if;
		if curr.f_108 <> orig.f_108 then err_cnt := err_cnt + 1; end if;
		if curr.f_109 <> orig.f_109 then err_cnt := err_cnt + 1; end if;
		if curr.f_110 <> orig.f_110 then err_cnt := err_cnt + 1; end if;
		if curr.f_111 <> orig.f_111 then err_cnt := err_cnt + 1; end if;
		if curr.f_112 <> orig.f_112 then err_cnt := err_cnt + 1; end if;
		if curr.f_113 <> orig.f_113 then err_cnt := err_cnt + 1; end if;
		if curr.f_114 <> orig.f_114 then err_cnt := err_cnt + 1; end if;
		if curr.f_115 <> orig.f_115 then err_cnt := err_cnt + 1; end if;
		if curr.f_116 <> orig.f_116 then err_cnt := err_cnt + 1; end if;
		if curr.f_117 <> orig.f_117 then err_cnt := err_cnt + 1; end if;
		if curr.f_118 <> orig.f_118 then err_cnt := err_cnt + 1; end if;
		if curr.f_119 <> orig.f_119 then err_cnt := err_cnt + 1; end if;
		if curr.f_120 <> orig.f_120 then err_cnt := err_cnt + 1; end if;
		if curr.f_121 <> orig.f_121 then err_cnt := err_cnt + 1; end if;
		if curr.f_122 <> orig.f_122 then err_cnt := err_cnt + 1; end if;
		if curr.f_123 <> orig.f_123 then err_cnt := err_cnt + 1; end if;
		if curr.f_124 <> orig.f_124 then err_cnt := err_cnt + 1; end if;
		if curr.f_125 <> orig.f_125 then err_cnt := err_cnt + 1; end if;
		if curr.f_126 <> orig.f_126 then err_cnt := err_cnt + 1; end if;
		if curr.f_127 <> orig.f_127 then err_cnt := err_cnt + 1; end if;
		if curr.f_128 <> orig.f_128 then err_cnt := err_cnt + 1; end if;
		if curr.f_129 <> orig.f_129 then err_cnt := err_cnt + 1; end if;
		if curr.f_130 <> orig.f_130 then err_cnt := err_cnt + 1; end if;
		if curr.f_131 <> orig.f_131 then err_cnt := err_cnt + 1; end if;
		if curr.f_132 <> orig.f_132 then err_cnt := err_cnt + 1; end if;
		if curr.f_133 <> orig.f_133 then err_cnt := err_cnt + 1; end if;
		if curr.f_134 <> orig.f_134 then err_cnt := err_cnt + 1; end if;
		if curr.f_135 <> orig.f_135 then err_cnt := err_cnt + 1; end if;
		if curr.f_136 <> orig.f_136 then err_cnt := err_cnt + 1; end if;
		if curr.f_137 <> orig.f_137 then err_cnt := err_cnt + 1; end if;
		if curr.f_138 <> orig.f_138 then err_cnt := err_cnt + 1; end if;
		if curr.f_139 <> orig.f_139 then err_cnt := err_cnt + 1; end if;
		if curr.f_140 <> orig.f_140 then err_cnt := err_cnt + 1; end if;
		if curr.f_141 <> orig.f_141 then err_cnt := err_cnt + 1; end if;
		if curr.f_142 <> orig.f_142 then err_cnt := err_cnt + 1; end if;
		if curr.f_143 <> orig.f_143 then err_cnt := err_cnt + 1; end if;
		if curr.f_144 <> orig.f_144 then err_cnt := err_cnt + 1; end if;
		if curr.f_145 <> orig.f_145 then err_cnt := err_cnt + 1; end if;
		if curr.f_146 <> orig.f_146 then err_cnt := err_cnt + 1; end if;
		if curr.f_147 <> orig.f_147 then err_cnt := err_cnt + 1; end if;
		if curr.f_148 <> orig.f_148 then err_cnt := err_cnt + 1; end if;
		if curr.f_149 <> orig.f_149 then err_cnt := err_cnt + 1; end if;
		if curr.f_150 <> orig.f_150 then err_cnt := err_cnt + 1; end if;
		if curr.f_151 <> orig.f_151 then err_cnt := err_cnt + 1; end if;
		if curr.f_152 <> orig.f_152 then err_cnt := err_cnt + 1; end if;
		if curr.f_153 <> orig.f_153 then err_cnt := err_cnt + 1; end if;
		if curr.f_154 <> orig.f_154 then err_cnt := err_cnt + 1; end if;
		if curr.f_155 <> orig.f_155 then err_cnt := err_cnt + 1; end if;
		if curr.f_156 <> orig.f_156 then err_cnt := err_cnt + 1; end if;
		if curr.f_157 <> orig.f_157 then err_cnt := err_cnt + 1; end if;
		if curr.f_158 <> orig.f_158 then err_cnt := err_cnt + 1; end if;
		if curr.f_159 <> orig.f_159 then err_cnt := err_cnt + 1; end if;
		if curr.f_160 <> orig.f_160 then err_cnt := err_cnt + 1; end if;
		if curr.f_161 <> orig.f_161 then err_cnt := err_cnt + 1; end if;
		if curr.f_162 <> orig.f_162 then err_cnt := err_cnt + 1; end if;
		if curr.f_163 <> orig.f_163 then err_cnt := err_cnt + 1; end if;
		if curr.f_164 <> orig.f_164 then err_cnt := err_cnt + 1; end if;
		if curr.f_165 <> orig.f_165 then err_cnt := err_cnt + 1; end if;
		if curr.f_166 <> orig.f_166 then err_cnt := err_cnt + 1; end if;
		if curr.f_167 <> orig.f_167 then err_cnt := err_cnt + 1; end if;
		if curr.f_168 <> orig.f_168 then err_cnt := err_cnt + 1; end if;
		if curr.f_169 <> orig.f_169 then err_cnt := err_cnt + 1; end if;
		if curr.f_170 <> orig.f_170 then err_cnt := err_cnt + 1; end if;
		if curr.f_171 <> orig.f_171 then err_cnt := err_cnt + 1; end if;
		if curr.f_172 <> orig.f_172 then err_cnt := err_cnt + 1; end if;
		if curr.f_173 <> orig.f_173 then err_cnt := err_cnt + 1; end if;
		if curr.f_174 <> orig.f_174 then err_cnt := err_cnt + 1; end if;
		if curr.f_175 <> orig.f_175 then err_cnt := err_cnt + 1; end if;
		if curr.f_176 <> orig.f_176 then err_cnt := err_cnt + 1; end if;
		if curr.f_177 <> orig.f_177 then err_cnt := err_cnt + 1; end if;
		if curr.f_178 <> orig.f_178 then err_cnt := err_cnt + 1; end if;
		if curr.f_179 <> orig.f_179 then err_cnt := err_cnt + 1; end if;
		if curr.f_180 <> orig.f_180 then err_cnt := err_cnt + 1; end if;
		if curr.f_181 <> orig.f_181 then err_cnt := err_cnt + 1; end if;
		if curr.f_182 <> orig.f_182 then err_cnt := err_cnt + 1; end if;
		if curr.f_183 <> orig.f_183 then err_cnt := err_cnt + 1; end if;
		if curr.f_184 <> orig.f_184 then err_cnt := err_cnt + 1; end if;
		if curr.f_185 <> orig.f_185 then err_cnt := err_cnt + 1; end if;
		if curr.f_186 <> orig.f_186 then err_cnt := err_cnt + 1; end if;
		if curr.f_187 <> orig.f_187 then err_cnt := err_cnt + 1; end if;
		if curr.f_188 <> orig.f_188 then err_cnt := err_cnt + 1; end if;
		if curr.f_189 <> orig.f_189 then err_cnt := err_cnt + 1; end if;
		if curr.f_190 <> orig.f_190 then err_cnt := err_cnt + 1; end if;
		if curr.f_191 <> orig.f_191 then err_cnt := err_cnt + 1; end if;
		if curr.f_192 <> orig.f_192 then err_cnt := err_cnt + 1; end if;
		if curr.f_193 <> orig.f_193 then err_cnt := err_cnt + 1; end if;
		if curr.f_194 <> orig.f_194 then err_cnt := err_cnt + 1; end if;
		if curr.f_195 <> orig.f_195 then err_cnt := err_cnt + 1; end if;
		if curr.f_196 <> orig.f_196 then err_cnt := err_cnt + 1; end if;
		if curr.f_197 <> orig.f_197 then err_cnt := err_cnt + 1; end if;
		if curr.f_198 <> orig.f_198 then err_cnt := err_cnt + 1; end if;
		if curr.f_199 <> orig.f_199 then err_cnt := err_cnt + 1; end if;
		if curr.f_200 <> orig.f_200 then err_cnt := err_cnt + 1; end if;
		if curr.f_201 <> orig.f_201 then err_cnt := err_cnt + 1; end if;
		if curr.f_202 <> orig.f_202 then err_cnt := err_cnt + 1; end if;
		if curr.f_203 <> orig.f_203 then err_cnt := err_cnt + 1; end if;
		if curr.f_204 <> orig.f_204 then err_cnt := err_cnt + 1; end if;
		if curr.f_205 <> orig.f_205 then err_cnt := err_cnt + 1; end if;
		if curr.f_206 <> orig.f_206 then err_cnt := err_cnt + 1; end if;
	
		if err_cnt > 0 then
			insert into migration_errors values("beneficiaries", v_bene_id, null, null, err_cnt);
			tot_err_cnt := tot_err_cnt + err_cnt;
			raise info 'DISCREPANCY, table: %, bene_id: %', v_tbl_name, v_bene_id;
		end if;
	end loop;
	
	if tot_err_cnt > 0 then
		raise info 'DISCREPANCY, table: %', v_tbl_name;
	else
		raise info 'NO ERRORS, table: %', v_tbl_name;
	end if;
END;
$$;