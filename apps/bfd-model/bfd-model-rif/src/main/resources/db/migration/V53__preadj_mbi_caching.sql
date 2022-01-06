
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
INSERT INTO "pre_adj"."MbiCache"("mbi","mbiHash")
SELECT DISTINCT "mbi", "mbiHash"
FROM (
         SELECT DISTINCT "mbi", "mbiHash"
         FROM "pre_adj"."FissClaims"
         UNION
         SELECT DISTINCT "idrClaimMbi" AS "mbi", "idrClaimMbiHash" AS "mbiHash"
         FROM "pre_adj"."McsClaims"
     ) x
WHERE "mbi" IS NOT NULL AND "mbiHash" IS NOT NULL;
