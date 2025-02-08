DROP TABLE IF EXISTS cms_vdm_view_mdcr_prd.v2_mdcr_bene;
DROP SCHEMA IF EXISTS cms_vdm_view_mdcr_prd;

CREATE SCHEMA cms_vdm_view_mdcr_prd
    CREATE TABLE v2_mdcr_bene (
        bene_sk BIGINT,
        bene_xref_efctv_sk BIGINT, 
        bene_mbi_id VARCHAR(11),
        bene_1st_name VARCHAR(30),
        bene_last_name VARCHAR(40),
        idr_trans_efctv_ts TIMESTAMPTZ,
        idr_trans_obslt_ts TIMESTAMPTZ
    );

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene(
    bene_sk, 
    bene_xref_efctv_sk, 
    bene_mbi_id, 
    bene_1st_name, 
    bene_last_name, 
    idr_trans_efctv_ts,
    idr_trans_obslt_ts)
VALUES(1, 1, '1S000000000', 'Chuck', 'Norris', NOW(), '9999-12-31');

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene(
    bene_sk, 
    bene_xref_efctv_sk, 
    bene_mbi_id, 
    bene_1st_name, 
    bene_last_name, 
    idr_trans_efctv_ts,
    idr_trans_obslt_ts)
VALUES(2, 2, '1S000000001', 'Bob', 'Saget', NOW(), '9999-12-31');

