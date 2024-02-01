-- NEW_SCHEMA_PARTD_EVENTS.SQL
-- flyway migration for PARTD_EVENTS table into
-- a new schema structure that:
--   1) changes data type of PDE_ID, BENE_ID from varchar to BIGINT.
--   2) change data type of CLM_GRP_ID from numeric to BIGINT
--   3) organizes parent claim table (PARTD_EVENTS) such that common
--      claims data columns are organized at top of table structure.
--
-- Once current table data is migrated to new table name/structure, a 
-- subsequent PR will be deployed that changes the ORM model(s) and
-- operational code for PartD Events table.
--
-- HSQL differs from PSQL (postgres) in that the table defintion
-- must be explicitly declared prior to loading data into the
-- target table. PSQL can derive the table structure based on
-- the data input (i.e., column name, data type). Thus, for HSQL,
-- we need to explicitly define the table structure prior to loading data.
--
-- For HSQL, explicitly define/create a new PARTD_EVENTS_NEW table in
-- the current PUBLIC schema

${logic.hsql-only}  create table public.partd_events_new (
${logic.hsql-only}    pde_id                        bigint not null,
${logic.hsql-only}    bene_id                       bigint not null,
${logic.hsql-only}    clm_grp_id                    bigint not null,
${logic.hsql-only}    last_updated                  timestamp with time zone,
${logic.hsql-only}    final_action                  char(1) not null,
${logic.hsql-only}    cmpnd_cd                      integer not null,
${logic.hsql-only}    drug_cvrg_stus_cd             char(1) not null,
${logic.hsql-only}    days_suply_num                numeric not null,
${logic.hsql-only}    srvc_dt                       date not null,
${logic.hsql-only}    pd_dt                         date,
${logic.hsql-only}    fill_num                      numeric not null,
${logic.hsql-only}    qty_dspnsd_num                numeric(10,3) not null,
${logic.hsql-only}    cvrd_d_plan_pd_amt            numeric(8,2) not null,
${logic.hsql-only}    gdc_abv_oopt_amt              numeric(8,2) not null,
${logic.hsql-only}    gdc_blw_oopt_amt              numeric(8,2) not null,
${logic.hsql-only}    lics_amt                      numeric(8,2) not null,
${logic.hsql-only}    ncvrd_plan_pd_amt             numeric(8,2) not null,
${logic.hsql-only}    othr_troop_amt                numeric(8,2) not null,
${logic.hsql-only}    plro_amt                      numeric(8,2) not null,
${logic.hsql-only}    ptnt_pay_amt                  numeric(8,2) not null,
${logic.hsql-only}    rptd_gap_dscnt_num            numeric(8,2) not null,
${logic.hsql-only}    ptnt_rsdnc_cd                 varchar(2) not null,
${logic.hsql-only}    tot_rx_cst_amt                numeric(8,2) not null,
${logic.hsql-only}    daw_prod_slctn_cd             char(1) not null,
${logic.hsql-only}    phrmcy_srvc_type_cd           varchar(2) not null,
${logic.hsql-only}    plan_cntrct_rec_id            varchar(5) not null,
${logic.hsql-only}    plan_pbp_rec_num              varchar(3) not null,
${logic.hsql-only}    prod_srvc_id                  varchar(19) not null,
${logic.hsql-only}    prscrbr_id                    varchar(15) not null,
${logic.hsql-only}    prscrbr_id_qlfyr_cd           varchar(2) not null,
${logic.hsql-only}    rx_srvc_rfrnc_num             numeric(12,0) not null,
${logic.hsql-only}    srvc_prvdr_id                 varchar(15) not null,
${logic.hsql-only}    srvc_prvdr_id_qlfyr_cd        varchar(2) not null,
${logic.hsql-only}    adjstmt_dltn_cd               char(1),
${logic.hsql-only}    brnd_gnrc_cd                  char(1),
${logic.hsql-only}    ctstrphc_cvrg_cd              char(1),
${logic.hsql-only}    dspnsng_stus_cd               char(1),
${logic.hsql-only}    nstd_frmt_cd                  char(1),
${logic.hsql-only}    prcng_excptn_cd               char(1),
${logic.hsql-only}    rx_orgn_cd                    char(1),
${logic.hsql-only}    submsn_clr_cd                 varchar(2),
${logic.hsql-only} constraint public.partd_events_new_pkey
${logic.hsql-only} primary key (pde_id) );

