Follow this runbook to load historical production access log data into BFD insights.

There are three Glue tables that are referenced in this runbook:
- `export` table - a table that is built on top of the raw export data from Cloudwatch
- `staging` table - a non-partitioned table that has the desired column structure and can be loaded from the `export` table efficiently
- `target` table - the partitioned table that is the final destination for the data (should already exist via terraform)

This runbook should be executed after the Kinesis Firehose has started to populate data into the `target` table.

1. Export the data from Cloudwatch.
   1. Review the exports that are available already in the export location: s3://bfd-insights-bfd-app-logs/export/prod/
      to see if the needed data has been exported previously. New exports should be contiguous and non-overlapping with
      existing exports. Start and end timestamps should be selected to run from the first day of a month at 00:00:00 UTC
      until the first day of a subsequent month at 00:00:00 UTC (exporting more than one month at a time is advisable).
      Removing an existing export in favor of exporting that
      data again with a later end date is an acceptable way to keep the number of exports manageable. The last export
      chronologically should have a small overlap of 12 hours with the data that is populated with Firehose. This
      overlap will be accounted for when populating the `staging` table.
   2. Navigate to Cloudwatch in the AWS console.
   3. Select `Log Groups`
   4. Select `/bfd/prod/bfd-server/access.json`
   5. Select `Actions -> Export Data to Amazon S3`
   6. Choose the time period 
   7. Select Account: *This Account*
   8. S3 Bucket Name: `bfd-insights-bfd-app-logs`
   9. S3 Bucket Prefix: `export/prod/YYYY_(MM-MM)` (For example, export/prod/2022_(01-06) for the data that spans from 
      2022-01-01 00:00:00 through 2022-07-01 00:00:00)
   10. Run the job. Production exports can take up to an hour per month depending on the activity level that month.
   11. When the job completes, remove the `aws-logs-write-test` subfolder that AWS creates for internal testing.

2. Create/Update the Glue `export` table.
   1. From the AWS Glue console, select Crawlers
   2. Select the `bfd_cw_export` crawler (this crawler is in terraform and should already exist)
   3. Select `Run`
   4. Verify that the `bfd_cw_export.prod` table has the expected data by inspecting the results of this query that
      retrieves the number of records per month and ensuring that the newly exported data is represented:
   
   ```sql
   select date_format(from_iso8601_timestamp("timestamp"), '%Y-%m'), count(*)
   from bfd_cw_export.prod
   group by 1
   order by 1
   ```

3. Determine the column list for the `staging` table.
   1. The column list for the `staging` table should be identical in names, data types, and ordering to the `target`
      table after removing the `year` and `month` partition columns. The column definition for the `target` table can be
      retrieved by navigating to the table in the AWS Glue console, selecting `Actions` and then `View properties` which
      makes a JSON schema for the table available which includes the ordered list of columns. The column names can be
      extracted from this file and should resemble the list below. Only include the column names from the `Columns`
      array -- do not include `PartitionKeys`.

      Sample output:
      ```text
      cw_timestamp
      cw_id
      timestamp
      level
      thread
      logger
      message
      context
      mdc_bene_id
      mdc_database_query_bene_by_coverage_batch
      ... <over 200 additional mdc columns>
      ```
   2. The column list for the `staging` table is constructed from the output from step 2 adding the column type (all
      columns are of string type).
   ```text
      cw_timestamp string,
      cw_id string,
      timestamp string,
      level string,
      thread string,
      logger string,
      message string,
      context string,
      mdc_bene_id string,
      mdc_database_query_bene_by_coverage_batch string,
      ... <all other mdc columns in order>
   ```

4. Create a non-partitioned Parquet Glue table to serve as the `staging` table.
   ```sql
   CREATE EXTERNAL TABLE prod_staging (
      cw_timestamp string, -- Cloudwatch timestamp
      cw_id string,        -- Cloudwatch ID, set to null for historical loads
      timestamp string,    -- BFD timestamp
      level string,        -- Fields from BFD JSON from here on down
      thread string,
      logger string,
      message string,
      context string,
      mdc_bene_id string,
      mdc_database_query_bene_by_coverage_batch string,
      ... <all other mdc columns in order>
   ) STORED AS parquet
   LOCATION 's3://bfd-insights-bfd-<account-id>/databases/bfd_cw_export/prod_staging/'
   ```

