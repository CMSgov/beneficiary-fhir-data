/* 
 BFD-1505
 There are periodic JDBC errors related to the Loaded Filter Manager bloom filter implementation.
 
 This may correct the issue by having postgres not do a sequential scan on loaded_batches with an index.
 */
 
${logic.psql-only} create index concurrently if not exists loaded_batches_loaded_file_id_idx on public.loaded_batches using btree (loaded_file_id DESC NULLS LAST);
