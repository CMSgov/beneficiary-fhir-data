DROP TABLE IF EXISTS idr.beneficiary;
DROP TABLE IF EXISTS idr.load_progress;
DROP SCHEMA IF EXISTS idr;

CREATE SCHEMA idr;
CREATE TABLE idr.beneficiary(
    bene_sk BIGINT NOT NULL PRIMARY KEY, 
    bene_xref_efctv_sk BIGINT NOT NULL, 
    bene_mbi_id VARCHAR(11),
    bene_1st_name VARCHAR(30),
    bene_midl_name VARCHAR(15),
    bene_last_name VARCHAR(40),
    bene_brth_dt DATE,
    bene_sex_cd VARCHAR(1),
    bene_race_cd VARCHAR(2),
    geo_usps_state_cd VARCHAR(2),
    geo_zip5_cd VARCHAR(5),
    geo_zip4_cd VARCHAR(4),
    geo_zip_plc_name VARCHAR(100),
    bene_line_1_adr VARCHAR(45),
    bene_line_2_adr VARCHAR(45),
    bene_line_3_adr VARCHAR(40),
    bene_line_4_adr VARCHAR(40),
    bene_line_5_adr VARCHAR(40),
    bene_line_6_adr VARCHAR(40),
    cntct_lang_cd VARCHAR(3),
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL);
    
CREATE TABLE idr.load_progress(
    id INT GENERATED ALWAYS AS IDENTITY,
    table_name TEXT NOT NULL UNIQUE,
    last_id TEXT NOT NULL,
    last_timestamp TIMESTAMPTZ NOT NULL
);
