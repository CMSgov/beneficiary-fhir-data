# BFD Insights: BFD Dashboards

BFD Insights captures data in near-real-time from the EC2 instances and provides the data for
analysis in QuickSight.

![Resource Diagram](docs/unique-bene-workflow.svg)

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
to api-requests/glue.tf and then the tables will need to be updated in AWS. The procedure is:

1. Add the new column to terraform.
2. Run terraform apply.
3. To verify that the new column has been added, wait about fifteen minutes to be sure that some
log files have been processed via Kinesis Firehose, and then run a quick Athena query such as:

```sql
SELECT
  "cw_id",
  "cw_timestamp",
  "<new_field>"
FROM
  "bfd_insights_bfd_<underscore_environment>_api_requests"
WHERE
  "<new_field>" IS NOT NULL
LIMIT 50;
```

## Manual Ingestion of Log Files

This process will be done via a series of complex Athena queries, in order to reduce the amount of
AWS Glue we have to perform. This approach is far more cost-effective and faster.

**TODO: Add a reference to the runbook once completed.**

## Beneficiaries

Beneficiaries is the portion that selects the beneficiary and timestamp from the API-Requests
table. Beneficiaries-Unique (which is included within this portion of BFD Insights) includes the
calculations of when each beneficiary was first queried.

### Structure

```mermaid
flowchart TD
    APIRequests["Glue Table: API Requests"] -->|Glue Job: Populate Beneficiaries| Beneficiaries["Glue Table: Beneficiaries"]
    Beneficiaries -->|Glue Job: Populate Beneficiary Unique| BeneUnique["Glue Table: Beneficiary Unique"]
    BeneUnique --> DataSet["QuickSight: DataSet"]
    DataSet --> Analysis["QuickSight: Analysis"]
    Analysis --> Dashboard["QuickSight: Dashboard"]
```

### Athena Views

Instead of using AWS Glue to build intermediate tables, we use Athena views to standardize data collection to some degree. Below are the `CREATE` statements used to build the views:

```sql
CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_partners" AS
    SELECT
        mdc_bene_id AS benes,
        CASE "mdc_http_access_request_clientssl_dn"
            WHEN 'C=US, CN=dpc.prod.client' then 'dpc'
            WHEN 'C=US,CN=dpc.prod.client' then 'dpc'
            WHEN 'CN=bluebutton-backend-prod-data-server-client-test' then 'bfd_test'
            WHEN 'CN=bcda-prod-client' then 'bcda'
            WHEN 'CN=ab2d-prod-client' then 'ab2d'
            WHEN 'CN=ab2d-prod-validation-client' then 'ab2d_test'
            WHEN 'EMAILADDRESS=ryan@adhocteam.us, CN=BlueButton Root CA, OU=BlueButton on FHIR API Root CA, O=Centers for Medicare and Medicaid Services, L=Baltimore, ST=Maryland, C=US' then 'bb2'
            WHEN '1.2.840.113549.1.9.1=#16117279616e406164686f637465616d2e7573,CN=BlueButton Root CA,OU=BlueButton on FHIR API Root CA,O=Centers for Medicare and Medicaid Services,L=Baltimore,ST=Maryland,C=US' then 'bb2'
            WHEN 'CN=bfd.cms.gov' then 'bfd'
            ELSE 'unknown'
        END AS partner,
  *
  FROM "bfd_insights_bfd_prod_api_requests" AS api_requests;
```

```sql
CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_beneficiaries" AS
    SELECT
        TRY(CAST(TRIM(bene) as bigint)) as bene,
        "bfd_insights_bfd_prod_partners".*
    FROM "bfd_insights_bfd_prod_partners"
    CROSS JOIN UNNEST(split(benes, ',')) as x(bene);
```

```sql
CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_monthly_unique_benes" AS
    SELECT
        first_date,
        COUNT(*) AS num_benes
    FROM (
        SELECT 
            bene,
            DATE_FORMAT(from_iso8601_timestamp(MIN("timestamp")), '%Y-%m') AS first_date
        FROM
            "bfd_insights_bfd_prod_beneficiaries"
        WHERE
            bene > 0
        GROUP BY bene
    )
    GROUP BY first_date
    ORDER BY first_date;
```
