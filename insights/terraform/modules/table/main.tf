data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket      = var.bucket
}

data "aws_kms_key" "bucket_cmk" {
  key_id      = var.bucket_cmk
}

locals {
  account_id = data.aws_caller_identity.current.account_id

  table_parameters = {
    json = {
      EXTERNAL = "TRUE"
    },
    parquet = {
      classification        = "parquet"
      EXTERNAL              = "TRUE"
      "parquet.compression" = "SNAPPY"
    }
  }

  storage_options = {
    json = {
      input_format  = "org.apache.hadoop.mapred.TextInputFormat"
      output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"
    },
    parquet = {
      input_format  = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
      output_format = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"
    }
  }

  serde_options = {
    json = {
      library = "org.apache.hive.hcatalog.data.JsonSerDe"
      params  = length(var.serde_parameters) == 0 ? {
        "ignore.malformed.json" = true,
        "dots.in.keys" = true,
        "timestamp.formats" = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z',yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss'Z'"
      } : var.serde_parameters
    },
    grok = {
      library = "com.amazonaws.glue.serde.GrokSerDe"
      params  = length(var.serde_parameters) == 0 ? {
        "ignore.malformed.json" = true,
        "dots.in.keys" = true,
        "timestamp.formats" = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z',yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss'Z'"
      } : var.serde_parameters
    },
    parquet = {
      library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"
      params  = length(var.serde_parameters) == 0 ? {
        "serialization.format" = 1
      } : var.serde_parameters
    }
  }
}

resource "aws_glue_catalog_table" "aws_glue_catalog_table" {
  name          = var.table
  database_name = var.database
  description   = var.description
  table_type    = "EXTERNAL_TABLE"
  owner         = "owner"

  parameters = local.table_parameters[var.storage_format]

  dynamic "partition_keys" {
    for_each = var.partitions

    content {
      name    = partition_keys.value.name
      type    = partition_keys.value.type
      comment = partition_keys.value.comment
    }
  }

  storage_descriptor {
    location      = "s3://${data.aws_s3_bucket.main.id}/databases/${var.database}/${var.table}"
    input_format  = local.storage_options[var.storage_format].input_format
    output_format = local.storage_options[var.storage_format].output_format
    compressed    = true

    dynamic "columns" {
      for_each = var.columns

      content {
        name    = columns.value.name
        type    = columns.value.type
        comment = columns.value.comment
      }
    }

    ser_de_info {
      name                  = var.table
      serialization_library = local.serde_options[var.serde_format].library
      parameters            = local.serde_options[var.serde_format].params
    }
  }

  # These things get changed by the Crawler (if there is one), and we don't
  # need to undo whatever changes the Crawler makes
  lifecycle {
    ignore_changes = [
      storage_descriptor,
      parameters
    ]
  }
}
