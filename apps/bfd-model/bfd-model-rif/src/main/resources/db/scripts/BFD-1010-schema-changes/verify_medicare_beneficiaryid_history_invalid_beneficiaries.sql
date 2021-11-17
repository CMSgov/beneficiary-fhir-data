do $$
DECLARE
  MAX_TESTS		INTEGER := 500000;		-- hopefully .5M tests are sufficient
  v_beneIds		BIGINT[];
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_bene_id		bigint  := 0;
  v_tbl_name	varchar(60) := 'medicare_beneficiaryid_history_invalid_beneficiaries';

BEGIN
	v_beneIds := ARRAY(
		SELECT distinct cast("beneficiaryId" as bigint)
		FROM "MedicareBeneficiaryIdHistoryInvalidBeneficiaries" TABLESAMPLE BERNOULLI(50)	-- bernoulli sample using 50% of table rows
		limit MAX_TESTS);

	for counter in 1..MAX_TESTS
	loop
		v_bene_id := v_beneIds[counter - 1];

		select into curr
			bene_mbi_id as f_1,
			bene_id as f_2,
			bene_clm_acnt_num as f_4,
			bene_ident_cd as f_5,
			bene_crnt_rec_ind_id as f_6,
			mbi_sqnc_num as f_7,
			mbi_num as f_8,
			mbi_efctv_bgn_dt as f_9,
			mbi_efctv_end_dt as f_10,
			mbi_bgn_rsn_cd as f_11,
			mbi_end_rsn_cd as f_12,
			mbi_card_rqst_dt as f_13,
			creat_user_id as f_14,
			creat_ts as f_15,
			updt_user_id as f_16,
			updt_ts as f_17
		from
			medicare_beneficiaryid_history_invalid_beneficiaries
		WHERE
			bene_id = v_bene_id;
		

		SELECT INTO orig
			"medicareBeneficiaryIdKey" as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			"claimAccountNumber" as f_4,
			"beneficiaryIdCode" as f_5,
			"mbiCrntRecIndId" as f_6,
			"mbiSequenceNumber" as f_7,
			"medicareBeneficiaryId" as f_8,
			"mbiEffectiveDate" as f_9,
			"mbiEndDate" as f_10,
			"mbiEffectiveReasonCode" as f_11,
			"mbiEndReasonCode" as f_12,
			"mbiCardRequestDate" as f_13,
			"mbiAddUser" as f_14,
			"mbiAddDate" as f_15,
			"mbiUpdateUser" as f_16,
			"mbiUpdateDate" as f_17
		from
			"MedicareBeneficiaryIdHistoryInvalidBeneficiaries"
		WHERE
			"beneficiaryId" = v_bene_id::text;
		
		if curr.f_1 <> orig.f_1 then err_cnt := err_cnt + 1; end if;
		if curr.f_2 <> orig.f_2 then err_cnt := err_cnt + 1; end if;
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
	
		if err_cnt > 0 then
			insert into migration_errors values(v_tbl_name, v_bene_id, null, null, err_cnt);
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