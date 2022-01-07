/*
 * Table to hold known MBI and their hashed equivalents.
 */
CREATE TABLE "pre_adj"."MbiCache" (
    "mbi"     VARCHAR(13) NOT NULL PRIMARY KEY,
    "mbiHash" VARCHAR(64) NOT NULL
);

/*
 * Preload all known MBI into the new table.
 */
INSERT INTO "pre_adj"."MbiCache"("mbi", "mbiHash")
SELECT DISTINCT "mbi", "mbiHash"
FROM (
    SELECT DISTINCT "mbi", "mbiHash"
    FROM "pre_adj"."FissClaims"
    UNION
    SELECT DISTINCT "idrClaimMbi" AS "mbi", "idrClaimMbiHash" AS "mbiHash"
    FROM "pre_adj"."McsClaims"
) x
WHERE "mbi" IS NOT NULL AND "mbiHash" IS NOT NULL;

/*
 * Replace the mbiHash column and add a constraint to enforce that mbi is known to exist in MbiCache.
 */
ALTER TABLE "pre_adj"."FissClaims" DROP "mbiHash";
ALTER TABLE "pre_adj"."FissClaims"
    ADD CONSTRAINT "FK_FissClaims_mbi" FOREIGN KEY ("mbi") REFERENCES "pre_adj"."MbiCache"("mbi");

/*
 * Replace the idrClaimMbiHash column and add a constraint to enforce that mbi is known to exist in MbiCache.
 */
ALTER TABLE "pre_adj"."McsClaims" DROP "idrClaimMbiHash";
ALTER TABLE "pre_adj"."McsClaims"
    ADD CONSTRAINT "FK_McsClaims_mbi" FOREIGN KEY ("idrClaimMbi") REFERENCES "pre_adj"."MbiCache"("mbi");

/*
 * We need these indexes to support queries by mbi and mbiHash.
 */
CREATE UNIQUE INDEX "MbiCache_mbi_hash_idx" on "pre_adj"."MbiCache"("mbiHash");
CREATE INDEX "FissClaims_mbi_idx" on "pre_adj"."FissClaims"("mbi");
CREATE INDEX "McsClaims_mbi_idx" on "pre_adj"."McsClaims"("idrClaimMbi");
