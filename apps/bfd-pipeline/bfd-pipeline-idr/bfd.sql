DROP TABLE IF EXISTS idr.beneficiary;
DROP TABLE IF EXISTS idr.beneficiary_history;
DROP TABLE IF EXISTS idr.load_progress;
DROP SCHEMA IF EXISTS idr;

CREATE SCHEMA idr;
CREATE TABLE idr.beneficiary(
    bene_sk BIGINT NOT NULL PRIMARY KEY, 
    bene_xref_efctv_sk BIGINT NOT NULL, 
    bene_xref_efctv_sk_computed BIGINT NOT NULL GENERATED ALWAYS 
        AS (CASE WHEN bene_xref_efctv_sk = 0 THEN bene_sk ELSE bene_xref_efctv_sk END) STORED,
    bene_mbi_id VARCHAR(11) NOT NULL,
    bene_1st_name VARCHAR(30) NOT NULL,
    bene_midl_name VARCHAR(15) NOT NULL,
    bene_last_name VARCHAR(40) NOT NULL,
    bene_brth_dt DATE NOT NULL,
    bene_death_dt DATE NOT NULL,
    bene_vrfy_death_day_sw VARCHAR(1) NOT NULL,
    bene_sex_cd VARCHAR(1) NOT NULL ,
    bene_race_cd VARCHAR(2) NOT NULL,
    geo_usps_state_cd VARCHAR(2) NOT NULL,
    geo_zip5_cd VARCHAR(5) NOT NULL,
    geo_zip_plc_name VARCHAR(100) NOT NULL,
    bene_line_1_adr VARCHAR(45) NOT NULL,
    bene_line_2_adr VARCHAR(45) NOT NULL,
    bene_line_3_adr VARCHAR(40) NOT NULL,
    bene_line_4_adr VARCHAR(40) NOT NULL,
    bene_line_5_adr VARCHAR(40) NOT NULL,
    bene_line_6_adr VARCHAR(40) NOT NULL,
    cntct_lang_cd VARCHAR(3) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.beneficiary_history(
    bene_sk BIGINT NOT NULL,
    bene_xref_efctv_sk BIGINT NOT NULL,
    bene_xref_efctv_sk_computed BIGINT NOT NULL GENERATED ALWAYS
        AS (CASE WHEN bene_xref_efctv_sk = 0 THEN bene_sk ELSE bene_xref_efctv_sk END) STORED,
    bene_mbi_id VARCHAR(11) NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_mbi_id (
    bene_mbi_id VARCHAR(11) NOT NULL,
    bene_mbi_efctv_dt DATE NOT NULL,
    bene_mbi_obslt_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_mbi_id, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_third_party (
    bene_sk BIGINT NOT NULL,
    bene_buyin_cd VARCHAR(2) NOT NULL,
    bene_tp_type_cd VARCHAR(1) NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_tp_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_status (
    bene_sk BIGINT NOT NULL,
    bene_mdcr_stus_cd VARCHAR(2) NOT NULL,
    mdcr_stus_bgn_dt DATE NOT NULL,
    mdcr_stus_end_dt DATE NOT NULL,
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, mdcr_stus_bgn_dt, mdcr_stus_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_entitlement (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_type_cd VARCHAR(1),
    bene_mdcr_entlmt_stus_cd VARCHAR(1),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_mdcr_entlmt_type_cd, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_entitlement_reason (
    bene_sk BIGINT NOT NULL,
    bene_rng_bgn_dt DATE NOT NULL,
    bene_rng_end_dt DATE NOT NULL,
    bene_mdcr_entlmt_rsn_cd VARCHAR(1),
    idr_trans_efctv_ts TIMESTAMPTZ NOT NULL,
    idr_trans_obslt_ts TIMESTAMPTZ NOT NULL,
    idr_updt_ts TIMESTAMPTZ,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, idr_trans_efctv_ts)
);

CREATE TABLE idr.beneficiary_election_period_usage (
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5),
    bene_pbp_num VARCHAR(3),
    bene_elctn_enrlmt_disenrlmt_cd VARCHAR(1),
    bene_elctn_aplctn_dt DATE,
    bene_enrlmt_efctv_dt DATE,
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(bene_sk, cntrct_pbp_sk, bene_enrlmt_efctv_dt)
);

CREATE TABLE idr.contract_pbp_number (
    cntrct_pbp_sk BIGINT NOT NULL PRIMARY KEY,
    cntrct_drug_plan_ind_cd VARCHAR(1),
    cntrct_pbp_type_cd VARCHAR(2),
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ NOT NULL
);

CREATE TABLE idr.load_progress(
    id INT GENERATED ALWAYS AS IDENTITY,
    table_name TEXT NOT NULL UNIQUE,
    last_ts TIMESTAMPTZ NOT NULL,
    batch_completion_ts TIMESTAMPTZ NOT NULL
);

CREATE MATERIALIZED VIEW idr.overshare_mbis AS 
SELECT bene_mbi_id FROM idr.beneficiary
GROUP BY bene_mbi_id
HAVING COUNT(DISTINCT bene_xref_efctv_sk) > 1;

-- required to refresh view with CONCURRENTLY
CREATE UNIQUE INDEX ON idr.overshare_mbis (bene_mbi_id);
