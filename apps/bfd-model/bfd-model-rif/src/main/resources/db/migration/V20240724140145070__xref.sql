ALTER TABLE ccw.beneficiaries
ADD COLUMN xref_grp_id BIGINT;

ALTER TABLE ccw.beneficiaries
ADD COLUMN xref_sw CHAR;

ALTER TABLE ccw.beneficiaries_history
ADD COLUMN xref_grp_id BIGINT;

ALTER TABLE ccw.beneficiaries_history
ADD COLUMN xref_sw CHAR;

CREATE INDEX on ccw.beneficiaries(xref_grp_id);
