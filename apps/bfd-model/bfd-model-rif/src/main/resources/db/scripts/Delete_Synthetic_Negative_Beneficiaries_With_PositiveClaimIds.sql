DELETE 
FROM "public"."CarrierClaimLines"
WHERE "parentClaim" IN (SELECT "claimId"
FROM "public"."CarrierClaims"
WHERE "claimId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%');


DELETE 
FROM "public"."CarrierClaims"
WHERE "claimId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%'

DELETE 
FROM "public"."InpatientClaimLines"
WHERE "parentClaim" IN (SELECT "claimId"
FROM "public"."InpatientClaims"
WHERE "claimId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%');

DELETE 
FROM "public"."InpatientClaims"
WHERE "claimId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%'

DELETE 
FROM "public"."OutpatientClaimLines"
WHERE "parentClaim" IN (SELECT "claimId"
FROM "public"."OutpatientClaims"
WHERE "claimId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%');

DELETE 
FROM "public"."OutpatientClaims"
WHERE "claimId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%'

DELETE 
FROM "public"."PartDEvents"
WHERE "eventId" NOT LIKE '-%' AND "beneficiaryId" LIKE '-%'