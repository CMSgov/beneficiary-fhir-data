do $$
DECLARE
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_bene_id		bigint  := 0;
  v_tbl_name	varchar(40) := 'beneficiaries_history';

BEGIN
	for counter in 1..1000
	loop
		-- randomly select a "beneficiaryId" from original table
		SELECT cast("beneficiaryId" as bigint) into v_bene_id
		FROM public."BeneficiariesHistory" TABLESAMPLE SYSTEM_ROWS(40)
		limit 1;

		select into curr
			beneficiary_history_id as f_1,
			bene_id as f_2,
			last_updated as f_3,
			bene_crnt_hic_num as f_4,
			hicn_unhashed as f_5,
			mbi_num as f_6,
			mbi_hash as f_7,
			mbi_efctv_bgn_dt as f_8,
			mbi_efctv_end_dt as f_9,
			bene_sex_ident_cd as f_10,
			bene_birth_dt as f_11
		from
			public.beneficiaries_history
		WHERE
			bene_id = v_bene_id;
		

		SELECT INTO orig
			"beneficiaryHistoryId" as f_1,
			cast("beneficiaryId" as bigint) as f_2,
			"lastupdated" as f_3,
			"hicn" as f_4,
			"hicnUnhashed" as f_5,
			"medicareBeneficiaryId" as f_6,
			"mbiHash" as f_7,
			"mbiEffectiveDate" as f_8,
			"mbiObsoleteDate" as f_9,
			"sex" as f_10,
			"birthDate" as f_11
		from
			public."BeneficiariesHistory"
		WHERE
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