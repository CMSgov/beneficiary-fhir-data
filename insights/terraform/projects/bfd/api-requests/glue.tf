# Creates AWS Glue Database named "bfd-insights-bfd-<environment>"
module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk = data.aws_kms_key.kms_key.arn
  tags       = local.tags
}

# API Requests
#
# Target location for ingested logs, no matter the method of ingestion.

# Target Glue Table where ingested logs are eventually stored
module "api-requests-table" {
  source         = "../../../modules/table"
  table          = "${local.full_name_underscore}_api_requests"
  description    = "Target Glue Table where ingested logs are eventually stored"
  database       = module.database.name
  bucket         = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk     = data.aws_kms_key.kms_key.arn
  storage_format = "parquet"
  serde_format   = "parquet"
  tags           = local.tags

  partitions = [
    {
      name    = "year"
      type    = "string"
      comment = "Year of request"
    },
    {
      name    = "month"
      type    = "string"
      comment = "Month of request"
    },
    {
      name    = "day"
      type    = "string"
      comment = "Day of request"
    }
  ]

  # Don't specify here, because the schema is complex and it's sufficient to
  # allow the crawler to define the columns
  columns = [] 
}

# Crawler for the API Requests table
resource "aws_glue_crawler" "api-requests-crawler" {
  classifiers   = []
  database_name = module.database.name
  configuration = jsonencode(
    {
      CrawlerOutput = {
        Partitions = {
          AddOrUpdateBehavior = "InheritFromTable"
        }
      }
      Grouping = {
        TableGroupingPolicy = "CombineCompatibleSchemas"
      }
      Version = 1
    }
  )
  name     = "${local.full_name}-api-requests-crawler"
  role     = data.aws_iam_role.glue-role.arn

  catalog_target {
    database_name = module.database.name
    tables = [
      module.api-requests-table.name,
    ]
  }

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}


# BFD History
#
# Storage and Jobs for ingesting unaltered json log files.

# Glue Table to store API History
module "api-history-table" {
  source         = "../../../modules/table"
  table          = "${local.full_name_underscore}_api_history"
  description    = "Store log files from BFD for analysis in BFD Insights"
  database       = module.database.name
  bucket         = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk     = data.aws_kms_key.kms_key.arn
  storage_format = "json"
  serde_format   = "grok"
  serde_parameters = {
    "input.format" = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
  tags           = local.tags
  partitions = [
    {
      name    = "partition_0"
      type    = "string"
      comment = ""
    },
    {
      name    = "partition_1"
      type    = "string"
      comment = ""
    }
  ]

  # Don't specify here, because the schema is complex and it's sufficient to
  # allow the crawler to define the columns
  columns = []
}

# Glue Crawler for the API History table
resource "aws_glue_crawler" "bfd-history-crawler" {
  database_name = module.database.name
  name          = "${local.full_name}-history-crawler"
  description   = "Glue Crawler to ingest logs into the API History Glue Table"
  role          = data.aws_iam_role.glue-role.arn

  classifiers = [
    aws_glue_classifier.bfd-historicals-local.name,
  ]

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  catalog_target {
    database_name = module.database.name
    tables        = [ module.api-history-table.name ]
  }

  schema_change_policy {
    delete_behavior = "LOG" # "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }

  configuration = jsonencode(
    {
      "Version": 1.0,
      "Grouping": {
        "TableGroupingPolicy": "CombineCompatibleSchemas"
      }
    }
  )
}

# Classifier for the History Crawler
resource "aws_glue_classifier" "bfd-historicals-local" {
  name = "${local.full_name}-historicals-local"

  grok_classifier {
    classification = "cw-history"
    grok_pattern   = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
}

# S3 object containing the Glue Script for history ingestion
resource "aws_s3_object" "bfd-history-ingest" {
  bucket             = data.aws_s3_bucket.bfd-insights-bucket.id
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/${local.environment}/bfd_history_ingest.py"
  metadata           = {}
  storage_class      = "STANDARD"
  source_hash        = filemd5("glue_src/bfd_history_ingest.py")
  source             = "glue_src/bfd_history_ingest.py"
}

# Glue Job for history ingestion
resource "aws_glue_job" "bfd-history-ingest-job" {
  name                      = "${local.full_name}-history-ingest"
  description               = "Ingest historical log data"
  connections               = []
  glue_version              = "3.0"
  max_retries               = 0
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = data.aws_iam_role.glue-role.arn
  timeout                   = 2880
  worker_type               = "G.1X"

  default_arguments = {
    "--TempDir"                          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-enable"
    "--job-language"                     = "python"
    "--spark-event-logs-path"            = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/sparkHistoryLogs/${local.environment}/"
    "--tempLocation"                     = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/temp/${local.environment}/history-ingest/"
    "--sourceDatabase"                   = module.database.name
    "--sourceTable"                      = module.api-history-table.name
    "--targetDatabase"                   = module.database.name
    "--targetTable"                      = module.api-requests-table.name
  }

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-history-ingest.bucket}/${aws_s3_object.bfd-history-ingest.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}


# Glue Workflow
#
# Organizes the Glue jobs / crawlers and runs them in sequence

# Glue Workflow Object
resource "aws_glue_workflow" "glue-workflow" {
  name = "${local.full_name}-api-requests-workflow"
  max_concurrent_runs = "1"
}

# Trigger for History Ingest Crawler. This will run every night at 4am UTC, but it can also be run
# manually through the Console
resource "aws_glue_trigger" "history-ingest-crawler-trigger" {
  name          = "${local.full_name}-history-ingest-crawler-trigger"
  workflow_name = aws_glue_workflow.glue-workflow.name
  type          = "SCHEDULED"
  schedule      = "cron(0 4 * * ? *)" # Every day at 4am UTC

  actions {
    crawler_name = aws_glue_crawler.bfd-history-crawler.name
  }
}

# Trigger for History Ingest Job
resource "aws_glue_trigger" "bfd-history-ingest-job-trigger" {
  name          = "${local.full_name}-history-ingest-trigger"
  description   = "Trigger to start the History Ingest Glue Job whenever the Crawler completes successfully"
  workflow_name = aws_glue_workflow.glue-workflow.name
  type          = "CONDITIONAL"

  actions {
    job_name = aws_glue_job.bfd-history-ingest-job.name
  }

  predicate {
    conditions {
      crawler_name = aws_glue_crawler.bfd-history-crawler.name
      crawl_state  = "SUCCEEDED"
    }
  }
}

# Trigger for API Requests Crawler
resource "aws_glue_trigger" "bfd-api-requests-crawler-trigger" {
  name          = "${local.full_name}-api-requests-crawler-trigger"
  description   = "Trigger to start the API Requests Crawler whenever the History Ingest Job completes successfully"
  workflow_name = aws_glue_workflow.glue-workflow.name
  type          = "CONDITIONAL"

  actions {
    crawler_name = aws_glue_crawler.api-requests-crawler.name
  }

  predicate {
    conditions {
      job_name = aws_glue_job.bfd-history-ingest-job.name
      state  = "SUCCEEDED"
    }
  }
}
