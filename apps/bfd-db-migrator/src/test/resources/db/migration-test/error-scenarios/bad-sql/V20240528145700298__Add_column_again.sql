/*
 * Test add that should not be reached, as we should have errored on the V3 script.
 */

alter table "Beneficiaries" add column "newField" varchar(15);
