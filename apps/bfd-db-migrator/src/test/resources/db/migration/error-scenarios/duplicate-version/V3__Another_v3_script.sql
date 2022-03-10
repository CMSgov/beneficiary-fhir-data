/*
 * A duplicate V3 script which should cause a flyway exception; the sql content doesn't matter much, since flyway
 * should blow up before running it.
 */

alter table "Beneficiaries"
    alter column "countyCode" ${logic.alter-column-type} varchar(11,16);
