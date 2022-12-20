-- NEW_SCHEMA_DME_CLAIMS.SQL
-- flyway migration for DME_CLAIMS and DME_CLAIM_LINES tables into
-- a new schema structure that:
--   1) changes data type of CLM_ID, BENE_ID from varchar to BIGINT.
--   2) change data type of CLM_GROUP_ID from numeric to BIGINT
--   3) organizes parent claim table (DME_CLAIMS_NEW) such that common
--      claims data columns are organized at top of table structure.
--
--      The following db columns were redefined from NUMERIC to more
--      appropriate data type(s):
--
--           line_num - changed to smallint
--
-- Once current table data is migrated to new table name/structure, a 
-- subsequent PR will be deployed that changes the ORM model(s) and
-- operational code for DME Claims table(s).
--
-- HSQL differs from PSQL (postgres) in that the table defintion
-- must be explicitly declared prior to loading data into the
-- target table. PSQL can derive the table structure based on
-- the data input (i.e., column name, data type). Thus, for HSQL,
-- we need to explicitly define the table structure prior to loading data.
--
-- For HSQL, explicitly define/create a new DME_CLAIMS_NEW table in
-- the current PUBLIC schema

${logic.hsql-only}  create table public.dme_claims_new (
${logic.hsql-only}    clm_id                                     bigint not null,
${logic.hsql-only}    bene_id                                    bigint not null,
${logic.hsql-only}    clm_grp_id                                 bigint not null,
${logic.hsql-only}    last_updated                               timestamp with time zone,
${logic.hsql-only}    clm_from_dt                                date not null,
${logic.hsql-only}    clm_thru_dt                                date not null,
${logic.hsql-only}    clm_disp_cd                                varchar(2) not null,
${logic.hsql-only}    final_action                               char(1) not null,
${logic.hsql-only}    clm_pmt_amt                                numeric(12,2) not null,
${logic.hsql-only}    carr_num                                   varchar(5) not null,
${logic.hsql-only}    carr_clm_cntl_num                          varchar(23),
${logic.hsql-only}    carr_clm_prvdr_asgnmt_ind_sw               char(1) not null,
${logic.hsql-only}    carr_clm_entry_cd                          char(1) not null,
${logic.hsql-only}    carr_clm_prmry_pyr_pd_amt                  numeric(12,2) not null,
${logic.hsql-only}    carr_clm_cash_ddctbl_apld_amt              numeric(12,2) not null,
${logic.hsql-only}    carr_clm_pmt_dnl_cd                        varchar(2) not null,
${logic.hsql-only}    carr_clm_hcpcs_yr_cd                       char(1),
${logic.hsql-only}    nch_clm_type_cd                            varchar(2) not null,
${logic.hsql-only}    nch_near_line_rec_ident_cd                 char(1) not null,
${logic.hsql-only}    nch_wkly_proc_dt                           date not null,
${logic.hsql-only}    nch_carr_clm_alowd_amt                     numeric(12,2) not null,
${logic.hsql-only}    nch_carr_clm_sbmtd_chrg_amt                numeric(12,2) not null,
${logic.hsql-only}    nch_clm_bene_pmt_amt                       numeric(12,2) not null,
${logic.hsql-only}    nch_clm_prvdr_pmt_amt                      numeric(12,2) not null,
${logic.hsql-only}    prncpal_dgns_cd                            varchar(7),
${logic.hsql-only}    prncpal_dgns_vrsn_cd                       char(1),
${logic.hsql-only}    rfr_physn_npi                              varchar(12),
${logic.hsql-only}    rfr_physn_upin                             varchar(12),
${logic.hsql-only}    clm_clncl_tril_num                         varchar(8),
${logic.hsql-only}    icd_dgns_cd1                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd1                          char(1),
${logic.hsql-only}    icd_dgns_cd2                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd2                          char(1),
${logic.hsql-only}    icd_dgns_cd3                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd3                          char(1),
${logic.hsql-only}    icd_dgns_cd4                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd4                          char(1),
${logic.hsql-only}    icd_dgns_cd5                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd5                          char(1),
${logic.hsql-only}    icd_dgns_cd6                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd6                          char(1),
${logic.hsql-only}    icd_dgns_cd7                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd7                          char(1),
${logic.hsql-only}    icd_dgns_cd8                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd8                          char(1),
${logic.hsql-only}    icd_dgns_cd9                               varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd9                          char(1),
${logic.hsql-only}    icd_dgns_cd10                              varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd10                         char(1),
${logic.hsql-only}    icd_dgns_cd11                              varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd11                         char(1),
${logic.hsql-only}    icd_dgns_cd12                              varchar(7),
${logic.hsql-only}    icd_dgns_vrsn_cd12                         char(1),
${logic.hsql-only}    constraint public.dme_claims_new_pkey
${logic.hsql-only}    primary key (clm_id) );

