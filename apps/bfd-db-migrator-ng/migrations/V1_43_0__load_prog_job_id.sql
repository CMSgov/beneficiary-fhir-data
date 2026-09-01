ALTER TABLE idr.load_progress 
ADD COLUMN job_id integer NOT NULL DEFAULT 1, 
ADD COLUMN max_run_ts TIMESTAMPTZ;

ALTER TABLE idr.load_progress DROP CONSTRAINT load_progress_table_name_batch_partition_key;
ALTER TABLE idr.load_progress
ADD UNIQUE(table_name, batch_partition,job_id);
