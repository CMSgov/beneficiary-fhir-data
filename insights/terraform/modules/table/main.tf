data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket      = var.bucket
}

data "aws_kms_key" "bucket_cmk" {
  key_id      = var.bucket_cmk
}

locals {
  account_id  = data.aws_caller_identity.current.account_id
}

resource "aws_glue_catalog_table" "aws_glue_catalog_table" {
  name          = var.table
  database_name = var.database
  description   = var.description

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL                = "TRUE"
  }

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
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

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
      serialization_library = "org.apache.hive.hcatalog.data.JsonSerDe"
      parameters = {
        "ignore.malformed.json" = true,
        "dots.in.keys" = true,
        "timestamp.formats" = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z',yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss'Z'"
      }
    }
  }
}