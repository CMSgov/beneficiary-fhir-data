/*
 * Resolves http://issues.hhsdevcloud.us/browse/CBBD-288, correcting the 
 * precision and scale of the `CarrierClaimLines.hctHgbTestResult` column.
 */

alter table "CarrierClaimLines" 
   alter column "hctHgbTestResult" ${logic.alter-column-type} numeric(4,1);
