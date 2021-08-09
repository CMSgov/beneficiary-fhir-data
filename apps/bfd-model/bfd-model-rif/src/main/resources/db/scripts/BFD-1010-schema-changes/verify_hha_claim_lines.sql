do $$
declare
  orig			record;
  curr			record;
  err_cnt	    integer := 0;
  tot_err_cnt	integer := 0;
  v_bene_id		bigint  := 0;
  v_clm_id      bigint  := 0;
  v_line_num	smallint := 0;
  v_tbl_name	varchar(40) := 'hha_claim_lines';

begin
	for counter in 1..1000
	loop
		-- randomly select a "beneficiaryid" from original table
		select cast("beneficiaryId" as bigint) into v_bene_id
		from public."HHAClaims" tablesample system_rows(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("claimId") as bigint) into v_clm_id
		from
			public."HHAClaims"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;
			
		-- need a claim line number for that claim
		select cast(max("lineNumber") as smallint) into v_line_num
		from
			public."HHAClaimLines"
		where
			cast("parentClaim" as bigint) = v_bene_id;
			
		select into curr		
			parent_claim as f_1,
			clm_line_num as f_2,
			line_pmt_amt as f_3,
			hcpcs_cd as f_4,
			hcpcs_1st_mdfr_cd as f_5,
			hcpcs_2nd_mdfr_cd as f_6,
			rndrng_physn_npi as f_7,
			rndrng_physn_upin as f_8,
			rev_cntr as f_9,
			rev_cntr_dt as f_10,
			rev_cntr_apc_hipps_cd as f_11,
			rev_cntr_ddctbl_coinsrnc_cd as f_12,
			rev_cntr_ndc_qty_qlfr_cd as f_13,
			rev_cntr_ndc_qty as f_14,
			rev_cntr_ncvrd_chrg_amt as f_15,
			rev_cntr_pmt_mthd_ind_cd as f_16,
			rev_cntr_rate_amt as f_17,
			rev_cntr_1st_ansi_cd as f_18,
			rev_cntr_stus_ind_cd as f_19,
			rev_cntr_tot_chrg_amt as f_20,
			rev_cntr_unit_cnt as f_21
		from
			public.hha_claim_lines
		where
			parent_claim = v_clm_id
		and
			clm_line_num = v_line_num;
		

		select into orig
			Cast("parentClaim" as bigint) as f_1,
			"lineNumber" as f_2,
			"paymentAmount" as f_3,
			"hcpcsCode" as f_4,
			"hcpcsInitialModifierCode" as f_5,
			"hcpcsSecondModifierCode" as f_6,
			"revenueCenterRenderingPhysicianNPI" as f_7,
			"revenueCenterRenderingPhysicianUPIN" as f_8,
			"revenueCenterCode" as f_9,
			"revenueCenterDate" as f_10,
			"apcOrHippsCode" as f_11,
			"deductibleCoinsuranceCd" as f_12,
			"nationalDrugCodeQualifierCode" as f_13,
			"nationalDrugCodeQuantity" as f_14,
			"nonCoveredChargeAmount" as f_15,
			"paymentMethodCode" as f_16,
			"rateAmount" as f_17,
			"revCntr1stAnsiCd" as f_18,
			"statusCode" as f_19,
			"totalChargeAmount" as f_20,
			"unitCount" as f_21			
		from
			public."HHAClaimLines"
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