ALTER TABLE idr.provider_history
ADD COLUMN idr_insrt_ts TIMESTAMPTZ;

ALTER TABLE idr.provider_history
ADD COLUMN idr_updt_ts TIMESTAMPTZ;