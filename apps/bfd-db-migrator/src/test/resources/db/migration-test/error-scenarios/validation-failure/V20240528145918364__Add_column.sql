/*
 * Simple test script to add a column; this is expected to succeed. Primarily here
 * to add some steps to the migration chain during the test.
 */

alter table "Beneficiaries" add column "hicnUnhashed" varchar(11);