-- create a new DME_CLAIM_LINES_NEW table in the current PUBLIC schema
--
${logic.hsql-only} CREATE TABLE IF NOT EXISTS public.dme_claim_lines_new (
${logic.hsql-only}    clm_id                                     bigint not null,
${logic.hsql-only}    line_num                                   smallint not null,
${logic.hsql-only}    line_1st_expns_dt                          date,
${logic.hsql-only}    line_last_expns_dt                         date,
${logic.hsql-only}    line_alowd_chrg_amt                        numeric(12,2) not null,
${logic.hsql-only}    line_coinsrnc_amt                          numeric(12,2) not null,
${logic.hsql-only}    line_sbmtd_chrg_amt                        numeric(12,2) not null,
${logic.hsql-only}    line_bene_pmt_amt                          numeric(12,2) not null,
${logic.hsql-only}    line_prvdr_pmt_amt                         numeric(12,2) not null,
${logic.hsql-only}    line_bene_prmry_pyr_cd                     char(1),
${logic.hsql-only}    line_bene_prmry_pyr_pd_amt                 numeric(12,2) not null,
${logic.hsql-only}    line_bene_ptb_ddctbl_amt                   numeric(12,2) not null,
${logic.hsql-only}    line_place_of_srvc_cd                      varchar(2) not null,
${logic.hsql-only}    line_pmt_80_100_cd                         char(1),
${logic.hsql-only}    line_srvc_cnt                              numeric not null,
${logic.hsql-only}    line_cms_type_srvc_cd                      char(1) not null,
${logic.hsql-only}    line_hct_hgb_type_cd                       varchar(2),
${logic.hsql-only}    line_hct_hgb_rslt_num                      numeric(3,1) not null,
${logic.hsql-only}    line_ndc_cd                                varchar(11),
${logic.hsql-only}    line_nch_pmt_amt                           numeric(12,2) not null,
${logic.hsql-only}    line_icd_dgns_cd                           varchar(7),
${logic.hsql-only}    line_icd_dgns_vrsn_cd                      char(1),
${logic.hsql-only}    line_dme_prchs_price_amt                   numeric(12,2) not null,
${logic.hsql-only}    line_prmry_alowd_chrg_amt                  numeric(12,2) not null,
${logic.hsql-only}    line_prcsg_ind_cd                          varchar(2),
${logic.hsql-only}    line_service_deductible                    char(1),
${logic.hsql-only}    betos_cd                                   varchar(3),
${logic.hsql-only}    hcpcs_cd                                   varchar(5),
${logic.hsql-only}    hcpcs_1st_mdfr_cd                          varchar(5),
${logic.hsql-only}    hcpcs_2nd_mdfr_cd                          varchar(5),
${logic.hsql-only}    hcpcs_3rd_mdfr_cd                          varchar(5),
${logic.hsql-only}    hcpcs_4th_mdfr_cd                          varchar(5),
${logic.hsql-only}    dmerc_line_mtus_cd                         char(1),
${logic.hsql-only}    dmerc_line_mtus_cnt                        numeric(12,3) not null,
${logic.hsql-only}    dmerc_line_prcng_state_cd                  varchar(2),
${logic.hsql-only}    dmerc_line_scrn_svgs_amt                   numeric(12,2),
${logic.hsql-only}    dmerc_line_supplr_type_cd                  char(1),
${logic.hsql-only}    prtcptng_ind_cd                            char(1),
${logic.hsql-only}    prvdr_npi                                  varchar(12),
${logic.hsql-only}    prvdr_num                                  varchar(10),
${logic.hsql-only}    prvdr_spclty                               varchar(3),
${logic.hsql-only}    prvdr_state_cd                             varchar(2) not null,
${logic.hsql-only}    tax_num                                    varchar(10) not null,
${logic.hsql-only}    constraint public.dme_claim_lines_new_pkey
${logic.hsql-only}    primary key (clm_id, line_num) );

