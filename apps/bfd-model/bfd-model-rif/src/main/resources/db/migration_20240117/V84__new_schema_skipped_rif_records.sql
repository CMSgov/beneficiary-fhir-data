-- NEW_SCHEMA_SKIPPED_RIF_RECORDS.SQL
-- flyway migration for SKIPPED_RIF_RECORDS table into
-- a new schema structure that:
--   1) changes data type of BENE_ID from varchar to BIGINT.
--
-- Once current table data is migrated to new table name/structure, a 
-- subsequent PR will be deployed that changes the ORM model(s) and
-- operational code for Beneficiaries table.
--
-- HSQL differs from PSQL (postgres) in that the table defintion
-- must be explicitly declared prior to loading data into the
-- target table. PSQL can derive the table structure based on
-- the data input (i.e., column name, data type). Thus, for HSQL,
-- we need to explicitly define the table structure prior to loading data.
--
-- For HSQL, explicitly define/create a new SKIPPED_RIF_RECORDS_NEW table in
-- the current PUBLIC schema
--
${logic.hsql-only}  create table public.skipped_rif_records_new (
${logic.hsql-only}    record_id                                  bigint not null,
${logic.hsql-only}    bene_id                                    bigint not null,
${logic.hsql-only}    skip_reason                                varchar(50) not null,
${logic.hsql-only}    rif_file_timestamp                         timestamp with time zone not null,
${logic.hsql-only}    rif_file_type                              varchar(48) not null,
${logic.hsql-only}    dml_ind                                    varchar(6) not null,
${logic.hsql-only}    rif_data                                   ${type.text} not null,
${logic.hsql-only} constraint public.skipped_rif_records_new_pkey
${logic.hsql-only} primary key (record_id) );
--
-- For PSQL, try to parallelize insert processing
${logic.psql-only} SET max_parallel_workers = 24;
${logic.psql-only} SET max_parallel_workers_per_gather = 20;
${logic.psql-only} SET parallel_leader_participation = off;
${logic.psql-only} SET parallel_tuple_cost = 0;
${logic.psql-only} SET parallel_setup_cost = 0;
--
--
${logic.hsql-only} insert into public.skipped_rif_records_new (
${logic.hsql-only}    record_id,
${logic.hsql-only}    bene_id,
${logic.hsql-only}    skip_reason,
${logic.hsql-only}    rif_file_timestamp,
${logic.hsql-only}    rif_file_type,
${logic.hsql-only}    dml_ind,
${logic.hsql-only}    rif_data )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} create table public.skipped_rif_records_new as
select 
                    record_id,
${logic.hsql-only}  convert(bene_id, SQL_BIGINT),
${logic.psql-only}  bene_id::bigint,
                    skip_reason,
                    rif_file_timestamp,
                    rif_file_type,
                    dml_ind,
                    rif_data
from
    public.skipped_rif_records;
    
${logic.psql-only} alter table public.skipped_rif_records_new
${logic.psql-only}    alter column record_id           SET NOT NULL,
${logic.psql-only}    alter column bene_id             SET NOT NULL,
${logic.psql-only}    alter column skip_reason         SET NOT NULL,
${logic.psql-only}    alter column rif_file_timestamp  SET NOT NULL,
${logic.psql-only}    alter column rif_file_type       SET NOT NULL,
${logic.psql-only}    alter column dml_ind             SET NOT NULL,
${logic.psql-only}    alter column rif_data            SET NOT NULL;

-- for PSQL need to define our primary key    
${logic.psql-only} alter table public.skipped_rif_records_new
${logic.psql-only}     add CONSTRAINT skipped_rif_records_new_pkey PRIMARY KEY (record_id);

-- create an index on bene_id
CREATE INDEX IF NOT EXISTS skipped_rif_records_new_bene_id_idx
    ON public.skipped_rif_records_new (bene_id);