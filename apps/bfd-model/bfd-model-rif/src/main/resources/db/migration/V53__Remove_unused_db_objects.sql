-- both psql and hsql support non-primary key index renaming
ALTER INDEX beneficiary_monthly_year_month_partd_contract_bene_id_idx RENAME TO beneficiary_monthly_partd_contract_year_month_bene_id_idx;

-- to check if an index is not used in postgres:
-- select * from pg_stat_all_indexes where schemaname = 'public' and relname = 'beneficiary_monthly'
DROP INDEX IF EXISTS beneficiary_monthly_partd_contract_number_year_month_idx;

DROP SEQUENCE IF EXISTS loaded_batches_loaded_batchid_seq restrict;
DROP SEQUENCE IF EXISTS loaded_files_loaded_fileid_seq restrict;
DROP SEQUENCE IF EXISTS beneficiaryhistory_bene_history_id_seq restrict;