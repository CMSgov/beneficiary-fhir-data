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
--

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

-- 
-- Add tables that tracks the ETL process
--
-- One row for each RIF file loaded. 
-- The timestamp represents the start time of the RIF file processing
-- 
create table "LoadedFiles" (
  "loadedFileId" bigint primary key,		             			        
  "rifType" varchar(48) not null,	
  "created" timestamp with time zone not null  
)
${logic.tablespaces-escape} tablespace "loadedfiles_ts"
;

create sequence loadedFiles_loadedFileId_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 1 cycle;

-- One row for each batch of beneficiaries loaded. Indexed on loadedFileId.
--
-- Dev Note: 
-- Many ways of storing beneIds where tried and considered. Since RIF records are written in
-- batches, creating a record that represents that batch was considered to be efficient enough.
-- More importantly since RIF record batches are put inside a transaction, the LoadedBatches
-- table will always be consistent with the loaded RIF records. Beneficiaries is not an array
-- because Hibernate doesn't work well with that type.
--
create table "LoadedBatches" (
  "loadedBatchId" bigint primary key,
  "loadedFileId" bigint not null,               
  "beneficiaries" varchar(20000) not null,    
  "created" timestamp with time zone not null
)
${logic.tablespaces-escape} tablespace "loadedbatches_ts"
;

alter table "LoadedBatches"         
  add constraint "loadedBatches_loadedFileId" 
    foreign key ("loadedFileId") 
    references "LoadedFiles";

create sequence loadedBatches_loadedBatchId_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 20  cycle;
