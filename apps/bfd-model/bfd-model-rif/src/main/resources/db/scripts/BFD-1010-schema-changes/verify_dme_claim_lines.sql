do $$
declare
  orig			record;
  curr			record;
  err_cnt	    integer := 0;
  tot_err_cnt	integer := 0;
  v_bene_id		bigint  := 0;
  v_clm_id      bigint  := 0;
  v_line_num	smallint := 0;
  v_tbl_name	varchar(40) := 'dme_claim_lines';

begin
	for counter in 1..1000
	loop
		-- randomly select a "beneficiaryid" from original table
		select cast("beneficiaryId" as bigint) into v_bene_id
		from public."DMEClaims" tablesample system_rows(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("claimId") as bigint) into v_clm_id
		from
			public."DMEClaims"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;
			
		-- need a claim line number for that claim
		select cast(max("lineNumber") as smallint) into v_line_num
		from
			public."DMEClaimLines"
		where
			cast("parentClaim" as bigint) = v_bene_id;
			
		select into curr		
			parent_claim as f_1,
			clm_line_num as f_2,
			line_pmt_amt as f_3,
			line_sbmtd_chrg_amt as f_4,
			line_alowd_chrg_amt as f_5,
			line_bene_ptb_ddctbl_amt as f_6,
			line_bene_pmt_amt as f_7,
			line_ndc_cd as f_8,
			line_cms_type_srvc_cd as f_9,
			line_coinsrnc_amt as f_10,
			line_icd_dgns_cd as f_11,
			line_icd_dgns_vrsn_cd as f_12,
			line_1st_expns_dt as f_13,
			line_hct_hgb_rslt_num as f_14,
			line_hct_hgb_type_cd as f_15,
			line_last_expns_dt as f_16,
			line_pmt_80_100_cd as f_17,
			line_place_of_srvc_cd as f_18,
			line_prmry_alowd_chrg_amt as f_19,
			line_bene_prmry_pyr_cd as f_20,
			line_prcsg_ind_cd as f_21,
			line_dme_prchs_price_amt as f_22,
			line_srvc_cnt as f_23,
			line_service_deductible as f_24,
			betos_cd as f_25,
			hcpcs_cd as f_26,
			hcpcs_4th_mdfr_cd as f_27,
			hcpcs_1st_mdfr_cd as f_28,
			hcpcs_2nd_mdfr_cd as f_29,
			hcpcs_3rd_mdfr_cd as f_30,
			dmerc_line_mtus_cd as f_31,
			dmerc_line_mtus_cnt as f_32,
			dmerc_line_prcng_state_cd as f_33,
			dmerc_line_scrn_svgs_amt as f_34,
			dmerc_line_supplr_type_cd as f_35,
			nch_prmry_pyr_clm_pd_amt as f_36,
			prvdr_num as f_37,
			prvdr_npi as f_38,
			prvdr_spclty as f_39,
			prvdr_state_cd as f_40,
			prvdr_tax_num as f_41,
			prtcptng_ind_cd as f_42,
			rev_cntr_prvdr_pmt_amt as f_43
		from
			public.dme_claim_lines
		where
			parent_claim = v_clm_id
		and
			clm_line_num = v_line_num;
		

		select into orig
			Cast("parentClaim" as bigint) as f_1,
			"lineNumber" as f_2,
			"paymentAmount" as f_3,
			"submittedChargeAmount" as f_4,
			"allowedChargeAmount" as f_5,
			"beneficiaryPartBDeductAmount" as f_6,
			"beneficiaryPaymentAmount" as f_7,
			"nationalDrugCode" as f_8,
			"cmsServiceTypeCode" as f_9,
			"coinsuranceAmount" as f_10,
			"diagnosisCode" as f_11,
			"diagnosisCodeVersion" as f_12,
			"firstExpenseDate" as f_13,
			"hctHgbTestResult" as f_14,
			"hctHgbTestTypeCode" as f_15,
			"lastExpenseDate" as f_16,
			"paymentCode" as f_17,
			"placeOfServiceCode" as f_18,
			"primaryPayerAllowedChargeAmount" as f_19,
			"primaryPayerCode" as f_20,
			"processingIndicatorCode" as f_21,
			"purchasePriceAmount" as f_22,
			"serviceCount" as f_23,
			"serviceDeductibleCode" as f_24,
			"betosCode" as f_25,
			"hcpcsCode" as f_26,
			"hcpcsFourthModifierCode" as f_27,
			"hcpcsInitialModifierCode" as f_28,
			"hcpcsSecondModifierCode" as f_29,
			"hcpcsThirdModifierCode" as f_30,
			"mtusCode" as f_31,
			"mtusCount" as f_32,
			"pricingStateCode" as f_33,
			"screenSavingsAmount" as f_34,
			"supplierTypeCode" as f_35,
			"primaryPayerPaidAmount" as f_36,
			"providerBillingNumber" as f_37,
			"providerNPI" as f_38,
			"providerSpecialityCode" as f_39,
			"providerStateCode" as f_40,
			"providerTaxNumber" as f_41,
			"providerParticipatingIndCode" as f_42,
			"providerPaymentAmount" as f_43	
		from
			public."DMEClaimLines"
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
		if curr.f_37 <> orig.f_37 then err_cnt := err_cnt + 1; end if;
		if curr.f_38 <> orig.f_38 then err_cnt := err_cnt + 1; end if;
		if curr.f_39 <> orig.f_39 then err_cnt := err_cnt + 1; end if;
		if curr.f_40 <> orig.f_40 then err_cnt := err_cnt + 1; end if;
		if curr.f_41 <> orig.f_41 then err_cnt := err_cnt + 1; end if;
		if curr.f_42 <> orig.f_42 then err_cnt := err_cnt + 1; end if;
		if curr.f_43 <> orig.f_43 then err_cnt := err_cnt + 1; end if;		
	
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