do $$
DECLARE
  MAX_TESTS		INTEGER := 500;		-- hopefully .5M tests are sufficient
  v_claims		BIGINT[];
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_clm_id      bigint  := 0;
  v_tbl_name	varchar(40) := 'hospice_claims';

BEGIN
	v_claims := ARRAY(
		SELECT cast("claimId" as bigint)
		FROM "HospiceClaims" TABLESAMPLE BERNOULLI(50)	-- bernoulli sample using 50% of table rows
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
			clm_hospc_start_dt_id as f_9,
			clm_mdcr_non_pmt_rsn_cd as f_10,
			clm_pmt_amt as f_11,
			clm_srvc_clsfctn_type_cd as f_12,
			clm_utlztn_day_cnt as f_13,
			at_physn_npi as f_14,
			at_physn_upin as f_15,
			bene_hospc_prd_cnt as f_16,
			fi_clm_proc_dt as f_17,
			fi_doc_clm_cntl_num as f_18,
			fi_num as f_19,
			fi_orig_clm_cntl_num as f_20,
			final_action as f_21,
			fst_dgns_e_cd as f_22,
			fst_dgns_e_vrsn_cd as f_23,
			nch_bene_dschrg_dt as f_24,
			nch_clm_type_cd as f_25,
			nch_near_line_rec_ident_cd as f_26,
			nch_prmry_pyr_cd as f_27,
			nch_prmry_pyr_clm_pd_amt as f_28,
			nch_ptnt_status_ind_cd as f_29,
			nch_wkly_proc_dt as f_30,
			org_npi_num as f_31,
			prncpal_dgns_cd as f_32,
			prncpal_dgns_vrsn_cd as f_33,
			prvdr_num as f_34,
			prvdr_state_cd as f_35,
			ptnt_dschrg_stus_cd as f_36,
			clm_tot_chrg_amt as f_37,
			icd_dgns_cd1 as f_38,
			icd_dgns_cd2 as f_39,
			icd_dgns_cd3 as f_40,
			icd_dgns_cd4 as f_41,
			icd_dgns_cd5 as f_42,
			icd_dgns_cd6 as f_43,
			icd_dgns_cd7 as f_44,
			icd_dgns_cd8 as f_45,
			icd_dgns_cd9 as f_46,
			icd_dgns_cd10 as f_47,
			icd_dgns_cd11 as f_48,
			icd_dgns_cd12 as f_49,
			icd_dgns_cd13 as f_50,
			icd_dgns_cd14 as f_51,
			icd_dgns_cd15 as f_52,
			icd_dgns_cd16 as f_53,
			icd_dgns_cd17 as f_54,
			icd_dgns_cd18 as f_55,
			icd_dgns_cd19 as f_56,
			icd_dgns_cd20 as f_57,
			icd_dgns_cd21 as f_58,
			icd_dgns_cd22 as f_59,
			icd_dgns_cd23 as f_60,
			icd_dgns_cd24 as f_61,
			icd_dgns_cd25 as f_62,
			icd_dgns_e_cd1 as f_63,
			icd_dgns_e_cd2 as f_64,
			icd_dgns_e_cd3 as f_65,
			icd_dgns_e_cd4 as f_66,
			icd_dgns_e_cd5 as f_67,
			icd_dgns_e_cd6 as f_68,
			icd_dgns_e_cd7 as f_69,
			icd_dgns_e_cd8 as f_70,
			icd_dgns_e_cd9 as f_71,
			icd_dgns_e_cd10 as f_72,
			icd_dgns_e_cd11 as f_73,
			icd_dgns_e_cd12 as f_74,
			icd_dgns_e_vrsn_cd1 as f_75,
			icd_dgns_e_vrsn_cd2 as f_76,
			icd_dgns_e_vrsn_cd3 as f_77,
			icd_dgns_e_vrsn_cd4 as f_78,
			icd_dgns_e_vrsn_cd5 as f_79,
			icd_dgns_e_vrsn_cd6 as f_80,
			icd_dgns_e_vrsn_cd7 as f_81,
			icd_dgns_e_vrsn_cd8 as f_82,
			icd_dgns_e_vrsn_cd9 as f_83,
			icd_dgns_e_vrsn_cd10 as f_84,
			icd_dgns_e_vrsn_cd11 as f_85,
			icd_dgns_e_vrsn_cd12 as f_86,
			icd_dgns_vrsn_cd1 as f_87,
			icd_dgns_vrsn_cd2 as f_88,
			icd_dgns_vrsn_cd3 as f_89,
			icd_dgns_vrsn_cd4 as f_90,
			icd_dgns_vrsn_cd5 as f_91,
			icd_dgns_vrsn_cd6 as f_92,
			icd_dgns_vrsn_cd7 as f_93,
			icd_dgns_vrsn_cd8 as f_94,
			icd_dgns_vrsn_cd9 as f_95,
			icd_dgns_vrsn_cd10 as f_96,
			icd_dgns_vrsn_cd11 as f_97,
			icd_dgns_vrsn_cd12 as f_98,
			icd_dgns_vrsn_cd13 as f_99,
			icd_dgns_vrsn_cd14 as f_100,
			icd_dgns_vrsn_cd15 as f_101,
			icd_dgns_vrsn_cd16 as f_102,
			icd_dgns_vrsn_cd17 as f_103,
			icd_dgns_vrsn_cd18 as f_104,
			icd_dgns_vrsn_cd19 as f_105,
			icd_dgns_vrsn_cd20 as f_106,
			icd_dgns_vrsn_cd21 as f_107,
			icd_dgns_vrsn_cd22 as f_108,
			icd_dgns_vrsn_cd23 as f_109,
			icd_dgns_vrsn_cd24 as f_110,
			icd_dgns_vrsn_cd25 as f_111
		from
			hospice_claims
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
			"claimHospiceStartDate" as f_9,
			"claimNonPaymentReasonCode" as f_10,
			"paymentAmount" as f_11,
			"claimServiceClassificationTypeCode" as f_12,
			"utilizationDayCount" as f_13,
			"attendingPhysicianNpi" as f_14,
			"attendingPhysicianUpin" as f_15,
			"hospicePeriodCount" as f_16,
			"fiscalIntermediaryClaimProcessDate" as f_17,
			"fiDocumentClaimControlNumber" as f_18,
			"fiscalIntermediaryNumber" as f_19,
			"fiOriginalClaimControlNumber" as f_20,
			"finalAction" as f_21,
			"diagnosisExternalFirstCode" as f_22,
			"diagnosisExternalFirstCodeVersion" as f_23,
			"beneficiaryDischargeDate" as f_24,
			"claimTypeCode" as f_25,
			"nearLineRecordIdCode" as f_26,
			"claimPrimaryPayerCode" as f_27,
			"primaryPayerPaidAmount" as f_28,
			"patientStatusCd" as f_29,
			"weeklyProcessDate" as f_30,
			"organizationNpi" as f_31,
			"diagnosisPrincipalCode" as f_32,
			"diagnosisPrincipalCodeVersion" as f_33,
			"providerNumber" as f_34,
			"providerStateCode" as f_35,
			"patientDischargeStatusCode" as f_36,
			"totalChargeAmount" as f_37,
			"diagnosis1Code" as f_38,
			"diagnosis2Code" as f_39,
			"diagnosis3Code" as f_40,
			"diagnosis4Code" as f_41,
			"diagnosis5Code" as f_42,
			"diagnosis6Code" as f_43,
			"diagnosis7Code" as f_44,
			"diagnosis8Code" as f_45,
			"diagnosis9Code" as f_46,
			"diagnosis10Code" as f_47,
			"diagnosis11Code" as f_48,
			"diagnosis12Code" as f_49,
			"diagnosis13Code" as f_50,
			"diagnosis14Code" as f_51,
			"diagnosis15Code" as f_52,
			"diagnosis16Code" as f_53,
			"diagnosis17Code" as f_54,
			"diagnosis18Code" as f_55,
			"diagnosis19Code" as f_56,
			"diagnosis20Code" as f_57,
			"diagnosis21Code" as f_58,
			"diagnosis22Code" as f_59,
			"diagnosis23Code" as f_60,
			"diagnosis24Code" as f_61,
			"diagnosis25Code" as f_62,
			"diagnosisExternal1Code" as f_63,
			"diagnosisExternal2Code" as f_64,
			"diagnosisExternal3Code" as f_65,
			"diagnosisExternal4Code" as f_66,
			"diagnosisExternal5Code" as f_67,
			"diagnosisExternal6Code" as f_68,
			"diagnosisExternal7Code" as f_69,
			"diagnosisExternal8Code" as f_70,
			"diagnosisExternal9Code" as f_71,
			"diagnosisExternal10Code" as f_72,
			"diagnosisExternal11Code" as f_73,
			"diagnosisExternal12Code" as f_74,
			"diagnosisExternal1CodeVersion" as f_75,
			"diagnosisExternal2CodeVersion" as f_76,
			"diagnosisExternal3CodeVersion" as f_77,
			"diagnosisExternal4CodeVersion" as f_78,
			"diagnosisExternal5CodeVersion" as f_79,
			"diagnosisExternal6CodeVersion" as f_80,
			"diagnosisExternal7CodeVersion" as f_81,
			"diagnosisExternal8CodeVersion" as f_82,
			"diagnosisExternal9CodeVersion" as f_83,
			"diagnosisExternal10CodeVersion" as f_84,
			"diagnosisExternal11CodeVersion" as f_85,
			"diagnosisExternal12CodeVersion" as f_86,
			"diagnosis1CodeVersion" as f_87,
			"diagnosis2CodeVersion" as f_88,
			"diagnosis3CodeVersion" as f_89,
			"diagnosis4CodeVersion" as f_90,
			"diagnosis5CodeVersion" as f_91,
			"diagnosis6CodeVersion" as f_92,
			"diagnosis7CodeVersion" as f_93,
			"diagnosis8CodeVersion" as f_94,
			"diagnosis9CodeVersion" as f_95,
			"diagnosis10CodeVersion" as f_96,
			"diagnosis11CodeVersion" as f_97,
			"diagnosis12CodeVersion" as f_98,
			"diagnosis13CodeVersion" as f_99,
			"diagnosis14CodeVersion" as f_100,
			"diagnosis15CodeVersion" as f_101,
			"diagnosis16CodeVersion" as f_102,
			"diagnosis17CodeVersion" as f_103,
			"diagnosis18CodeVersion" as f_104,
			"diagnosis19CodeVersion" as f_105,
			"diagnosis20CodeVersion" as f_106,
			"diagnosis21CodeVersion" as f_107,
			"diagnosis22CodeVersion" as f_108,
			"diagnosis23CodeVersion" as f_109,
			"diagnosis24CodeVersion" as f_110,
			"diagnosis25CodeVersion" as f_111
		from
			"HospiceClaims"
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