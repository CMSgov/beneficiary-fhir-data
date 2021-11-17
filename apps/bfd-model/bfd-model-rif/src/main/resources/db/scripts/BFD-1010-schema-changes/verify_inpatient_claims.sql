do $$
DECLARE
  MAX_TESTS		INTEGER := 500000;		-- hopefully .5M tests are sufficient
  v_claims		BIGINT[];
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_clm_id      bigint  := 0;
  v_tbl_name	varchar(40) := 'inpatient_claims';

BEGIN
	v_claims := ARRAY(
		SELECT cast("claimId" as bigint)
		FROM "InpatientClaims" TABLESAMPLE BERNOULLI(50)	-- bernoulli sample using 50% of table rows
		limit MAX_TESTS;

	for counter in 1..MAX_TESTS
	loop
		v_clm_id := v_claims[counter - 1];

		select into curr
			clm_id as f_1,
			bene_id as f_2,
			clm_grp_id as f_3,
			last_updated as f_4,
			clm_from_dt as f_5,
			clm_thru_dt as f_6,
			clm_admsn_dt as f_7,
			clm_drg_cd as f_8,
			clm_drg_outlier_stay_cd as f_9,
			clm_fac_type_cd as f_10,
			clm_freq_cd as f_11,
			clm_pmt_amt as f_12,
			clm_ip_admsn_type_cd as f_13,
			clm_mco_pd_sw as f_14,
			clm_mdcr_non_pmt_rsn_cd as f_15,
			clm_non_utlztn_days_cnt as f_16,
			clm_pass_thru_per_diem_amt as f_17,
			clm_pps_cptl_drg_wt_num as f_18,
			clm_pps_cptl_dsprprtnt_shr_amt as f_19,
			clm_pps_cptl_excptn_amt as f_20,
			clm_pps_cptl_fsp_amt as f_21,
			clm_pps_cptl_ime_amt as f_22,
			clm_pps_cptl_outlier_amt as f_23,
			clm_pps_ind_cd as f_24,
			clm_pps_old_cptl_hld_hrmls_amt as f_25,
			clm_src_ip_admsn_cd as f_26,
			clm_srvc_clsfctn_type_cd as f_27,
			clm_tot_pps_cptl_amt as f_28,
			clm_uncompd_care_pmt_amt as f_29,
			clm_utlztn_day_cnt as f_30,
			admtg_dgns_cd as f_31,
			admtg_dgns_vrsn_cd as f_32,
			at_physn_npi as f_33,
			at_physn_upin as f_34,
			bene_lrd_used_cnt as f_35,
			bene_tot_coinsrnc_days_cnt as f_36,
			claim_query_code as f_37,
			dsh_op_clm_val_amt as f_38,
			fi_clm_actn_cd as f_39,
			fi_clm_proc_dt as f_40,
			fi_doc_clm_cntl_num as f_41,
			fi_num as f_42,
			fi_orig_clm_cntl_num as f_43,
			final_action as f_44,
			fst_dgns_e_cd as f_45,
			fst_dgns_e_vrsn_cd as f_46,
			ime_op_clm_val_amt as f_47,
			nch_actv_or_cvrd_lvl_care_thru as f_48,
			nch_bene_blood_ddctbl_lblty_am as f_49,
			nch_bene_dschrg_dt as f_50,
			nch_bene_ip_ddctbl_amt as f_51,
			nch_bene_mdcr_bnfts_exhtd_dt_i as f_52,
			nch_bene_pta_coinsrnc_lblty_am as f_53,
			nch_blood_pnts_frnshd_qty as f_54,
			nch_clm_type_cd as f_55,
			nch_drg_outlier_aprvd_pmt_amt as f_56,
			nch_ip_ncvrd_chrg_amt as f_57,
			nch_ip_tot_ddctn_amt as f_58,
			nch_near_line_rec_ident_cd as f_59,
			nch_prmry_pyr_cd as f_60,
			nch_prmry_pyr_clm_pd_amt as f_61,
			nch_profnl_cmpnt_chrg_amt as f_62,
			nch_ptnt_status_ind_cd as f_63,
			nch_vrfd_ncvrd_stay_from_dt as f_64,
			nch_vrfd_ncvrd_stay_thru_dt as f_65,
			nch_wkly_proc_dt as f_66,
			op_physn_npi as f_67,
			op_physn_upin as f_68,
			org_npi_num as f_69,
			ot_physn_npi as f_70,
			ot_physn_upin as f_71,
			prncpal_dgns_cd as f_72,
			prncpal_dgns_vrsn_cd as f_73,
			prvdr_num as f_74,
			prvdr_state_cd as f_75,
			ptnt_dschrg_stus_cd as f_76,
			clm_tot_chrg_amt as f_77,
			clm_e_poa_ind_sw1 as f_78,
			clm_e_poa_ind_sw2 as f_79,
			clm_e_poa_ind_sw3 as f_80,
			clm_e_poa_ind_sw4 as f_81,
			clm_e_poa_ind_sw5 as f_82,
			clm_e_poa_ind_sw6 as f_83,
			clm_e_poa_ind_sw7 as f_84,
			clm_e_poa_ind_sw8 as f_85,
			clm_e_poa_ind_sw9 as f_86,
			clm_e_poa_ind_sw10 as f_87,
			clm_e_poa_ind_sw11 as f_88,
			clm_e_poa_ind_sw12 as f_89,
			clm_poa_ind_sw1 as f_90,
			clm_poa_ind_sw2 as f_91,
			clm_poa_ind_sw3 as f_92,
			clm_poa_ind_sw4 as f_93,
			clm_poa_ind_sw5 as f_94,
			clm_poa_ind_sw6 as f_95,
			clm_poa_ind_sw7 as f_96,
			clm_poa_ind_sw8 as f_97,
			clm_poa_ind_sw9 as f_98,
			clm_poa_ind_sw10 as f_99,
			clm_poa_ind_sw11 as f_100,
			clm_poa_ind_sw12 as f_101,
			clm_poa_ind_sw13 as f_102,
			clm_poa_ind_sw14 as f_103,
			clm_poa_ind_sw15 as f_104,
			clm_poa_ind_sw16 as f_105,
			clm_poa_ind_sw17 as f_106,
			clm_poa_ind_sw18 as f_107,
			clm_poa_ind_sw19 as f_108,
			clm_poa_ind_sw20 as f_109,
			clm_poa_ind_sw21 as f_110,
			clm_poa_ind_sw22 as f_111,
			clm_poa_ind_sw23 as f_112,
			clm_poa_ind_sw24 as f_113,
			clm_poa_ind_sw25 as f_114,
			icd_dgns_cd1 as f_115,
			icd_dgns_cd2 as f_116,
			icd_dgns_cd3 as f_117,
			icd_dgns_cd4 as f_118,
			icd_dgns_cd5 as f_119,
			icd_dgns_cd6 as f_120,
			icd_dgns_cd7 as f_121,
			icd_dgns_cd8 as f_122,
			icd_dgns_cd9 as f_123,
			icd_dgns_cd10 as f_124,
			icd_dgns_cd11 as f_125,
			icd_dgns_cd12 as f_126,
			icd_dgns_cd13 as f_127,
			icd_dgns_cd14 as f_128,
			icd_dgns_cd15 as f_129,
			icd_dgns_cd16 as f_130,
			icd_dgns_cd17 as f_131,
			icd_dgns_cd18 as f_132,
			icd_dgns_cd19 as f_133,
			icd_dgns_cd20 as f_134,
			icd_dgns_cd21 as f_135,
			icd_dgns_cd22 as f_136,
			icd_dgns_cd23 as f_137,
			icd_dgns_cd24 as f_138,
			icd_dgns_cd25 as f_139,
			icd_dgns_e_cd1 as f_140,
			icd_dgns_e_cd2 as f_141,
			icd_dgns_e_cd3 as f_142,
			icd_dgns_e_cd4 as f_143,
			icd_dgns_e_cd5 as f_144,
			icd_dgns_e_cd6 as f_145,
			icd_dgns_e_cd7 as f_146,
			icd_dgns_e_cd8 as f_147,
			icd_dgns_e_cd9 as f_148,
			icd_dgns_e_cd10 as f_149,
			icd_dgns_e_cd11 as f_150,
			icd_dgns_e_cd12 as f_151,
			icd_dgns_e_vrsn_cd1 as f_152,
			icd_dgns_e_vrsn_cd2 as f_153,
			icd_dgns_e_vrsn_cd3 as f_154,
			icd_dgns_e_vrsn_cd4 as f_155,
			icd_dgns_e_vrsn_cd5 as f_156,
			icd_dgns_e_vrsn_cd6 as f_157,
			icd_dgns_e_vrsn_cd7 as f_158,
			icd_dgns_e_vrsn_cd8 as f_159,
			icd_dgns_e_vrsn_cd9 as f_160,
			icd_dgns_e_vrsn_cd10 as f_161,
			icd_dgns_e_vrsn_cd11 as f_162,
			icd_dgns_e_vrsn_cd12 as f_163,
			icd_dgns_vrsn_cd1 as f_164,
			icd_dgns_vrsn_cd2 as f_165,
			icd_dgns_vrsn_cd3 as f_166,
			icd_dgns_vrsn_cd4 as f_167,
			icd_dgns_vrsn_cd5 as f_168,
			icd_dgns_vrsn_cd6 as f_169,
			icd_dgns_vrsn_cd7 as f_170,
			icd_dgns_vrsn_cd8 as f_171,
			icd_dgns_vrsn_cd9 as f_172,
			icd_dgns_vrsn_cd10 as f_173,
			icd_dgns_vrsn_cd11 as f_174,
			icd_dgns_vrsn_cd12 as f_175,
			icd_dgns_vrsn_cd13 as f_176,
			icd_dgns_vrsn_cd14 as f_177,
			icd_dgns_vrsn_cd15 as f_178,
			icd_dgns_vrsn_cd16 as f_179,
			icd_dgns_vrsn_cd17 as f_180,
			icd_dgns_vrsn_cd18 as f_181,
			icd_dgns_vrsn_cd19 as f_182,
			icd_dgns_vrsn_cd20 as f_183,
			icd_dgns_vrsn_cd21 as f_184,
			icd_dgns_vrsn_cd22 as f_185,
			icd_dgns_vrsn_cd23 as f_186,
			icd_dgns_vrsn_cd24 as f_187,
			icd_dgns_vrsn_cd25 as f_188,
			icd_prcdr_cd1 as f_189,
			icd_prcdr_cd2 as f_190,
			icd_prcdr_cd3 as f_191,
			icd_prcdr_cd4 as f_192,
			icd_prcdr_cd5 as f_193,
			icd_prcdr_cd6 as f_194,
			icd_prcdr_cd7 as f_195,
			icd_prcdr_cd8 as f_196,
			icd_prcdr_cd9 as f_197,
			icd_prcdr_cd10 as f_198,
			icd_prcdr_cd11 as f_199,
			icd_prcdr_cd12 as f_200,
			icd_prcdr_cd13 as f_201,
			icd_prcdr_cd14 as f_202,
			icd_prcdr_cd15 as f_203,
			icd_prcdr_cd16 as f_204,
			icd_prcdr_cd17 as f_205,
			icd_prcdr_cd18 as f_206,
			icd_prcdr_cd19 as f_207,
			icd_prcdr_cd20 as f_208,
			icd_prcdr_cd21 as f_209,
			icd_prcdr_cd22 as f_210,
			icd_prcdr_cd23 as f_211,
			icd_prcdr_cd24 as f_212,
			icd_prcdr_cd25 as f_213,
			icd_prcdr_vrsn_cd1 as f_214,
			icd_prcdr_vrsn_cd2 as f_215,
			icd_prcdr_vrsn_cd3 as f_216,
			icd_prcdr_vrsn_cd4 as f_217,
			icd_prcdr_vrsn_cd5 as f_218,
			icd_prcdr_vrsn_cd6 as f_219,
			icd_prcdr_vrsn_cd7 as f_220,
			icd_prcdr_vrsn_cd8 as f_221,
			icd_prcdr_vrsn_cd9 as f_222,
			icd_prcdr_vrsn_cd10 as f_223,
			icd_prcdr_vrsn_cd11 as f_224,
			icd_prcdr_vrsn_cd12 as f_225,
			icd_prcdr_vrsn_cd13 as f_226,
			icd_prcdr_vrsn_cd14 as f_227,
			icd_prcdr_vrsn_cd15 as f_228,
			icd_prcdr_vrsn_cd16 as f_229,
			icd_prcdr_vrsn_cd17 as f_230,
			icd_prcdr_vrsn_cd18 as f_231,
			icd_prcdr_vrsn_cd19 as f_232,
			icd_prcdr_vrsn_cd20 as f_233,
			icd_prcdr_vrsn_cd21 as f_234,
			icd_prcdr_vrsn_cd22 as f_235,
			icd_prcdr_vrsn_cd23 as f_236,
			icd_prcdr_vrsn_cd24 as f_237,
			icd_prcdr_vrsn_cd25 as f_238,
			prcdr_dt1 as f_239,
			prcdr_dt2 as f_240,
			prcdr_dt3 as f_241,
			prcdr_dt4 as f_242,
			prcdr_dt5 as f_243,
			prcdr_dt6 as f_244,
			prcdr_dt7 as f_245,
			prcdr_dt8 as f_246,
			prcdr_dt9 as f_247,
			prcdr_dt10 as f_248,
			prcdr_dt11 as f_249,
			prcdr_dt12 as f_250,
			prcdr_dt13 as f_251,
			prcdr_dt14 as f_252,
			prcdr_dt15 as f_253,
			prcdr_dt16 as f_254,
			prcdr_dt17 as f_255,
			prcdr_dt18 as f_256,
			prcdr_dt19 as f_257,
			prcdr_dt20 as f_258,
			prcdr_dt21 as f_259,
			prcdr_dt22 as f_260,
			prcdr_dt23 as f_261,
			prcdr_dt24 as f_262,
			prcdr_dt25 as f_263
		from
			inpatient_claims
		WHERE
			clm_id = v_clm_id;
		

		SELECT INTO orig
			cast("claimId" as bigint) as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			cast("claimGroupId" as bigint) as f_3,
			"lastupdated" as f_4,
			"dateFrom" as f_5,
			"dateThrough" as f_6,
			"claimAdmissionDate" as f_7,
			"diagnosisRelatedGroupCd" as f_8,
			"diagnosisRelatedGroupOutlierStayCd" as f_9,
			"claimFacilityTypeCode" as f_10,
			"claimFrequencyCode" as f_11,
			"paymentAmount" as f_12,
			"admissionTypeCd" as f_13,
			"mcoPaidSw" as f_14,
			"claimNonPaymentReasonCode" as f_15,
			"nonUtilizationDayCount" as f_16,
			"passThruPerDiemAmount" as f_17,
			"claimPPSCapitalDrgWeightNumber" as f_18,
			"claimPPSCapitalDisproportionateShareAmt" as f_19,
			"claimPPSCapitalExceptionAmount" as f_20,
			"claimPPSCapitalFSPAmount" as f_21,
			"claimPPSCapitalIMEAmount" as f_22,
			"claimPPSCapitalOutlierAmount" as f_23,
			"prospectivePaymentCode" as f_24,
			"claimPPSOldCapitalHoldHarmlessAmount" as f_25,
			"sourceAdmissionCd" as f_26,
			"claimServiceClassificationTypeCode" as f_27,
			"claimTotalPPSCapitalAmount" as f_28,
			"claimUncompensatedCareAmount" as f_29,
			"utilizationDayCount" as f_30,
			"diagnosisAdmittingCode" as f_31,
			"diagnosisAdmittingCodeVersion" as f_32,
			"attendingPhysicianNpi" as f_33,
			"attendingPhysicianUpin" as f_34,
			"lifetimeReservedDaysUsedCount" as f_35,
			"coinsuranceDayCount" as f_36,
			"claimQueryCode" as f_37,
			"disproportionateShareAmount" as f_38,
			"fiscalIntermediaryClaimActionCode" as f_39,
			"fiscalIntermediaryClaimProcessDate" as f_40,
			"fiDocumentClaimControlNumber" as f_41,
			"fiscalIntermediaryNumber" as f_42,
			"fiOriginalClaimControlNumber" as f_43,
			"finalAction" as f_44,
			"diagnosisExternalFirstCode" as f_45,
			"diagnosisExternalFirstCodeVersion" as f_46,
			"indirectMedicalEducationAmount" as f_47,
			"coveredCareThoughDate" as f_48,
			"bloodDeductibleLiabilityAmount" as f_49,
			"beneficiaryDischargeDate" as f_50,
			"deductibleAmount" as f_51,
			"medicareBenefitsExhaustedDate" as f_52,
			"partACoinsuranceLiabilityAmount" as f_53,
			"bloodPintsFurnishedQty" as f_54,
			"claimTypeCode" as f_55,
			"drgOutlierApprovedPaymentAmount" as f_56,
			"noncoveredCharge" as f_57,
			"totalDeductionAmount" as f_58,
			"nearLineRecordIdCode" as f_59,
			"claimPrimaryPayerCode" as f_60,
			"primaryPayerPaidAmount" as f_61,
			"professionalComponentCharge" as f_62,
			"patientStatusCd" as f_63,
			"noncoveredStayFromDate" as f_64,
			"noncoveredStayThroughDate" as f_65,
			"weeklyProcessDate" as f_66,
			"operatingPhysicianNpi" as f_67,
			"operatingPhysicianUpin" as f_68,
			"organizationNpi" as f_69,
			"otherPhysicianNpi" as f_70,
			"otherPhysicianUpin" as f_71,
			"diagnosisPrincipalCode" as f_72,
			"diagnosisPrincipalCodeVersion" as f_73,
			"providerNumber" as f_74,
			"providerStateCode" as f_75,
			"patientDischargeStatusCode" as f_76,
			"totalChargeAmount" as f_77,
			"diagnosisExternal1PresentOnAdmissionCode" as f_78,
			"diagnosisExternal2PresentOnAdmissionCode" as f_79,
			"diagnosisExternal3PresentOnAdmissionCode" as f_80,
			"diagnosisExternal4PresentOnAdmissionCode" as f_81,
			"diagnosisExternal5PresentOnAdmissionCode" as f_82,
			"diagnosisExternal6PresentOnAdmissionCode" as f_83,
			"diagnosisExternal7PresentOnAdmissionCode" as f_84,
			"diagnosisExternal8PresentOnAdmissionCode" as f_85,
			"diagnosisExternal9PresentOnAdmissionCode" as f_86,
			"diagnosisExternal10PresentOnAdmissionCode" as f_87,
			"diagnosisExternal11PresentOnAdmissionCode" as f_88,
			"diagnosisExternal12PresentOnAdmissionCode" as f_89,
			"diagnosis1PresentOnAdmissionCode" as f_90,
			"diagnosis2PresentOnAdmissionCode" as f_91,
			"diagnosis3PresentOnAdmissionCode" as f_92,
			"diagnosis4PresentOnAdmissionCode" as f_93,
			"diagnosis5PresentOnAdmissionCode" as f_94,
			"diagnosis6PresentOnAdmissionCode" as f_95,
			"diagnosis7PresentOnAdmissionCode" as f_96,
			"diagnosis8PresentOnAdmissionCode" as f_97,
			"diagnosis9PresentOnAdmissionCode" as f_98,
			"diagnosis10PresentOnAdmissionCode" as f_99,
			"diagnosis11PresentOnAdmissionCode" as f_100,
			"diagnosis12PresentOnAdmissionCode" as f_101,
			"diagnosis13PresentOnAdmissionCode" as f_102,
			"diagnosis14PresentOnAdmissionCode" as f_103,
			"diagnosis15PresentOnAdmissionCode" as f_104,
			"diagnosis16PresentOnAdmissionCode" as f_105,
			"diagnosis17PresentOnAdmissionCode" as f_106,
			"diagnosis18PresentOnAdmissionCode" as f_107,
			"diagnosis19PresentOnAdmissionCode" as f_108,
			"diagnosis20PresentOnAdmissionCode" as f_109,
			"diagnosis21PresentOnAdmissionCode" as f_110,
			"diagnosis22PresentOnAdmissionCode" as f_111,
			"diagnosis23PresentOnAdmissionCode" as f_112,
			"diagnosis24PresentOnAdmissionCode" as f_113,
			"diagnosis25PresentOnAdmissionCode" as f_114,
			"diagnosis1Code" as f_115,
			"diagnosis2Code" as f_116,
			"diagnosis3Code" as f_117,
			"diagnosis4Code" as f_118,
			"diagnosis5Code" as f_119,
			"diagnosis6Code" as f_120,
			"diagnosis7Code" as f_121,
			"diagnosis8Code" as f_122,
			"diagnosis9Code" as f_123,
			"diagnosis10Code" as f_124,
			"diagnosis11Code" as f_125,
			"diagnosis12Code" as f_126,
			"diagnosis13Code" as f_127,
			"diagnosis14Code" as f_128,
			"diagnosis15Code" as f_129,
			"diagnosis16Code" as f_130,
			"diagnosis17Code" as f_131,
			"diagnosis18Code" as f_132,
			"diagnosis19Code" as f_133,
			"diagnosis20Code" as f_134,
			"diagnosis21Code" as f_135,
			"diagnosis22Code" as f_136,
			"diagnosis23Code" as f_137,
			"diagnosis24Code" as f_138,
			"diagnosis25Code" as f_139,
			"diagnosisExternal1Code" as f_140,
			"diagnosisExternal2Code" as f_141,
			"diagnosisExternal3Code" as f_142,
			"diagnosisExternal4Code" as f_143,
			"diagnosisExternal5Code" as f_144,
			"diagnosisExternal6Code" as f_145,
			"diagnosisExternal7Code" as f_146,
			"diagnosisExternal8Code" as f_147,
			"diagnosisExternal9Code" as f_148,
			"diagnosisExternal10Code" as f_149,
			"diagnosisExternal11Code" as f_150,
			"diagnosisExternal12Code" as f_151,
			"diagnosisExternal1CodeVersion" as f_152,
			"diagnosisExternal2CodeVersion" as f_153,
			"diagnosisExternal3CodeVersion" as f_154,
			"diagnosisExternal4CodeVersion" as f_155,
			"diagnosisExternal5CodeVersion" as f_156,
			"diagnosisExternal6CodeVersion" as f_157,
			"diagnosisExternal7CodeVersion" as f_158,
			"diagnosisExternal8CodeVersion" as f_159,
			"diagnosisExternal9CodeVersion" as f_160,
			"diagnosisExternal10CodeVersion" as f_161,
			"diagnosisExternal11CodeVersion" as f_162,
			"diagnosisExternal12CodeVersion" as f_163,
			"diagnosis1CodeVersion" as f_164,
			"diagnosis2CodeVersion" as f_165,
			"diagnosis3CodeVersion" as f_166,
			"diagnosis4CodeVersion" as f_167,
			"diagnosis5CodeVersion" as f_168,
			"diagnosis6CodeVersion" as f_169,
			"diagnosis7CodeVersion" as f_170,
			"diagnosis8CodeVersion" as f_171,
			"diagnosis9CodeVersion" as f_172,
			"diagnosis10CodeVersion" as f_173,
			"diagnosis11CodeVersion" as f_174,
			"diagnosis12CodeVersion" as f_175,
			"diagnosis13CodeVersion" as f_176,
			"diagnosis14CodeVersion" as f_177,
			"diagnosis15CodeVersion" as f_178,
			"diagnosis16CodeVersion" as f_179,
			"diagnosis17CodeVersion" as f_180,
			"diagnosis18CodeVersion" as f_181,
			"diagnosis19CodeVersion" as f_182,
			"diagnosis20CodeVersion" as f_183,
			"diagnosis21CodeVersion" as f_184,
			"diagnosis22CodeVersion" as f_185,
			"diagnosis23CodeVersion" as f_186,
			"diagnosis24CodeVersion" as f_187,
			"diagnosis25CodeVersion" as f_188,
			"procedure1Code" as f_189,
			"procedure2Code" as f_190,
			"procedure3Code" as f_191,
			"procedure4Code" as f_192,
			"procedure5Code" as f_193,
			"procedure6Code" as f_194,
			"procedure7Code" as f_195,
			"procedure8Code" as f_196,
			"procedure9Code" as f_197,
			"procedure10Code" as f_198,
			"procedure11Code" as f_199,
			"procedure12Code" as f_200,
			"procedure13Code" as f_201,
			"procedure14Code" as f_202,
			"procedure15Code" as f_203,
			"procedure16Code" as f_204,
			"procedure17Code" as f_205,
			"procedure18Code" as f_206,
			"procedure19Code" as f_207,
			"procedure20Code" as f_208,
			"procedure21Code" as f_209,
			"procedure22Code" as f_210,
			"procedure23Code" as f_211,
			"procedure24Code" as f_212,
			"procedure25Code" as f_213,
			"procedure1CodeVersion" as f_214,
			"procedure2CodeVersion" as f_215,
			"procedure3CodeVersion" as f_216,
			"procedure4CodeVersion" as f_217,
			"procedure5CodeVersion" as f_218,
			"procedure6CodeVersion" as f_219,
			"procedure7CodeVersion" as f_220,
			"procedure8CodeVersion" as f_221,
			"procedure9CodeVersion" as f_222,
			"procedure10CodeVersion" as f_223,
			"procedure11CodeVersion" as f_224,
			"procedure12CodeVersion" as f_225,
			"procedure13CodeVersion" as f_226,
			"procedure14CodeVersion" as f_227,
			"procedure15CodeVersion" as f_228,
			"procedure16CodeVersion" as f_229,
			"procedure17CodeVersion" as f_230,
			"procedure18CodeVersion" as f_231,
			"procedure19CodeVersion" as f_232,
			"procedure20CodeVersion" as f_233,
			"procedure21CodeVersion" as f_234,
			"procedure22CodeVersion" as f_235,
			"procedure23CodeVersion" as f_236,
			"procedure24CodeVersion" as f_237,
			"procedure25CodeVersion" as f_238,
			"procedure1Date" as f_239,
			"procedure2Date" as f_240,
			"procedure3Date" as f_241,
			"procedure4Date" as f_242,
			"procedure5Date" as f_243,
			"procedure6Date" as f_244,
			"procedure7Date" as f_245,
			"procedure8Date" as f_246,
			"procedure9Date" as f_247,
			"procedure10Date" as f_248,
			"procedure11Date" as f_249,
			"procedure12Date" as f_250,
			"procedure13Date" as f_251,
			"procedure14Date" as f_252,
			"procedure15Date" as f_253,
			"procedure16Date" as f_254,
			"procedure17Date" as f_255,
			"procedure18Date" as f_256,
			"procedure19Date" as f_257,
			"procedure20Date" as f_258,
			"procedure21Date" as f_259,
			"procedure22Date" as f_260,
			"procedure23Date" as f_261,
			"procedure24Date" as f_262,
			"procedure25Date" as f_263
		from
			"InpatientClaims"
		where
			"claimId" = v_clm_id::text;
		
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
		if curr.f_207 <> orig.f_207 then err_cnt := err_cnt + 1; end if;
		if curr.f_208 <> orig.f_208 then err_cnt := err_cnt + 1; end if;
		if curr.f_209 <> orig.f_209 then err_cnt := err_cnt + 1; end if;
		if curr.f_210 <> orig.f_210 then err_cnt := err_cnt + 1; end if;
		if curr.f_211 <> orig.f_211 then err_cnt := err_cnt + 1; end if;
		if curr.f_212 <> orig.f_212 then err_cnt := err_cnt + 1; end if;
		if curr.f_213 <> orig.f_213 then err_cnt := err_cnt + 1; end if;
		if curr.f_214 <> orig.f_214 then err_cnt := err_cnt + 1; end if;
		if curr.f_215 <> orig.f_215 then err_cnt := err_cnt + 1; end if;
		if curr.f_216 <> orig.f_216 then err_cnt := err_cnt + 1; end if;
		if curr.f_217 <> orig.f_217 then err_cnt := err_cnt + 1; end if;
		if curr.f_218 <> orig.f_218 then err_cnt := err_cnt + 1; end if;
		if curr.f_219 <> orig.f_219 then err_cnt := err_cnt + 1; end if;
		if curr.f_220 <> orig.f_220 then err_cnt := err_cnt + 1; end if;
		if curr.f_221 <> orig.f_221 then err_cnt := err_cnt + 1; end if;
		if curr.f_222 <> orig.f_222 then err_cnt := err_cnt + 1; end if;
		if curr.f_223 <> orig.f_223 then err_cnt := err_cnt + 1; end if;
		if curr.f_224 <> orig.f_224 then err_cnt := err_cnt + 1; end if;
		if curr.f_225 <> orig.f_225 then err_cnt := err_cnt + 1; end if;
		if curr.f_226 <> orig.f_226 then err_cnt := err_cnt + 1; end if;
		if curr.f_227 <> orig.f_227 then err_cnt := err_cnt + 1; end if;
		if curr.f_228 <> orig.f_228 then err_cnt := err_cnt + 1; end if;
		if curr.f_229 <> orig.f_229 then err_cnt := err_cnt + 1; end if;
		if curr.f_230 <> orig.f_230 then err_cnt := err_cnt + 1; end if;
		if curr.f_231 <> orig.f_231 then err_cnt := err_cnt + 1; end if;
		if curr.f_232 <> orig.f_232 then err_cnt := err_cnt + 1; end if;
		if curr.f_233 <> orig.f_233 then err_cnt := err_cnt + 1; end if;
		if curr.f_234 <> orig.f_234 then err_cnt := err_cnt + 1; end if;
		if curr.f_235 <> orig.f_235 then err_cnt := err_cnt + 1; end if;
		if curr.f_236 <> orig.f_236 then err_cnt := err_cnt + 1; end if;
		if curr.f_237 <> orig.f_237 then err_cnt := err_cnt + 1; end if;
		if curr.f_238 <> orig.f_238 then err_cnt := err_cnt + 1; end if;
		if curr.f_239 <> orig.f_239 then err_cnt := err_cnt + 1; end if;
		if curr.f_240 <> orig.f_240 then err_cnt := err_cnt + 1; end if;
		if curr.f_241 <> orig.f_241 then err_cnt := err_cnt + 1; end if;
		if curr.f_242 <> orig.f_242 then err_cnt := err_cnt + 1; end if;
		if curr.f_243 <> orig.f_243 then err_cnt := err_cnt + 1; end if;
		if curr.f_244 <> orig.f_244 then err_cnt := err_cnt + 1; end if;
		if curr.f_245 <> orig.f_245 then err_cnt := err_cnt + 1; end if;
		if curr.f_246 <> orig.f_246 then err_cnt := err_cnt + 1; end if;
		if curr.f_247 <> orig.f_247 then err_cnt := err_cnt + 1; end if;
		if curr.f_248 <> orig.f_248 then err_cnt := err_cnt + 1; end if;
		if curr.f_249 <> orig.f_249 then err_cnt := err_cnt + 1; end if;
		if curr.f_250 <> orig.f_250 then err_cnt := err_cnt + 1; end if;
		if curr.f_251 <> orig.f_251 then err_cnt := err_cnt + 1; end if;
		if curr.f_252 <> orig.f_252 then err_cnt := err_cnt + 1; end if;
		if curr.f_253 <> orig.f_253 then err_cnt := err_cnt + 1; end if;
		if curr.f_254 <> orig.f_254 then err_cnt := err_cnt + 1; end if;
		if curr.f_255 <> orig.f_255 then err_cnt := err_cnt + 1; end if;
		if curr.f_256 <> orig.f_256 then err_cnt := err_cnt + 1; end if;
		if curr.f_257 <> orig.f_257 then err_cnt := err_cnt + 1; end if;
		if curr.f_258 <> orig.f_258 then err_cnt := err_cnt + 1; end if;
		if curr.f_259 <> orig.f_259 then err_cnt := err_cnt + 1; end if;
		if curr.f_260 <> orig.f_260 then err_cnt := err_cnt + 1; end if;
		if curr.f_261 <> orig.f_261 then err_cnt := err_cnt + 1; end if;
		if curr.f_262 <> orig.f_262 then err_cnt := err_cnt + 1; end if;
		if curr.f_263 <> orig.f_263 then err_cnt := err_cnt + 1; end if;
	
		if err_cnt > 0 then
			insert into migration_errors values(v_tbl_name, v_bene_id, v_clm_id, null, err_cnt);
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