-- migrate data via INSERT from current PARTD_EVENTS table to PARTD_EVENTS_NEW table
--
${logic.hsql-only} insert into public.partd_events_new (
${logic.hsql-only}    pde_id,
${logic.hsql-only}    bene_id,
${logic.hsql-only}    clm_grp_id,
${logic.hsql-only}    last_updated,
${logic.hsql-only}    final_action,
${logic.hsql-only}    cmpnd_cd,
${logic.hsql-only}    drug_cvrg_stus_cd,
${logic.hsql-only}    days_suply_num,
${logic.hsql-only}    srvc_dt,
${logic.hsql-only}    pd_dt,
${logic.hsql-only}    fill_num,
${logic.hsql-only}    qty_dspnsd_num,
${logic.hsql-only}    cvrd_d_plan_pd_amt,
${logic.hsql-only}    gdc_abv_oopt_amt,
${logic.hsql-only}    gdc_blw_oopt_amt,
${logic.hsql-only}    lics_amt,
${logic.hsql-only}    ncvrd_plan_pd_amt,
${logic.hsql-only}    othr_troop_amt,
${logic.hsql-only}    plro_amt,
${logic.hsql-only}    ptnt_pay_amt,
${logic.hsql-only}    rptd_gap_dscnt_num,
${logic.hsql-only}    ptnt_rsdnc_cd,
${logic.hsql-only}    tot_rx_cst_amt,
${logic.hsql-only}    daw_prod_slctn_cd,
${logic.hsql-only}    phrmcy_srvc_type_cd,
${logic.hsql-only}    plan_cntrct_rec_id,
${logic.hsql-only}    plan_pbp_rec_num,
${logic.hsql-only}    prod_srvc_id,
${logic.hsql-only}    prscrbr_id,
${logic.hsql-only}    prscrbr_id_qlfyr_cd,
${logic.hsql-only}    rx_srvc_rfrnc_num,
${logic.hsql-only}    srvc_prvdr_id,
${logic.hsql-only}    srvc_prvdr_id_qlfyr_cd,
${logic.hsql-only}    adjstmt_dltn_cd,
${logic.hsql-only}    brnd_gnrc_cd,
${logic.hsql-only}    ctstrphc_cvrg_cd,
${logic.hsql-only}    dspnsng_stus_cd,
${logic.hsql-only}    nstd_frmt_cd,
${logic.hsql-only}    prcng_excptn_cd,
${logic.hsql-only}    rx_orgn_cd,
${logic.hsql-only}    submsn_clr_cd )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} SET max_parallel_workers = 24;
${logic.psql-only} SET max_parallel_workers_per_gather = 20;
${logic.psql-only} SET parallel_leader_participation = off;
${logic.psql-only} SET parallel_tuple_cost = 0;
${logic.psql-only} SET parallel_setup_cost = 0;

${logic.psql-only} create table public.partd_events_new as
select
${logic.hsql-only}    convert(pde_id, SQL_BIGINT),
${logic.hsql-only}    convert(bene_id, SQL_BIGINT),
${logic.hsql-only}    convert(clm_grp_id, SQL_BIGINT),
${logic.psql-only}    pde_id::bigint,
${logic.psql-only}    bene_id::bigint,
${logic.psql-only}    clm_grp_id::bigint,
                      last_updated,
                      final_action,
                      cmpnd_cd,
                      drug_cvrg_stus_cd,
                      days_suply_num,
                      srvc_dt,
                      pd_dt,
                      fill_num,
                      qty_dspnsd_num,
                      cvrd_d_plan_pd_amt,
                      gdc_abv_oopt_amt,
                      gdc_blw_oopt_amt,
                      lics_amt,
                      ncvrd_plan_pd_amt,
                      othr_troop_amt,
                      plro_amt,
                      ptnt_pay_amt,
                      rptd_gap_dscnt_num,
                      ptnt_rsdnc_cd,
                      tot_rx_cst_amt,
                      daw_prod_slctn_cd,
                      phrmcy_srvc_type_cd,
                      plan_cntrct_rec_id,
                      plan_pbp_rec_num,
                      prod_srvc_id,
                      prscrbr_id,
                      prscrbr_id_qlfyr_cd,
                      rx_srvc_rfrnc_num,
                      srvc_prvdr_id,
                      srvc_prvdr_id_qlfyr_cd,
                      adjstmt_dltn_cd,
                      brnd_gnrc_cd,
                      ctstrphc_cvrg_cd,
                      dspnsng_stus_cd,
                      nstd_frmt_cd,
                      prcng_excptn_cd,
                      rx_orgn_cd,
                      submsn_clr_cd