${logic.psql-only} SET max_parallel_workers = 24;
${logic.psql-only} SET max_parallel_workers_per_gather = 20;
${logic.psql-only} SET parallel_leader_participation = off;
${logic.psql-only} SET parallel_tuple_cost = 0;
${logic.psql-only} SET parallel_setup_cost = 0;

-- migrate data via INSERT from current DME_CLAIMS table to DME_CLAIMS_NEW table
--
${logic.hsql-only} insert into public.dme_claims_new (
${logic.hsql-only}    clm_id,
${logic.hsql-only}    bene_id,
${logic.hsql-only}    clm_grp_id,
${logic.hsql-only}    last_updated,
${logic.hsql-only}    clm_from_dt,
${logic.hsql-only}    clm_thru_dt,
${logic.hsql-only}    clm_disp_cd,
${logic.hsql-only}    final_action,
${logic.hsql-only}    clm_pmt_amt,
${logic.hsql-only}    carr_num,
${logic.hsql-only}    carr_clm_cntl_num,
${logic.hsql-only}    carr_clm_prvdr_asgnmt_ind_sw,
${logic.hsql-only}    carr_clm_entry_cd,
${logic.hsql-only}    carr_clm_prmry_pyr_pd_amt,
${logic.hsql-only}    carr_clm_cash_ddctbl_apld_amt,
${logic.hsql-only}    carr_clm_pmt_dnl_cd,
${logic.hsql-only}    carr_clm_hcpcs_yr_cd,
${logic.hsql-only}    nch_clm_type_cd,
${logic.hsql-only}    nch_near_line_rec_ident_cd,
${logic.hsql-only}    nch_wkly_proc_dt,
${logic.hsql-only}    nch_carr_clm_alowd_amt,
${logic.hsql-only}    nch_carr_clm_sbmtd_chrg_amt,
${logic.hsql-only}    nch_clm_bene_pmt_amt,
${logic.hsql-only}    nch_clm_prvdr_pmt_amt,
${logic.hsql-only}    prncpal_dgns_cd,
${logic.hsql-only}    prncpal_dgns_vrsn_cd,
${logic.hsql-only}    rfr_physn_npi,
${logic.hsql-only}    rfr_physn_upin,
${logic.hsql-only}    clm_clncl_tril_num,
${logic.hsql-only}    icd_dgns_cd1,
${logic.hsql-only}    icd_dgns_vrsn_cd1,
${logic.hsql-only}    icd_dgns_cd2,
${logic.hsql-only}    icd_dgns_vrsn_cd2,
${logic.hsql-only}    icd_dgns_cd3,
${logic.hsql-only}    icd_dgns_vrsn_cd3,
${logic.hsql-only}    icd_dgns_cd4,
${logic.hsql-only}    icd_dgns_vrsn_cd4,
${logic.hsql-only}    icd_dgns_cd5,
${logic.hsql-only}    icd_dgns_vrsn_cd5,
${logic.hsql-only}    icd_dgns_cd6,
${logic.hsql-only}    icd_dgns_vrsn_cd6,
${logic.hsql-only}    icd_dgns_cd7,
${logic.hsql-only}    icd_dgns_vrsn_cd7,
${logic.hsql-only}    icd_dgns_cd8,
${logic.hsql-only}    icd_dgns_vrsn_cd8,
${logic.hsql-only}    icd_dgns_cd9,
${logic.hsql-only}    icd_dgns_vrsn_cd9,
${logic.hsql-only}    icd_dgns_cd10,
${logic.hsql-only}    icd_dgns_vrsn_cd10,
${logic.hsql-only}    icd_dgns_cd11,
${logic.hsql-only}    icd_dgns_vrsn_cd11,
${logic.hsql-only}    icd_dgns_cd12,
${logic.hsql-only}    icd_dgns_vrsn_cd12 )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} create table public.dme_claims_new as
select
${logic.hsql-only}    convert(clm_id, SQL_BIGINT),
${logic.hsql-only}    convert(bene_id, SQL_BIGINT),
${logic.hsql-only}    convert(clm_grp_id, SQL_BIGINT),
${logic.psql-only}    cast(clm_id as bigint),
${logic.psql-only}    cast(bene_id as bigint),
${logic.psql-only}    cast(clm_grp_id as bigint), 
                      last_updated,    
                      clm_from_dt,
                      clm_thru_dt,
                      clm_disp_cd,
                      final_action,
                      clm_pmt_amt,
                      carr_num,
                      carr_clm_cntl_num,
                      carr_clm_prvdr_asgnmt_ind_sw,
                      carr_clm_entry_cd,
                      carr_clm_prmry_pyr_pd_amt,
                      carr_clm_cash_ddctbl_apld_amt,
                      carr_clm_pmt_dnl_cd,
                      carr_clm_hcpcs_yr_cd,
                      nch_clm_type_cd,
                      nch_near_line_rec_ident_cd,
                      nch_wkly_proc_dt,
                      nch_carr_clm_alowd_amt,
                      nch_carr_clm_sbmtd_chrg_amt,
                      nch_clm_bene_pmt_amt,
                      nch_clm_prvdr_pmt_amt,
                      prncpal_dgns_cd,
                      prncpal_dgns_vrsn_cd,
                      rfr_physn_npi,
                      rfr_physn_upin,
                      clm_clncl_tril_num,
                      icd_dgns_cd1,
                      icd_dgns_vrsn_cd1,
                      icd_dgns_cd2,
                      icd_dgns_vrsn_cd2,
                      icd_dgns_cd3,
                      icd_dgns_vrsn_cd3,
                      icd_dgns_cd4,
                      icd_dgns_vrsn_cd4,
                      icd_dgns_cd5,
                      icd_dgns_vrsn_cd5,
                      icd_dgns_cd6,
                      icd_dgns_vrsn_cd6,
                      icd_dgns_cd7,
                      icd_dgns_vrsn_cd7,
                      icd_dgns_cd8,
                      icd_dgns_vrsn_cd8,
                      icd_dgns_cd9,
                      icd_dgns_vrsn_cd9,
                      icd_dgns_cd10,
                      icd_dgns_vrsn_cd10,
                      icd_dgns_cd11,
                      icd_dgns_vrsn_cd11,
                      icd_dgns_cd12,
                      icd_dgns_vrsn_cd12
