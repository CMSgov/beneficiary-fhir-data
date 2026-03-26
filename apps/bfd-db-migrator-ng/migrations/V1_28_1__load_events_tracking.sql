ALTER TABLE idr.idr_load_events
ADD COLUMN start_time TIMESTAMPTZ,
ADD COLUMN completion_time TIMESTAMPTZ,
ADD COLUMN failure_time TIMESTAMPTZ;

CREATE INDEX ON idr.idr_load_events (start_time);

CREATE INDEX ON idr.idr_load_events (completion_time);
