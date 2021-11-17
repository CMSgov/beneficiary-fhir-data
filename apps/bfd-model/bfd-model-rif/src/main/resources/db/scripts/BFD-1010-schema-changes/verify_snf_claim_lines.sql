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
  v_tbl_name	varchar(40) := 'snf_claim_lines';

begin
	for counter in 1..MAX_TESTS
	loop
		-- randomly select a "beneficiaryid" from original table
		select cast("beneficiaryId" as bigint) into v_bene_id
		from "SNFClaims" tablesample system_rows(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("claimId") as bigint) into v_clm_id
		from
			"SNFClaims"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;
			
		-- need a claim line number for that claim
		select cast(max("lineNumber") as smallint) into v_line_num
		from
			"SNFClaimLines"
		where
			cast("parentClaim" as bigint) = v_bene_id;
			
		select into curr		
			parent_claim as f_1,
			clm_line_num as f_2,
			hcpcs_cd as f_3,
			rev_cntr as f_4,
			rev_cntr_ndc_qty_qlfr_cd as f_5,
			rev_cntr_ndc_qty as f_6,
			rev_cntr_ncvrd_chrg_amt as f_7,
			rev_cntr_rate_amt as f_8,
			rev_cntr_tot_chrg_amt as f_9,
			rev_cntr_ddctbl_coinsrnc_cd as f_10,
			rev_cntr_unit_cnt as f_11,
			rndrng_physn_npi as f_12,
			rndrng_physn_upin as f_13
		from
			snf_claim_lines
		where
			parent_claim = v_clm_id
		and
			clm_line_num = v_line_num;
		

		select into orig
			Cast("parentClaim" as bigint) as f_1,
			"lineNumber" as f_2,
			"hcpcsCode" as f_3,
			"revenueCenter" as f_4,
			"nationalDrugCodeQualifierCode" as f_5,
			"nationalDrugCodeQuantity" as f_6,
			"nonCoveredChargeAmount" as f_7,
			"rateAmount" as f_8,
			"totalChargeAmount" as f_9,
			"deductibleCoinsuranceCd" as f_10,
			"unitCount" as f_11,
			"revenueCenterRenderingPhysicianNPI" as f_12,
			"revenueCenterRenderingPhysicianUPIN" as f_13				
		from
			"SNFClaimLines"
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