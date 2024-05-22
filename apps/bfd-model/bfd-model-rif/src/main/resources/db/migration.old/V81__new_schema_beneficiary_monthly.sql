-- NEW_SCHEMA_BENEFICIARY_MONTHLY.SQL
-- flyway migration for BENEFICIARY_MONTHLY table into
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
-- For HSQL, explicitly define/create a new BENEFICIARY_MONTHLY_NEW table in
-- the current PUBLIC schema
--
${logic.hsql-only}  create table public.beneficiary_monthly_new (
${logic.hsql-only}    bene_id                                    bigint not null,
${logic.hsql-only}    year_month                                 date not null,
${logic.hsql-only}    partd_contract_number_id                   varchar(5),
${logic.hsql-only}    partc_contract_number_id                   varchar(5),
${logic.hsql-only}    medicare_status_code                       varchar(2),
${logic.hsql-only}    fips_state_cnty_code                       varchar(5),
${logic.hsql-only}    entitlement_buy_in_ind                     char(1),
${logic.hsql-only}    hmo_indicator_ind                          char(1),
${logic.hsql-only}    medicaid_dual_eligibility_code             varchar(2),
${logic.hsql-only}    partd_pbp_number_id                        varchar(3),
${logic.hsql-only}    partd_retiree_drug_subsidy_ind             char(1),
${logic.hsql-only}    partd_segment_number_id                    varchar(3),
${logic.hsql-only}    partd_low_income_cost_share_group_code     varchar(2),
${logic.hsql-only}    partc_pbp_number_id                        varchar(3),
${logic.hsql-only}    partc_plan_type_code                       varchar(3),
${logic.hsql-only} constraint public.beneficiary_monthly_new_pkey
${logic.hsql-only} primary key (bene_id, year_month) );
--
-- For PSQL, try to parallelize insert processing
${logic.psql-only} SET max_parallel_workers = 24;
${logic.psql-only} SET max_parallel_workers_per_gather = 20;
${logic.psql-only} SET parallel_leader_participation = off;
${logic.psql-only} SET parallel_tuple_cost = 0;
${logic.psql-only} SET parallel_setup_cost = 0;
--
--
${logic.hsql-only} insert into public.beneficiary_monthly_new (
${logic.hsql-only}    bene_id,
${logic.hsql-only}    year_month,
${logic.hsql-only}    partd_contract_number_id,
${logic.hsql-only}    partc_contract_number_id,
${logic.hsql-only}    medicare_status_code,
${logic.hsql-only}    fips_state_cnty_code,
${logic.hsql-only}    entitlement_buy_in_ind,
${logic.hsql-only}    hmo_indicator_ind,
${logic.hsql-only}    medicaid_dual_eligibility_code,
${logic.hsql-only}    partd_pbp_number_id,
${logic.hsql-only}    partd_retiree_drug_subsidy_ind,
${logic.hsql-only}    partd_segment_number_id,
${logic.hsql-only}    partd_low_income_cost_share_group_code,
${logic.hsql-only}    partc_pbp_number_id,
${logic.hsql-only}    partc_plan_type_code )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} create table public.beneficiary_monthly_new as
select 
${logic.hsql-only}  convert(bene_id, SQL_BIGINT),
${logic.psql-only}  bene_id::bigint,
                    year_month,
                    partd_contract_number_id,
                    partc_contract_number_id,
                    medicare_status_code,
                    fips_state_cnty_code,
                    entitlement_buy_in_ind,
                    hmo_indicator_ind,
                    medicaid_dual_eligibility_code,
                    partd_pbp_number_id,
                    partd_retiree_drug_subsidy_ind,
                    partd_segment_number_id,
                    partd_low_income_cost_share_group_code,
                    partc_pbp_number_id,
                    partc_plan_type_code 
from
    public.beneficiary_monthly;
    
${logic.psql-only} alter table public.beneficiary_monthly_new
${logic.psql-only}    alter column bene_id             SET NOT NULL,
${logic.psql-only}    alter column year_month          SET NOT NULL;

-- for PSQL need to define our primary key    
${logic.psql-only} alter table public.beneficiary_monthly_new
${logic.psql-only}     add CONSTRAINT beneficiary_monthly_new_pkey PRIMARY KEY (bene_id, year_month);

-- create an index to optimized Part D Contract by date
CREATE INDEX IF NOT EXISTS beneficiary_monthly_new_partd_contract_year_month_bene_id_idx
    ON public.beneficiary_monthly_new (partd_contract_number_id, year_month, bene_id);

-- define foreign key constraints to beneficiary table.
ALTER TABLE IF EXISTS public.beneficiary_monthly_new
    ADD CONSTRAINT beneficiary_monthly_new_to_benficiaries FOREIGN KEY (bene_id)
        REFERENCES public.beneficiaries_new (bene_id);