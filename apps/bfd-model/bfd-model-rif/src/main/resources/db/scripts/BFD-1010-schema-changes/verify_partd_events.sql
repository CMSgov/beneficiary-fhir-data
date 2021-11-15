do $$
DECLARE
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_bene_id		bigint  := 0;
  v_clm_id      bigint  := 0;
  v_tbl_name	varchar(40) := 'partd_events';

BEGIN
	for counter in 1..1000
	loop
		-- randomly select a "beneficiaryId" from original table
		SELECT cast("beneficiaryId" as bigint) into v_bene_id
		FROM "PartDEvents" TABLESAMPLE SYSTEM_ROWS(40)
		limit 1;
		
		-- need a claim for that bene
		select cast(max("eventId") as bigint) into v_clm_id
		from
			"PartDEvents"
		where
			cast("beneficiaryId" as bigint) = v_bene_id;

		select into curr
			clm_id as f_1,
			bene_id as f_2,
			clm_grp_id as f_3,
			last_updated as f_4,
			adjstmt_dltn_cd as f_5,
			brnd_gnrc_cd as f_6,
			cmpnd_cd as f_7,
			ctstrphc_cvrg_cd as f_8,
			cvrd_d_plan_pd_amt as f_9,
			daw_prod_slctn_cd as f_10,
			days_suply_num as f_11,
			drug_cvrg_stus_cd as f_12,
			dspnsng_stus_cd as f_13,
			fill_num as f_14,
			final_action as f_15,
			gdc_abv_oopt_amt as f_16,
			gdc_blw_oopt_amt as f_17,
			lics_amt as f_18,
			prod_srvc_id as f_19,
			ncvrd_plan_pd_amt as f_20,
			nstd_frmt_cd as f_21,
			othr_troop_amt as f_22,
			pd_dt as f_23,
			phrmcy_srvc_type_cd as f_24,
			plan_cntrct_rec_id as f_25,
			plan_pbp_rec_num as f_26,
			plro_amt as f_27,
			prcng_excptn_cd as f_28,
			prscrbr_id as f_29,
			prscrbr_id_qlfyr_cd as f_30,
			ptnt_pay_amt as f_31,
			ptnt_rsdnc_cd as f_32,
			qty_dspnsd_num as f_33,
			rptd_gap_dscnt_num as f_34,
			rx_orgn_cd as f_35,
			rx_srvc_rfrnc_num as f_36,
			srvc_dt as f_37,
			srvc_prvdr_id as f_38,
			srvc_prvdr_id_qlfyr_cd as f_39,
			submsn_clr_cd as f_40,
			tot_rx_cst_amt as f_41
		from
			partd_events
		WHERE
			clm_id = v_clm_id
		AND
			bene_id = v_bene_id;
		

		SELECT INTO orig
			cast("eventId" as bigint) as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			cast("claimGroupId" as bigint) as f_3,	
			"lastupdated" as f_4,
			"adjustmentDeletionCode" as f_5,
			"brandGenericCode" as f_6,
			"compoundCode" as f_7,
			"catastrophicCoverageCode" as f_8,
			"partDPlanCoveredPaidAmount" as f_9,
			"dispenseAsWrittenProductSelectionCode" as f_10,
			"daysSupply" as f_11,
			"drugCoverageStatusCode" as f_12,
			"dispensingStatusCode" as f_13,
			"fillNumber" as f_14,
			"finalAction" as f_15,
			"grossCostAboveOutOfPocketThreshold" as f_16,
			"grossCostBelowOutOfPocketThreshold" as f_17,
			"lowIncomeSubsidyPaidAmount" as f_18,
			"nationalDrugCode" as f_19,
			"partDPlanNonCoveredPaidAmount" as f_20,
			"nonstandardFormatCode" as f_21,
			"otherTrueOutOfPocketPaidAmount" as f_22,
			"paymentDate" as f_23,
			"pharmacyTypeCode" as f_24,
			"planContractId" as f_25,
			"planBenefitPackageId" as f_26,
			"patientLiabilityReductionOtherPaidAmount" as f_27,
			"pricingExceptionCode" as f_28,
			"prescriberId" as f_29,
			"prescriberIdQualifierCode" as f_30,
			"patientPaidAmount" as f_31,
			"patientResidenceCode" as f_32,
			"quantityDispensed" as f_33,
			"gapDiscountAmount" as f_34,
			"prescriptionOriginationCode" as f_35,
			"prescriptionReferenceNumber" as f_36,
			"prescriptionFillDate" as f_37,
			"serviceProviderId" as f_38,
			"serviceProviderIdQualiferCode" as f_39,
			"submissionClarificationCode" as f_40,
			"totalPrescriptionCost" as f_41
		from
			"PartDEvents"
		where
			"eventId" = v_clm_id::text
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