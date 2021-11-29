do $$
DECLARE
  MAX_TESTS		INTEGER := 500000;		-- hopefully .5M tests are sufficient
  v_claims		BIGINT[];
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_clm_id      bigint  := 0;
  v_tbl_name	varchar(40) := 'dme_claims';

BEGIN
	v_claims := ARRAY(
		SELECT cast("claimId" as bigint)
		FROM "DMEClaims" TABLESAMPLE BERNOULLI(50)	-- bernoulli sample using 50% of table rows
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
			clm_disp_cd as f_7,
			clm_pmt_amt as f_8,
			clm_clncl_tril_num as f_9,
			carr_num as f_10,
			carr_clm_cntl_num as f_11,
			carr_clm_entry_cd as f_12,
			carr_clm_prvdr_asgnmt_ind_sw as f_13,
			carr_clm_hcpcs_yr_cd as f_14,
			carr_clm_pmt_dnl_cd as f_15,
			nch_carr_clm_alowd_amt as f_16,
			nch_carr_clm_sbmtd_chrg_amt as f_17,
			carr_clm_cash_ddctbl_apld_amt as f_18,
			nch_clm_bene_pmt_amt as f_19,
			nch_clm_type_cd as f_20,
			nch_near_line_rec_ident_cd as f_21,
			nch_wkly_proc_dt as f_22,
			carr_clm_prmry_pyr_pd_amt as f_23,
			prncpal_dgns_cd as f_24,
			prncpal_dgns_vrsn_cd as f_25,
			nch_clm_prvdr_pmt_amt as f_26,
			rfr_physn_npi as f_27,
			rfr_physn_upin as f_28,
			final_action as f_29,
			icd_dgns_cd1 as f_30,
			icd_dgns_cd2 as f_31,
			icd_dgns_cd3 as f_32,
			icd_dgns_cd4 as f_33,
			icd_dgns_cd5 as f_34,
			icd_dgns_cd6 as f_35,
			icd_dgns_cd7 as f_36,
			icd_dgns_cd8 as f_37,
			icd_dgns_cd9 as f_38,
			icd_dgns_cd10 as f_39,
			icd_dgns_cd11 as f_40,
			icd_dgns_cd12 as f_41,
			icd_dgns_vrsn_cd1 as f_42,
			icd_dgns_vrsn_cd2 as f_43,
			icd_dgns_vrsn_cd3 as f_44,
			icd_dgns_vrsn_cd4 as f_45,
			icd_dgns_vrsn_cd5 as f_46,
			icd_dgns_vrsn_cd6 as f_47,
			icd_dgns_vrsn_cd7 as f_48,
			icd_dgns_vrsn_cd8 as f_49,
			icd_dgns_vrsn_cd9 as f_50,
			icd_dgns_vrsn_cd10 as f_51,
			icd_dgns_vrsn_cd11 as f_52,
			icd_dgns_vrsn_cd12 as f_53
		from
			dme_claims
		WHERE
			clm_id = v_clm_id;


		SELECT INTO orig
			cast("claimId" as bigint) as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			cast("claimGroupId" as bigint) as f_3,
			"lastupdated" as f_4,
			"dateFrom" as f_5,
			"dateThrough" as f_6,
			"claimDispositionCode" as f_7,
			"paymentAmount" as f_8,
			"clinicalTrialNumber" as f_9,
			"carrierNumber" as f_10,
			"claimCarrierControlNumber" as f_11,
			"claimEntryCode" as f_12,
			"providerAssignmentIndicator" as f_13,
			"hcpcsYearCode" as f_14,
			"paymentDenialCode" as f_15,
			"allowedChargeAmount" as f_16,
			"submittedChargeAmount" as f_17,
			"beneficiaryPartBDeductAmount" as f_18,
			"beneficiaryPaymentAmount" as f_19,
			"claimTypeCode" as f_20,
			"nearLineRecordIdCode" as f_21,
			"weeklyProcessDate" as f_22,
			"primaryPayerPaidAmount" as f_23,
			"diagnosisPrincipalCode" as f_24,
			"diagnosisPrincipalCodeVersion" as f_25,
			"providerPaymentAmount" as f_26,
			"referringPhysicianNpi" as f_27,
			"referringPhysicianUpin" as f_28,
			"finalAction" as f_29,
			"diagnosis1Code" as f_30,
			"diagnosis2Code" as f_31,
			"diagnosis3Code" as f_32,
			"diagnosis4Code" as f_33,
			"diagnosis5Code" as f_34,
			"diagnosis6Code" as f_35,
			"diagnosis7Code" as f_36,
			"diagnosis8Code" as f_37,
			"diagnosis9Code" as f_38,
			"diagnosis10Code" as f_39,
			"diagnosis11Code" as f_40,
			"diagnosis12Code" as f_41,
			"diagnosis1CodeVersion" as f_42,
			"diagnosis2CodeVersion" as f_43,
			"diagnosis3CodeVersion" as f_44,
			"diagnosis4CodeVersion" as f_45,
			"diagnosis5CodeVersion" as f_46,
			"diagnosis6CodeVersion" as f_47,
			"diagnosis7CodeVersion" as f_48,
			"diagnosis8CodeVersion" as f_49,
			"diagnosis9CodeVersion" as f_50,
			"diagnosis10CodeVersion" as f_51,
			"diagnosis11CodeVersion" as f_52,
			"diagnosis12CodeVersion" as f_53	
		from
			"DMEClaims"
		WHERE
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