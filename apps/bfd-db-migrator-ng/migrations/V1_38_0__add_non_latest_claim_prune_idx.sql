CREATE INDEX ON idr.claim_professional_ss(clm_idr_ld_dt, clm_uniq_id) WHERE clm_ltst_clm_ind = 'N';

CREATE INDEX ON idr.claim_institutional_ss(clm_idr_ld_dt, clm_uniq_id) WHERE clm_ltst_clm_ind = 'N';
