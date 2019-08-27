/*
 * Resolves http://issues.hhsdevcloud.us/browse/CBBF-147, correcting the 
 * precision and scale of the `InpatientClaim.indirectMedicalEducationAmount` 
 * and `InpatientClaim.disproportionateShareAmount` columns.
 */

alter table "InpatientClaims" 
   alter column "indirectMedicalEducationAmount" ${logic.alter-column-type} numeric(12,2);
alter table "InpatientClaims" 
   alter column "disproportionateShareAmount" ${logic.alter-column-type} numeric(12,2);
