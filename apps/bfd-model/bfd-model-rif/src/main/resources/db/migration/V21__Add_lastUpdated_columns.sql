-- Add a column without writing a data to every row
--
-- From the PostgreSQL 9.6 ALTER command documentation...
--
-- When a column is added with ADD COLUMN, all existing rows in the table are initialized with the
-- column's default value (NULL if no DEFAULT clause is specified). If there is no DEFAULT clause, this is merely a
-- metadata change and does not require any immediate update of the table's data; the added NULL values are supplied
-- on readout, instead.
--
-- Based on this information, alters have the implicit default null


alter table "Beneficiaries" add column lastUpdated timestamp with time zone;

alter table "BeneficiariesHistory" add column lastUpdated timestamp with time zone;

alter table "MedicareBeneficiaryIdHistory" add column lastUpdated timestamp with time zone;

alter table "PartDEvents" add column lastUpdated timestamp with time zone;

alter table "CarrierClaims" add column lastUpdated timestamp with time zone;

alter table "InpatientClaims" add column lastUpdated timestamp with time zone;

alter table "OutpatientClaims" add column lastUpdated timestamp with time zone;

alter table "HHAClaims" add column lastUpdated timestamp with time zone;

alter table "DMEClaims" add column lastUpdated timestamp with time zone;

alter table "HospiceClaims" add column lastUpdated timestamp with time zone;

alter table "SNFClaims" add column lastUpdated timestamp with time zone;
