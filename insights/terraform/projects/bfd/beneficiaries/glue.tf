# Beneficiaries
#
# Beneficiaries, which captures when a beneficiary is queried.

# Glue Catalog Table to hold Beneficiaries
resource "aws_glue_catalog_table" "beneficiaries-table" {
  catalog_id    = data.aws_caller_identity.current.account_id
  database_name = local.database
  name          = "${local.full_name}-beneficiaries"
  owner         = "owner"
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
    input_format      = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/${local.database}/beneficiaries/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"
    stored_as_sub_directories = false

    ser_de_info {
      parameters = {
        "serialization.format" = "1"
      }
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"
    }
  }
}

# S3 Object for Glue Script
resource "aws_s3_object" "bfd-populate-beneficiaries" {
  bucket             = data.aws_s3_bucket.bfd-insights-bucket.id
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/${local.environment}/bfd_populate_beneficiaries.py"
  metadata           = {}
  storage_class      = "STANDARD"
  tags               = {}
  tags_all           = {}
  source             = "glue_src/bfd_populate_beneficiaries.py"
  # etag               = filemd5("glue_src/bfd_populate_beneficiaries.py")
}

# Glue Job to populate the beneficiaries table
resource "aws_glue_job" "bfd-populate-beneficiaries-job" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-enable"
    "--job-language"                     = "python"
    "--sourceDatabase"                   = local.database
    "--sourceTable"                      = "${replace(local.full_name, "-", "_")}_api_requests" # "${local.full_name}-api-requests"
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = local.database
    "--targetTable"                      = aws_glue_catalog_table.beneficiaries-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "${local.full_name}-populate-beneficiaries"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = data.aws_iam_role.glue-role.arn
  tags                      = {}
  tags_all                  = {}
  timeout                   = 2880
  worker_type               = "G.1X"

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/${aws_s3_object.bfd-populate-beneficiaries.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}


# Beneficiary Unique
#
# Beneficiary Unique table to track the first time each beneficiary was queried.

# Glue Catalog Table for unique beneficiaries
resource "aws_glue_catalog_table" "beneficiaries-unique-table" {
  catalog_id    = data.aws_caller_identity.current.account_id
  database_name = local.database
  name          = "${local.full_name}-beneficiaries-unique"
  owner         = "owner"
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

  storage_descriptor {
    bucket_columns    = []
    compressed        = false
    input_format      = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/${local.database}/beneficiaries_unique/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"
    stored_as_sub_directories = false

    columns {
      name       = "bene_id"
      parameters = {}
      type       = "bigint"
    }
    columns {
      name       = "first_seen"
      parameters = {}
      type       = "timestamp"
    }

    ser_de_info {
      parameters = {
        "serialization.format" = "1"
      }
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"
    }
  }
}

# S3 Object for the Glue Script
resource "aws_s3_object" "bfd-populate-beneficiary-unique" {
  bucket             = data.aws_s3_bucket.bfd-insights-bucket.id
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/${local.environment}/bfd_populate_beneficiary_unique.py"
  metadata           = {}
  storage_class      = "STANDARD"
  tags               = {}
  tags_all           = {}
  source             = "glue_src/bfd_populate_beneficiary_unique.py"
  # etag               = filemd5("glue_src/bfd_populate_beneficiary_unique.py")
}

# Glue Job to populate the beneficiary_unique table
resource "aws_glue_job" "bfd-populate-beneficiary-unique-job" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/temporary/${local.environment}/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--initialize"                       = "True"
    "--job-bookmark-option"              = "job-bookmark-disable" # We need to process all records
    "--job-language"                     = "python"
    "--sourceDatabase"                   = local.database
    "--sourceTable"                      = aws_glue_catalog_table.beneficiaries-table.name
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = local.database
    "--targetTable"                      = aws_glue_catalog_table.beneficiaries-unique-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "${local.full_name}-populate-beneficiary-unique"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = data.aws_iam_role.glue-role.arn
  tags                      = {}
  tags_all                  = {}
  timeout                   = 2880
  worker_type               = "G.1X"

  command {
    name            = "glueetl"
    python_version  = "3"
    script_location = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/${aws_s3_object.bfd-populate-beneficiary-unique.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}


# Crawler
#
# Crawler on a schedule to classify log files and ensure they are put into the Glue Tables.
resource "aws_glue_crawler" "beneficiaries-recurring-crawler" {
  classifiers   = []
  database_name = local.database
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
  name     = "${local.full_name}-beneficiaries-recurring-crawler"
  role     = data.aws_iam_role.glue-role.name
  schedule = "cron(59 9 * * ? *)"
  tags     = {}
  tags_all = {}

  catalog_target {
    database_name = local.database
    tables = [
      aws_glue_catalog_table.beneficiaries-table.name,
      aws_glue_catalog_table.beneficiaries-unique-table.name
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