from
    public.dme_claims;
    

-- for PSQL need to define our not null constraints; for HSQL,
-- we explicitly defined a table structure before migrating data.
--
${logic.psql-only} alter table public.dme_claims_new
${logic.psql-only}    alter column clm_id                           SET NOT NULL,
${logic.psql-only}    alter column bene_id                          SET NOT NULL,
${logic.psql-only}    alter column clm_grp_id                       SET NOT NULL,
${logic.psql-only}    alter column clm_from_dt                      SET NOT NULL,
${logic.psql-only}    alter column clm_thru_dt                      SET NOT NULL,
${logic.psql-only}    alter column clm_disp_cd                      SET NOT NULL,
${logic.psql-only}    alter column final_action                     SET NOT NULL,
${logic.psql-only}    alter column clm_pmt_amt                      SET NOT NULL,
${logic.psql-only}    alter column carr_num                         SET NOT NULL,
${logic.psql-only}    alter column carr_clm_prvdr_asgnmt_ind_sw     SET NOT NULL,
${logic.psql-only}    alter column carr_clm_entry_cd                SET NOT NULL,
${logic.psql-only}    alter column carr_clm_prmry_pyr_pd_amt        SET NOT NULL,
${logic.psql-only}    alter column carr_clm_cash_ddctbl_apld_amt    SET NOT NULL,
${logic.psql-only}    alter column carr_clm_pmt_dnl_cd              SET NOT NULL,
${logic.psql-only}    alter column nch_clm_type_cd                  SET NOT NULL,
${logic.psql-only}    alter column nch_near_line_rec_ident_cd       SET NOT NULL,
${logic.psql-only}    alter column nch_wkly_proc_dt                 SET NOT NULL,
${logic.psql-only}    alter column nch_carr_clm_alowd_amt           SET NOT NULL,
${logic.psql-only}    alter column nch_carr_clm_sbmtd_chrg_amt      SET NOT NULL,
${logic.psql-only}    alter column nch_clm_bene_pmt_amt             SET NOT NULL,
${logic.psql-only}    alter column nch_clm_prvdr_pmt_amt            SET NOT NULL;

