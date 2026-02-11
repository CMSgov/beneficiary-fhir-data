-- entitlement data is a bit weird because there can be multiple active records with no discernable difference
-- when this happens, we take the one with the more recent begin date and get the earliest begin date
CREATE OR REPLACE VIEW idr.beneficiary_entitlement_latest AS
SELECT DISTINCT ON (bene_sk, bene_mdcr_entlmt_type_cd) *,
    MIN(bene_rng_bgn_dt) OVER (
    PARTITION BY bene_sk, bene_mdcr_entlmt_type_cd
    ) AS original_bgn_dt
FROM idr.beneficiary_entitlement
WHERE idr_ltst_trans_flg = 'Y'
  AND bene_rng_bgn_dt <= (NOW() - INTERVAL '12 hours')
ORDER BY bene_sk, bene_mdcr_entlmt_type_cd, bene_rng_bgn_dt DESC;