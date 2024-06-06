/*
 * The pre_adj schema has been replaced by the rda schema.  This migration drops the old schema and all
 * of its contents since they are now redundant.
 */

drop schema if exists "pre_adj" cascade;
