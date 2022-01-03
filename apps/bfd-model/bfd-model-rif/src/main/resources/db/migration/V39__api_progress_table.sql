/*
 * Creates a table to track the highest sequence number imported into the database.
 * A query of this table can be used to track the sequence number to pass as a
 * {@code since} parameter to the RDA API.
 */

create table "pre_adj"."RdaApiProgress" (
    "claimType"          VARCHAR(20) NOT NULL PRIMARY KEY,
    "lastSequenceNumber" BIGINT      NOT NULL,
    "lastUpdated"        timestamp with time zone
);

/* These are no longer required since we are tracking the replay sequence number in RdaApiProgress. */
drop index "pre_adj"."FissClaims_sequenceNumber_idx";
drop index "pre_adj"."McsClaims_sequenceNumber_idx";
