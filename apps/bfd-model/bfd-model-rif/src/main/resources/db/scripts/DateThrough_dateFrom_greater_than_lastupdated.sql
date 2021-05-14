
SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."CarrierClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";


SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."DMEClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";


SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."HHAClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";


SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."HospiceClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";


SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."InpatientClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";


SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."OutpatientClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";


SELECT "claimId",  "beneficiaryId", "dateFrom", "dateThrough"
FROM public."SNFClaims"
WHERE lastupdated>="dateFrom" OR lastupdated>="dateThrough";