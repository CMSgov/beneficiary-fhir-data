/*
 * Migrate all existing MBI values into the MbiCache table.
 */
INSERT INTO "pre_adj"."MbiCache"("mbi", "hash", "lastUpdated")
SELECT "mbi", "mbiHash" as "hash", CURRENT_TIMESTAMP as "lastUpdated"
FROM (
    SELECT DISTINCT "mbi", "mbiHash"
    FROM "pre_adj"."FissClaims"
    UNION
    SELECT DISTINCT "idrClaimMbi" AS "mbi", "idrClaimMbiHash" AS "mbiHash"
    FROM "pre_adj"."McsClaims"
) x
WHERE "mbi" IS NOT NULL AND "mbiHash" IS NOT NULL;

/*
 * Update the mbiId foreign key column.
 */
UPDATE "pre_adj"."FissClaims"
SET "mbiId"=(SELECT "mbiId" FROM "pre_adj"."MbiCache" WHERE "hash" = "mbiHash");

UPDATE "pre_adj"."McsClaims"
SET "mbiId"=(SELECT "mbiId" FROM "pre_adj"."MbiCache" WHERE "hash" = "idrClaimMbiHash");
