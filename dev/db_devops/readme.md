# DB scripts

## Weekly PG_DUMP Scripts

The entire database dump process takes over 24 hours to dump the database and save the files into S3. It involves several scripts.

1. `weekly_pg_dump_backup_schedule.txt`
   This is the Unix Cron Schedule
   It runs every Tuesday at 5pm EST
   It runs for over 24 hours
   
2. `fhirdb_database_tables_dumps.sh`
   This script generates the pg_dump run script (e.g. fhirdb_pg_dump_all_tabs_20190514.sh) for all tables in the PUBLIC schema.
   This is to ensure that new tables added to the database are backup without editing script.

3. `pg_dump_fhir_tables.sh`
   This is the script that actually runs the pg_dump. It takes a table name as parameter
   
4. `fhirdb_pg_dump_all_tabs_20190514.sh`
   This is the script that has been generated for May 14, 2019.
   It runs all the pg_dump concurrently, so they can complete in a reasonable time. It waits until all pg_dump are done, upload to AW S3 and sent a report out to a distribution list.

5. `db_backup.sh`
    This script takes a saved pg_dump file and stores it into S3 for safe keeping. To run this script, AWS CLI and the AWS KMS key id for this S3 bucket is required.

### Prerequisites
- *TODO Access groups*
- *TODO Environment variables*

## PG_RESTORE Scripts

The restore scripts are kept in the restore folder. There are scripts to grab the *.dmp files from S3 and to load a db with this files. Please see the [README.MD](restore/readme.md) of the restore folder first for details on the process.  

A full restore was done during late June of 2019. The full process worked and was verified. It took a little more than 4 days. 

## Replica Promote Scripts

Replica promotion was also practiced during the late June 2019. Make a new replica will take about 24 hours in our tests. The scripts and procedures are put in the [Replicate and Promote](TS03_Replicate_And_Promote.txt) file. Several procedures are outlined in the text. Including:
- Promoting a replica to master
- How to stop replication of master
- How to setup replication from on server to another
