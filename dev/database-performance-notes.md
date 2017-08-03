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