from
    public.partd_events;
    
-- for PSQL need to define our not null constraints; for HSQL,
-- we explicitly defined a table structure before migrating data.
--
${logic.psql-only} alter table public.partd_events_new
${logic.psql-only}    alter column pde_id                   SET NOT NULL,
${logic.psql-only}    alter column bene_id                  SET NOT NULL,
${logic.psql-only}    alter column clm_grp_id               SET NOT NULL,
${logic.psql-only}    alter column final_action             SET NOT NULL,
${logic.psql-only}    alter column cmpnd_cd                 SET NOT NULL,
${logic.psql-only}    alter column drug_cvrg_stus_cd        SET NOT NULL,
${logic.psql-only}    alter column days_suply_num           SET NOT NULL,
${logic.psql-only}    alter column srvc_dt                  SET NOT NULL,
${logic.psql-only}    alter column fill_num                 SET NOT NULL,
${logic.psql-only}    alter column qty_dspnsd_num           SET NOT NULL,
${logic.psql-only}    alter column cvrd_d_plan_pd_amt       SET NOT NULL,
${logic.psql-only}    alter column gdc_abv_oopt_amt         SET NOT NULL,
${logic.psql-only}    alter column gdc_blw_oopt_amt         SET NOT NULL,
${logic.psql-only}    alter column lics_amt                 SET NOT NULL,
${logic.psql-only}    alter column ncvrd_plan_pd_amt        SET NOT NULL,
${logic.psql-only}    alter column othr_troop_amt           SET NOT NULL,
${logic.psql-only}    alter column plro_amt                 SET NOT NULL,
${logic.psql-only}    alter column ptnt_pay_amt             SET NOT NULL,
${logic.psql-only}    alter column rptd_gap_dscnt_num       SET NOT NULL,
${logic.psql-only}    alter column ptnt_rsdnc_cd            SET NOT NULL,
${logic.psql-only}    alter column tot_rx_cst_amt           SET NOT NULL,
${logic.psql-only}    alter column daw_prod_slctn_cd        SET NOT NULL,
${logic.psql-only}    alter column phrmcy_srvc_type_cd      SET NOT NULL,
${logic.psql-only}    alter column plan_cntrct_rec_id       SET NOT NULL,
${logic.psql-only}    alter column plan_pbp_rec_num         SET NOT NULL,
${logic.psql-only}    alter column prod_srvc_id             SET NOT NULL,
${logic.psql-only}    alter column prscrbr_id               SET NOT NULL,
${logic.psql-only}    alter column prscrbr_id_qlfyr_cd      SET NOT NULL,
${logic.psql-only}    alter column rx_srvc_rfrnc_num        SET NOT NULL,
${logic.psql-only}    alter column srvc_prvdr_id            SET NOT NULL,
${logic.psql-only}    alter column srvc_prvdr_id_qlfyr_cd   SET NOT NULL;

-- for PSQL need to define our primary key    
${logic.psql-only} alter table public.partd_events_new
${logic.psql-only}     add CONSTRAINT partd_events_new_pkey PRIMARY KEY (pde_id);

-- create an index of the BENE_ID in partd_events_new table
CREATE INDEX IF NOT EXISTS partd_events_new_bene_id_idx
    ON public.partd_events_new (bene_id);          