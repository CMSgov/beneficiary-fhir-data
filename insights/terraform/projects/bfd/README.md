# BFD Insights: BFD Dashboards

BFD Insights captures data in near-real-time from the EC2 instances and provides the data for
analysis in QuickSight.

![Resource Diagram](docs/unique-bene-workflow.svg)

## Known Data Discrepencies

While the goal is to completely capture all historical and real-time data, sometimes errors occur.
The following is a list of known discrepencies in the data, with dates and reasons.

* 09/02/22 - 09/09/22: Due to a typo in the column name, the
`mdc_jpa_query_eobs_by_bene_id_snf_record_count` field was not being captured.
* 10/11/22 - 11/18/22: The EOB calls have some cases where zero is returned for `mdc_resources_returned_count` even though there are in fact more than zero resources 
returned in the payload.

## Known data limitations for bene_id

The bene_id field is populated on requests for Patient, Coverage, and ExplanationOfBenefit resources
and contains a list of all beneficiary IDs whose data was either explicitly requested or whose data
was returned from a call to get Patient resources by contract. The support for this field has evolved
over time as follows:

* 03/26/2021 - Initial support added that included support for requests that return resources for a single beneficiary. (BFD-693)
* 10/28/2021 - Support added for `Patient By Contract` requests which typically return multiple beneficiaries per call. (BFD-1209)
* 07/22/2022 - Fixed a defect that was causing Coverage `read` operations to not log the beneficiary ID. (BFD-1897)
* 09/15/2022 - Fixed a defect that was causing some requests that resulted in empty responses from logging the beneficiary ID. (BFD-2119)

## API-Requests

API-Requests is the portion of the project that ingests the logs and stores them in Glue tables.
Normally, this happens in real time through AWS Kinesis Firehose, but it can also be done manually
by exporting logs from CloudWatch and running a Glue Job to ingest them into the API-Requests
table. Most other parts of this project will depend upon API-Requests.

## Naming Conventions

### AWS Resources

Somewhere in the BFD documentation (which I cannot presently find; please update if found), there
was a convention to name AWS resources to clearly identify that the resource belongs to BFD
Insights and to which project (BFD, BB2, AB2D, etc.), plus an identifier for the environment (prod,
prod-sbx, test). The convention is: `bfd-insights-<project>-<environment>-<identifier>` in kebab
case (lower-case words separated by hyphens). The exception is for AWS Glue / Athena table names,
which must be in snake case (lower-case separated by underscores), because the hyphen is not a
valid character in Athena table names. For example, we have
`bfd-insights-bfd-prod-sbx-firehose-ingester` and `bfd-insights-bfd-test-api-requests-crawler`.
However, for Glue Tables, we have `bfd_insights_bfd_prod_sbx_api_requests`.

### Terraform Resources

The terraform resource names do not need to be labeled with the
`bfd-insights-<project>-<environment>-` prefix, because it should be clear from context what project
they belong in, and environment is derived from the workspace. However, we have decided on a naming
convention like `<function>-<identifier>` in kebab case (lower-case words separated by hyphens), so
that even the modules, which do not clearly indicate the type of AWS resource they represent, will
be clear. For example, we have `module.glue-table-api-requests` and
`aws_glue_crawler.glue-crawler-api-requests`.

## Adding new columns

The `api_requests` table has hard-coded column fields, which is unavoidable due to limitations in
Kinesis Firehose's format_conversion feature.

Based on historical log files, this list is meant to contain every field ever used *so far*.
However, when new fields are added to the FHIR server's log files, they will also need to be added
to api-requests/glue.tf and then the tables will need to be updated in AWS. It is perfectly fine
(preferable, maybe) to update the table schema _before_ the changes go live on the FHIR server.

The procedure for adding a new column is:

1. Add the new column to terraform.
2. Run terraform apply.
3. *IMPORTANT*: If you update the schema (such as adding a new column), you also need to be sure to
update the Views in Athena. See below, under Athena Views.
4. To verify that the new column has been added, wait about fifteen minutes to be sure that some
log files have been processed via Kinesis Firehose, and then run a quick Athena query such as these:

```sql
SELECT * FROM "bfd_insights_bfd_prod_daily_combined";
```

OR

```sql
`SELECT "<column>" FROM "bfd_insights_bfd_prod_partners" WHERE "<column>" IS NOT NULL;
```

## Manual Ingestion of Log Files

This process will be done via a series of complex Athena queries, in order to reduce the amount of
AWS Glue we have to perform. This approach is far more cost-effective and faster.

The process is outlined in this [Runbook](../../../../runbooks/how-to-load-cloudwatch-historical-data.md).

## Analysis

The Analysis section is handled through Athena views and QuickSight dashboards, and is designed to
be efficient and cost-effective.

```mermaid
flowchart TD
    CloudWatch["CloudWatch: Historical Logs"] -->|Manual Export| S3
    S3["S3 Bucket"] -->|Crawler: History| History["Glue Table: API History"]
    History -->|Glue Job: History Ingest| APIRequests["Glue Table: API Requests"]

    EC2["CloudWatch Log Subscription (Real-Time)"] --> Firehose["Kinesis Firehose"]
    Firehose -->|Lambda| History
```

## Athena Views

Instead of using AWS Glue to build intermediate tables, we use Athena views to standardize data
collection to some degree.

*IMPORTANT NOTES*:
  * You will have to re-create (or otherwise "refresh") each of these views whenever you update the
  underlying table schema for `api-requests` or you will get an error saying that the view is
  stale. You can copy/paste the queries below, or from the editor click "Insert into Editor" on the
  view.
  * The prod version of these Athena views is listed, as that is the only environment on which we
  are presently doing analysis. They will definitely need to be adapted if we choose to use these
  views / QuickSight in other environments.

**TODO**: When terraform supports Athena Views, put these into terraform.

[SQL for creating Athena views](./athena-queries/create_views.sql)

## QuickSight Dashboards

The QuickSight dashboards are the portion that displays the data to users. They rely heavily on
the Athena views and are set up to run once per day, just before midnight UTC.

Please see the
["How to Create BFD Insights QuickSight"](../../../../runbooks/how-to-create-bfd-insights-quicksight.md)
runbook for how to create these if they ever need to be recreated.
