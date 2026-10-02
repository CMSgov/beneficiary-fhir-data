DROP INDEX idr.claim_professional_nch_clm_ltst_clm_ind_idx;
DROP INDEX idr.claim_professional_ss_clm_ltst_clm_ind_idx;
DROP INDEX idr.claim_institutional_nch_clm_ltst_clm_ind_idx;
DROP INDEX idr.claim_institutional_ss_clm_ltst_clm_ind_idx;

CREATE INDEX ON idr.claim_professional_nch(clm_uniq_id, clm_ltst_clm_ind) WHERE clm_ltst_clm_ind = 'N';
CREATE INDEX ON idr.claim_professional_ss(clm_uniq_id, clm_ltst_clm_ind) WHERE clm_ltst_clm_ind = 'N';
CREATE INDEX ON idr.claim_institutional_nch(clm_uniq_id, clm_ltst_clm_ind) WHERE clm_ltst_clm_ind = 'N';
CREATE INDEX ON idr.claim_institutional_ss(clm_uniq_id, clm_ltst_clm_ind) WHERE clm_ltst_clm_ind = 'N';
