data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "moderate_bucket" {
  bucket      = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

data "aws_kms_alias" "s3" {
  name = "alias/aws/s3"
}

locals {
  account_id  = data.aws_caller_identity.current.account_id
}

resource "aws_glue_catalog_table" "aws_glue_catalog_table" {
  name          = var.table
  database_name = var.database

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
    location      = "s3://${data.aws_s3_bucket.moderate_bucket.id}/databases/${var.database}/${var.table}"
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
        "ignore.malformed.json" = true
      }
    }
  }
}