5. Load the `staging` table from the `export` table.
   The `staging` table must be loaded in batches of no more than ~300 million records to avoid hitting the 30 minute
   Athena timeout. This is accomplished by running the following statement with different where clauses in the with
   clause that load a portion of the data each time. Note that in order to avoid duplication with the running
   firehose, the most recent export should have a small overlap with the firehose data and include a where clause
   that performs de-duplication against the `target` table. The where clauses for the initial load are included below,
   commented out, with the elapsed time to load as a comment.
   ```sql
   insert into prod_staging
   with dataset as (
   select
      timestamp as "cw_timestamp",
      null as "cw_id",
      json_extract_scalar(message,'$.timestamp') as "timestamp",
      json_extract_scalar(message,'$.level') as "level",
      json_extract_scalar(message,'$.thread') as "thread",
      json_extract_scalar(message,'$.logger') as "logger",
      json_extract_scalar(message,'$.message') as "message",
      json_extract_scalar(message,'$.context') as "context",
      transform_keys(cast(json_extract(message, '$.mdc') as MAP(VARCHAR, VARCHAR)), (k, v) -> replace(lower(k), '.', '_')) as stuff
   from prod
    --where partition_0 = '2019_(01-12)'                      --  3m30s, 5.84GB
    --where partition_0 = '2020_(01-12)'                      --  9m13s, 32.58GB
    --where partition_0 = '2021_(01-06)'                      -- 14m14s, 54.34GB
    --where partition_0 = '2021_(07-12)'                      --  7m40s, 28.03GB
    --where partition_0 = '2022_(01-05)'                      -- 13m21s, 52.49GB
    --where partition_0 = '2022_(06-0903)'
    --and month(from_iso8601_timestamp("timestamp")) <= 6   --  9m20s, 92.13GB
    --where partition_0 = '2022_(06-0903)'
    --and month(from_iso8601_timestamp("timestamp")) in (7,8) -- 16m05s, 92.13GB
    where partition_0 = '2022_(06-0903)'                      --  4m08s, 92.46GB
      and month(from_iso8601_timestamp("timestamp")) >= 9
      and json_extract_scalar(message,'$.mdc["http_access_response_header_X-Request-ID"]') not in (
            select "mdc_http_access_response_header_x-request-id"
            from "bfd-insights-bfd-prod".bfd_insights_bfd_prod_api_requests
      )
   ) select
      cw_timestamp,
      cw_id,
      timestamp,
      level,
      thread,
      logger,
      message,
      context,
      stuff['bene_id'] as "bene_id",
      stuff['database_query_bene_by_coverage_batch'] as "database_query_bene_by_coverage_batch",
      ... <repeat for all mdc columns in order since this is positional>
      from dataset
   ```

6. Load the `target` table from the `staging` table.
   1. The `target` table definition resides in terraform and should match the `staging` table columns and ordering with the one
      difference being that the `staging` table does not include the `year` and `month` partition columns. Verify that
      the table structure is the same if not done already. 
   2. Similar to the loading of the `staging` table, this must be done in batches to avoid the Athena 30 minute timeout.
      The batches here can be larger than for the `staging` table but ~500 million is the limit.
      ```sql
      insert into "bfd_insights_bfd_prod_api_requests" 
      select
         cw_timestamp,
         cw_id,
         timestamp,
         level,
         thread,
         logger,
         message,
         context,
         "mdc_bene_id",
         "mdc_database_query_bene_by_coverage_batch",
         "mdc_jpa_query_eobs_by_bene_id_snf_record_count",
         ... <all mdc columns here>
         date_format(from_iso8601_timestamp("timestamp"), '%Y') as "year",
         date_format(from_iso8601_timestamp("timestamp"), '%m') as "month"
      from bfd_cw_export.api_requests_no_partitions
      --where year(from_iso8601_timestamp("timestamp")) = 2019
      --where year(from_iso8601_timestamp("timestamp")) = 2020
      --where year(from_iso8601_timestamp("timestamp")) = 2021
      --where year(from_iso8601_timestamp("timestamp")) = 2022
        --and month(from_iso8601_timestamp("timestamp")) < 8
      where year(from_iso8601_timestamp("timestamp")) = 2022
         and month(from_iso8601_timestamp("timestamp")) = 8
      ```
   
7. Verify the load.
   1. Select a sample of the data and inspect the most important columns: timestamp, mdc_bene_id, mdc_http* to ensure
      that the columns are populated sensibly. Note that many of the other columns are only sparsely populated.
      ```sql
      select *
      from bfd_insights_bfd_prod_api_requests
      limit 100;
      ```
      
   2. Compare the count of records by month between the `export` table and the `target` table. The counts for each month
      should match (accounting for any data in the `target` table that was loaded independently by firehose).
      ```sql
      -- Retrieve count of records by month for the target table
      select date_format(from_iso8601_timestamp(timestamp), '%Y-%m'), count(*)
      from bfd_insights_bfd_prod_api_requests
      group by 1
      order by 1
      
      -- Retrieve count of records by month for the export table
      select date_format(from_iso8601_timestamp(timestamp), '%Y-%m'), count(*)
      from bfd_cw_export.prod
      group by 1
      order by 1
      ```
   
Reference:

The following query was used to extract the canonical ordered list of JSON MDC keys from the Cloudwatch exports to
define the initial table schema for the `export` table.

   ```sql
   with dataset AS (
      select map_keys(cast(json_extract(message, '$.mdc') as MAP(VARCHAR, VARCHAR))) as things
      from bfd_cw_export.prod
   )
   select distinct concat('mdc_', replace(lower(name), '.', '_')) as column_name from dataset
   cross join unnest(things) as t(name)
   order by column_name;
   ```