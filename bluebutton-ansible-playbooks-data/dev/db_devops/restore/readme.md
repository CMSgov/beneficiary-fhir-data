# FULL DATABASE TABLE RESTORE README


## ASSUMPTIONS

- A Database system  is available. 
- The FHIRDB Tables need to be restored.
- Dump files are available either on s3 or on local filesystem.
- Know the backup dumpfile date to restore
	e.g. Restore dump file for May 7, 2019 (2019-05-07)
- The default dump file location on file system is /u01/backups/fhirdb/<BAK_DATE>
-   where backup date format is 'YYYY-MM-DD'
- BAK_DATE parameter as backup date
- Only the last backup is on fileystem.
- All fhirdb tables are in public schema.

## PROCEDURES
  
1. LOCATE THE BACKUP DUMP FILES ON S3
	* Check if backup files are on S3 by running /opt/backups/db_list.bash script. 
         e.g. for backup date 2019-05-07, run the following:
 
		```
		$ /opt/backups/db_list.bash | grep 2019-05-07/
							   PRE 2019-05-07/
		```

	* List the backup files for the backup date (e.g. 2019-05-07)

		```
		$ /opt/backups/db_list.bash 2019-05-07
		2019-05-09 04:39:32 | 5417855931 | tbl_Beneficiaries.dmp
		2019-05-09 04:40:02 |       3563 | tbl_BeneficiariesHistory.dmp
		..............
		```

	* Restore Backup From S3 To Local Disk for e.g 2019-05-07.
        This 

		```
		$ /opt/backups/db_restore.bash 2019-05-07
		tbl_Beneficiaries.dmp
		tbl_BeneficiariesHistory.dmp
		...........
		```

2. CREATE/REVIEW RESTORE SCRIPT pg_restore_tables.sh

    > This script takes one parameter BAK_DATE (format 'YYYY-MM-DD')
	
	* review the pg_restore_tables.sh script to ensure all tables in the dump files to be restored are included.
		> IMPORTANT : All other tables in the database reference the 
		> Beneficiaries table, therefore this table
		> needs to be restored first. This is effected by using the 'wait' > after its restore.
		> Also, other tables are restored concurrently in order to maximize restore time.

	* Review the contents of each table restore script. e.g. for Beneficiaries and CarrierClaims tables

		```	
		-- Beneficiaries_res.sh
		echo " Starting Beneficiaries Table Restore " 
		date
		pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_Beneficiaries.dmp
		echo " End Beneficiaries Table Restore "
		date

		--CarrierClaims_res.sh
		echo " Starting CarrierClaims Table Restore "
		date
		pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_CarrierClaims.dmp
		pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_CarrierClaimLines.dmp
		echo " End CarrierClaims Table Restore "
		date
		```

3. DROP EXISTING TABLES
	* Ensure that the tables to be restored are dropped from the environment.
	   The following query can generate the drop commands for all the tables in the fhirdb database that are in public schema.

	* Generate drop sql scripts
		```
		fhirdb=# select 'DROP TABLE '||schemaname||'."'||tablename||'" CASCADE;' from pg_tables where schemaname='public';
								 ?column?
		-----------------------------------------------------------
		 DROP TABLE public."DMEClaims" CASCADE;
		 DROP TABLE public."SNFClaims" CASCADE;
         ...............
		 ```

	* Run the drop sql commands
    	   copy to a file and run as script or copy and paste at the psql prompt

		```
		fhirdb=#  DROP TABLE public."Beneficiaries" CASCADE;
		NOTICE:  drop cascades to 3 other objects
		DETAIL:  drop cascades to constraint OutpatientClaims_beneficiaryId_to_Beneficiaries on table "OutpatientClaims"
		drop cascades to constraint InpatientClaims_beneficiaryId_to_Beneficiaries on table "InpatientClaims"
		drop cascades to constraint CarrierClaims_beneficiaryId_to_Beneficiaries on table "CarrierClaims"
		DROP TABLE
		Time: 6.554 ms
		fhirdb=#  DROP TABLE public."CarrierClaimLines" CASCADE;
		DROP TABLE
		....................
		```

	* Check for existence of dropped tables. Should return no rows.
		
		```
		fhirdb=# select * from pg_tables where schemaname='public';
		 schemaname |          tablename           |  tableowner  |           tablespace            | hasindexes | hasrules | hastriggers | rowsecurity
		------------+------------------------------+--------------+---------------------------------+------------+----------+-------------+-------------

		(0 rows)
		```

