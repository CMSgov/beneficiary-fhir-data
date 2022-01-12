/*
 * NOTE: This migration will fail if there are any claims in the FissClaims or McsClaims tables.
 */

/*
 * Table to hold known MBI and their hashed equivalents.
 */
CREATE TABLE "pre_adj"."MbiCache" (
    "mbiId"   bigint      NOT NULL PRIMARY KEY,
    "mbi"     VARCHAR(13) NOT NULL,
    "hash"    VARCHAR(64) NOT NULL,
    "oldHash" VARCHAR(64)
);

/*
 * We need these indexes to support queries by mbi, hash, and oldHash.  Note that oldHash
 * is usually null (only set to a value during hash rotation) so the index cannot be unique.
 */
CREATE UNIQUE INDEX "MbiCache_mbi_idx" on "pre_adj"."MbiCache"("mbi");
CREATE UNIQUE INDEX "MbiCache_hash_idx" on "pre_adj"."MbiCache"("hash");
CREATE INDEX "MbiCache_alt_hash_idx" on "pre_adj"."MbiCache"("oldHash");

/*
 * FIXME For consistency, sequence names should be mixed-case, but can't be, due
 * to https://hibernate.atlassian.net/browse/HHH-9431.
 */
create sequence mbi_cache_mbi_id_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 50;

/*
 * Replace the mbiHash column and add a constraint to enforce that mbi is known to exist in MbiCache.
 */
ALTER TABLE "pre_adj"."FissClaims" DROP "mbi";
ALTER TABLE "pre_adj"."FissClaims" DROP "mbiHash";
ALTER TABLE "pre_adj"."FissClaims"
    ADD "mbiId" bigint;
ALTER TABLE "pre_adj"."FissClaims"
    ADD CONSTRAINT "FK_FissClaims_mbiId" FOREIGN KEY ("mbiId") REFERENCES "pre_adj"."MbiCache"("mbiId");

/*
 * Replace the idrClaimMbiHash column and add a constraint to enforce that mbi is known to exist in MbiCache.
 */
ALTER TABLE "pre_adj"."McsClaims" DROP "idrClaimMbi";
ALTER TABLE "pre_adj"."McsClaims" DROP "idrClaimMbiHash";
ALTER TABLE "pre_adj"."McsClaims"
    ADD "mbiId" bigint;
ALTER TABLE "pre_adj"."McsClaims"
    ADD CONSTRAINT "FK_McsClaims_mbiId" FOREIGN KEY ("mbiId") REFERENCES "pre_adj"."MbiCache"("mbiId");

/*
 * We need these indexes to efficiently find claims using join from MbiCache.
 */
CREATE INDEX "FissClaims_mbiId_idx" on "pre_adj"."FissClaims"("mbiId");
CREATE INDEX "McsClaims_mbiId_idx" on "pre_adj"."McsClaims"("mbiId");
