/*
 * Drops all hfj tablespaces
 */

 -- DROP RELATED TABLESPACES---------------------------
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_cmn_ts;
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_date_ts;
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_link_ts;
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_resource_ts;
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_string_ts;
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_token_ts;
${logic.drop-tablespaces-escape} DROP TABLESPACE IF EXISTS fhirdb_ver_ts;