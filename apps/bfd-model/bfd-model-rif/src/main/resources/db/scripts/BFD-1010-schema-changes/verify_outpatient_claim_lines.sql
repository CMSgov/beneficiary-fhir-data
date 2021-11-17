do $$
declare
  MAX_TESTS		INTEGER := 40000;
  orig			record;
  curr			record;
  err_cnt	    integer := 0;
  tot_err_cnt	integer := 0;
  v_bene_id		bigint  := 0;
  v_clm_id      bigint  := 0;
  v_line_num	smallint := 0;
  v_tbl_name	varchar(40) := 'outpatient_claim_lines';

begin
	for counter in 1..MAX_TESTS
	loop
		-- randomly select a "beneficiaryid" from original table
		select cast("beneficiaryId" as bigint) into v_bene_id
		from "OutpatientClaims" tablesample system_rows(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("claimId") as bigint) into v_clm_id
		from
			"OutpatientClaims"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;
			
		-- need a claim line number for that claim
		select cast(max("lineNumber") as smallint) into v_line_num
		from
			"OutpatientClaimLines"
		where
			cast("parentClaim" as bigint) = v_bene_id;
			
		select into curr		
			parent_claim as f_1,
			clm_line_num as f_2,
			rev_cntr_ide_ndc_upc_num as f_3,
			hcpcs_cd as f_4,
			hcpcs_1st_mdfr_cd as f_5,
			hcpcs_2nd_mdfr_cd as f_6,
			rndrng_physn_npi as f_7,
			rndrng_physn_upin as f_8,
			rev_cntr as f_9,
			rev_cntr_dt as f_10,
			rev_cntr_pmt_amt_amt as f_11,
			rev_cntr_apc_hipps_cd as f_12,
			rev_cntr_bene_pmt_amt as f_13,
			rev_cntr_blood_ddctbl_amt as f_14,
			rev_cntr_cash_ddctbl_amt as f_15,
			rev_cntr_dscnt_ind_cd as f_16,
			rev_cntr_1st_msp_pd_amt as f_17,
			rev_cntr_ndc_qty_qlfr_cd as f_18,
			rev_cntr_ndc_qty as f_19,
			rev_cntr_ncvrd_chrg_amt as f_20,
			rev_cntr_otaf_pmt_cd as f_21,
			rev_cntr_packg_ind_cd as f_22,
			rev_cntr_ptnt_rspnsblty_pmt as f_23,
			rev_cntr_pmt_mthd_ind_cd as f_24,
			rev_cntr_prvdr_pmt_amt as f_25,
			rev_cntr_rate_amt as f_26,
			rev_cntr_rdcd_coinsrnc_amt as f_27,
			rev_cntr_1st_ansi_cd as f_28,
			rev_cntr_2nd_ansi_cd as f_29,
			rev_cntr_3rd_ansi_cd as f_30,
			rev_cntr_4th_ansi_cd as f_31,
			rev_cntr_2nd_msp_pd_amt as f_32,
			rev_cntr_stus_ind_cd as f_33,
			rev_cntr_tot_chrg_amt as f_34,
			rev_cntr_unit_cnt as f_35,
			rev_cntr_coinsrnc_wge_adjstd_amt as f_36
		from
			outpatient_claim_lines
		where
			parent_claim = v_clm_id
		and
			clm_line_num = v_line_num;
		

		select into orig
			Cast("parentClaim" as bigint) as f_1,
			"lineNumber" as f_2,
			"nationalDrugCode" as f_3,
			"hcpcsCode" as f_4,
			"hcpcsInitialModifierCode" as f_5,
			"hcpcsSecondModifierCode" as f_6,
			"revenueCenterRenderingPhysicianNPI" as f_7,
			"revenueCenterRenderingPhysicianUPIN" as f_8,
			"revenueCenterCode" as f_9,
			"revenueCenterDate" as f_10,
			"paymentAmount" as f_11,
			"apcOrHippsCode" as f_12,
			"benficiaryPaymentAmount" as f_13,
			"bloodDeductibleAmount" as f_14,
			"cashDeductibleAmount" as f_15,
			"discountCode" as f_16,
			"firstMspPaidAmount" as f_17,
			"nationalDrugCodeQualifierCode" as f_18,
			"nationalDrugCodeQuantity" as f_19,
			"nonCoveredChargeAmount" as f_20,
			"obligationToAcceptAsFullPaymentCode" as f_21,
			"packagingCode" as f_22,
			"patientResponsibilityAmount" as f_23,
			"paymentMethodCode" as f_24,
			"providerPaymentAmount" as f_25,
			"rateAmount" as f_26,
			"reducedCoinsuranceAmount" as f_27,
			"revCntr1stAnsiCd" as f_28,
			"revCntr2ndAnsiCd" as f_29,
			"revCntr3rdAnsiCd" as f_30,
			"revCntr4thAnsiCd" as f_31,
			"secondMspPaidAmount" as f_32,
			"statusCode" as f_33,
			"totalChargeAmount" as f_34,
			"unitCount" as f_35,
			"wageAdjustedCoinsuranceAmount" as f_36			
		from
			"OutpatientClaimLines"
		where
			"parentClaim" = v_clm_id::text
		and
			"lineNumber" = v_line_num;
		
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
		
		if err_cnt > 0 then
			insert into migration_errors values(v_tbl_name, v_bene_id, v_clm_id, v_line_num, err_cnt);
			tot_err_cnt := tot_err_cnt + err_cnt;
			raise info 'discrepancy, table: %, bene_id: %', v_tbl_name, v_bene_id;
		end if;
	end loop;
	
	if tot_err_cnt > 0 then
		raise info 'discrepancy, table: %', v_tbl_name;
	else
		raise info 'no errors, table: %', v_tbl_name;
	end if;
end;
$$;