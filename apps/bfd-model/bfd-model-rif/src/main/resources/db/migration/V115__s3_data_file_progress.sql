/*
 * Adds a column to track progress of the file at the record level.
 */
ALTER TABLE s3_data_files ADD last_record_number bigint;
