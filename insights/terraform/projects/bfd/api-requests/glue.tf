# API Requests
#
# Target location for ingested logs, no matter the method of ingestion.

# Target Glue Table where ingested logs are eventually stored
resource "aws_glue_catalog_table" "api-requests-table" {
  catalog_id    = data.aws_caller_identity.current.account_id
  database_name = module.database.name
  name          = "${replace(local.full_name, "-", "_")}_api_requests"
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
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/${local.database}/api_requests/"
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
  name     = "${local.full_name}-api-requests-recurring-crawler"
  role     = aws_iam_role.glue-role.name
  schedule = "cron(59 10 * * ? *)"
  tags     = {}
  tags_all = {}

  catalog_target {
    database_name = module.database.name
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
  database_name = module.database.name
  name          = "${replace(local.full_name, "-", "_")}_api_history"
  description   = "Store log files from BFD for analysis in BFD Insights"
  owner         = "owner"
  retention     = 0
  table_type    = "EXTERNAL_TABLE"

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
  database_name = module.database.name
  name          = "${local.full_name}-history-crawler"
  description   = "Glue Crawler to ingest logs into the API History Glue Table"
  role          = aws_iam_role.glue-role.name
  tags          = {}
  tags_all      = {}

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
    database_name = aws_glue_catalog_table.api-history.database_name
    tables        = [ aws_glue_catalog_table.api-history.name ]
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

# Glue Classifier to read data from the data store and generate the schema
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
  tags               = local.tags
  tags_all           = local.tags_all
  source             = "glue_src/bfd_history_ingest.py"
}

# Glue Job for history ingestion
resource "aws_glue_job" "bfd-history-ingest-job" {
  name                      = "${local.full_name}-history-ingest"
  description               = "Ingest historical log data"
  tags                      = {}
  tags_all                  = {}
  connections               = []
  glue_version              = "3.0"
  max_retries               = 0
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = aws_iam_role.glue-role.arn
  timeout                   = 2880
  worker_type               = "G.1X"

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
    "--sourceDatabase"                   = module.database.name
    "--sourceTable"                      = aws_glue_catalog_table.api-history.name
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-history-ingest.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = module.database.name
    "--targetTable"                      = aws_glue_catalog_table.api-requests-table.name
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
