# BFD Insights: BFD Dashboards

## Structure

## Manual Ingestion of Log Files


1. CloudWatch > Log Groups > `/bfd/<environment>/bfd-server/access.json`
    - Actions > Export Data to Amazon S3
        - Select Account: *This Account*
        - S3 Bucket Name: `bfd-insights/bfd-app-logs`
        - S3 Bucket Prefix: ``

2. AWS Glue > Crawlers > `bfd-<environment>-history-crawler`
    - Run Crawler

3. AWS Glue > Jobs > `bfd-<environment>-history-ingest`
    - Run.
    - Wait. This step might take an hour or more, depending on the volume of logs to ingest. In testing/development, it took about 45 minutes to run two weeks' worth of prod-sbx logs.

4. AWS Glue > Jobs > `bfd-<environment>-populate-beneficiaries`
    - Run.
