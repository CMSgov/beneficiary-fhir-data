ALTER TABLE ccw.beneficiaries
ADD COLUMN IF NOT EXISTS xref_grp_id BIGINT;

ALTER TABLE ccw.beneficiaries
ADD COLUMN IF NOT EXISTS xref_sw CHAR;

ALTER TABLE ccw.beneficiaries_history
ADD COLUMN IF NOT EXISTS xref_grp_id BIGINT;

ALTER TABLE ccw.beneficiaries_history
ADD COLUMN IF NOT EXISTS xref_sw CHAR;

CREATE INDEX IF NOT EXISTS beneficiaries_xref_grp_id_idx on ccw.beneficiaries(xref_grp_id);
