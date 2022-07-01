-- NEW_SCHEMA_BENEFICIARIES_HISTORY.SQL
-- flyway migration for BENEFICIARIES_HISTORY table into
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
-- For HSQL, explicitly define/create a new BENEFICIARIES_HISTORY_NEW table in
-- the current PUBLIC schema
--
${logic.hsql-only}  create table public.beneficiaries_history_new (
${logic.hsql-only}    bene_history_id      bigint not null,
${logic.hsql-only}    bene_id              bigint not null,
${logic.hsql-only}    hicn_unhashed        varchar(11),
${logic.hsql-only}    mbi_num              varchar(11),
${logic.hsql-only}    efctv_bgn_dt         date,
${logic.hsql-only}    efctv_end_dt         date,
${logic.hsql-only}    bene_crnt_hic_num    varchar(64) not null,
${logic.hsql-only}    mbi_hash             varchar(64),
${logic.hsql-only}    bene_birth_dt        date not null,
${logic.hsql-only}    bene_sex_ident_cd    char(1) not null,
${logic.hsql-only}    last_updated         timestamp with time zone,
${logic.hsql-only} constraint public.beneficiaries_history_new_pkey
${logic.hsql-only} primary key (bene_history_id) );
--
--
${logic.psql-only} SET max_parallel_workers = 24;
${logic.psql-only} SET max_parallel_workers_per_gather = 20;
${logic.psql-only} SET parallel_leader_participation = off;
${logic.psql-only} SET parallel_tuple_cost = 0;
${logic.psql-only} SET parallel_setup_cost = 0;
--
--
${logic.hsql-only} insert into public.beneficiaries_history_new (
${logic.hsql-only}    bene_history_id,
${logic.hsql-only}    bene_id,
${logic.hsql-only}    hicn_unhashed,
${logic.hsql-only}    mbi_num,
${logic.hsql-only}    efctv_bgn_dt,
${logic.hsql-only}    efctv_end_dt,
${logic.hsql-only}    bene_crnt_hic_num,
${logic.hsql-only}    mbi_hash,
${logic.hsql-only}    bene_birth_dt,
${logic.hsql-only}    bene_sex_ident_cd,
${logic.hsql-only}    last_updated )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} create table public.beneficiaries_history_new as
select 
                    bene_history_id,
${logic.hsql-only}  convert(bene_id, SQL_BIGINT),
${logic.psql-only}  bene_id::bigint,
                    hicn_unhashed,
                    mbi_num,
                    efctv_bgn_dt,
                    efctv_end_dt,
                    bene_crnt_hic_num,
                    mbi_hash,
                    bene_birth_dt,
                    bene_sex_ident_cd,
                    last_updated
from
    public.beneficiaries_history;
    
${logic.psql-only} alter table public.beneficiaries_history_new
${logic.psql-only}    alter column bene_history_id     SET NOT NULL,
${logic.psql-only}    alter column bene_id             SET NOT NULL,
${logic.psql-only}    alter column bene_crnt_hic_num   SET NOT NULL,
${logic.psql-only}    alter column bene_birth_dt       SET NOT NULL,
${logic.psql-only}    alter column bene_sex_ident_cd   SET NOT NULL;

-- for PSQL need to define our primary key    
${logic.psql-only} alter table public.beneficiaries_history_new
${logic.psql-only}     add CONSTRAINT beneficiaries_history_new_pkey PRIMARY KEY (bene_history_id);

-- create an index of the BENE_ID
CREATE INDEX IF NOT EXISTS beneficiaries_history_new_bene_id_idx
    ON public.beneficiaries_history_new (bene_id);
    
-- create an index of the BENE_CRNT_HIC_NUM
CREATE INDEX IF NOT EXISTS beneficiaries_history_new_hicn_idx
    ON public.beneficiaries_history_new (bene_crnt_hic_num);
    
-- create an index of the MBI_HASH
CREATE INDEX IF NOT EXISTS beneficiaries_history_new_mbi_hash_idx
    ON public.beneficiaries_history_new (mbi_hash);

-- define foreign key constraints to beneficiary table.
ALTER TABLE IF EXISTS public.beneficiaries_history_new
    ADD CONSTRAINT beneficiaries_history_new_to_benficiaries FOREIGN KEY (bene_id)
        REFERENCES public.beneficiaries_new (bene_id);