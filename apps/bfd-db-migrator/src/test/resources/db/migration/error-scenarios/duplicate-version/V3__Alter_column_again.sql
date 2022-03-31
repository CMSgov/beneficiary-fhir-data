/*
 * Test alter that exists just to add a step to the flyway chain. This step is expected to succeed.
 */

alter table "Beneficiaries"
    alter column "countyCode" ${logic.alter-column-type} varchar(11,16);