-- migrate data via INSERT from current DME_CLAIM_LINES table to DME_CLAIM_LINES_NEW table
--

${logic.hsql-only} insert into public.dme_claim_lines_new (
${logic.hsql-only}    clm_id,
${logic.hsql-only}    line_num,
${logic.hsql-only}    line_1st_expns_dt,
${logic.hsql-only}    line_last_expns_dt,
${logic.hsql-only}    line_alowd_chrg_amt,
${logic.hsql-only}    line_coinsrnc_amt,
${logic.hsql-only}    line_sbmtd_chrg_amt,
${logic.hsql-only}    line_bene_pmt_amt,
${logic.hsql-only}    line_prvdr_pmt_amt,
${logic.hsql-only}    line_bene_prmry_pyr_cd,
${logic.hsql-only}    line_bene_prmry_pyr_pd_amt,
${logic.hsql-only}    line_bene_ptb_ddctbl_amt,
${logic.hsql-only}    line_place_of_srvc_cd,
${logic.hsql-only}    line_pmt_80_100_cd,
${logic.hsql-only}    line_srvc_cnt,
${logic.hsql-only}    line_cms_type_srvc_cd,
${logic.hsql-only}    line_hct_hgb_type_cd,
${logic.hsql-only}    line_hct_hgb_rslt_num,
${logic.hsql-only}    line_ndc_cd,
${logic.hsql-only}    line_nch_pmt_amt,
${logic.hsql-only}    line_icd_dgns_cd,
${logic.hsql-only}    line_icd_dgns_vrsn_cd,
${logic.hsql-only}    line_dme_prchs_price_amt,
${logic.hsql-only}    line_prmry_alowd_chrg_amt,
${logic.hsql-only}    line_prcsg_ind_cd,
${logic.hsql-only}    line_service_deductible,
${logic.hsql-only}    betos_cd,
${logic.hsql-only}    hcpcs_cd,
${logic.hsql-only}    hcpcs_1st_mdfr_cd,
${logic.hsql-only}    hcpcs_2nd_mdfr_cd,
${logic.hsql-only}    hcpcs_3rd_mdfr_cd,
${logic.hsql-only}    hcpcs_4th_mdfr_cd,
${logic.hsql-only}    dmerc_line_mtus_cd,
${logic.hsql-only}    dmerc_line_mtus_cnt,
${logic.hsql-only}    dmerc_line_prcng_state_cd,
${logic.hsql-only}    dmerc_line_scrn_svgs_amt,
${logic.hsql-only}    dmerc_line_supplr_type_cd,
${logic.hsql-only}    prtcptng_ind_cd,
${logic.hsql-only}    prvdr_npi,
${logic.hsql-only}    prvdr_num,
${logic.hsql-only}    prvdr_spclty,
${logic.hsql-only}    prvdr_state_cd,
${logic.hsql-only}    tax_num )
--
-- PSQL allows us to dynamically create a table via the
-- associated SELECT statement used to populate the table.
--
${logic.psql-only} create table public.dme_claim_lines_new as
select
${logic.hsql-only}    convert(clm_id, SQL_BIGINT),
${logic.hsql-only}    convert(line_num, SQL_SMALLINT),
${logic.psql-only}    cast(clm_id as bigint),
${logic.psql-only}    cast(line_num as smallint),
                      line_1st_expns_dt,
                      line_last_expns_dt,
                      line_alowd_chrg_amt,
                      line_coinsrnc_amt,
                      line_sbmtd_chrg_amt,
                      line_bene_pmt_amt,
                      line_prvdr_pmt_amt,
                      line_bene_prmry_pyr_cd,
                      line_bene_prmry_pyr_pd_amt,
                      line_bene_ptb_ddctbl_amt,
                      line_place_of_srvc_cd,
                      line_pmt_80_100_cd,
                      line_srvc_cnt,
                      line_cms_type_srvc_cd,
                      line_hct_hgb_type_cd,
                      line_hct_hgb_rslt_num,
                      line_ndc_cd,
                      line_nch_pmt_amt,
                      line_icd_dgns_cd,
                      line_icd_dgns_vrsn_cd,
                      line_dme_prchs_price_amt,
                      line_prmry_alowd_chrg_amt,
                      line_prcsg_ind_cd,
                      line_service_deductible,
                      betos_cd,
                      hcpcs_cd,
                      hcpcs_1st_mdfr_cd,
                      hcpcs_2nd_mdfr_cd,
                      hcpcs_3rd_mdfr_cd,
                      hcpcs_4th_mdfr_cd,
                      dmerc_line_mtus_cd,
                      dmerc_line_mtus_cnt,
                      dmerc_line_prcng_state_cd,
                      dmerc_line_scrn_svgs_amt,
                      dmerc_line_supplr_type_cd,
                      prtcptng_ind_cd,
                      prvdr_npi,
                      prvdr_num,
                      prvdr_spclty,
                      prvdr_state_cd,
                      tax_num
