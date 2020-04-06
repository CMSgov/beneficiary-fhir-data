/*
 * The column doesn't have a default value to avoid updating the column on migration. The pipeline server
 * will populate the column as new beneficaries are added or existing beneficaries are updated. 
 */

alter table "Beneficiaries" add column "mbiHash" varchar(64);

alter table "BeneficiariesHistory" add column "mbiHash" varchar(64);

