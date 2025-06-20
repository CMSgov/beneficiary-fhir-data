# Table of Contents

- [BB2 BFD-Insights Athena Tables for QuickSight Summary](#summary)
  - [Per Application Metrics Table](#per-app-table)
  - [Top Level Metrics Table](#top-level-table)
  - [Weekly Update of the Metric Tables](#weekly-update-tables)
- [HOW-TO: Testing SQL Templates Individually](#how-to-test-sql-templates)
- [Lambda Function to Update Metrics Tables](#lambda-function)
- [HOW-TO: Update QuickSight DataSets](#how-to-update-qs-datasets)
- [HOW-TO: A Walk-Through Example of Adding New Metrics](#how-to-new-metrics)
  - [Adding a New Per Application Metric](#how-to-add-per-app-metric)
  - [Adding a New Top Level Metric](#how-to-add-top-level-metric)
  - [Testing the New SQL Templates with the Lambda Function](#testing-templates-lambda)

# BB2 BFD-Insights Athena Tables for QuickSight Summary<a id="summary"></a>

In between the Kinesis Firehose log streams and QuickSights there are Athena tables setup for mapping the reporting data. The reporting data is used to build dashboards and analysis in QuickSight.

This document contains information about the tables, templates with Athena SQL used to generate metrics, the Lambda function that is scheduled to run weekly and how to develop & add new metrics.

## Per Application Metrics Table<a id="per-app-table"></a>

This table contains one row per BB2 application for each of the report dates (`report_date` column). It contains metrics related to each application.

|            |                                                                     |
| ---------- | ------------------------------------------------------------------- |
| TABLE NAME | \<impl\|prod\>_global_state_per_app                           |
| TEMPLATE | `sql_templates/template_generate_per_app_metrics_for_report_date.sql` |

NOTE: There is a separate table for each BB2 ENV enviornment (prod, impl).

### Summary:

* This is a JOIN of top level BB2 stats from the `bb2.events_<impl|prod>_perf_mon` table with type="global_state_metrics_per_app" and per application stats from the same table with type="global_state_metrics".

* This returns sets of data grouped by `vpc`, and `report_date`.

* This excludes our internal use applications from the per application results.

* To test or view results via the Athena SQL editor use the following for the target ENV:
  ```sql
  /* Show all records */
  SELECT * FROM "bb2"."<impl|prod>_global_state_per_app"
  ```
  ```sql
  /* Show distinct report dates */
  SELECT DISTINCT report_date FROM "bb2"."<impl|prod>_global_state_per_app"
  ```

## Top Level Metrics Table<a id="top-level-table"></a>

This table contains one row per report date (`report_date` column). It contains metrics related to the entire API system as a whole. A `sum()` of metrics from the per application table is also included. 

|            |                                                                     |
| ---------- | ------------------------------------------------------------------- |
| TABLE NAME | \<impl\|prod\>_global_state                           |
| TEMPLATE | `sql_templates/template_generate_metrics_for_report_date.sql` |

Summary:

* This utilizes the previously run `<impl|prod>_global_state_per_app` table results to provide counts of apps and bene data grouped by `vpc` and `report_date`.

* To test or view results via the Athena SQL editor use the following for the target ENV:
  ```sql
  /* Show all records */
  SELECT * FROM "bb2"."<impl|prod>_global_state"
  ```
  ```sql
  /* Show distinct report dates */
  SELECT DISTINCT report_date FROM "bb2"."<impl|prod>_global_state"
  ```

# Weekly Update of the Metric Tables<a id="weekly-update-tables"></a>

A scheduled AWS Lambda function is used to update the tables used for dashboards and anylysis in AWS QuickSight.

The lambda function, templates and development tools are included in the `insights/lambdas/update_athena_metric_tables` directory.

The files in this directory are:

- `lambda_function.py`:
  - Python Lambda function setup on a schedule in the BFD AWS account.
    - It takes parameters for the target enviornment (prod/impl) and metrics reporting table basenames used.
    - There is one scheduled call per environment and is run weekly.
  - This can be tested in local development. See [Lambda Function to Update Metrics Tables](#lambda-function) for more info.
- `sql_templates/template_generate_per_app_metrics_for_report_date.sql`:
  - Used to generate the per application metrics.
  - One row per application is produced.
    - These can be linked together using the `vpc` and `report_date` fields. 
- `sql_templates/template_generate_metrics_for_report_date.sql`:
  - Used to generate the top-level metrics for the report week.
  - One row is produced with the week's metrics.
- `test_run_sql_template_on_athena.py`:
  - Used from the command line to run and test Athena SQL queries using a template. With this you can preview the SQL template results with out updating the reporting tables.
  - This is a tool useful when developing and adding new metrics to the template files.
- `alter_table_schema_for_new_metrics_added.py`:
  - Utility to ALTER a TARGET table with schema changes from a SOURCE table (using
the column differences).
  - After developing new metrics, this utility is used to alter (add new columns) to the main tables used by QuickSight.
  - NOTE: There is a bug with using this to alter the table when adding a `boolean` field. See the backlog Jira story [BB2-3133](https://jira.cms.gov/browse/BB2-3133) for more details and upcoming fix.
- `test_lambda_function_local.py`:
  - Utility to test the `lambda_handler()` in `lambda_function.py` in local development.


# HOW-TO: Testing SQL Templates Individually<a id="how-to-test-sql-templates"></a>

This command line tool can be used to test out the templates individually with out updating the reporting tables.

The Python program is:  `test_run_sql_template_on_athena.py`

The command's help (via `-h` option) has info about the parameters that can be used.

The following is an example of usage for testing out the per application template using `report_date = "2023-01-09"` and `vpc = "prod"`:

```
clear;rm out.csv; python test_run_sql_template_on_athena.py -t 2023-01-09 -e prod -i sql_templates/template_generate_per_app_metrics_for_report_date_testing1.sql -o out.csv
```

NOTE: When using the `-o` option, the results are also written to a CSV file for your review. You can also view the query results and run time information via the AWS console Athena Query Editor.

NOTE: When working on the main top level SQL template, it is expecting an entry to already exist in the related per application metric table. The `-b` option can be used to specifiy the table you are working with. For example:  `-b global_state_per_app_testing1`.

```
clear;rm out.csv; python test_run_sql_template_on_athena.py -t 2023-01-09 -e prod -i sql_templates/template_generate_per_app_metrics_for_report_date.sql -o out.csv -b global_state_per_app
```

Another useful command line option is the `-a` for appending SQL at the end of the rendered template. When testing you can use this to add a WHERE clause to select only the results of interest. The following are some examples:
- To see results for the `TEST_APP` application only add this to the command:
```
-a "WHERE t0.name = 'TEST_APP'"
```
- To see results for the `app_sdk_requests_python_count` metric that have a result greater than zero:
  ```
  -a "WHERE app_sdk_requests_python_count > 0"
  ```
The per-application table is used by the top-level template. When you are finished adding new metrics to the per-application template and ready to update the top-level template, use the following command line option to create or update the table:
- Add the `--update-per-app-table` to the command line options.
- This is OFF by default.

# Lambda Function to Update Metrics Tables<a id="lambda-function"></a>

The Python Lambda program is:  `lambda_function.py`

## Summary:

- The Lambda function is installed in the BFD AWS account w/ BB2 permissions.
- On a weekly schedule, the function is called via an EventBridge schedule with parameters for the target VPC (impl/prod). 
- Runs a query to check if the per application table exists.
- Runs a query to check if entries exist for the `report_date`.
  - Skips updating the table, if entries exist.
- Updates or creates the per application table with results using the per application SQL template.
  - Retries this up to 3 times, in case there are time-out issues. Note that these occasionally occur for unknown reasons. Re-running the same query is usually successful.
- Runs a query to check if the top-level table exists.
- Runs a query to check if an entry exists for the `report_date`.
  - Skips updating the table, if an entry exists.
- Updates or creates the top-level table with results using the SQL template.
  - Retries this up to 3 times if there are time-out issues.

## Lambda Parameters:

A dictionary of parameters is passed in to the Lambda function.

These parameters are:

- REGION: AWS region. Ex. "us-east-1"
- WORKGROUP: The Athena workgroup. Ex. "bb2"
- DATABASE: The database. Ex. "bb2",
- ENV: The target BB2 environment (prod/impl). Ex. "prod"
- BASENAME_MAIN: The basename for the top-level table. Ex. "global_state",
- BASENAME_PER_APP: The basename for the per-application table. Ex. "global_state_per_app"
- TARGET_DATE: Target report week to generate metrics for. Ex. "2023-01-09"
  - If this is blank (""), the report week will be selected based on the current date.

### The following is an example dictionary:

```python
event = {
    "REGION": "us-east-1",
    "WORKGROUP": "bb2",
    "DATABASE": "bb2",
    "ENV": "prod",
    "BASENAME_MAIN": "global_state_new1",
    "BASENAME_PER_APP": "global_state_per_app_new1",
    "TARGET_DATE": "2023-01-09"
}
```
- NOTE: If TARGET_DATE = "", today's date will be used to determine the current report_date.



- The Lambda program can be run locally for development testing. See [Testing the New SQL Templates with the Lambda Function](#testing-templates-lambda).
- The main part of the code has an area where the `event` parameters can be setup and tested via a call to the `lambda_handler(event, context)`. Be sure to backup the main tables and update the table basenames used when running locally for development. For an example, see: [HOW-TO: A Walk-Through Example of Adding New Metrics](#how-to-new-metrics).


# HOW-TO: Update QuickSight DataSets<a id="how-to-update-qs-datasets"></a>

The following is a general procedure for updating QuickSight datasets. After the tables are updated in Athena, they must be refreshed (manually or scheduled) to be used in QuickSight.

1. Login to AWS Quicksight.

2. Go to the `Datasets` section.

3. Select the data set related to the view that was modified. You can also create a new view in a similar way.

4. Click on the `Refresh now` button. This start a refresh for the dataset.

5. If there are changes to field names, you will be prompted to map the old fields to the new ones.

6. Edit your existing Analyses that are utlizing the dataset related to the changes. Use the SHARE and PUBLISH to update the related dashboard.

  - NOTE: Terraform is now used for `test` and `prod` workspace versions of the DataSets. See this [README](../../../../quicksight/README.md) for more details.


# HOW-TO: A Walk-Through Example of Adding New Metrics<a id="how-to-new-metrics"></a>

In the following example, we are wanting to add a count metric related to our Python SDK usage.

We will be using a `report_date` of `2023-02-13` that contains metrics for the week of `2023-02-06` thru `2023-02-12`.

The BB2 API application log events will contain a field `req_header_bluebutton_sdk` that has a value of: "python", "node" or empty. This shows requests that were made using one of our SDKs.  This is a link to the related [code](https://github.com/CMSgov/bluebutton-web-server/blob/efdc585c87f787575c2c4f292401e95976cf0d7f/hhs_oauth_server/request_logging.py#L236)

We are wanting to get counts of usage at both the per application and top level (system wide).

Our new metric fields will be:

- Per application: 
  - `app_sdk_requests_python_count`
- Top level:
  - `sdk_requests_python_count`

Use Splunk to identify and find entries in the logs for the report_date period used: `2023-02-13`. We will use this Splunk search as an example for building our Athena SQL query. The following shows a count of `python` SDK requests:

```
index=bluebutton source="/var/log/pyapps/perf_mon.log*" env=impl message.type="request_response_middleware" message.req_header_bluebutton_sdk="python" | stats count
```

To begin, change your working directory via:
```
cd insights/terraform/projects/bb2/services/analytics/modules/lambda/update_athena_metric_tables
```

## Adding a New Per Application Metric<a id="how-to-add-per-app-metric"></a>

1. Identify a similar existing metric in the `sql_templates/template_generate_per_app_metrics_for_report_date.sql` template.
  - For example, we will use the metric `app_fhir_v1_call_synthetic_count`.
  - In the `enabled_metrics_list` list, comment out the other metrics besides this one or a few. A smaller list will make the query run faster.
  - Test that you are able to run the SQL using the following command line. For more info see: [HOW-TO: Testing SQL Templates Individually](#how-to-test-sql-templates).
  ```
  clear;rm out.csv; python test_run_sql_template_on_athena.py -t 2023-02-13 -e impl -i sql_templates/template_generate_per_app_metrics_for_report_date.sql -o out.csv
  ```
  - Review the results via the Athena console or out.csv file.
    - You should see values for this metric and 0 values for the ones disabled.

2. Add the new metric name to the `enabled_metrics_list`:
  - Add the following at the bottom while commenting out the other metrics.
  ```SQL
     /*
     ... other metrics listed ....
     ...
     'app_approval_view_post_fail_count',
     */
     'app_sdk_requests_python_count'
  ```
3. Add a COALESCE entry for the new metric. 
  - Search on `app_approval_view_post_fail_count` to find an example that is currently at the end of the list.
  - Add the new entry after the last one:
  ```sql
  COALESCE(t227.app_approval_view_post_fail_count, 0)
    app_approval_view_post_fail_count,
  COALESCE(t228.app_sdk_requests_python_count, 0)
    app_sdk_requests_python_count
  ``` 

4. Add a section with the SQL that counts the events in the middleware event logs. Use the example metric and Splunk search as a guide.
  - Add the following `LEFT JOIN` section at the end of the file for the `python` metric:
  ```sql
  LEFT JOIN
  (
    SELECT
      COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,'')) as app_name,
      count(*) as app_sdk_requests_python_count
    FROM
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'app_sdk_requests_python_count')

        AND req_header_bluebutton_sdk = 'python'
      )
    GROUP BY COALESCE(NULLIF(app_name,''), NULLIF(application.name,''),
        NULLIF(auth_app_name,''), NULLIF(req_app_name,''),
        NULLIF(resp_app_name,''))
  ) t228 ON t228.app_name = t0.name 
  ```
  - Test the results using the command line from #1.
  - To help with testing, you can append a WHERE clause that will only show results that are non-zero. Add the `-a APPEND_SQL` command line option to do this:
  ```
  clear;rm out.csv; python test_run_sql_template_on_athena.py -t 2023-02-13 -e impl -i sql_templates/template_generate_per_app_metrics_for_report_date.sql -o out.csv -a "WHERE app_sdk_requests_python_count > 0"
  ```
5. Verify the results vs. the results in Splunk. If successful, we are ready to continue working on the top-level metric.

6. Add the full list of metrics back to the `enabled_metrics_list`.
  - This is by removing the comments around the disabled metrics from #2.

7. Create or Update the per application table you are using for testing.
  - This table is utilized in the top level template we will be updating in the next section. 
  - NOTE: This SQL will take longer to run with the full metrics list enabled. 
  - For our example, we will be using the following setting for our Lambda function testing later:
  ```python
    "BASENAME_PER_APP": "global_state_per_app_testing1",
  ```
  - Run the command line to create or update the per applications table in Athena. 
    - Add the `--update-per-app-table` option to the command line:
    ```
    clear; python test_run_sql_template_on_athena.py -t 2023-02-13 -e impl -i sql_templates/template_generate_per_app_metrics_for_report_date.sql --update-per-app-table
    ``` 
    - This will create or update the table.
    - The default table is `global_state_per_app_testing1`. Use `-b BASENAME_PER_APP` option, if you want to use a different basename.
    - Use the folowing SQL in the Athena query editor to verify the update was successful:
    ```SQL
    SELECT * FROM bb2.impl_global_state_per_app_testing1
    ```
    - If you have a need to remove/drop the table and re-create it, use the following command:
    ```SQL
    DROP TABLE bb2.impl_global_state_per_app_testing1
    ```

8. Adding a new metric to the PER APPLICATION SQL template is COMPLETE!
  - We are ready to add the related top-level metrics in the next section.


## Adding a New Top Level Metric<a id="how-to-add-top-level-metric"></a>

1. Identify that similar existing metric from #1 in the previous section in the top-level template.
  - This in the `sql_templates/template_generate_metrics_for_report_date.sql` template.
  - For example, we will use the metric `fhir_v1_call_synthetic_count`.
  - In the `enabled_metrics_list` list, comment out the other metrics besides this one or a few. A smaller list will make the query run faster.
  - Test that you are able to run the SQL using the following command line. See previous details in this README for more info about this command.
  ```
  clear;rm out.csv; python test_run_sql_template_on_athena.py -t 2023-02-13 -e impl -i sql_templates/template_generate_metrics_for_report_date.sql -o out.csv
  ```
  - Add the `-b BASENAME_PER_APP` option to the command, if not using the `global_state_per_app_testing1` default value. 
  - Review the results via the Athena console or out.csv file.

2. Add the new metric name to the `enabled_metrics_list`:
  - Add the following at the bottom while commenting out the other metrics.
  ```SQL
     /*
     ... other metrics listed ....
     ...

     'auth_demoscope_not_required_deny_synthetic_bene_count',
     */
     'sdk_requests_python_count'
  ```

3. Add a section with the SQL that counts the events in the middleware event logs. Use the example metric and Splunk search as a guide.
  - Add the following section at the end of the related SELECT block.
    - This is a [link](https://github.com/CMSgov/bluebutton-web-server/blob/329056cf4027ae29e22e75a33659fd84a501547e/insights/lambdas/update_athena_metric_tables/sql_templates/template_generate_metrics_for_report_date.sql#L1314) to the location of the the current `auth_demoscope_not_required_deny_synthetic_bene_count` last metric. 
    - We will add our new SQL after this line. This last line will be shown below before the new SQL code to help with syntax.
  ```sql
  ) as auth_demoscope_not_required_deny_synthetic_bene_count,
  (
    select
      count(*)
    from
      request_response_middleware_events
    WHERE
      (
        CONTAINS((SELECT enabled_metrics_list FROM report_params),
          'sdk_requests_python_count')
        AND req_header_bluebutton_sdk = 'python'
      )
  ) as sdk_requests_python_count
  ```
  - Test the results using the command line from #1.

4. Add a SUM() of of the per-application metrics to the `global_state_metrics_per_app_for_max_group_timestamp` sub-select section.
  - This performs a SUM() on the values in the per-application table. It should match or closely match the `sdk_requests_python_count` top-level metrics---minus counts for internal applications.
  - Currently the last SUM() metric is `app_all_approval_view_post_fail_count` located [here](https://github.com/CMSgov/bluebutton-web-server/blob/329056cf4027ae29e22e75a33659fd84a501547e/insights/lambdas/update_athena_metric_tables/sql_templates/template_generate_metrics_for_report_date.sql#L608).
  - The SUM() type metrics are prefixed with "`app_all_`".
  - We will add our new SQL after this line. This last line will be shown below before the new SQL code to help with syntax.
  ```sql
      ) app_all_approval_view_post_fail_count,
      "sum"(
       app_sdk_requests_python_count
      ) app_all_sdk_requests_python_count
  ```
  - Test the results using the command line from #1.

5. Verify the results vs. the results in Splunk. If successful, we are ready to test the new templates using the Lambda function.

6. Add the full list of metrics back to the `enabled_metrics_list`.
  - This is by removing the comments around the disabled metrics from #2.

7. Adding a new metric to the TOP LEVEL SQL template is COMPLETE!
  - We are ready to test the templates with the Lambda function!

## Testing the New SQL Templates with the Lambda Function<a id="testing-templates-lambda"></a>

The Python Lambda program is:  `lambda_function.py`

There is a command line utility that can be used to test the `lambda_handler()` function.

This utility is:  `test_lambda_function_local.py`


1. Determine the parameters to match those you have been using so far.
  - The following is the `event` we are wanting to test and pass to the `lambda_hanlder()`.
  ```python
  event = {
      "REGION": "us-east-1",
      "WORKGROUP": "bb2",
      "DATABASE": "bb2",
      "ENV": "impl",
      "BASENAME_MAIN": "global_state_testing1",
      "BASENAME_PER_APP": "global_state_per_app_testing1",
      "TARGET_DATE": "2023-02-13",
  }
  ```
2. Run and test the Lambda locally.
  - Run the following command line:
    ```
    clear; python test_lambda_function_local.py  -e impl -t 2023-02-13 -m global_state_testing1 -p global_state_per_app_testing1
    ```
  - NOTE: From your previous testing, the per-application table may already be updated and skipped. The top-level table will be updated or created.
  - Use the folowing SQL in the Athena query editor to verify the update was successful:
    ```SQL
    SELECT * FROM bb2.impl_global_state_testing1
    ```
    ```SQL
    SELECT * FROM bb2.impl_global_state_per_app_testing1
    ```
3. Test the Lambda using other `ENV` settings. In this example, we used `impl` for Sandbox. For production use `"ENV": "prod"`.

4. Verify the results! If successful, we are ready to add the new fields to the production tables and optionally regenerate metrics for previous report dates---if desired.


## Adding New Fields to the Tables Used for QuickSight

The metrics tables used for QuickSight will need to be altered to support inserting entries with the new fields we created.

### Athena Tables Used by QuickSight:

  | ENV  | TableName   |
  |------|---------------------------|
  | impl | impl_global_state_per_app |     
  | impl | impl_global_state         |     
  | prod | prod_global_state_per_app |     
  | prod | prod_global_state         |     


The following is a summary of the procedure:
  - Backup the current tables for `impl`, and `prod` enviornments.
  - Create a working copy of the tables.
    - The report date entries that we wish to recreate can be excluded from the copy, if desired.
    - We announced our SDKs on 12/21/2022, so we will recreate our metrics for the report dates from 2022-12-26 thru 2023-02-13 (current).
    - We will copy entries with a `report_date` before 2022-12-26.
  

1. BACKUP the current metrics tables using the AWS Athena Query Editor.
  - **Repeat for the prod enviornment too.**
  - Replace "2023_02_17" with your date.
  - This is to create a backup of tables, just in case!
    - These backups can be used to restore metrics history in case something went wrong.

  ```sql
  /* Main (top-level) metrics table backup. */
  CREATE TABLE bb2.impl_global_state_bak_2023_02_17 AS
    SELECT * FROM bb2.impl_global_state
  ```
  ```sql
  /* Show report_date entries */
  SELECT DISTINCT report_date FROM bb2.impl_global_state_bak_2023_02_17
  ```

  ```sql
  /* PER_APP metrics table backup. */
  CREATE TABLE bb2.impl_global_state_per_app_bak_2023_02_17 AS
    SELECT * FROM bb2.impl_global_state_per_app
  ```
  ```sql
  /* Show report_date entries */
  SELECT DISTINCT report_date FROM bb2.impl_global_state_per_app_bak_2023_02_17
  ```

2. Copy the current metrics tables to the following.
  - Table names:
    - bb2.impl_global_state_copy1
    - bb2.impl_global_state_per_app_copy1
  - **Repeat for the prod enviornment too.**
  - These will have the schema BEFORE you added the new metrics.
  - We will copy entries with a `report_date` before 2022-12-26, so that we can recreate the desired entries since our SDK announcement.
  - Drop the `_copy1` tables, if they already exist.
  ```sql
  DROP TABLE bb2.impl_global_state_copy1
  ```
  ```sql
  DROP TABLE bb2.impl_global_state_per_app_copy1
  ```
  - Use the following SQL commands to copy the tables. Repeat for `prod`!
  ```sql
  CREATE TABLE bb2.impl_global_state_copy1 AS
    SELECT * FROM bb2.impl_global_state
      WHERE report_date < CAST('2022-12-26' as Date)
  ```
  ```sql
  /* Show report_date entries */
  SELECT DISTINCT report_date FROM bb2.impl_global_state_copy1
  ```
  ```sql
  CREATE TABLE bb2.impl_global_state_per_app_copy1 AS
    SELECT * FROM bb2.impl_global_state_per_app
      WHERE report_date < CAST('2022-12-26' as Date)
  ```
  ```sql
  /* Show report_date entries */
  SELECT DISTINCT report_date FROM bb2.impl_global_state_per_app_copy1
  ```

3. Review the SCHEMA for the `_testing1` tables with the new metric fields.
  - The following command is useful for comparing the schemas with the new metric field:
    ```sql
    SHOW COLUMNS FROM bb2.impl_global_state_testing1
    ```
    ```sql
    SHOW COLUMNS FROM bb2.impl_global_state_per_app_testing1
    ```

4. Use the `alter_table_schema_for_new_metrics_added.py` tool to ALTER the table.

This command line tool can be used to ALTER a TARGET table with the schema changes of a SOURCE table. Note that this tool only works for additions and not removing columns.

In our example, we want to alter the `_copy1` table with the additinal columns we added to the `_testing1` table.

Repeat the following for the `prod` environment.

  - Review the per application table with the following command:
  ```
  clear; python alter_table_schema_for_new_metrics_added.py  -s "impl_global_state_per_app_testing1" -t "impl_global_state_per_app_copy1"
  ``` 
  - Verify that the expected column changes are OK.
    - There should be one column to add: `app_sdk_requests_python_count`.
  - If OK, ALTER the table by adding the `--alter-table` command line option.  
  ```
  clear; python alter_table_schema_for_new_metrics_added.py  -s "impl_global_state_per_app_testing1" -t "impl_global_state_per_app_copy1" --alter-table
  ``` 

  - Review the top-level table with the following command:
  ```
  clear; python alter_table_schema_for_new_metrics_added.py  -s "impl_global_state_testing1" -t "impl_global_state_copy1"
  ``` 
  - Verify that the expected column changes are OK.
    - There should be two columns to add: `app_sdk_requests_python_count` and `app_all_sdk_requests_python_count`.
  - If OK, ALTER the table by adding the `--alter-table` command line option.  
  ```
  clear; python alter_table_schema_for_new_metrics_added.py  -s "impl_global_state_testing1" -t "impl_global_state_copy1" --alter-table
  ``` 

5. Determine the parameters to match those you have been using so far.

In this step we will be verifying that the lambda can update our `_copy1` tables that will be replacing the main ones, after re-running metrics for report dates since 2022-12-26.

  - Determine the parameters to match those you have been using so far:

  ```python
  event = {
      "REGION": "us-east-1",
      "WORKGROUP": "bb2",
      "DATABASE": "bb2",
      "ENV": "impl",
      "BASENAME_MAIN": "global_state_copy1",
      "BASENAME_PER_APP": "global_state_per_app_copy1",
      "TARGET_DATE": "2022-12-26"
  }
  ```
  - Run the lambda function to test locally with the `test_lambda_function_local.py` utility program:
  ```
  clear; python test_lambda_function_local.py  -e impl -t 2022-12-26 -m global_state_copy1 -p global_state_per_app_copy1
  ```

6. Verify the rows were added to the `_copy1` tables.

  - Verify per application rows.
  ```
  SELECT * FROM bb2.impl_global_state_per_app_copy1
    WHERE report_date = CAST('2022-12-26' as Date)
  ```
  - Verify top-level rows.
  ```
  SELECT * FROM bb2.impl_global_state_copy1
    WHERE report_date = CAST('2022-12-26' as Date)
  ```

7. Run for additional report dates after 2022-12-26 up until the current.

  - NOTE: Multiple target dates can be added to the list. This is useful when regenerating the metrics entries for desired report dates, after new metrics are developed and added. 

  - Use the `-t / --target-report-dates` option with the `test_lambda_function_local.py` utility to use the lambda program locally to update the `_copy1` table with entries.
  - For example, we can update a batch of 4 report_dates with entries from 2022-12-26 thru 2023-01-16 with the following command line:
  ```
  clear; python test_lambda_function_local.py  -e impl -t 2022-12-26,2023-01-02,2023-01-09,2023-01-16 -m global_state_copy1 -p global_state_per_app_copy1
  ```

8. Verify the new rows were added to the `_copy1` tables since 2022-12-26.

  - Verify per application rows.
  ```
  SELECT * FROM bb2.impl_global_state_per_app_copy1
    WHERE report_date > CAST('2022-12-26' as Date)
  ```
  - Verify top-level rows.
  ```
  SELECT * FROM bb2.impl_global_state_copy1
    WHERE report_date > CAST('2022-12-26' as Date)
  ```

9. Review if there are any missing `report_date` entries.

  - Verify per application:
  ```sql
  SELECT DISTINCT report_date FROM bb2.impl_global_state_per_app_copy1
  ```
  - Verify top-level rows:
  ```sql
  SELECT DISTINCT report_date FROM bb2.impl_global_state_copy1
  ```
  - If there are missing entries, follow the instruction in #7 to update them.

10. Copy the `_copy1` tables in to place for use in QuickSight.

  - Verify that report date entries are complete per #9 for each table for `impl` and `prod` enviornments.
  - Verify that the related tables have been backed up per #1.
  - Remove the main tables using the following Athena SQL example:
  - NOTE: Terraform is now used for `test` and `prod` workspace versions of the DataSets. See this [README](../../../../quicksight/README.md) for more details.
    - Before adding new metrics to the `prod` workspace, use the `test` workspace for your development, testing and review!
    - The Athena tables with a `_test` appended are connected with the `-test` versions of the DataSets in Quicksight.
    - When ready to apply to the `prod` workspace, redo the following SQL with the "_test" part removed.
      - For example, "impl_global_state_test" would become "impl_global_state".

  ```sql
  DROP TABLE bb2.impl_global_state_test
  ```
  - Copy the `_copy1` version of the table in to place:
  ```sql
  CREATE TABLE bb2.impl_global_state_test AS
    SELECT * FROM bb2.impl_global_state_copy1
  ```
  - Repeat this for each table that was updated.

11. View the changes in QuickSight analyses

  - Follow [HOW-TO: Update QuickSight DataSets](#how-to-update-qs-datasets).
  - Refresh the datasets for each of the tables.
  - Update the related Terraform code in the DataSet & Analysis modules "main.tf" files.
    - Some tipes for working on this:
      - Look for a similar metric in the related main.tf files and use that as an example for adding the new metric TF code.
        - The code can vary depending on where inside a sheet a new metric is being located or what type of visual used. So finding a similar metric as an example is useful.
      - You can also add and place a new metric via the Quicksight UI. Afterward run a "terraform plan" to see what changes match up and use this to create the related TF code in the main.tf files.
  - In QuickSight, view the `BB2-DASG-Metrics-test` and `PROD-APPLICATIONS-test` analyses to verify that the new metrics are showing.
    - For production versions, view the `BB2-DASG-Metrics-prod` and `PROD-APPLICATIONS-prod` analyses to verify that the new metrics are showing.
  - Follow the "Publishing Dashboards Using Analyses" section in the [/quicksight/README.md](../../../../quicksight/README.md) for how to use the resulting analyses to publish the related dashboards.