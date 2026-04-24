CREATE TABLE
    idr.idr_load_events (
        id uuid NOT NULL PRIMARY KEY,
        job_name VARCHAR(25) NOT NULL,
        job_message VARCHAR(25) NOT NULL,
        event_time TIMESTAMPTZ NOT NULL
    );

CREATE INDEX ON idr.idr_load_events (event_time);
