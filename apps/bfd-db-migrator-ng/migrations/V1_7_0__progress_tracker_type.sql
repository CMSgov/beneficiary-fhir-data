ALTER TABLE idr.load_progress
ADD COLUMN batch_partition TEXT NOT NULL DEFAULT('');
