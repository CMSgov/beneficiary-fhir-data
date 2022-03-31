/*
 * Test alter that should not be reached, as we should have errored on the V3 script.
 */

alter table "Beneficiaries"
    alter column "countyCode" ${logic.alter-column-type} varchar(11,16);
