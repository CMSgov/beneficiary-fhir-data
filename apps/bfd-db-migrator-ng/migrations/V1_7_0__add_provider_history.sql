CREATE TABLE idr.provider_history (
    prvdr_sk BIGINT NOT NULL,
    prvdr_hstry_efctv_dt DATE NOT NULL,
    prvdr_mdl_name VARCHAR(25),
    prvdr_txnmy_cmpst_cd VARCHAR(150),
    prvdr_oscar_num VARCHAR(13) NOT NULL,
    prvdr_1st_name VARCHAR(35),
    prvdr_name VARCHAR(70),
    prvdr_hstry_obslt_dt DATE NOT NULL,
    prvdr_npi_num VARCHAR(10),
    prvdr_lgl_name VARCHAR(100),
    prvdr_emplr_id_num VARCHAR(10),
    prvdr_last_name VARCHAR(35),
    bfd_created_ts TIMESTAMPTZ,
    bfd_updated_ts TIMESTAMPTZ,
    PRIMARY KEY(prvdr_sk, prvdr_hstry_efctv_dt)
);