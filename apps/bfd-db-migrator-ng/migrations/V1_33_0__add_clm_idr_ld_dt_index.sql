CREATE INDEX ON idr.claim_professional_ss(clm_type_cd, clm_idr_ld_dt) WHERE clm_type_cd BETWEEN 1000 and 1999

CREATE INDEX ON idr.claim_institutional_ss(clm_type_cd, clm_idr_ld_dt) WHERE clm_type_cd BETWEEN 1000 and 1999