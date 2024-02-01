-- NEW_SCHEMA_MEDICARE_BENEFICIARY_HISTORY.SQL
-- flyway migration for MEDICARE_BENEFICIARY_HISTORY table into
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
-- For HSQL, explicitly define/create a new MEDICARE_BENEFICIARY_HISTORY_NEW table in
-- the current PUBLIC schema
--
${logic.hsql-only}  create table public.medicare_beneficiaryid_history_new (
${logic.hsql-only}    bene_mbi_id                    numeric not null,
${logic.hsql-only}    bene_id                        bigint,
${logic.hsql-only}    mbi_sqnc_num                   numeric,
${logic.hsql-only}    bene_ident_cd                  varchar(2),
${logic.hsql-only}    bene_clm_acnt_num              varchar(9),
${logic.hsql-only}    bene_crnt_rec_ind_id           numeric,
${logic.hsql-only}    mbi_num                        varchar(11),
${logic.hsql-only}    mbi_card_rqst_dt               date,
${logic.hsql-only}    mbi_bgn_rsn_cd                 varchar(5),
${logic.hsql-only}    mbi_end_rsn_cd                 varchar(5),
${logic.hsql-only}    mbi_efctv_bgn_dt               date,
${logic.hsql-only}    mbi_efctv_end_dt               date,
${logic.hsql-only}    last_updated                   timestamp with time zone,
${logic.hsql-only}    creat_user_id                  varchar(30),
${logic.hsql-only}    creat_ts                       timestamp without time zone,
${logic.hsql-only}    updt_user_id                   varchar(30),
${logic.hsql-only}    updt_ts                        timestamp without time zone,
${logic.hsql-only} constraint public.medicare_beneficiaryid_history_new_pkey
${logic.hsql-only} primary key (bene_mbi_id) );
--
--
${logic.psql-only} SET max_parallel_workers = 24;
${logic.psql-only} SET max_parallel_workers_per_gather = 20;
${logic.psql-only} SET parallel_leader_participation = off;
${logic.psql-only} SET parallel_tuple_cost = 0;
${logic.psql-only} SET parallel_setup_cost = 0;
--
--
${logic.hsql-only} insert into public.medicare_beneficiaryid_history_new (
${logic.hsql-only}    bene_mbi_id,
${logic.hsql-only}    bene_id,
${logic.hsql-only}    mbi_sqnc_num,
${logic.hsql-only}    bene_ident_cd,
${logic.hsql-only}    bene_clm_acnt_num,
${logic.hsql-only}    bene_crnt_rec_ind_id,
${logic.hsql-only}    mbi_num,
${logic.hsql-only}    mbi_card_rqst_dt,
${logic.hsql-only}    mbi_bgn_rsn_cd,
${logic.hsql-only}    mbi_end_rsn_cd,
${logic.hsql-only}    mbi_efctv_bgn_dt,
${logic.hsql-only}    mbi_efctv_end_dt,
${logic.hsql-only}    last_updated,
${logic.hsql-only}    creat_user_id,
${logic.hsql-only}    creat_ts,
${logic.hsql-only}    updt_user_id,
${logic.hsql-only}    updt_ts )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} create table public.medicare_beneficiaryid_history_new as
select 
                    bene_mbi_id,
${logic.hsql-only}  convert(bene_id, SQL_BIGINT),
${logic.psql-only}  bene_id::bigint,
                    mbi_sqnc_num, 
                    bene_ident_cd,
                    bene_clm_acnt_num,
                    bene_crnt_rec_ind_id,
                    mbi_num,
                    mbi_card_rqst_dt,
                    mbi_bgn_rsn_cd,
                    mbi_end_rsn_cd,
                    mbi_efctv_bgn_dt,
                    mbi_efctv_end_dt,
                    last_updated,
                    creat_user_id,
                    creat_ts,
                    updt_user_id,
                    updt_ts
from
    public.medicare_beneficiaryid_history;

${logic.psql-only} alter table public.medicare_beneficiaryid_history_new
${logic.psql-only}    alter column bene_mbi_id             SET NOT NULL;

-- for PSQL need to define our primary key    
${logic.psql-only} alter table public.medicare_beneficiaryid_history_new
${logic.psql-only}     add CONSTRAINT medicare_beneficiaryid_history_new_pkey PRIMARY KEY (bene_mbi_id);

-- create an index of the BENE_ID
CREATE INDEX IF NOT EXISTS medicare_beneficiaryid_history_new_bene_id_idx
    ON public.medicare_beneficiaryid_history_new (bene_id);

-- define foreign key constraints to beneficiary table.
ALTER TABLE IF EXISTS public.medicare_beneficiaryid_history_new
    ADD CONSTRAINT medicare_beneficiaryid_history_new_to_benficiaries FOREIGN KEY (bene_id)
        REFERENCES public.beneficiaries_new (bene_id);
