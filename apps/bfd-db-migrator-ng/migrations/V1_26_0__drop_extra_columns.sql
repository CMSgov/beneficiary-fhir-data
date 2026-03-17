-- Need to use CASCADE here to delete and recreate dependent views
ALTER TABLE idr.beneficiary
DROP COLUMN bene_xref_efctv_sk_computed CASCADE;

CREATE VIEW idr.valid_beneficiary AS
SELECT *
FROM idr.beneficiary b
WHERE NOT EXISTS(SELECT 1 FROM idr.beneficiary_overshare_mbi om WHERE om.bene_mbi_id = b.bene_mbi_id);

CREATE VIEW idr.beneficiary_identity AS
SELECT DISTINCT
    bene.bene_sk,
    bene.bene_xref_efctv_sk,
    bene.bene_mbi_id,
    bene_mbi.bene_mbi_efctv_dt,
    bene_mbi.bene_mbi_obslt_dt
FROM idr.valid_beneficiary bene
LEFT JOIN idr.beneficiary_mbi_id bene_mbi
    ON bene.bene_mbi_id = bene_mbi.bene_mbi_id
    AND bene_mbi.idr_ltst_trans_flg = 'Y';

ALTER TABLE claim_item_institutional_ss
DROP COLUMN clm_rlt_cond_cd;

ALTER TABLE claim_item_institutional_nch
DROP COLUMN clm_rlt_cond_cd;
