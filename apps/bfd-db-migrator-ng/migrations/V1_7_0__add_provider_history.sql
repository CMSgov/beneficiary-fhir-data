CREATE TABLE idr.provider_history (
    prvdr_sk BIGINT NOT NULL,
    prvdr_hstry_efctv_dt DATE NOT NULL,
    prvdr_mdl_name VARCHAR(25) NOT NULL,
    prvdr_type_cd VARCHAR(2) NOT NULL,
    prvdr_txnmy_cmpst_cd VARCHAR(150) NOT NULL,
    prvdr_oscar_num VARCHAR(13) NOT NULL,
    prvdr_1st_name VARCHAR(35) NOT NULL,
    prvdr_name VARCHAR(70) NOT NULL,
    prvdr_hstry_obslt_dt DATE NOT NULL,
    prvdr_npi_num VARCHAR(10) NOT NULL,
    prvdr_lgl_name VARCHAR(100) NOT NULL,
    prvdr_emplr_id_num VARCHAR(10) NOT NULL,
    prvdr_last_name VARCHAR(35) NOT NULL,
    bfd_created_ts TIMESTAMPTZ,
    bfd_updated_ts TIMESTAMPTZ,
    PRIMARY KEY(prvdr_sk, prvdr_hstry_efctv_dt)
);

CREATE VIEW idr.provider_history_latest AS
SELECT DISTINCT ON (prvdr_sk) *
FROM idr.provider_history
WHERE prvdr_hstry_efctv_dt > '1000-01-01'
ORDER BY prvdr_sk, prvdr_hstry_efctv_dt DESC;