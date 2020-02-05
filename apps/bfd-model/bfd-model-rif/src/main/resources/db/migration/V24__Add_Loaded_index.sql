/*
 * Add an index on the created timestamp of LoadedBatches table which is a frequently searched column.
 * The index creation is done in a separate migration to avoid mixed
 * transactional and non-transactional statements in migrations
 */

create index ${logic.index-create-concurrently} "LoadedBatches_created_index"
    on "LoadedBatches" ("created");