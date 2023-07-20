Database Notes
==============

## Index Performance

Here are some useful queries for determining an index's characteristics:

* How many pages of disk space (8 KB each) is an index using?
    
    ```
    SELECT c2.relname, c2.relpages
    FROM pg_class c, pg_class c2, pg_index i
    WHERE c.relname = 'CarrierClaims' AND
          c.oid = i.indrelid AND
          c2.oid = i.indexrelid
    ORDER BY c2.relname;
    ```
    
* The `psql` environment has a builtin `\timing` command that will collect timing data for every command run. This makes it easy to determine how much an index speeds up, say counting a table's rows, by enabling `\timing` and then running a `select count(*) ...` on the table.

The following observations were collected for indexes in production:

* Enabling the primary key constraint on `CarrierClaims` when it was only 42% of the way through an initial load:
    * The table had 811,339,652 rows.
    * Creating the constraint took 12781424.203 ms, or 3.5 hours.
    * The constraint's index used 3123974 pages, or 24 GB of disk space.
    * Running a `select count(*) ...` query against the table took 297451.441 ms, or 5 minutes, though this seemed to move around a good bit when re-run.
    * Dropping the constraint while the load was stopped took 6.5 seconds.
    * After the constraint was dropped, a `select count(*) ...` query on the table took 304039.462 ms, or 5 minutes, and returned 838,343,050 rows.
* Enabling the primary key constraint on `CarrierClaims` when it was 96% of the way through an initial load:
    * Row count: 1,889,316,270 rows
    * `select count(*) ...` duration:
        * Without index: 18 min (1097351.137 ms).
        * With index: 11 min (660468.124 ms).
    * Constraint creation duration: 10.5 hours (37977002.349 ms).
    * Constraint disk usage: 58.2 GB (7274597 pages).
    * `select * from "CarrierClaims" where "CarrierClaims"."claimId" ='foo'`:
        * Prior to `vacuum analyze`: 10 min (612366.423 ms), and didn't use index.
        * After `vacuum analyze`: XXX min (XXX ms), and didn't use index.
* Enabling the primary key constraint on `Beneficiaries` when it was 100% loaded:
    * The table had 62,897,512 rows.
    * Didn't collect timing data, but I'd say it took less than an hour to create the constraint.
    * The constraint's index used 237237 pages, or 2 GB of disk space.
    * Running a `select count(*) ...` query against the table took 27052.280 ms, or 0.5 minutes.
* Index creation times after initial load:
    * `PartDEvents_beneficiaryId_idx`: 16.3 hours (58539202.853 ms)
    * `SNFClaims_beneficiaryId_idx`: 53 seconds (53267.559 ms)

## Verifying Row Counts

The following query can be used to count the number of rows in all user tables:

```
select
  table_schema, 
  table_name, 
  (xpath('/row/cnt/text()', xml_count))[1]::text::bigint as row_count
from (
  select
    table_name,
    table_schema, 
    query_to_xml(format('select count(*) as cnt from %I.%I', table_schema, table_name), false, true, '') as xml_count
  from information_schema.tables
  where table_schema = 'public' --<< change here for the schema you want
) t
sort by
  table_schema asc,
  table_name asc;
```

This query took 122 min (7326516.742 ms) to run, with most indexes missing.

## Query Times

Queries such as the following can be used to select random rows from a table:

    fhirdb2=# select * from "PartDEvents" offset random() * (select count(*) from "PartDEvents") limit 1;
    Time: 2026784.711 ms

(Note that this query will _not_ use any indexes and will be quite slow for large tables.)

Here's the observed query performance of various tables after the initial production load:

* 
    
    fhirdb2=# select * from "PartDEvents" where "eventId" = 'XXX';
    Time: 12.154 ms
    
* 
     
    fhirdb2=# select * from "CarrierClaims" where "claimId" = 'XXX';
    Time: 11.436 ms
    
* 
    
    fhirdb2=# select * from "CarrierClaimLines" where "parentClaim" = 'XXX';
    Time: 483844.546 ms
    
    * Note that the poor performance here is due to [http://issues.hhsdevcloud.us/browse/CBBD-297](CBBD-297: Claim line indices have columns in wrong order, leading to poor query performance).

## Disk Space Usage

The following query was run after the initial load to determine the total disk space usage of the database:

```
fhirdb2=# SELECT
fhirdb2-#     pg_size_pretty(sum(pg_relation_size(C.oid))) AS "size"
fhirdb2-#   FROM pg_class C
fhirdb2-#     LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace)
fhirdb2-#   WHERE nspname = 'public';
  size   
---------
 3245 GB
(1 row)

Time: 14.564 ms
```

And the following query broke that usage out by each table and index:

```
SELECT
    nspname || '.' || relname AS "relation",
    pg_size_pretty(pg_relation_size(C.oid)) AS "size"
  FROM pg_class C
    LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace)
  WHERE nspname = 'public'
  ORDER BY relname ASC;

                 relation                  |    size    
-------------------------------------------+------------
 public.Beneficiaries                      | 9981 MB
 public.Beneficiaries_pkey                 | 1853 MB
 public.CarrierClaimLines                  | 825 GB
 public.CarrierClaimLines_pkey             | 155 GB
 public.CarrierClaims                      | 354 GB
 public.CarrierClaims_beneficiaryId_idx    | 55 GB
 public.CarrierClaims_pkey                 | 56 GB
 public.DMEClaimLines                      | 48 GB
 public.DMEClaimLines_pkey                 | 9282 MB
 public.DMEClaims                          | 24 GB
 public.DMEClaims_beneficiaryId_idx        | 4011 MB
 public.DMEClaims_pkey                     | 4118 MB
 public.HHAClaimLines                      | 27 GB
 public.HHAClaimLines_pkey                 | 7919 MB
 public.HHAClaims                          | 3011 MB
 public.HHAClaims_beneficiaryId_idx        | 378 MB
 public.HHAClaims_pkey                     | 392 MB
 public.HospiceClaimLines                  | 12 GB
 public.HospiceClaimLines_pkey             | 3898 MB
 public.HospiceClaims                      | 1210 MB
 public.HospiceClaims_beneficiaryId_idx    | 157 MB
 public.HospiceClaims_pkey                 | 163 MB
 public.InpatientClaimLines                | 27 GB
 public.InpatientClaimLines_pkey           | 11 GB
 public.InpatientClaims                    | 10082 MB
 public.InpatientClaims_beneficiaryId_idx  | 630 MB
 public.InpatientClaims_pkey               | 650 MB
 public.OutpatientClaimLines               | 343 GB
 public.OutpatientClaimLines_pkey          | 74 GB
 public.OutpatientClaims                   | 82 GB
 public.OutpatientClaims_beneficiaryId_idx | 10 GB
 public.OutpatientClaims_pkey              | 11 GB
 public.PartDEvents                        | 826 GB
 public.PartDEvents_beneficiaryId_idx      | 118 GB
 public.PartDEvents_pkey                   | 121 GB
 public.SNFClaimLines                      | 6160 MB
 public.SNFClaimLines_pkey                 | 2361 MB
 public.SNFClaims                          | 3254 MB
 public.SNFClaims_beneficiaryId_idx        | 273 MB
 public.SNFClaims_pkey                     | 285 MB
 public.schema_version                     | 8192 bytes
 public.schema_version_pk                  | 16 kB
 public.schema_version_s_idx               | 16 kB
```
