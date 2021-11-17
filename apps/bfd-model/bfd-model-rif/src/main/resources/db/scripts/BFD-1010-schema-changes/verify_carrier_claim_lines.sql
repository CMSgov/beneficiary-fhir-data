do $$
declare
  MAX_TESTS		INTEGER := 30000;
  orig			record;
  curr			record;
  err_cnt	    integer := 0;
  tot_err_cnt	integer := 0;
  v_bene_id		bigint  := 0;
  v_clm_id      bigint  := 0;
  v_line_num	smallint := 0;
  v_tbl_name	varchar(40) := 'carrier_claim_lines';

begin
	for counter in 1..MAX_TESTS
	loop
		-- randomly select a "beneficiaryid" from original table
		select cast("beneficiaryId" as bigint) into v_bene_id
		from "CarrierClaims" tablesample system_rows(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("claimId") as bigint) into v_clm_id
		from
			"CarrierClaims"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;
			
		-- need a claim line number for that claim
		select cast(max("lineNumber") as smallint) into v_line_num
		from
			"CarrierClaimLines"
		where
			cast("parentClaim" as bigint) = v_bene_id;
			
		select into curr		
			parent_claim as f_1,
			clm_line_num as f_2,
			line_nch_pmt_amt as f_3,
			line_1st_expns_dt as f_4,
			line_alowd_chrg_amt as f_5,
			line_bene_pmt_amt as f_6,
			line_bene_prmry_pyr_cd as f_7,
			line_bene_prmry_pyr_pd_amt as f_8,
			line_bene_ptb_ddctbl_amt as f_9,
			line_cms_type_srvc_cd as f_10,
			line_coinsrnc_amt as f_11,
			line_hct_hgb_rslt_num as f_12,
			line_hct_hgb_type_cd as f_13,
			line_icd_dgns_cd as f_14,
			line_icd_dgns_vrsn_cd as f_15,
			line_last_expns_dt as f_16,
			line_ndc_cd as f_17,
			line_place_of_srvc_cd as f_18,
			line_pmt_80_100_cd as f_19,
			line_prcsg_ind_cd as f_20,
			line_sbmtd_chrg_amt as f_21,
			line_service_deductible as f_22,
			line_srvc_cnt as f_23,
			carr_line_mtus_cd as f_24,
			carr_line_mtus_cnt as f_25,
			betos_cd as f_26,
			carr_line_ansthsa_unit_cnt as f_27,
			carr_line_clia_lab_num as f_28,
			carr_line_prcng_lclty_cd as f_29,
			carr_line_prvdr_type_cd as f_30,
			carr_line_rdcd_pmt_phys_astn_c as f_31,
			carr_line_rx_num as f_32,
			carr_prfrng_pin_num as f_33,
			hcpcs_1st_mdfr_cd as f_34,
			hcpcs_2nd_mdfr_cd as f_35,
			hcpcs_cd as f_36,
			hpsa_scrcty_ind_cd as f_37,
			org_npi_num as f_38,
			prf_physn_npi as f_39,
			prf_physn_upin as f_40,
			prtcptng_ind_cd as f_41,
			prvdr_spclty as f_42,
			prvdr_state_cd as f_43,
			prvdr_zip as f_44,
			prvdr_tax_num as f_45,
			line_prvdr_pmt_amt as f_46		
		from
			carrier_claim_lines
		where
			parent_claim = v_clm_id
		and
			clm_line_num = v_line_num;
		

		select into orig
			Cast("parentClaim" as bigint) as f_1,
			"lineNumber" as f_2,
			"paymentAmount" as f_3,
			"firstExpenseDate" as f_4,
			"allowedChargeAmount" as f_5,
			"beneficiaryPaymentAmount" as f_6,
			"primaryPayerCode" as f_7,
			"primaryPayerPaidAmount" as f_8,
			"beneficiaryPartBDeductAmount" as f_9,
			"cmsServiceTypeCode" as f_10,
			"coinsuranceAmount" as f_11,
			"hctHgbTestResult" as f_12,
			"hctHgbTestTypeCode" as f_13,
			"diagnosisCode" as f_14,
			"diagnosisCodeVersion" as f_15,
			"lastExpenseDate" as f_16,
			"nationalDrugCode" as f_17,
			"placeOfServiceCode" as f_18,
			"paymentCode" as f_19,
			"processingIndicatorCode" as f_20,
			"submittedChargeAmount" as f_21,
			"serviceDeductibleCode" as f_22,
			"serviceCount" as f_23,
			"mtusCode" as f_24,
			"mtusCount" as f_25,
			"betosCode" as f_26,
			"anesthesiaUnitCount" as f_27,
			"cliaLabNumber" as f_28,
			"linePricingLocalityCode" as f_29,
			"providerTypeCode" as f_30,
			"reducedPaymentPhysicianAsstCode" as f_31,
			"rxNumber" as f_32,
			"performingProviderIdNumber" as f_33,
			"hcpcsInitialModifierCode" as f_34,
			"hcpcsSecondModifierCode" as f_35,
			"hcpcsCode" as f_36,
			"hpsaScarcityCode" as f_37,
			"organizationNpi" as f_38,
			"performingPhysicianNpi" as f_39,
			"performingPhysicianUpin" as f_40,
			"providerParticipatingIndCode" as f_41,
			"providerSpecialityCode" as f_42,
			"providerStateCode" as f_43,
			"providerZipCode" as f_44,
			"providerTaxNumber" as f_45,
			"providerPaymentAmount" as f_46	
		from
			"CarrierClaimLines"
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
		if curr.f_44 <> orig.f_44 then err_cnt := err_cnt + 1; end if;
		if curr.f_45 <> orig.f_45 then err_cnt := err_cnt + 1; end if;
		if curr.f_46 <> orig.f_46 then err_cnt := err_cnt + 1; end if;		
	
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