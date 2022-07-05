# BFD Insights: BFD Dashboards

BFD Insights captures data in near-real-time from the EC2 instances and provides the data for
analysis in QuickSight.

![Resource Diagram](docs/unique-bene-workflow-poc.svg)

## API-Requests

API-Requests is the portion of the project that ingests the logs and stores them in Glue tables.
Normally, this happens in real time through AWS Kinesis Firehose, but it can also be done manually
by exporting logs from CloudWatch and running a Glue Job to ingest them into the API-Requests
table. Most other parts of this project will depend upon API-Requests.

### Structure

```mermaid
flowchart TD
    CloudWatch["CloudWatch: Historical Logs"] -->|Manual Export| S3
    S3["S3 Bucket"] -->|Crawler: History| History["Glue Table: API History"]
    History -->|Glue Job: History Ingest| APIRequests["Glue Table: API Requests"]

    EC2["CloudWatch Log Subscription (Real-Time)"] --> Firehose["Kinesis Firehose"]
    Firehose -->|Lambda| APIRequests
```

### Manual Ingestion of Log Files

Note: Replace `<environment>` and `<account-number>` with the name of your environment, such as
`prod` or `prod-sbx`, and the AWS account number, respectively. Replace any `-` with `_` in
`<underscore_environment>` (Athena doesn't like hyphens in table names).

1. CloudWatch > Log Groups > `/bfd/<environment>/bfd-server/access.json`
    - Actions > Export Data to Amazon S3
        - Choose time period
        - Select Account: *This Account*
        - S3 Bucket Name: `bfd-insights-bfd-app-logs`
        - S3 Bucket Prefix: `history/temp-<environment>`.
    - Export. This took about 80 minutes for 3 weeks of prod-sbx logs.

2. S3
    - Select bucket `bfd-insights-bfd-app-logs`
    - Go to path `history/temp-<environment>`
    - Select all objects *except* `aws-logs-write-test`
    - Actions > Move
        - Bucket
        - Destination: `bfd-insights-bfd-<account-number>/databases/bfd-insights-bfd-<environment>/bfd_insights_bfd_<underscore_environment>_api_history/`
        - Move

3. AWS Glue > Workflows > `bfd-insights-bfd-<environment>-history-workflow`
    - Actions > Run. The entire workflow may take a bit to run through, but you can track progress in the graph: History > (choose the top item) > Run Details.

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

## Manual Creation of QuickSight Dashboards

Note: Replace `<environment>` with the name of your environment, such as `prod` or `prod-sbx`.
Replace any `-` with `_` in `<underscore_environment>` (Athena doesn't like hyphens in table
names).

1. Go to [QuickSight](https://us-east-1.quicksight.aws.amazon.com/).
2. Datasets. New Dataset.
    - Athena.
        - Name your data source. Example: `bfd-<environment>-beneficiaries`
        - Athena Workgroup: `bfd`
        - Create Data Source.
    - Choose Your Table.
        - Catalog: `AwsDataCatalog`
        - Database: `bfd-<environment>`
        - Table: Choose the one you want to query. Ex: `bfd_<underscore_environment>_beneficiaries`
        - Select.
    - Finish dataset creation.
        - Directly query your data.
        - Visualize.
3. Create an analysis.
    - Add a Count sheet (Unique Beneficiaries only)
        - Under Visual Types (on the left), select `Insight` (it looks like an old-school lightbulb with a lightning bolt)
        - Drag `bene_id` from the left to the chart.
        - Click on Customize Insight on the chart.
        - Computations > Add one.
        - Total aggregation. Next.
        - Select `bene_id` from the dropdown (it should already be selected by default). Add.
        - Save.
    - Add a Line Chart sheet.
        - Under Visual Types (on the left), select `Line Chart`.
        - Expand Field Wells at the top.
        - Drag `# bene_id` from the left to "Value" under the Field Wells.
        - Drag `timestamp` (beneficiaries table) or `last_seen` (beneficiaries_unique table) to the "X Axis" under the Field Wells.
        - In the upper-right, click Share > Publish Dashboard. Choose a name. Example: `bfd-<environment>-beneficiaries`. The default options should be fine, so click Publish Dashboard.
