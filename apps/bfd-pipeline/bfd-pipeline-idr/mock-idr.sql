DROP TABLE IF EXISTS cms_vdm_view_mdcr_prd.v2_mdcr_bene;
DROP TABLE IF EXISTS cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry;
DROP SCHEMA IF EXISTS cms_vdm_view_mdcr_prd;

CREATE SCHEMA cms_vdm_view_mdcr_prd;
CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene (
    bene_sk BIGINT NOT NULL PRIMARY KEY, 
    bene_xref_efctv_sk BIGINT NOT NULL, 
    bene_mbi_id VARCHAR(11),
    bene_ssn_num VARCHAR(9),
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
    idr_updt_ts TIMESTAMPTZ
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry (
    bene_sk BIGINT NOT NULL,
    bene_xref_efctv_sk BIGINT NOT NULL,
    bene_mbi_id VARCHAR(11),
    bene_ssn_num VARCHAR(9),
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ,
    PRIMARY KEY(bene_sk, idr_trans_efctv_ts)
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_bene_elctn_prd_usg (
    bene_sk BIGINT NOT NULL,
    cntrct_pbp_sk BIGINT NOT NULL,
    bene_cntrct_num VARCHAR(5),
    bene_pbp_num VARCHAR(3),
    bene_elctn_enrlmt_disenrlmt_cd VARCHAR(1),
    bene_elctn_aplctn_dt DATE,
    bene_enrlmt_efctv_dt DATE,
    idr_trans_efctv_ts TIMESTAMPTZ,
    idr_trans_obslt_ts TIMESTAMPTZ,
    idr_updt_ts TIMESTAMPTZ
);

CREATE TABLE cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num (
    cntrct_pbp_sk BIGINT NOT NULL,
    cntrct_drug_plan_ind_cd VARCHAR(1),
    cntrct_pbp_type_cd VARCHAR(2),
    cntrct_pbp_sk_obslt_dt DATE
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene(
    bene_sk, 
    bene_xref_efctv_sk, 
    bene_mbi_id,
    bene_ssn_num,
    bene_1st_name,
    bene_midl_name,
    bene_last_name,
    bene_brth_dt,
    bene_sex_cd,
    bene_race_cd,
    geo_usps_state_cd,
    geo_zip5_cd,
    geo_zip4_cd,
    geo_zip_plc_name,
    bene_line_1_adr,
    bene_line_2_adr,
    bene_line_3_adr,
    bene_line_4_adr,
    bene_line_5_adr,
    bene_line_6_adr,
    cntct_lang_cd,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts)
VALUES(
    1, -- bene_sk, 
    1, -- bene_xref_efctv_sk, 
    '1S000000000',-- bene_mbi_id,
    '000000000',-- bene_ssn_num,
    'CHUCK',-- bene_1st_name,
    'R',-- bene_midl_name,
    'NORRIS',-- bene_last_name,
    '1960-01-01',-- bene_brth_dt,
    2,-- bene_sex_cd,
    1,-- bene_race_cd,
    'VA',-- geo_usps_state_cd,
    '22473',-- geo_zip5_cd,
    '0000',-- geo_zip4_cd,
    'RICHMOND',-- geo_zip_plc_name,
    '100 MAIN ST',-- bene_line_1_adr,
    '',-- bene_line_2_adr,
    '',-- bene_line_3_adr,
    '',-- bene_line_4_adr,
    '',-- bene_line_5_adr,
    '',-- bene_line_6_adr,
    'ENG',-- cntct_lang_cd,
    NOW(),-- idr_trans_efctv_ts,
    '2099-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
),
(
    2, -- bene_sk, 
    2, -- bene_xref_efctv_sk, 
    '1S000000001',-- bene_mbi_id,
    '000000001',-- bene_ssn_num,
    'BOB',-- bene_1st_name,
    'J',-- bene_midl_name,
    'SAGET',-- bene_last_name,
    '1960-01-01',-- bene_brth_dt,
    2,-- bene_sex_cd,
    1,-- bene_race_cd,
    'WA',-- geo_usps_state_cd,
    '98119',-- geo_zip5_cd,
    '0000',-- geo_zip4_cd,
    'SEATTLE',-- geo_zip_plc_name,
    '200 LANE ST',-- bene_line_1_adr,
    '',-- bene_line_2_adr,
    '',-- bene_line_3_adr,
    '',-- bene_line_4_adr,
    '',-- bene_line_5_adr,
    '',-- bene_line_6_adr,
    'ENG',-- cntct_lang_cd,
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry (
    bene_sk,
    bene_xref_efctv_sk,
    bene_mbi_id,
    bene_ssn_num,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1, -- bene_sk, 
    1, -- bene_xref_efctv_sk, 
    '1S000000000',-- bene_mbi_id,
    '000000000',-- bene_ssn_num,
    NOW(),-- idr_trans_efctv_ts,
    '9999-12-31',-- idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_bene_elctn_prd_usg (
    bene_sk,
    cntrct_pbp_sk,
    bene_cntrct_num,
    bene_pbp_num,
    bene_elctn_enrlmt_disenrlmt_cd,
    bene_elctn_aplctn_dt,
    bene_enrlmt_efctv_dt,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_updt_ts
)
VALUES(
    1, -- bene_sk,
    1, -- cntrct_pbp_sk,
    'S0001', -- bene_cntrct_num,
    '001', -- bene_pbp_num,
    'E', -- bene_elctn_enrlmt_disenrlmt_cd,
    NOW(),--bene_elctn_aplctn_dt DATE,
    NOW(),--bene_enrlmt_efctv_dt DATE,
    NOW(),-- idr_trans_efctv_ts
    '9999-12-31', --idr_trans_obslt_ts
    NULL-- idr_updt_ts
);

INSERT INTO cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num
(
    cntrct_pbp_sk,
    cntrct_drug_plan_ind_cd,
    cntrct_pbp_type_cd,
    cntrct_pbp_sk_obslt_dt
)
VALUES (
    1, -- cntrct_pbp_sk,
    '1', -- cntrct_drug_plan_ind_cd,
    '01', --cntrct_pbp_type_cd,
    '9999-12-31' --cntrct_pbp_sk_obslt_dt DATE
);
