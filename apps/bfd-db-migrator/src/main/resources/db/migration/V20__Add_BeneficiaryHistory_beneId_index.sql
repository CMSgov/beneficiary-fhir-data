/*
 * Adds an index to the BeneficiariesHistory table to support queries against
 * the beneficiaryId column.
 * 
 * See: https://jira.cms.gov/browse/BLUEBUTTON-1110
 */

create index ${logic.index-create-concurrently} "BeneficiariesHistory_beneficiaryId_idx"
  on "BeneficiariesHistory" ("beneficiaryId");