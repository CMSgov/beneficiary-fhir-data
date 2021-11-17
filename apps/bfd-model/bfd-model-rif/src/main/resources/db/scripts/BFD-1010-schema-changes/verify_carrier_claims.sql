do $$
DECLARE
  MAX_TESTS		INTEGER := 30000;
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_bene_id		bigint  := 0;
  v_clm_id      bigint  := 0;
  v_tbl_name	varchar(40) := 'carrier_claims';

BEGIN
	for counter in 1..MAX_TESTS
	loop
		-- randomly select a "beneficiaryId" from original table
		SELECT cast("beneficiaryId" as bigint) into v_bene_id
		FROM "CarrierClaims" TABLESAMPLE SYSTEM_ROWS(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("claimId") as bigint) into v_clm_id
		from
			"CarrierClaims"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;

		select into curr
			clm_id as f_1,
			bene_id as f_2,
			clm_grp_id as f_3,
			last_updated as f_4,
			clm_from_dt as f_5,
			clm_thru_dt as f_6,
			clm_clncl_tril_num as f_7,
			clm_disp_cd as f_8,
			clm_pmt_amt as f_9,
			carr_clm_cntl_num as f_10,
			carr_clm_entry_cd as f_11,
			carr_clm_hcpcs_yr_cd as f_12,
			carr_clm_pmt_dnl_cd as f_13,
			carr_clm_prvdr_asgnmt_ind_sw as f_14,
			carr_clm_rfrng_pin_num as f_15,
			carr_num as f_16,
			final_action as f_17,
			nch_carr_clm_alowd_amt as f_18,
			nch_carr_clm_sbmtd_chrg_amt as f_19,
			nch_clm_bene_pmt_amt as f_20,
			nch_clm_prvdr_pmt_amt as f_21,
			carr_clm_cash_ddctbl_apld_amt as f_22,
			nch_clm_type_cd as f_23,
			nch_near_line_rec_ident_cd as f_24,
			carr_clm_prmry_pyr_pd_amt as f_25,
			nch_wkly_proc_dt as f_26,
			prncpal_dgns_cd as f_27,
			prncpal_dgns_vrsn_cd as f_28,
			rfr_physn_npi as f_29,
			rfr_physn_upin as f_30,
			icd_dgns_cd1 as f_31,
			icd_dgns_cd2 as f_32,
			icd_dgns_cd3 as f_33,
			icd_dgns_cd4 as f_34,
			icd_dgns_cd5 as f_35,
			icd_dgns_cd6 as f_36,
			icd_dgns_cd7 as f_37,
			icd_dgns_cd8 as f_38,
			icd_dgns_cd9 as f_39,
			icd_dgns_cd10 as f_40,
			icd_dgns_cd11 as f_41,
			icd_dgns_cd12 as f_42,
			icd_dgns_vrsn_cd1 as f_43,
			icd_dgns_vrsn_cd2 as f_44,
			icd_dgns_vrsn_cd3 as f_45,
			icd_dgns_vrsn_cd4 as f_46,
			icd_dgns_vrsn_cd5 as f_47,
			icd_dgns_vrsn_cd6 as f_48,
			icd_dgns_vrsn_cd7 as f_49,
			icd_dgns_vrsn_cd8 as f_50,
			icd_dgns_vrsn_cd9 as f_51,
			icd_dgns_vrsn_cd10 as f_52,
			icd_dgns_vrsn_cd11 as f_53,
			icd_dgns_vrsn_cd12 as f_54
		from
			carrier_claims
		WHERE
			clm_id = v_clm_id
		AND
			bene_id = v_bene_id;
		
		SELECT INTO orig
			cast("claimId" as bigint) as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			cast("claimGroupId" as bigint) as f_3,
			"lastupdated" as f_4,
			"dateFrom" as f_5,
			"dateThrough" as f_6,
			"clinicalTrialNumber" as f_7,
			"claimDispositionCode" as f_8,
			"paymentAmount" as f_9,
			"claimCarrierControlNumber" as f_10,
			"claimEntryCode" as f_11,
			"hcpcsYearCode" as f_12,
			"paymentDenialCode" as f_13,
			"providerAssignmentIndicator" as f_14,
			"referringProviderIdNumber" as f_15,
			"carrierNumber" as f_16,
			"finalAction" as f_17,
			"allowedChargeAmount" as f_18,
			"submittedChargeAmount" as f_19,
			"beneficiaryPaymentAmount" as f_20,
			"providerPaymentAmount" as f_21,
			"beneficiaryPartBDeductAmount" as f_22,
			"claimTypeCode" as f_23,
			"nearLineRecordIdCode" as f_24,
			"primaryPayerPaidAmount" as f_25,
			"weeklyProcessDate" as f_26,
			"diagnosisPrincipalCode" as f_27,
			"diagnosisPrincipalCodeVersion" as f_28,
			"referringPhysicianNpi" as f_29,
			"referringPhysicianUpin" as f_30,
			"diagnosis1Code" as f_31,
			"diagnosis2Code" as f_32,
			"diagnosis3Code" as f_33,
			"diagnosis4Code" as f_34,
			"diagnosis5Code" as f_35,
			"diagnosis6Code" as f_36,
			"diagnosis7Code" as f_37,
			"diagnosis8Code" as f_38,
			"diagnosis9Code" as f_39,
			"diagnosis10Code" as f_40,
			"diagnosis11Code" as f_41,
			"diagnosis12Code" as f_42,
			"diagnosis1CodeVersion" as f_43,
			"diagnosis2CodeVersion" as f_44,
			"diagnosis3CodeVersion" as f_45,
			"diagnosis4CodeVersion" as f_46,
			"diagnosis5CodeVersion" as f_47,
			"diagnosis6CodeVersion" as f_48,
			"diagnosis7CodeVersion" as f_49,
			"diagnosis8CodeVersion" as f_50,
			"diagnosis9CodeVersion" as f_51,
			"diagnosis10CodeVersion" as f_52,
			"diagnosis11CodeVersion" as f_53,
			"diagnosis12CodeVersion" as f_54
		from
			"CarrierClaims"
		WHERE
			"claimId" = v_clm_id::text
		AND
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