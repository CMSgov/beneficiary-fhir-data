/*
 * Creates the beneficiary table; example table is extremely simple for testing purposes.
 * This simple script is expected to succeed.
 */

${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "Beneficiaries" (
   "beneficiaryId" varchar(15) not null,
    "birthDate" date not null,
    "countyCode" varchar(10) not null,
    constraint "Beneficiaries_pkey" primary key ("beneficiaryId")
)
${logic.tablespaces-escape} tablespace "beneficiaries_ts"
;
