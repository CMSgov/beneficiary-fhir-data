/*
 * Queries for finding and removing skipped synthetic RIF records in our public databases. 
 * If there is an issue with our ETL process or filtering logic, these sets of queries will come in handy.
 */

 -- Find the number of skipped RIF records. 
 -- The value should match the number of UPDATE operatons in the generated synthea RIF files i.e. "beneficiary_interem.csv".
select count(*) from skipped_rif_records
where cast(bene_id as numeric) < 0;

 -- Delete skipped RIF records. 
delete from skipped_rif_records
where cast(bene_id as numeric) < 0;