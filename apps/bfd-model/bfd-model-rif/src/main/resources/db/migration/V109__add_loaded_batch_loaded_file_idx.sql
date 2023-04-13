/* 
 BFD-1505
 There are periodic JDBC errors related to the Loaded Filter Manager bloom filter implementation.
 
 This may correct the issue by having postgres not do a sequential scan on loaded_batches with an index.
 */
 
CREATE INDEX CONCURRENTLY IF NOT EXISTS loaded_batches_loaded_file_id_idx
    ON public.loaded_batches USING btree
    (loaded_file_id DESC NULLS LAST);