from
    public.dme_claim_lines;
    
-- for PSQL need to define our not null constraints; for HSQL,
-- we explicitly defined a table structure before migrating data.
--
${logic.psql-only} alter table public.dme_claim_lines_new
${logic.psql-only}    alter column clm_id                           SET NOT NULL,
${logic.psql-only}    alter column line_num                         SET NOT NULL,
${logic.psql-only}    alter column line_alowd_chrg_amt              SET NOT NULL,
${logic.psql-only}    alter column line_coinsrnc_amt                SET NOT NULL,
${logic.psql-only}    alter column line_sbmtd_chrg_amt              SET NOT NULL,
${logic.psql-only}    alter column line_bene_pmt_amt                SET NOT NULL,
${logic.psql-only}    alter column line_prvdr_pmt_amt               SET NOT NULL,
${logic.psql-only}    alter column line_bene_prmry_pyr_pd_amt       SET NOT NULL,
${logic.psql-only}    alter column line_bene_ptb_ddctbl_amt         SET NOT NULL,
${logic.psql-only}    alter column line_place_of_srvc_cd            SET NOT NULL,
${logic.psql-only}    alter column line_srvc_cnt                    SET NOT NULL,
${logic.psql-only}    alter column line_cms_type_srvc_cd            SET NOT NULL,
${logic.psql-only}    alter column line_hct_hgb_rslt_num            SET NOT NULL,
${logic.psql-only}    alter column line_nch_pmt_amt                 SET NOT NULL,
${logic.psql-only}    alter column line_dme_prchs_price_amt         SET NOT NULL,
${logic.psql-only}    alter column line_prmry_alowd_chrg_amt        SET NOT NULL,
${logic.psql-only}    alter column dmerc_line_mtus_cnt              SET NOT NULL,
${logic.psql-only}    alter column prvdr_state_cd                   SET NOT NULL,
${logic.psql-only}    alter column tax_num                          SET NOT NULL;
                    
-- for PSQL need to define our primary key    
${logic.psql-only} alter table public.dme_claims_new
${logic.psql-only}     add CONSTRAINT dme_claims_new_pkey PRIMARY KEY (clm_id);

-- for PSQL need to define our primary key    
${logic.psql-only} ALTER TABLE public.dme_claim_lines_new
${logic.psql-only}     ADD CONSTRAINT dme_claim_lines_new_pkey PRIMARY KEY (clm_id, line_num);

-- define foreign key constraints between claim lineitems and a parent claims table.
ALTER TABLE IF EXISTS public.dme_claim_lines_new
    ADD CONSTRAINT dme_claim_lines_clm_id_to_dme_claims_new FOREIGN KEY (clm_id)
        REFERENCES public.dme_claims_new (clm_id);

-- create an index of the BENE_ID in parent claims table
CREATE INDEX IF NOT EXISTS dme_claims_new_bene_id_idx
    ON public.dme_claims_new (bene_id);