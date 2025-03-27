ALTER TABLE rda.mcs_tags
DROP CONSTRAINT mcs_tags_clm_id_fkey,
ADD CONSTRAINT mcs_tags_clm_id_fkey
FOREIGN KEY (clm_id)
REFERENCES rda.mcs_claims(idr_clm_hd_icn)
ON DELETE CASCADE
NOT VALID;

ALTER TABLE rda.fiss_tags
DROP CONSTRAINT fiss_tags_clm_id_fkey,
ADD CONSTRAINT fiss_tags_clm_id_fkey
FOREIGN KEY (clm_id)
REFERENCES rda.fiss_claims(claim_id)
ON DELETE CASCADE
NOT VALID;
