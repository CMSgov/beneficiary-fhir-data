--
-- NOTES:
--   1. when you rename a table, indexes/constraints will trickle down to contraint & index directives,
--      BUT do not modify constraint or index names themselves
--   2. don't try to rename a column that already has the name (i.e., "hicn" ${logic.rename-to} hicn)
--   3. optionally rename contraint and/or index names (i.e., remove camelCase)
--
-- LoadedFiles to loaded_files
--
-- We have a bit of a funky condition between psql and hsql; for both loaded_files and loaded_batches
-- there is a column called "created". For psql there is no need to do a rename; in fact if we tried
-- to do something like:
--
--      psql: alter table public.loaded_files rename column "created" to created
--
-- we'd get an error. So in theory, maybe we don't even need to do a rename for that type of condition.
-- However, in hsql, if we don't do a rename, we end up with a column called "created" (literally,
-- meaning the double-quotes are an integral part of the column name). So for hsql we do need to
-- perform the rename so we can rid the column name of the double-quotes.
--
--      ${logic.hsql-only-alter}
--          psql: "--"
--          hsql: "alter"
--
alter table public."LoadedFiles" rename to loaded_files;
alter table public.loaded_files ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_file_id;
alter table public.loaded_files ${logic.alter-rename-column} "rifType" ${logic.rename-to} rif_type;
${logic.hsql-only-alter} table public.loaded_files ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- LoadedBatches to loaded_batches
--
alter table public."LoadedBatches" rename to loaded_batches;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedBatchId" ${logic.rename-to} loaded_batch_id;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_file_id;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "beneficiaries" ${logic.rename-to} beneficiaries;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "created" ${logic.rename-to} created;

-- psql only
${logic.psql-only-alter} index if exists public."LoadedBatches_pkey" rename to loaded_batches_pkey;
${logic.psql-only-alter} index if exists public."LoadedFiles_pkey" rename to loaded_files_pkey;

${logic.psql-only-alter} table public.loaded_batches rename constraint "loadedBatches_loadedFileId" to loaded_batches_loaded_file_id;

-- hsql only - primary key names already exist
-- loaded_batches_pkey already exists
-- loaded_files_pkey already exists

${logic.hsql-only-alter} table public.loaded_batches ADD CONSTRAINT loaded_batches_loaded_file_id FOREIGN KEY (loaded_file_id) REFERENCES public.loaded_files (loaded_file_id);

-- both psql and hsql
ALTER INDEX "LoadedBatches_created_index" RENAME TO loaded_batches_created_idx;