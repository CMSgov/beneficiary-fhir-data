# History

# S3 object containing the Glue Script for history ingestion
resource "aws_s3_object" "bfd-history-ingest" {
  bucket             = local.external.s3_glue_assets_bucket
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/bfd_history_ingest.py"
  metadata           = {}
  storage_class      = "STANDARD"
  tags               = {}
  tags_all           = {}
  source             = "glue_src/bfd_history_ingest.py"
  etag               = filemd5("glue_src/bfd_history_ingest.py")
}

# S3 Object for Glue Script
resource "aws_s3_object" "bfd-populate-beneficiaries" {
  bucket             = local.external.s3_glue_assets_bucket
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/bfd_populate_beneficiaries.py"
  metadata           = {}
  storage_class      = "STANDARD"
  tags               = {}
  tags_all           = {}
  source             = "glue_src/bfd_populate_beneficiaries.py"
  etag               = filemd5("glue_src/bfd_populate_beneficiaries.py")
}

# S3 Object for the Glue Script
resource "aws_s3_object" "bfd-populate-beneficiary-unique" {
  bucket             = local.external.s3_glue_assets_bucket
  bucket_key_enabled = false
  content_type       = "application/octet-stream; charset=UTF-8"
  key                = "scripts/bfd_populate_beneficiary_unique.py"
  metadata           = {}
  storage_class      = "STANDARD"
  tags               = {}
  tags_all           = {}
  source             = "glue_src/bfd_populate_beneficiary_unique.py"
  etag               = filemd5("glue_src/bfd_populate_beneficiary_unique.py")
}

resource "aws_glue_catalog_table" "test_api_history" {
  catalog_id    = local.account_id
  database_name = "bfd"
  name          = "test_api_history"
  owner         = "owner"
  parameters = {
    "CrawlerSchemaDeserializerVersion" = "1.0"
    "CrawlerSchemaSerializerVersion"   = "1.0"
    "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-history-crawler.name
    "averageRecordSize"                = "2857"
    "classification"                   = "cw-history"
    "compressionType"                  = "gzip"
    "grokPattern"                      = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
    "objectCount"                      = "209"
    "recordCount"                      = "86598"
    "sizeKey"                          = "284588120"
    "typeOfData"                       = "file"
  }
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
    location          = "s3://${aws_s3_bucket.bfd-insights-bfd-app-logs.bucket}/history/test_api_history/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    parameters = {
      "CrawlerSchemaDeserializerVersion" = "1.0"
      "CrawlerSchemaSerializerVersion"   = "1.0"
      "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-history-crawler.name
      "averageRecordSize"                = "2857"
      "classification"                   = "cw-history"
      "compressionType"                  = "gzip"
      "grokPattern"                      = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
      "objectCount"                      = "209"
      "recordCount"                      = "86598"
      "sizeKey"                          = "284588120"
      "typeOfData"                       = "file"
    }
    stored_as_sub_directories = false

    columns {
      name       = "timestamp"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "message"
      parameters = {}
      type       = "string"
    }

    ser_de_info {
      parameters = {
        "input.format" = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
      }
      serialization_library = "com.amazonaws.glue.serde.GrokSerDe"
    }
  }
}


# API History

