/*
 * Every claim change from the RDA API contains a 64-bit sequence number that can be used
 * to request a replay of past updates.  Each claim in the database will have the most recent
 * sequence number that affected that claim stored in a column.
 */

/*
 * Add the sequence number column used to track which update from RDA API was
 * used to create or update the claim.
 */
alter table "pre_adj"."FissClaims"
    add "sequenceNumber" bigint default 0 not null;
alter table "pre_adj"."McsClaims"
    add "sequenceNumber" bigint default 0 not null;

/* Remove the default values on the columns.   They were only needed to add the not null column. */
ALTER TABLE "pre_adj"."FissClaims"
    ALTER COLUMN "sequenceNumber" DROP DEFAULT;
ALTER TABLE "pre_adj"."McsClaims"
    ALTER COLUMN "sequenceNumber" DROP DEFAULT;

/* Index to allow quick determination of where to resume stream in calls to RDA API. */
create index "FissClaims_sequenceNumber_idx" on "pre_adj"."FissClaims" ("sequenceNumber");
create index "McsClaims_sequenceNumber_idx" on "pre_adj"."McsClaims" ("sequenceNumber");

/* Index to allow fast query by update time by BFD API. */
create index "FissClaims_lastUpdated_idx" on "pre_adj"."FissClaims" ("lastUpdated");
create index "McsClaims_lastUpdated_idx" on "pre_adj"."McsClaims" ("lastUpdated");
