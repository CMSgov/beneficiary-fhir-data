ALTER TABLE idr.load_progress
ADD COLUMN batch_partition TEXT NOT NULL DEFAULT('');

ALTER TABLE idr.load_progress
DROP CONSTRAINT load_progress_table_name_key;

ALTER TABLE idr.load_progress
ADD UNIQUE(table_name, batch_partition);