4. SET AUTOVACUUM OFF BEFORE STARTING RESTORE

      Edit the /u01/local/pgsql/9.6/data/postgresql.conf file and turn off autovacuum,save and exit file.
	```
	-- vi /u01/local/pgsql/9.6/data/postgresql.conf
		#autovacuum = on                        # Enable autovacuum subprocess?  'on'
		autovacuum = off                        # Enable autovacuum subprocess?  'on'
     
   -- Restart Service
		[root@tscw10db03 ~]# /bin/systemctl stop postgresql-9.6.service -l
		[root@tscw10db03 ~]# /bin/systemctl start postgresql-9.6.service -l
		
   -- verify 
		psql -d fhirdb 
		fhirdb=# \x
		Expanded display is on.
		fhirdb=# select * from pg_settings where name='autovacuum';
		-[ RECORD 1 ]---+------------------------------------------
		name            | autovacuum
		setting         | off
		unit            |
		category        | Autovacuum
		short_desc      | Starts the autovacuum subprocess.
		extra_desc      |
		context         | sighup
		vartype         | bool
		source          | configuration file
		min_val         |
		max_val         |
		enumvals        |
		boot_val        | on
		reset_val       | off
		sourcefile      | /u01/local/pgsql/9.6/data/postgresql.conf
		sourceline      | 519
		pending_restart | f
	```
		
5. RUN pg_restore_tables.sh SCRIPT
     e.g. restore backup of 2019-05-07 
 
	```
	/var/lib/pgsql/scripts/pg_restores/pg_restore_tables.sh '2019-05-07' > /var/lib/pgsql/scripts/pg_restores/logs/pg_restore_tables.log 2>&1
	```

6. LOG OUTPUT SHOWING RESTORE ELAPSED TIME FOR EACH SCRIPT
    > Note: check the logs in the directory /var/lib/pgsql/scripts/pg_restores/logs/ for any issues 
	        and elapsed time for each table restore.
	
	 e.g.
	```
		$ cat Beneficiaries_res.log
		 Starting Beneficiaries Table Restore
		Wed May 29 17:49:02 EDT 2019
		 End Beneficiaries Table Restore
		Wed May 29 18:19:33 EDT 2019

		$ cat CarrierClaimsLines_res.log
		 Starting CarrierClaimsLines Table Restore
		Tue Jun 11 12:31:07 EDT 2019
		 End CarrierClaimsLines Table Restore
		Sat Jun 15 12:07:30 EDT 2019
	```

7. SET AUTOVACUUM ON AFTER RESTORE COMPLETES SUCCESSFULLY

      Edit the /u01/local/pgsql/9.6/data/postgresql.conf file and turn on autovacuum,save and exit file.

	```
	-- vi /u01/local/pgsql/9.6/data/postgresql.conf

	-- set autovacuum = on  and restart PosgreSQL service
	-- vi postgresql.conf
		autovacuum = on                        # Enable autovacuum subprocess?  'on'
		#autovacuum = off                        # Enable autovacuum subprocess?  'on'
     
	-- Restart Service
		[root@tscw10db03 ~]# /bin/systemctl stop postgresql-9.6.service -l
		[root@tscw10db03 ~]# /bin/systemctl start postgresql-9.6.service -l
		
	fhirdb=# select * from pg_settings where name='autovacuum';
	-[ RECORD 1 ]---+------------------------------------------
	name            | autovacuum
	setting         | on
	unit            |
	category        | Autovacuum
	short_desc      | Starts the autovacuum subprocess.
	extra_desc      |
	context         | sighup
	vartype         | bool
	source          | configuration file
	min_val         |
	max_val         |
	enumvals        |
	boot_val        | on
	reset_val       | on
	sourcefile      | /u01/local/pgsql/9.6/data/postgresql.conf
	sourceline      | 518
	pending_restart | f
	```

