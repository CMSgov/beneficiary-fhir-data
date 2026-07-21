CREATE INDEX ON idr.claim_professional_ss(bfd_updated_ts) WHERE clm_type_cd BETWEEN 1000 and 1999;
CREATE INDEX ON idr.claim_item_professional_ss(clm_uniq_id, bfd_updated_ts);

CREATE INDEX ON idr.claim_institutional_ss(bfd_updated_ts) WHERE clm_type_cd BETWEEN 1000 and 1999;
CREATE INDEX ON idr.claim_item_institutional_ss(clm_uniq_id, bfd_updated_ts);