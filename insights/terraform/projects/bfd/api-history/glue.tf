# History

resource "aws_glue_catalog_database" "bfd-database" {
  name = local.database
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
    "--sourceTable"                      = "${replace(local.environment, "-", "_")}_api_history"
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-history-ingest.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--targetTable"                      = aws_glue_catalog_table.api-requests-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-${local.environment}-history-ingest"
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

# Glue Crawler to process the ingested logs and populate the S3 target
resource "aws_glue_crawler" "bfd-history-crawler" {
  classifiers = [
    aws_glue_classifier.bfd-historicals-local.name,
  ]
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "bfd-${local.environment}-history-crawler"
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
    path       = "s3://${data.aws_s3_bucket.bfd-app-logs.bucket}/history/${local.environment}_api_history"
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


# Beneficiaries

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
  etag               = filemd5("glue_src/bfd_populate_beneficiaries.py")
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
    "--sourceDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--sourceTable"                      = aws_glue_catalog_table.api-requests-table.name
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--targetTable"                      = aws_glue_catalog_table.beneficiaries-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-${local.environment}-populate-beneficiaries"
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
    script_location = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/${aws_s3_object.bfd-populate-beneficiaries.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}

# Glue Catalog Table to hold Beneficiaries
resource "aws_glue_catalog_table" "beneficiaries-table" {
  catalog_id    = local.account_id
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "${local.environment}-beneficiaries"
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
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/bfd/${local.environment}_beneficiaries/"
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


# Beneficiary Unique

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
  etag               = filemd5("glue_src/bfd_populate_beneficiary_unique.py")
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
    "--sourceDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--sourceTable"                      = aws_glue_catalog_table.beneficiaries-table.name
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/sparkHistoryLogs/${local.environment}/"
    "--targetDatabase"                   = aws_glue_catalog_database.bfd-database.name
    "--targetTable"                      = aws_glue_catalog_table.beneficiaries-unique-table.name
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-${local.environment}-populate-beneficiary-unique"
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
    script_location = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/${aws_s3_object.bfd-populate-beneficiary-unique.key}"
  }

  execution_property {
    max_concurrent_runs = 1
  }
}

# Glue Catalog Table for unique beneficiaries
resource "aws_glue_catalog_table" "beneficiaries-unique-table" {
  catalog_id    = local.account_id
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "${local.environment}-beneficiaries-unique"
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
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/bfd/${local.environment}_beneficiaries_unique/"
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


# API Requests

resource "aws_glue_crawler" "bfd-api-requests-recurring-crawler" {
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
  name     = "bfd-${local.environment}-api-requests-recurring-crawler"
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
  catalog_target {
    database_name = aws_glue_catalog_database.bfd-database.name
    tables = [
      aws_glue_catalog_table.beneficiaries-table.name,
    ]
  }
  catalog_target {
    database_name = aws_glue_catalog_database.bfd-database.name
    tables = [
      aws_glue_catalog_table.beneficiaries-unique-table.name,
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


# API History

# Glue Catalog Table to store API History
resource "aws_glue_catalog_table" "api-history" {
  catalog_id    = local.account_id
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "${local.environment}-api-history"
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
    location          = "s3://${data.aws_s3_bucket.bfd-app-logs.bucket}/history/${local.environment}_api_history/"
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

resource "aws_glue_catalog_table" "api-requests-table" {
  catalog_id    = local.account_id
  database_name = aws_glue_catalog_database.bfd-database.name
  name          = "${local.environment}-api-requests"
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
    location          = "s3://${data.aws_s3_bucket.bfd-insights-bucket.id}/databases/${local.database}/${local.environment}_api_requests/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    stored_as_sub_directories = false

    ser_de_info {
      # parameters = {
      #   "paths" = "context,level,logger,mdc.bene_id,mdc.database_query.bene_by_id.include_hicns_and_mbis.batch,mdc.database_query.bene_by_id.include_hicns_and_mbis.batch_size,mdc.database_query.bene_by_id.include_hicns_and_mbis.datasource_name,mdc.database_query.bene_by_id.include_hicns_and_mbis.duration_milliseconds,mdc.database_query.bene_by_id.include_hicns_and_mbis.query,mdc.database_query.bene_by_id.include_hicns_and_mbis.size,mdc.database_query.bene_by_id.include_hicns_and_mbis.success,mdc.database_query.bene_by_id.include_hicns_and_mbis.type,mdc.database_query.by_hash.collision.distinct_bene_ids,mdc.database_query.eobs_by_bene_id.carrier.batch,mdc.database_query.eobs_by_bene_id.carrier.batch_size,mdc.database_query.eobs_by_bene_id.carrier.datasource_name,mdc.database_query.eobs_by_bene_id.carrier.duration_milliseconds,mdc.database_query.eobs_by_bene_id.carrier.query,mdc.database_query.eobs_by_bene_id.carrier.size,mdc.database_query.eobs_by_bene_id.carrier.success,mdc.database_query.eobs_by_bene_id.carrier.type,mdc.database_query.eobs_by_bene_id.dme.batch,mdc.database_query.eobs_by_bene_id.dme.batch_size,mdc.database_query.eobs_by_bene_id.dme.datasource_name,mdc.database_query.eobs_by_bene_id.dme.duration_milliseconds,mdc.database_query.eobs_by_bene_id.dme.query,mdc.database_query.eobs_by_bene_id.dme.size,mdc.database_query.eobs_by_bene_id.dme.success,mdc.database_query.eobs_by_bene_id.dme.type,mdc.database_query.eobs_by_bene_id.hha.batch,mdc.database_query.eobs_by_bene_id.hha.batch_size,mdc.database_query.eobs_by_bene_id.hha.datasource_name,mdc.database_query.eobs_by_bene_id.hha.duration_milliseconds,mdc.database_query.eobs_by_bene_id.hha.query,mdc.database_query.eobs_by_bene_id.hha.size,mdc.database_query.eobs_by_bene_id.hha.success,mdc.database_query.eobs_by_bene_id.hha.type,mdc.database_query.eobs_by_bene_id.hospice.batch,mdc.database_query.eobs_by_bene_id.hospice.batch_size,mdc.database_query.eobs_by_bene_id.hospice.datasource_name,mdc.database_query.eobs_by_bene_id.hospice.duration_milliseconds,mdc.database_query.eobs_by_bene_id.hospice.query,mdc.database_query.eobs_by_bene_id.hospice.size,mdc.database_query.eobs_by_bene_id.hospice.success,mdc.database_query.eobs_by_bene_id.hospice.type,mdc.database_query.eobs_by_bene_id.inpatient.batch,mdc.database_query.eobs_by_bene_id.inpatient.batch_size,mdc.database_query.eobs_by_bene_id.inpatient.datasource_name,mdc.database_query.eobs_by_bene_id.inpatient.duration_milliseconds,mdc.database_query.eobs_by_bene_id.inpatient.query,mdc.database_query.eobs_by_bene_id.inpatient.size,mdc.database_query.eobs_by_bene_id.inpatient.success,mdc.database_query.eobs_by_bene_id.inpatient.type,mdc.database_query.eobs_by_bene_id.outpatient.batch,mdc.database_query.eobs_by_bene_id.outpatient.batch_size,mdc.database_query.eobs_by_bene_id.outpatient.datasource_name,mdc.database_query.eobs_by_bene_id.outpatient.duration_milliseconds,mdc.database_query.eobs_by_bene_id.outpatient.query,mdc.database_query.eobs_by_bene_id.outpatient.size,mdc.database_query.eobs_by_bene_id.outpatient.success,mdc.database_query.eobs_by_bene_id.outpatient.type,mdc.database_query.eobs_by_bene_id.pde.batch,mdc.database_query.eobs_by_bene_id.pde.batch_size,mdc.database_query.eobs_by_bene_id.pde.datasource_name,mdc.database_query.eobs_by_bene_id.pde.duration_milliseconds,mdc.database_query.eobs_by_bene_id.pde.query,mdc.database_query.eobs_by_bene_id.pde.size,mdc.database_query.eobs_by_bene_id.pde.success,mdc.database_query.eobs_by_bene_id.pde.type,mdc.database_query.eobs_by_bene_id.snf.batch,mdc.database_query.eobs_by_bene_id.snf.batch_size,mdc.database_query.eobs_by_bene_id.snf.datasource_name,mdc.database_query.eobs_by_bene_id.snf.duration_milliseconds,mdc.database_query.eobs_by_bene_id.snf.query,mdc.database_query.eobs_by_bene_id.snf.size,mdc.database_query.eobs_by_bene_id.snf.success,mdc.database_query.eobs_by_bene_id.snf.type,mdc.database_query.unknown.batch,mdc.database_query.unknown.batch_size,mdc.database_query.unknown.datasource_name,mdc.database_query.unknown.duration_milliseconds,mdc.database_query.unknown.query,mdc.database_query.unknown.size,mdc.database_query.unknown.success,mdc.database_query.unknown.type,mdc.http_access.request.clientSSL.DN,mdc.http_access.request.header.Accept,mdc.http_access.request.header.Accept-Encoding,mdc.http_access.request.header.Cache-Control,mdc.http_access.request.header.Connection,mdc.http_access.request.header.Host,mdc.http_access.request.header.IncludeIdentifiers,mdc.http_access.request.header.User-Agent,mdc.http_access.request.http_method,mdc.http_access.request.operation,mdc.http_access.request.query_string,mdc.http_access.request.uri,mdc.http_access.request.url,mdc.http_access.request_type,mdc.http_access.response.duration_milliseconds,mdc.http_access.response.header.Cache-Control,mdc.http_access.response.header.Content-Encoding,mdc.http_access.response.header.Content-Location,mdc.http_access.response.header.Content-Type,mdc.http_access.response.header.Date,mdc.http_access.response.header.Last-Modified,mdc.http_access.response.header.X-Powered-By,mdc.http_access.response.header.X-Request-ID,mdc.http_access.response.status,mdc.jpa_query.bene_by_id.include_.duration_milliseconds,mdc.jpa_query.bene_by_id.include_.duration_nanoseconds,mdc.jpa_query.bene_by_id.include_.record_count,mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_milliseconds,mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_nanoseconds,mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.record_count,mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds,mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_nanoseconds,mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.record_count,mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_milliseconds,mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_nanoseconds,mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.record_count,mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_milliseconds,mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_nanoseconds,mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.record_count,mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_milliseconds,mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_nanoseconds,mdc.jpa_query.benes_by_year_month_part_d_contract_id.record_count,mdc.jpa_query.eobs_by_bene_id.carrier.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.carrier.record_count,mdc.jpa_query.eobs_by_bene_id.dme.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.dme.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.dme.record_count,mdc.jpa_query.eobs_by_bene_id.hha.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.hha.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.hha.record_count,mdc.jpa_query.eobs_by_bene_id.hospice.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.hospice.record_count,mdc.jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.inpatient.record_count,mdc.jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.outpatient.record_count,mdc.jpa_query.eobs_by_bene_id.pde.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.pde.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.pde.record_count,mdc.jpa_query.eobs_by_bene_id.snf.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.snf.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.snf.record_count,message,thread,timestamp"
      # }
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }
  }
}
