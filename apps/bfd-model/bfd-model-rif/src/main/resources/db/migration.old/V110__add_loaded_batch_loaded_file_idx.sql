/* 
 BFD-1505
 There are periodic JDBC errors related to the Loaded Filter Manager bloom filter implementation.
 
 This may correct the issue by having postgres not do a sequential scan on loaded_batches with an index.
 */
 
create index ${logic.index-create-concurrently}
if not exists loaded_batches_loaded_file_id_idx on loaded_batches (loaded_file_id DESC ${logic.psql-only} nulls last
);