resource "aws_glue_crawler" "bfd-test-api-requests-recurring-crawler" {
  classifiers   = []
  database_name = "bfd"
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
  name     = "bfd-test-api-requests-recurring-crawler"
  role     = local.external.insights_glue_role
  schedule = "cron(59 10 * * ? *)"
  tags     = {}
  tags_all = {}

  catalog_target {
    database_name = "bfd"
    tables = [
      "test_api_requests",
    ]
  }
  catalog_target {
    database_name = "bfd"
    tables = [
      "test_beneficiaries",
    ]
  }
  catalog_target {
    database_name = "bfd"
    tables = [
      "test_beneficiaries_unique",
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

resource "aws_glue_job" "bfd-populate-beneficiary-unique" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/temporary/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--initialize"                       = "True"
    "--job-bookmark-option"              = "job-bookmark-disable"
    "--job-language"                     = "python"
    "--sourceTable"                      = "test_beneficiaries"
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiary-unique.bucket}/sparkHistoryLogs/"
    "--targetTable"                      = "test_beneficiaries_unique"
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-populate-beneficiary-unique"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = local.external.insights_glue_role_arn
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

resource "aws_glue_job" "bfd-populate-beneficiaries" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/temporary/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-disable"
    "--job-language"                     = "python"
    "--sourceTable"                      = "test_api_requests"
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-populate-beneficiaries.bucket}/sparkHistoryLogs/"
    "--targetTable"                      = "test_beneficiaries"
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-populate-beneficiaries"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = local.external.insights_glue_role_arn
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

resource "aws_glue_job" "bfd-history-ingest" {
  connections = []
  default_arguments = {
    "--TempDir"                          = "s3://${aws_s3_object.bfd-history-ingest.bucket}/temporary/"
    "--class"                            = "GlueApp"
    "--enable-continuous-cloudwatch-log" = "true"
    "--enable-glue-datacatalog"          = "true"
    "--enable-job-insights"              = "true"
    "--enable-metrics"                   = "true"
    "--enable-spark-ui"                  = "true"
    "--job-bookmark-option"              = "job-bookmark-disable"
    "--job-language"                     = "python"
    "--sourceTable"                      = "test"
    "--spark-event-logs-path"            = "s3://${aws_s3_object.bfd-history-ingest.bucket}/sparkHistoryLogs/"
    "--targetTable"                      = "test_api_requests"
  }
  glue_version              = "3.0"
  max_retries               = 0
  name                      = "bfd-history-ingest"
  non_overridable_arguments = {}
  number_of_workers         = 10
  role_arn                  = local.external.insights_glue_role_arn
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

resource "aws_glue_catalog_table" "test_beneficiaries_unique" {
  catalog_id    = local.account_id
  database_name = "bfd"
  name          = "test_beneficiaries_unique"
  owner         = "owner"
  parameters = {
    "CrawlerSchemaDeserializerVersion" = "1.0"
    "CrawlerSchemaSerializerVersion"   = "1.0"
    "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-api-requests-recurring-crawler.name
    "averageRecordSize"                = "21"
    "classification"                   = "parquet"
    "compressionType"                  = "none"
    "objectCount"                      = "37"
    "recordCount"                      = "50032"
    "sizeKey"                          = "637218"
    "typeOfData"                       = "file"
  }
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
    # TODO: Change this to a canonical name-spaced location
    location          = "s3://${module.bucket.id}/databases/bfd/test_beneficiaries_unique/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"
    parameters = {
      "CrawlerSchemaDeserializerVersion" = "1.0"
      "CrawlerSchemaSerializerVersion"   = "1.0"
      "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-api-requests-recurring-crawler.name
      "averageRecordSize"                = "21"
      "classification"                   = "parquet"
      "compressionType"                  = "none"
      "objectCount"                      = "37"
      "recordCount"                      = "50032"
      "sizeKey"                          = "637218"
      "typeOfData"                       = "file"
    }
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

resource "aws_glue_catalog_table" "test_beneficiaries" {
  catalog_id    = local.account_id
  database_name = "bfd"
  name          = "test_beneficiaries"
  owner         = "owner"
  parameters = {
    "CrawlerSchemaDeserializerVersion" = "1.0"
    "CrawlerSchemaSerializerVersion"   = "1.0"
    "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-api-requests-recurring-crawler.name
    "averageRecordSize"                = "49"
    "classification"                   = "parquet"
    "compressionType"                  = "none"
    "objectCount"                      = "847"
    "recordCount"                      = "2997471"
    "sizeKey"                          = "85681749"
    "typeOfData"                       = "file"
  }
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
    # TODO: Change this to a canonical name-spaced location
    location          = "s3://${module.bucket.id}/databases/bfd/test_beneficiaries/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"
    parameters = {
      "CrawlerSchemaDeserializerVersion" = "1.0"
      "CrawlerSchemaSerializerVersion"   = "1.0"
      "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-api-requests-recurring-crawler.name
      "averageRecordSize"                = "49"
      "classification"                   = "parquet"
      "compressionType"                  = "none"
      "objectCount"                      = "847"
      "recordCount"                      = "2997471"
      "sizeKey"                          = "85681749"
      "typeOfData"                       = "file"
    }
    stored_as_sub_directories = false

    columns {
      name       = "bene_id"
      parameters = {}
      type       = "bigint"
    }
    columns {
      name       = "timestamp"
      parameters = {}
      type       = "timestamp"
    }
    columns {
      name       = "clientssl_dn"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "operation"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "uri"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "query_string"
      parameters = {}
      type       = "string"
    }

    ser_de_info {
      parameters = {
        "serialization.format" = "1"
      }
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"
    }
  }
}


resource "aws_glue_catalog_table" "test_api_requests" {
  catalog_id    = local.account_id
  database_name = "bfd"
  name          = "test_api_requests"
  owner         = "owner"
  parameters = {
    "CrawlerSchemaDeserializerVersion" = "1.0"
    "CrawlerSchemaSerializerVersion"   = "1.0"
    "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-api-requests-recurring-crawler.name
    "UpdatedByJob"                     = aws_glue_job.bfd-history-ingest.name
    "UpdatedByJobRun"                  = "jr_0d9384dec6facd8b8f8d22433e9d7dc3aaa35cd36d82aeb4e63accfad04a743c"
    "averageRecordSize"                = "9486"
    "classification"                   = "json"
    "compressionType"                  = "none"
    "objectCount"                      = "246"
    "recordCount"                      = "4925483"
    "sizeKey"                          = "46771821038"
    "typeOfData"                       = "file"
  }
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
    # TODO: Change this to a canonical name-spaced location
    location          = "s3://${module.bucket.id}/databases/${local.database}/test_api_requests/"
    number_of_buckets = -1
    output_format     = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    parameters = {
      "CrawlerSchemaDeserializerVersion" = "1.0"
      "CrawlerSchemaSerializerVersion"   = "1.0"
      "UPDATED_BY_CRAWLER"               = aws_glue_crawler.bfd-test-api-requests-recurring-crawler.name
      "UpdatedByJob"                     = aws_glue_job.bfd-history-ingest.name
      "UpdatedByJobRun"                  = "jr_0d9384dec6facd8b8f8d22433e9d7dc3aaa35cd36d82aeb4e63accfad04a743c"
      "averageRecordSize"                = "9486"
      "classification"                   = "json"
      "compressionType"                  = "none"
      "objectCount"                      = "246"
      "recordCount"                      = "4925483"
      "sizeKey"                          = "46771821038"
      "typeOfData"                       = "file"
    }
    stored_as_sub_directories = false

    columns {
      name       = "timestamp"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "level"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "thread"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.clientssl.dn"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.user-agent"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.date"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request_type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.accept"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.host"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.http_method"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.url"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.operation"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.uri"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.query_string"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.x-request-id"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.content-type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.x-powered-by"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.status"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.content-location"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_id.include_.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_id.include_.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_id.include_.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.last-modified"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.bene_id"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.snf.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.carrier.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.inpatient.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.outpatient.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.dme.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.pde.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.carrier.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.snf.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.dme.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.dme.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.snf.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.pde.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.hha.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.hospice.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.hha.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.hha.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.pde.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.hospice.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.connection"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.content-encoding"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.accept-encoding"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.snf.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.outpatient.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.type"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.batch"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.datasource_name"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.batch_size"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.unknown.success"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.by_hash.collision.distinct_bene_ids"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.benes_by_year_month_part_d_contract_id.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.includeidentifiers"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_milliseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_nanoseconds"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.record_count"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.dme.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.inpatient.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.pde.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.carrier.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hospice.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.eobs_by_bene_id.hha.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.response.header.cache-control"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.http_access.request.header.cache-control"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "mdc.database_query.bene_by_id.include_hicns_and_mbis.query"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "logger"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "message"
      parameters = {}
      type       = "string"
    }
    columns {
      name       = "context"
      parameters = {}
      type       = "string"
    }

    ser_de_info {
      parameters = {
        "paths" = "context,level,logger,mdc.bene_id,mdc.database_query.bene_by_id.include_hicns_and_mbis.batch,mdc.database_query.bene_by_id.include_hicns_and_mbis.batch_size,mdc.database_query.bene_by_id.include_hicns_and_mbis.datasource_name,mdc.database_query.bene_by_id.include_hicns_and_mbis.duration_milliseconds,mdc.database_query.bene_by_id.include_hicns_and_mbis.query,mdc.database_query.bene_by_id.include_hicns_and_mbis.size,mdc.database_query.bene_by_id.include_hicns_and_mbis.success,mdc.database_query.bene_by_id.include_hicns_and_mbis.type,mdc.database_query.by_hash.collision.distinct_bene_ids,mdc.database_query.eobs_by_bene_id.carrier.batch,mdc.database_query.eobs_by_bene_id.carrier.batch_size,mdc.database_query.eobs_by_bene_id.carrier.datasource_name,mdc.database_query.eobs_by_bene_id.carrier.duration_milliseconds,mdc.database_query.eobs_by_bene_id.carrier.query,mdc.database_query.eobs_by_bene_id.carrier.size,mdc.database_query.eobs_by_bene_id.carrier.success,mdc.database_query.eobs_by_bene_id.carrier.type,mdc.database_query.eobs_by_bene_id.dme.batch,mdc.database_query.eobs_by_bene_id.dme.batch_size,mdc.database_query.eobs_by_bene_id.dme.datasource_name,mdc.database_query.eobs_by_bene_id.dme.duration_milliseconds,mdc.database_query.eobs_by_bene_id.dme.query,mdc.database_query.eobs_by_bene_id.dme.size,mdc.database_query.eobs_by_bene_id.dme.success,mdc.database_query.eobs_by_bene_id.dme.type,mdc.database_query.eobs_by_bene_id.hha.batch,mdc.database_query.eobs_by_bene_id.hha.batch_size,mdc.database_query.eobs_by_bene_id.hha.datasource_name,mdc.database_query.eobs_by_bene_id.hha.duration_milliseconds,mdc.database_query.eobs_by_bene_id.hha.query,mdc.database_query.eobs_by_bene_id.hha.size,mdc.database_query.eobs_by_bene_id.hha.success,mdc.database_query.eobs_by_bene_id.hha.type,mdc.database_query.eobs_by_bene_id.hospice.batch,mdc.database_query.eobs_by_bene_id.hospice.batch_size,mdc.database_query.eobs_by_bene_id.hospice.datasource_name,mdc.database_query.eobs_by_bene_id.hospice.duration_milliseconds,mdc.database_query.eobs_by_bene_id.hospice.query,mdc.database_query.eobs_by_bene_id.hospice.size,mdc.database_query.eobs_by_bene_id.hospice.success,mdc.database_query.eobs_by_bene_id.hospice.type,mdc.database_query.eobs_by_bene_id.inpatient.batch,mdc.database_query.eobs_by_bene_id.inpatient.batch_size,mdc.database_query.eobs_by_bene_id.inpatient.datasource_name,mdc.database_query.eobs_by_bene_id.inpatient.duration_milliseconds,mdc.database_query.eobs_by_bene_id.inpatient.query,mdc.database_query.eobs_by_bene_id.inpatient.size,mdc.database_query.eobs_by_bene_id.inpatient.success,mdc.database_query.eobs_by_bene_id.inpatient.type,mdc.database_query.eobs_by_bene_id.outpatient.batch,mdc.database_query.eobs_by_bene_id.outpatient.batch_size,mdc.database_query.eobs_by_bene_id.outpatient.datasource_name,mdc.database_query.eobs_by_bene_id.outpatient.duration_milliseconds,mdc.database_query.eobs_by_bene_id.outpatient.query,mdc.database_query.eobs_by_bene_id.outpatient.size,mdc.database_query.eobs_by_bene_id.outpatient.success,mdc.database_query.eobs_by_bene_id.outpatient.type,mdc.database_query.eobs_by_bene_id.pde.batch,mdc.database_query.eobs_by_bene_id.pde.batch_size,mdc.database_query.eobs_by_bene_id.pde.datasource_name,mdc.database_query.eobs_by_bene_id.pde.duration_milliseconds,mdc.database_query.eobs_by_bene_id.pde.query,mdc.database_query.eobs_by_bene_id.pde.size,mdc.database_query.eobs_by_bene_id.pde.success,mdc.database_query.eobs_by_bene_id.pde.type,mdc.database_query.eobs_by_bene_id.snf.batch,mdc.database_query.eobs_by_bene_id.snf.batch_size,mdc.database_query.eobs_by_bene_id.snf.datasource_name,mdc.database_query.eobs_by_bene_id.snf.duration_milliseconds,mdc.database_query.eobs_by_bene_id.snf.query,mdc.database_query.eobs_by_bene_id.snf.size,mdc.database_query.eobs_by_bene_id.snf.success,mdc.database_query.eobs_by_bene_id.snf.type,mdc.database_query.unknown.batch,mdc.database_query.unknown.batch_size,mdc.database_query.unknown.datasource_name,mdc.database_query.unknown.duration_milliseconds,mdc.database_query.unknown.query,mdc.database_query.unknown.size,mdc.database_query.unknown.success,mdc.database_query.unknown.type,mdc.http_access.request.clientSSL.DN,mdc.http_access.request.header.Accept,mdc.http_access.request.header.Accept-Encoding,mdc.http_access.request.header.Cache-Control,mdc.http_access.request.header.Connection,mdc.http_access.request.header.Host,mdc.http_access.request.header.IncludeIdentifiers,mdc.http_access.request.header.User-Agent,mdc.http_access.request.http_method,mdc.http_access.request.operation,mdc.http_access.request.query_string,mdc.http_access.request.uri,mdc.http_access.request.url,mdc.http_access.request_type,mdc.http_access.response.duration_milliseconds,mdc.http_access.response.header.Cache-Control,mdc.http_access.response.header.Content-Encoding,mdc.http_access.response.header.Content-Location,mdc.http_access.response.header.Content-Type,mdc.http_access.response.header.Date,mdc.http_access.response.header.Last-Modified,mdc.http_access.response.header.X-Powered-By,mdc.http_access.response.header.X-Request-ID,mdc.http_access.response.status,mdc.jpa_query.bene_by_id.include_.duration_milliseconds,mdc.jpa_query.bene_by_id.include_.duration_nanoseconds,mdc.jpa_query.bene_by_id.include_.record_count,mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_milliseconds,mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_nanoseconds,mdc.jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.record_count,mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds,mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_nanoseconds,mdc.jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.record_count,mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_milliseconds,mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.duration_nanoseconds,mdc.jpa_query.bene_count_by_year_month_part_d_contract_id.record_count,mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_milliseconds,mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_nanoseconds,mdc.jpa_query.bene_ids_by_year_month_part_d_contract_id.record_count,mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_milliseconds,mdc.jpa_query.benes_by_year_month_part_d_contract_id.duration_nanoseconds,mdc.jpa_query.benes_by_year_month_part_d_contract_id.record_count,mdc.jpa_query.eobs_by_bene_id.carrier.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.carrier.record_count,mdc.jpa_query.eobs_by_bene_id.dme.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.dme.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.dme.record_count,mdc.jpa_query.eobs_by_bene_id.hha.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.hha.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.hha.record_count,mdc.jpa_query.eobs_by_bene_id.hospice.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.hospice.record_count,mdc.jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.inpatient.record_count,mdc.jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.outpatient.record_count,mdc.jpa_query.eobs_by_bene_id.pde.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.pde.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.pde.record_count,mdc.jpa_query.eobs_by_bene_id.snf.duration_milliseconds,mdc.jpa_query.eobs_by_bene_id.snf.duration_nanoseconds,mdc.jpa_query.eobs_by_bene_id.snf.record_count,message,thread,timestamp"
      }
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }
  }
}

resource "aws_glue_crawler" "bfd-test-history-crawler" {
  classifiers = [
    "test_historicals_local",
  ]
  database_name = "bfd"
  name          = "bfd-test-history-crawler"
  role          = local.external.insights_glue_role
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
    path       = "s3://${aws_s3_bucket.bfd-insights-bfd-app-logs.bucket}/history/test_api_history"
  }

  schema_change_policy {
    delete_behavior = "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}

resource "aws_glue_classifier" "test_historicals_local" {
  name = "test_historicals_local"

  grok_classifier {
    classification = "cw-history"
    grok_pattern   = "%%{TIMESTAMP_ISO8601:timestamp:string} %%{GREEDYDATA:message:string}"
  }
}
