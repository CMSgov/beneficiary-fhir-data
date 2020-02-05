/*
 * Add an index on the created timestamp of LoadedBatches table which is frequently searched for column.
 * The index creation is done to avoid mixin transactional and non-transactional statements in migrations
 */

create index ${logic.index-create-concurrently} "LoadedBatches_created_index"
    on "LoadedBatches" ("created");