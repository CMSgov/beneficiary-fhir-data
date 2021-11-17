do $$
DECLARE
  MAX_TESTS		INTEGER := 1000;
  orig			record;
  curr			record;
  err_cnt	    INTEGER := 0;
  tot_err_cnt	INTEGER := 0;
  v_batch_id	bigint  := 0;
  v_tbl_name	varchar(40) := 'loaded_batches';

BEGIN
	for counter in 1..MAX_TESTS
	loop
		-- randomly select a "loadedBatchId" from original table
		SELECT "loadedBatchId" into v_batch_id
		FROM "LoadedBatches" TABLESAMPLE BERNOULLI(40)
		limit 1;

		select into curr
			loaded_batchid as f_1,
			loaded_fileid as f_2,
			beneficiaries as f_3,
			created as f_4
		from
			loaded_batches
		WHERE
			loaded_batchid = v_batch_id;
		

		SELECT INTO orig
			"loadedBatchId"  as f_1,
			"loadedFileId" as f_2,
			beneficiaries as f_3,
			created as f_4
		from
			"LoadedBatches"
		where
			"loadedBatchId" = v_batch_id;
		
		if curr.f_1 <> orig.f_1 then err_cnt := err_cnt + 1; end if;
		if curr.f_2 <> orig.f_2 then err_cnt := err_cnt + 1; end if;
		if curr.f_3 <> orig.f_3 then err_cnt := err_cnt + 1; end if;
		if curr.f_4 <> orig.f_4 then err_cnt := err_cnt + 1; end if;

		if err_cnt > 0 then
			insert into migration_errors values(v_tbl_name, v_batch_id, null, null, err_cnt);
			tot_err_cnt := tot_err_cnt + err_cnt;
			raise info 'DISCREPANCY, table: %, batch_id: %', v_tbl_name, v_batch_id;
		end if;
	end loop;
	
	if tot_err_cnt > 0 then
		raise info 'DISCREPANCY, table: %', v_tbl_name;
	else
		raise info 'NO ERRORS, table: %', v_tbl_name;
	end if;
END;
$$;