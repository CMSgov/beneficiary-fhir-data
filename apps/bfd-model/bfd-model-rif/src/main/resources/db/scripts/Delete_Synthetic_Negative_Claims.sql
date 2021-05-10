DELETE 
FROM "public"."CarrierClaimLines"
WHERE "parentClaim" LIKE '-%';

DELETE 
FROM "public"."CarrierClaims"
WHERE "claimId" LIKE '-%';

DELETE 
FROM "public"."InpatientClaimLines"
WHERE "parentClaim" LIKE '-%';

DELETE 
FROM "public"."InpatientClaims"
WHERE "claimId" LIKE '-%';

DELETE 
FROM "public"."OutpatientClaimLines"
WHERE "parentClaim" LIKE '-%';

DELETE 
FROM "public"."OutpatientClaims"
WHERE "claimId" LIKE '-%';

DELETE 
FROM "public"."PartDEvents"
WHERE "eventId" LIKE '-%';