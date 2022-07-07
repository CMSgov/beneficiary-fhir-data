-- Due to hibernate logging corresponding data in constraint violations, we're removing the unique
-- constraint on the mbi.  The hash is also unique, so it *should* provide duplicate row protection
-- without leaking sensitive information to the logs

drop index rda.mbi_cache_mbi_idx;

-- Create new index without unique constraint
create index mbi_cache_mbi_idx on rda.mbi_cache(mbi);