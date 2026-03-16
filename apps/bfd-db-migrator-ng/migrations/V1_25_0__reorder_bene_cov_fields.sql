-- All with an obsolete timestamp of 9999-12-31 have IDR_LTST_TRANS_FLG = 'Y', but not vice versa
-- Since we don't delete old records, having idr_trans_efctv_ts is useful regardless of if filtering exists.
CREATE OR REPLACE VIEW idr.beneficiary_status_latest AS
SELECT DISTINCT ON (bene_sk) *
FROM idr.beneficiary_status
WHERE idr_trans_obslt_ts >= '9999-12-31'
  AND idr_ltst_trans_flg = 'Y' 
  AND mdcr_stus_bgn_dt <= NOW() - INTERVAL '12 hours'
ORDER BY bene_sk, mdcr_stus_bgn_dt, idr_trans_efctv_ts DESC;

-- entitlement data is a bit weird because there can be multiple active records with no discernable difference
-- when this happens, we take the one with the more recent begin date and get the earliest begin date
CREATE OR REPLACE VIEW idr.beneficiary_entitlement_latest AS
SELECT DISTINCT ON (bene_sk, bene_mdcr_entlmt_type_cd) *,
    MIN(bene_rng_bgn_dt) OVER (
    PARTITION BY bene_sk, bene_mdcr_entlmt_type_cd
    ) AS original_bgn_dt
FROM idr.beneficiary_entitlement
WHERE idr_trans_obslt_ts >= '9999-12-31'
  AND idr_ltst_trans_flg = 'Y'
  AND bene_rng_bgn_dt <= (NOW() - INTERVAL '12 hours')
ORDER BY bene_sk, bene_mdcr_entlmt_type_cd, bene_rng_bgn_dt, idr_trans_efctv_ts DESC;

CREATE OR REPLACE VIEW idr.beneficiary_third_party_latest AS
SELECT DISTINCT ON (bene_sk, bene_tp_type_cd) *
FROM idr.beneficiary_third_party
WHERE idr_trans_obslt_ts >= '9999-12-31'
  AND idr_ltst_trans_flg = 'Y' 
  AND bene_rng_bgn_dt <= NOW() - INTERVAL '12 hours'
ORDER BY bene_sk, bene_tp_type_cd, bene_rng_bgn_dt, idr_trans_efctv_ts DESC;

CREATE OR REPLACE VIEW idr.beneficiary_entitlement_reason_latest AS
SELECT DISTINCT ON (bene_sk) *
FROM idr.beneficiary_entitlement_reason
WHERE idr_trans_obslt_ts >= '9999-12-31'
  AND idr_ltst_trans_flg = 'Y' 
  AND bene_rng_bgn_dt <= NOW() - INTERVAL '12 hours'
ORDER BY bene_sk, bene_rng_bgn_dt, idr_trans_efctv_ts DESC;

CREATE OR REPLACE VIEW idr.beneficiary_dual_eligibility_latest AS
SELECT DISTINCT ON (bene_sk) *
FROM idr.beneficiary_dual_eligibility
WHERE idr_trans_obslt_ts >= '9999-12-31'
  AND idr_ltst_trans_flg = 'Y' 
  AND bene_mdcd_elgblty_bgn_dt <= NOW() - INTERVAL '12 hours'
ORDER BY bene_sk, bene_mdcd_elgblty_bgn_dt, idr_trans_efctv_ts DESC;
