/*
 * Simple test script to alter a column; this is expected to succeed. Primarily here
 * to add some steps to the migration chain during the test.
 */

alter table "Beneficiaries"
   alter column "countyCode" ${logic.alter-column-type} varchar(10,11);
