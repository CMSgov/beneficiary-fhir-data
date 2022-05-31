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
