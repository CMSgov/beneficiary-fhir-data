SET search_path TO rda;

DO $$
DECLARE
  --mcs (comment this and uncomment fiss below for fiss)
  parent_table VARCHAR(50) := 'mcs_claims';
  child_tables VARCHAR[] := ARRAY['mcs_adjustments','mcs_audits','mcs_details','mcs_diagnosis_codes','mcs_locations'];
  claim_id VARCHAR(50) := 'idr_clm_hd_icn'; -- key to use for joins

  -- fiss
  -- parent_table VARCHAR(50) := 'fiss_claims';
  -- child_tables VARCHAR[] := ARRAY['fiss_revenue_lines', 'fiss_payers', 'fiss_diagnosis_codes', 'fiss_proc_codes', 'fiss_audit_trails'];
  -- claim_id VARCHAR(50) := 'claim_id'; -- key to use for joins

  -- knobs
  aged_claim_days CONSTANT VARCHAR(25) := '63 days'; -- claims with last_updated older than this will be deleted
  batch_size CONSTANT INTEGER := 100000000; -- how many claim_ids per loop to delete
  max_lag_threshold_ms interval := '1000'; -- sleep if replication lag of any reader is greater than this
  min_lag_cont_threshold_ms interval := '250'; -- until all readers fall below this
  sleep_seconds integer := 15; -- sleep time

  -- loop vars and counters
  deleted_rows integer;
  aged_claim_count bigint;
  duration_minutes numeric;
  start_loop timestamp;
  end_loop timestamp;
  loop_time interval;
  start_time timestamp;
  end_time timestamp;
  current_lag_ms interval;
  child_table VARCHAR(50);
BEGIN
  EXECUTE format('CREATE TEMPORARY TABLE aged_claim_ids (%I VARCHAR(43) PRIMARY KEY);', claim_id);
  -- disable autovacuum and autoanalyze
  ALTER TABLE aged_claim_ids SET (autovacuum_enabled = false);
  ALTER TABLE parent_table SET (autovacuum_enabled = false);
  FOREACH child_table IN ARRAY child_tables LOOP
    EXECUTE format('ALTER TABLE %I SET (autovacuum_enabled = false)', child_table);
  END LOOP;

  -- begin the batch delete process
  LOOP
    -- empty the temp table and get a new batch
    SELECT INTO start_loop clock_timestamp();
    RAISE NOTICE '% fetching % % claims older than %', timeofday(), batch_size, parent_table, aged_claim_days;
    TRUNCATE aged_claim_ids;
    EXECUTE format('INSERT INTO aged_claim_ids(%I) SELECT %I FROM %I WHERE last_updated < NOW() - INTERVAL ''%s''', claim_id, claim_id, parent_table, aged_claim_days) || ' LIMIT ' || batch_size;
    SELECT COUNT(*) INTO aged_claim_count FROM aged_claim_ids;
    RAISE NOTICE '% found % aged claims... running batch delete', timeofday(), aged_claim_count;

    -- do deletes in a transaction and rollback on any errors
    BEGIN
      FOREACH child_table IN ARRAY child_tables LOOP
        start_time := clock_timestamp();
        EXECUTE format('DELETE FROM %I USING aged_claim_ids WHERE %I.%I = aged_claim_ids.%I', child_table, child_table, claim_id, claim_id);
        GET DIAGNOSTICS deleted_rows := ROW_COUNT;
        end_time := clock_timestamp();
        duration_minutes := extract(epoch from (end_time - start_time)) / 60;
        RAISE NOTICE '% removed % aged claims from % (duration: % minutes)', timeofday(), deleted_rows, child_table, duration_minutes;

       	--replication lag backoff loop (if lag > than max then sleep until lag is less than min)
        SELECT max(replica_lag_in_msec) INTO current_lag_ms FROM aurora_replica_status() WHERE 'master_session_id' != session_id;
        IF current_lag_ms > max_lag_threshold_ms THEN
          LOOP -- sleep until all readers are below the min threshold
            RAISE NOTICE '% replication lag exceeds thresholds. sleeping for % seconds.', timeofday(),sleep_seconds;
            PERFORM pg_sleep(sleep_seconds);
            SELECT max(replica_lag_in_msec) INTO current_lag_ms FROM aurora_replica_status() WHERE 'master_session_id' != session_id;
            EXIT WHEN current_lag_ms < min_lag_cont_threshold_ms;
          END LOOP;
        END IF;
      END LOOP;
    EXCEPTION
      WHEN OTHERS THEN
        -- Rollback the transaction in case of any error
        ROLLBACK;
        RAISE EXCEPTION 'Error occurred during deletion: %', SQLERRM;
    END;
    -- commit and exit if we are on the last batch of available claims
    COMMIT;
    SELECT INTO end_loop clock_timestamp();
    SELECT INTO loop_time end_loop - start_loop;
    RAISE NOTICE '% finished batch loop in % minutes', timeofday(), extract(epoch from loop_time) / 60;
    EXIT WHEN aged_claim_count < batch_size;
  END LOOP;
  -- cleanup
  DROP TABLE aged_claim_ids;
  -- optional but recommended if not repacking afterwards
  -- FOREACH child_table IN ARRAY child_tables LOOP
  --   RAISE NOTICE '% vacuuming and analyzing %', timeofday(), child_table;
  --   EXECUTE format('VACUUM ANALYZE %I', child_table);
  -- END LOOP;
  -- RAISE NOTICE '% vacuuming and analyzing %', timeofday(), parent_table;
  -- EXECUTE format('VACUUM ANALYZE %I', parent_table);
  
  -- re-enable autovacuum and autoanalyze
  ALTER TABLE parent_table SET (autovacuum_enabled = true);
  FOREACH child_table IN ARRAY child_tables LOOP
    EXECUTE format('ALTER TABLE %I SET (autovacuum_enabled = true)', child_table);
  END LOOP;

  -- RAISE NOTICE '% % batch delete job completed successfully', timeofday(), parent_table;
END $$;

