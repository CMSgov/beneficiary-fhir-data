/*
 * Drop the now obsolete mbi and hash columns from claims tables.
 */

ALTER TABLE "pre_adj"."FissClaims" DROP "mbi";
ALTER TABLE "pre_adj"."FissClaims" DROP "mbiHash";

ALTER TABLE "pre_adj"."McsClaims" DROP "idrClaimMbi";
ALTER TABLE "pre_adj"."McsClaims" DROP "idrClaimMbiHash";
