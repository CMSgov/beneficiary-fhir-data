do $$
DECLARE
  MAX_TESTS		INTEGER := 500;		-- hopefully .5M tests are sufficient
  v_claims		BIGINT[];
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_clm_id      bigint  := 0;
  v_tbl_name	varchar(40) := 'outpatient_claims';

BEGIN
	v_claims := ARRAY(
		SELECT cast("claimId" as bigint)
		FROM "OutpatientClaims" TABLESAMPLE BERNOULLI(50)	-- bernoulli sample using 50% of table rows
		limit MAX_TESTS);

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
			clm_fac_type_cd as f_7,
			clm_freq_cd as f_8,
			clm_mco_pd_sw as f_9,
			clm_mdcr_non_pmt_rsn_cd as f_10,
			clm_pmt_amt as f_11,
			clm_srvc_clsfctn_type_cd as f_12,
			at_physn_npi as f_13,
			at_physn_upin as f_14,
			claim_query_code as f_15,
			fi_clm_proc_dt as f_16,
			fi_doc_clm_cntl_num as f_17,
			fi_num as f_18,
			fi_orig_clm_cntl_num as f_19,
			final_action as f_20,
			fst_dgns_e_cd as f_21,
			fst_dgns_e_vrsn_cd as f_22,
			clm_op_bene_pmt_amt as f_23,
			nch_bene_ptb_coinsrnc_amt as f_24,
			nch_bene_blood_ddctbl_lblty_am as f_25,
			nch_bene_ptb_ddctbl_amt as f_26,
			nch_clm_type_cd as f_27,
			nch_near_line_rec_ident_cd as f_28,
			nch_prmry_pyr_cd as f_29,
			nch_prmry_pyr_clm_pd_amt as f_30,
			nch_profnl_cmpnt_chrg_amt as f_31,
			nch_wkly_proc_dt as f_32,
			op_physn_npi as f_33,
			op_physn_upin as f_34,
			org_npi_num as f_35,
			ot_physn_npi as f_36,
			ot_physn_upin as f_37,
			prncpal_dgns_cd as f_38,
			prncpal_dgns_vrsn_cd as f_39,
			prvdr_num as f_40,
			prvdr_state_cd as f_41,
			ptnt_dschrg_stus_cd as f_42,
			clm_op_prvdr_pmt_amt as f_43,
			clm_tot_chrg_amt as f_44,
			rsn_visit_cd1 as f_45,
			rsn_visit_vrsn_cd1 as f_46,
			rsn_visit_cd2 as f_47,
			rsn_visit_vrsn_cd2 as f_48,
			rsn_visit_cd3 as f_49,
			rsn_visit_vrsn_cd3 as f_50,
			icd_dgns_cd1 as f_51,
			icd_dgns_cd2 as f_52,
			icd_dgns_cd3 as f_53,
			icd_dgns_cd4 as f_54,
			icd_dgns_cd5 as f_55,
			icd_dgns_cd6 as f_56,
			icd_dgns_cd7 as f_57,
			icd_dgns_cd8 as f_58,
			icd_dgns_cd9 as f_59,
			icd_dgns_cd10 as f_60,
			icd_dgns_cd11 as f_61,
			icd_dgns_cd12 as f_62,
			icd_dgns_cd13 as f_63,
			icd_dgns_cd14 as f_64,
			icd_dgns_cd15 as f_65,
			icd_dgns_cd16 as f_66,
			icd_dgns_cd17 as f_67,
			icd_dgns_cd18 as f_68,
			icd_dgns_cd19 as f_69,
			icd_dgns_cd20 as f_70,
			icd_dgns_cd21 as f_71,
			icd_dgns_cd22 as f_72,
			icd_dgns_cd23 as f_73,
			icd_dgns_cd24 as f_74,
			icd_dgns_cd25 as f_75,
			icd_dgns_e_cd1 as f_76,
			icd_dgns_e_cd2 as f_77,
			icd_dgns_e_cd3 as f_78,
			icd_dgns_e_cd4 as f_79,
			icd_dgns_e_cd5 as f_80,
			icd_dgns_e_cd6 as f_81,
			icd_dgns_e_cd7 as f_82,
			icd_dgns_e_cd8 as f_83,
			icd_dgns_e_cd9 as f_84,
			icd_dgns_e_cd10 as f_85,
			icd_dgns_e_cd11 as f_86,
			icd_dgns_e_cd12 as f_87,
			icd_dgns_e_vrsn_cd1 as f_88,
			icd_dgns_e_vrsn_cd2 as f_89,
			icd_dgns_e_vrsn_cd3 as f_90,
			icd_dgns_e_vrsn_cd4 as f_91,
			icd_dgns_e_vrsn_cd5 as f_92,
			icd_dgns_e_vrsn_cd6 as f_93,
			icd_dgns_e_vrsn_cd7 as f_94,
			icd_dgns_e_vrsn_cd8 as f_95,
			icd_dgns_e_vrsn_cd9 as f_96,
			icd_dgns_e_vrsn_cd10 as f_97,
			icd_dgns_e_vrsn_cd11 as f_98,
			icd_dgns_e_vrsn_cd12 as f_99,
			icd_dgns_vrsn_cd1 as f_100,
			icd_dgns_vrsn_cd2 as f_101,
			icd_dgns_vrsn_cd3 as f_102,
			icd_dgns_vrsn_cd4 as f_103,
			icd_dgns_vrsn_cd5 as f_104,
			icd_dgns_vrsn_cd6 as f_105,
			icd_dgns_vrsn_cd7 as f_106,
			icd_dgns_vrsn_cd8 as f_107,
			icd_dgns_vrsn_cd9 as f_108,
			icd_dgns_vrsn_cd10 as f_109,
			icd_dgns_vrsn_cd11 as f_110,
			icd_dgns_vrsn_cd12 as f_111,
			icd_dgns_vrsn_cd13 as f_112,
			icd_dgns_vrsn_cd14 as f_113,
			icd_dgns_vrsn_cd15 as f_114,
			icd_dgns_vrsn_cd16 as f_115,
			icd_dgns_vrsn_cd17 as f_116,
			icd_dgns_vrsn_cd18 as f_117,
			icd_dgns_vrsn_cd19 as f_118,
			icd_dgns_vrsn_cd20 as f_119,
			icd_dgns_vrsn_cd21 as f_120,
			icd_dgns_vrsn_cd22 as f_121,
			icd_dgns_vrsn_cd23 as f_122,
			icd_dgns_vrsn_cd24 as f_123,
			icd_dgns_vrsn_cd25 as f_124,
			icd_prcdr_cd1 as f_125,
			icd_prcdr_cd2 as f_126,
			icd_prcdr_cd3 as f_127,
			icd_prcdr_cd4 as f_128,
			icd_prcdr_cd5 as f_129,
			icd_prcdr_cd6 as f_130,
			icd_prcdr_cd7 as f_131,
			icd_prcdr_cd8 as f_132,
			icd_prcdr_cd9 as f_133,
			icd_prcdr_cd10 as f_134,
			icd_prcdr_cd11 as f_135,
			icd_prcdr_cd12 as f_136,
			icd_prcdr_cd13 as f_137,
			icd_prcdr_cd14 as f_138,
			icd_prcdr_cd15 as f_139,
			icd_prcdr_cd16 as f_140,
			icd_prcdr_cd17 as f_141,
			icd_prcdr_cd18 as f_142,
			icd_prcdr_cd19 as f_143,
			icd_prcdr_cd20 as f_144,
			icd_prcdr_cd21 as f_145,
			icd_prcdr_cd22 as f_146,
			icd_prcdr_cd23 as f_147,
			icd_prcdr_cd24 as f_148,
			icd_prcdr_cd25 as f_149,
			icd_prcdr_vrsn_cd1 as f_150,
			icd_prcdr_vrsn_cd2 as f_151,
			icd_prcdr_vrsn_cd3 as f_152,
			icd_prcdr_vrsn_cd4 as f_153,
			icd_prcdr_vrsn_cd5 as f_154,
			icd_prcdr_vrsn_cd6 as f_155,
			icd_prcdr_vrsn_cd7 as f_156,
			icd_prcdr_vrsn_cd8 as f_157,
			icd_prcdr_vrsn_cd9 as f_158,
			icd_prcdr_vrsn_cd10 as f_159,
			icd_prcdr_vrsn_cd11 as f_160,
			icd_prcdr_vrsn_cd12 as f_161,
			icd_prcdr_vrsn_cd13 as f_162,
			icd_prcdr_vrsn_cd14 as f_163,
			icd_prcdr_vrsn_cd15 as f_164,
			icd_prcdr_vrsn_cd16 as f_165,
			icd_prcdr_vrsn_cd17 as f_166,
			icd_prcdr_vrsn_cd18 as f_167,
			icd_prcdr_vrsn_cd19 as f_168,
			icd_prcdr_vrsn_cd20 as f_169,
			icd_prcdr_vrsn_cd21 as f_170,
			icd_prcdr_vrsn_cd22 as f_171,
			icd_prcdr_vrsn_cd23 as f_172,
			icd_prcdr_vrsn_cd24 as f_173,
			icd_prcdr_vrsn_cd25 as f_174,
			prcdr_dt1 as f_175,
			prcdr_dt2 as f_176,
			prcdr_dt3 as f_177,
			prcdr_dt4 as f_178,
			prcdr_dt5 as f_179,
			prcdr_dt6 as f_180,
			prcdr_dt7 as f_181,
			prcdr_dt8 as f_182,
			prcdr_dt9 as f_183,
			prcdr_dt10 as f_184,
			prcdr_dt11 as f_185,
			prcdr_dt12 as f_186,
			prcdr_dt13 as f_187,
			prcdr_dt14 as f_188,
			prcdr_dt15 as f_189,
			prcdr_dt16 as f_190,
			prcdr_dt17 as f_191,
			prcdr_dt18 as f_192,
			prcdr_dt19 as f_193,
			prcdr_dt20 as f_194,
			prcdr_dt21 as f_195,
			prcdr_dt22 as f_196,
			prcdr_dt23 as f_197,
			prcdr_dt24 as f_198,
			prcdr_dt25 as f_199
		from
			outpatient_claims
		WHERE
			clm_id = v_clm_id;
		

		SELECT INTO orig
			cast("claimId" as bigint) as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			cast("claimGroupId" as bigint) as f_3,	
			"lastupdated" as f_4,
			"dateFrom" as f_5,
			"dateThrough" as f_6,
			"claimFacilityTypeCode" as f_7,
			"claimFrequencyCode" as f_8,
			"mcoPaidSw" as f_9,
			"claimNonPaymentReasonCode" as f_10,
			"paymentAmount" as f_11,
			"claimServiceClassificationTypeCode" as f_12,
			"attendingPhysicianNpi" as f_13,
			"attendingPhysicianUpin" as f_14,
			"claimQueryCode" as f_15,
			"fiscalIntermediaryClaimProcessDate" as f_16,
			"fiDocumentClaimControlNumber" as f_17,
			"fiscalIntermediaryNumber" as f_18,
			"fiOriginalClaimControlNumber" as f_19,
			"finalAction" as f_20,
			"diagnosisExternalFirstCode" as f_21,
			"diagnosisExternalFirstCodeVersion" as f_22,
			"beneficiaryPaymentAmount" as f_23,
			"coinsuranceAmount" as f_24,
			"bloodDeductibleLiabilityAmount" as f_25,
			"deductibleAmount" as f_26,
			"claimTypeCode" as f_27,
			"nearLineRecordIdCode" as f_28,
			"claimPrimaryPayerCode" as f_29,
			"primaryPayerPaidAmount" as f_30,
			"professionalComponentCharge" as f_31,
			"weeklyProcessDate" as f_32,
			"operatingPhysicianNpi" as f_33,
			"operatingPhysicianUpin" as f_34,
			"organizationNpi" as f_35,
			"otherPhysicianNpi" as f_36,
			"otherPhysicianUpin" as f_37,
			"diagnosisPrincipalCode" as f_38,
			"diagnosisPrincipalCodeVersion" as f_39,
			"providerNumber" as f_40,
			"providerStateCode" as f_41,
			"patientDischargeStatusCode" as f_42,
			"providerPaymentAmount" as f_43,
			"totalChargeAmount" as f_44,
			"diagnosisAdmission1Code" as f_45,
			"diagnosisAdmission1CodeVersion" as f_46,
			"diagnosisAdmission2Code" as f_47,
			"diagnosisAdmission2CodeVersion" as f_48,
			"diagnosisAdmission3Code" as f_49,
			"diagnosisAdmission3CodeVersion" as f_50,
			"diagnosis1Code" as f_51,
			"diagnosis2Code" as f_52,
			"diagnosis3Code" as f_53,
			"diagnosis4Code" as f_54,
			"diagnosis5Code" as f_55,
			"diagnosis6Code" as f_56,
			"diagnosis7Code" as f_57,
			"diagnosis8Code" as f_58,
			"diagnosis9Code" as f_59,
			"diagnosis10Code" as f_60,
			"diagnosis11Code" as f_61,
			"diagnosis12Code" as f_62,
			"diagnosis13Code" as f_63,
			"diagnosis14Code" as f_64,
			"diagnosis15Code" as f_65,
			"diagnosis16Code" as f_66,
			"diagnosis17Code" as f_67,
			"diagnosis18Code" as f_68,
			"diagnosis19Code" as f_69,
			"diagnosis20Code" as f_70,
			"diagnosis21Code" as f_71,
			"diagnosis22Code" as f_72,
			"diagnosis23Code" as f_73,
			"diagnosis24Code" as f_74,
			"diagnosis25Code" as f_75,
			"diagnosisExternal1Code" as f_76,
			"diagnosisExternal2Code" as f_77,
			"diagnosisExternal3Code" as f_78,
			"diagnosisExternal4Code" as f_79,
			"diagnosisExternal5Code" as f_80,
			"diagnosisExternal6Code" as f_81,
			"diagnosisExternal7Code" as f_82,
			"diagnosisExternal8Code" as f_83,
			"diagnosisExternal9Code" as f_84,
			"diagnosisExternal10Code" as f_85,
			"diagnosisExternal11Code" as f_86,
			"diagnosisExternal12Code" as f_87,
			"diagnosisExternal1CodeVersion" as f_88,
			"diagnosisExternal2CodeVersion" as f_89,
			"diagnosisExternal3CodeVersion" as f_90,
			"diagnosisExternal4CodeVersion" as f_91,
			"diagnosisExternal5CodeVersion" as f_92,
			"diagnosisExternal6CodeVersion" as f_93,
			"diagnosisExternal7CodeVersion" as f_94,
			"diagnosisExternal8CodeVersion" as f_95,
			"diagnosisExternal9CodeVersion" as f_96,
			"diagnosisExternal10CodeVersion" as f_97,
			"diagnosisExternal11CodeVersion" as f_98,
			"diagnosisExternal12CodeVersion" as f_99,
			"diagnosis1CodeVersion" as f_100,
			"diagnosis2CodeVersion" as f_101,
			"diagnosis3CodeVersion" as f_102,
			"diagnosis4CodeVersion" as f_103,
			"diagnosis5CodeVersion" as f_104,
			"diagnosis6CodeVersion" as f_105,
			"diagnosis7CodeVersion" as f_106,
			"diagnosis8CodeVersion" as f_107,
			"diagnosis9CodeVersion" as f_108,
			"diagnosis10CodeVersion" as f_109,
			"diagnosis11CodeVersion" as f_110,
			"diagnosis12CodeVersion" as f_111,
			"diagnosis13CodeVersion" as f_112,
			"diagnosis14CodeVersion" as f_113,
			"diagnosis15CodeVersion" as f_114,
			"diagnosis16CodeVersion" as f_115,
			"diagnosis17CodeVersion" as f_116,
			"diagnosis18CodeVersion" as f_117,
			"diagnosis19CodeVersion" as f_118,
			"diagnosis20CodeVersion" as f_119,
			"diagnosis21CodeVersion" as f_120,
			"diagnosis22CodeVersion" as f_121,
			"diagnosis23CodeVersion" as f_122,
			"diagnosis24CodeVersion" as f_123,
			"diagnosis25CodeVersion" as f_124,
			"procedure1Code" as f_125,
			"procedure2Code" as f_126,
			"procedure3Code" as f_127,
			"procedure4Code" as f_128,
			"procedure5Code" as f_129,
			"procedure6Code" as f_130,
			"procedure7Code" as f_131,
			"procedure8Code" as f_132,
			"procedure9Code" as f_133,
			"procedure10Code" as f_134,
			"procedure11Code" as f_135,
			"procedure12Code" as f_136,
			"procedure13Code" as f_137,
			"procedure14Code" as f_138,
			"procedure15Code" as f_139,
			"procedure16Code" as f_140,
			"procedure17Code" as f_141,
			"procedure18Code" as f_142,
			"procedure19Code" as f_143,
			"procedure20Code" as f_144,
			"procedure21Code" as f_145,
			"procedure22Code" as f_146,
			"procedure23Code" as f_147,
			"procedure24Code" as f_148,
			"procedure25Code" as f_149,
			"procedure1CodeVersion" as f_150,
			"procedure2CodeVersion" as f_151,
			"procedure3CodeVersion" as f_152,
			"procedure4CodeVersion" as f_153,
			"procedure5CodeVersion" as f_154,
			"procedure6CodeVersion" as f_155,
			"procedure7CodeVersion" as f_156,
			"procedure8CodeVersion" as f_157,
			"procedure9CodeVersion" as f_158,
			"procedure10CodeVersion" as f_159,
			"procedure11CodeVersion" as f_160,
			"procedure12CodeVersion" as f_161,
			"procedure13CodeVersion" as f_162,
			"procedure14CodeVersion" as f_163,
			"procedure15CodeVersion" as f_164,
			"procedure16CodeVersion" as f_165,
			"procedure17CodeVersion" as f_166,
			"procedure18CodeVersion" as f_167,
			"procedure19CodeVersion" as f_168,
			"procedure20CodeVersion" as f_169,
			"procedure21CodeVersion" as f_170,
			"procedure22CodeVersion" as f_171,
			"procedure23CodeVersion" as f_172,
			"procedure24CodeVersion" as f_173,
			"procedure25CodeVersion" as f_174,
			"procedure1Date" as f_175,
			"procedure2Date" as f_176,
			"procedure3Date" as f_177,
			"procedure4Date" as f_178,
			"procedure5Date" as f_179,
			"procedure6Date" as f_180,
			"procedure7Date" as f_181,
			"procedure8Date" as f_182,
			"procedure9Date" as f_183,
			"procedure10Date" as f_184,
			"procedure11Date" as f_185,
			"procedure12Date" as f_186,
			"procedure13Date" as f_187,
			"procedure14Date" as f_188,
			"procedure15Date" as f_189,
			"procedure16Date" as f_190,
			"procedure17Date" as f_191,
			"procedure18Date" as f_192,
			"procedure19Date" as f_193,
			"procedure20Date" as f_194,
			"procedure21Date" as f_195,
			"procedure22Date" as f_196,
			"procedure23Date" as f_197,
			"procedure24Date" as f_198,
			"procedure25Date" as f_199
		from
			"OutpatientClaims"
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