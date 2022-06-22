# Database
#
# Glue database for all of the below Glue tables.

resource "aws_glue_catalog_database" "bfd-database" {
  name        = local.database
  description = "BFD Insights database for the ${local.environment} environment"
}


# API Requests
#
# Target location for ingested logs, no matter the method of ingestion.

# Target Glue Table where ingested logs are eventually stored
resource "aws_glue_catalog_table" "api-requests-table" {
  catalog_id    = data.aws_caller_identity.current.account_id
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "api-requests"
  retention  = 0
  table_type = "EXTERNAL_TABLE"

  partition_keys {
    name = "year"
    type = "string"
  }
  partition_keys {
    name = "month"
    type = "string"
  }
  partition_keys {
    name = "day"
    type = "string"
  }

  storage_descriptor {
    bucket_columns    = []
    compressed        = false
    input_format      = "org.apache.hadoop.mapred.TextInputFormat"
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/${local.database}/api-requests/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    stored_as_sub_directories = false

    ser_de_info {
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }
  }
}

# Crawler on a schedule to classify log files and ensure they are put into the Glue Table.
resource "aws_glue_crawler" "api-requests-recurring-crawler" {
  classifiers   = []
  database_name = aws_glue_catalog_database.bfd-database.name
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
  name     = "bfd-insights-bfd-${local.environment}-api-requests-recurring-crawler"
  role     = aws_iam_role.glue-role.name
  schedule = "cron(59 10 * * ? *)"
  tags     = {}
  tags_all = {}

  catalog_target {
    database_name = aws_glue_catalog_database.bfd-database.name
    tables = [
      aws_glue_catalog_table.api-requests-table.name,
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
# Storage and Jobs for manually ingesting historical logs.

# Glue Catalog Table to store API History
resource "aws_glue_catalog_table" "api-history" {
  catalog_id    = data.aws_caller_identity.current.account_id
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "api-history"
  owner         = "owner"
  retention  = 0
  table_type = "EXTERNAL_TABLE"

  partition_keys {
    name = "partition_0"
    type = "string"
  }
  partition_keys {
    name = "partition_1"
    type = "string"
  }

  storage_descriptor {
    bucket_columns    = []
    compressed        = true
    input_format      = "org.apache.hadoop.mapred.TextInputFormat"
    location          = "s3://${data.aws_s3_bucket.bfd-app-logs.bucket}/history/${local.environment}/api_history/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    stored_as_sub_directories = false

    ser_de_info {
      parameters = {
        "input.format" = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
      }
      serialization_library = "com.amazonaws.glue.serde.GrokSerDe"
    }
  }
}

# Glue Crawler to process the ingested logs and populate the S3 target
resource "aws_glue_crawler" "bfd-history-crawler" {
  classifiers = [
    aws_glue_classifier.bfd-historicals-local.name,
  ]
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "bfd-insights-bfd-${local.environment}-history-crawler"
  role          = aws_iam_role.glue-role.name
  tags          = {}
  tags_all      = {}

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_EVERYTHING"
  }

  s3_target {
    exclusions = []
    path       = aws_glue_catalog_table.api-history.storage_descriptor[0].location
  }

  schema_change_policy {
    delete_behavior = "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}

# Glue Classifier to read data from the data store and generate the schema
resource "aws_glue_classifier" "bfd-historicals-local" {
  name = "bfd-${local.environment}-historicals-local"

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
  tags               = {}
  tags_all           = {}
  source             = "glue_src/bfd_history_ingest.py"
  etag               = filemd5("glue_src/bfd_history_ingest.py")
}

# Glue Job for history ingestion
resource "aws_glue_job" "bfd-history-ingest-job" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-history-ingest.bucket}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-enable"
    "--job-language"                     = "python"
    "--sourceDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--sourceTable"                      = aws_glue_catalog_table.api-history.name
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-history-ingest.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--targetTable"                      = aws_glue_catalog_table.api-requests-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-insights-${local.project}-${local.environment}-history-ingest"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = aws_iam_role.glue-role.arn
  tags                      = {}
  tags_all                  = {}
  timeout                   = 2880
  worker_type               = "G.1X"

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-history-ingest.bucket}/${aws_s3_object.bfd-history-ingest.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}