8. ANALYZE TABLES

	```
	-- analyze_table_scripts.sql
	-- psql -d fhirdb -c "SELECT 'ANALYZE VERBOSE '||schemaname||'.'||'\"'||relname||'\"'||';' FROM pg_stat_user_tables where schemaname='public' order by relname;"

	-bash-4.2$ . ./analyze_table_scripts.sql > analyze_table_scripts_run.sql

	-bash-4.2$ cat analyze_table_scripts_run.sql
							?column?
	--------------------------------------------------------
	ANALYZE VERBOSE public."Beneficiaries";
	ANALYZE VERBOSE public."BeneficiariesHistory";
	ANALYZE VERBOSE public."BeneficiariesHistoryTemp";
	ANALYZE VERBOSE public."CarrierClaimLines";
	..............

	-- check row count for each table
	-- If this data was capture with the backup file
	-- we could have compared before and after restore
	-- to ensure rowcount matches for each table.

	fhirdb=# SELECT schemaname,relname, n_live_tup FROM pg_stat_user_tables WHERE schemaname='public' ;
	schemaname |           relname            | n_live_tup
	------------+------------------------------+------------
	public     | HHAClaims                    |   16481040
	public     | PartDEvents                  | 3999198174
	public     | DMEClaims                    |  162500882
	public     | OutpatientClaimLines         | 3127683852
	public     | schema_version               |         12
	.................
	```

9. RUN WEEKLY TABLE SIZE REPORT FOR 2019-05-06 AND 2019-06-17

	this data is stored in the table dba_util.relation_sizes every monday
	this will be the same if no update on the table before pg_dump

	```
	-- generate report: WEEKLY SPACE USAGE REPORT FOR 2019-05-06

	fhirdb=# select * from dba_util.relation_sizes where to_char(created_at,'YYYY-MM-DD')='2019-05-06';
	schema |           relation           |  table_size   |  index_size  | total_relation_size |          created_at
	--------+------------------------------+---------------+--------------+---------------------+-------------------------------
	public | CarrierClaimLines            | 1176771878912 | 192516382720 |       1369288269824 | 2019-05-06 05:00:01.947124-04
	public | PartDEvents                  |  885445165056 | 295465164800 |       1181128015872 | 2019-05-06 05:00:01.947124-04
	public | OutpatientClaimLines         |  624926490624 |  98654535680 |        723581034496 | 2019-05-06 05:00:01.947124-04
	public | CarrierClaims                |  529898676224 | 139373436928 |        669272113152 | 2019-05-06 05:00:01.947124-04
	................

	-- generate report: WEEKLY SPACE USAGE REPORT FOR 2019-06-17

	fhirdb=# select * from dba_util.relation_sizes where to_char(created_at,'YYYY-MM-DD')='2019-06-17';
	schema |           relation           |  table_size   |  index_size  | total_relation_size |          created_at
	--------+------------------------------+---------------+--------------+---------------------+-------------------------------
	public | CarrierClaimLines            | 1176771878912 | 192516382720 |       1369577562112 | 2019-06-17 10:57:54.139939-04
	public | PartDEvents                  |  885441699840 | 246074458112 |       1131733843968 | 2019-06-17 10:57:54.139939-04
	public | OutpatientClaimLines         |  624926490624 |  98654535680 |        723734667264 | 2019-06-17 10:57:54.139939-04
	public | CarrierClaims                |  529898676224 | 139373436928 |        669402390528 | 2019-06-17 10:57:54.139939-04
	..............



	-- 8. PG_DUMPS FOR 2019-05-07 AND 2019-06-17

	-bash-4.2$ pwd
	/u01/backups/fhirdb/2019-05-07

	-bash-4.2$ ls -ltr
	total 1640475260
	-rw-r--r-- 1 postgres postgres   5417855931 May  9 04:39 tbl_Beneficiaries.dmp
	-rw-r--r-- 1 postgres postgres 531978493684 May  9 04:40 tbl_CarrierClaimLines.dmp
	-rw-r--r-- 1 postgres postgres         2694 May  9 04:40 tbl_BeneficiariesHistoryTemp.dmp
	-rw-r--r-- 1 postgres postgres         3563 May  9 04:40 tbl_BeneficiariesHistory.dmp
	................

	-- Ppg_dump backup files after Table restore
	-bash-4.2$ pwd
	/u01/backups/fhirdb/2019-06-17
	-bash-4.2$ ls -ltr
	total 1651056980
	-rw-r--r-- 1 postgres postgres         3099 Jun 17 13:00 tbl_schema_version.dmp
	-rw-r--r-- 1 postgres postgres         1329 Jun 17 13:00 tbl_duplicate_record_ids.dmp
	-rw-r--r-- 1 postgres postgres         2384 Jun 17 13:00 tbl_TestJo_old.dmp
	-rw-r--r-- 1 postgres postgres         2694 Jun 17 13:00 tbl_BeneficiariesHistoryTemp.dmp
	-rw-r--r-- 1 postgres postgres         3563 Jun 17 13:00 tbl_BeneficiariesHistory.dmp
	.................

	```


      