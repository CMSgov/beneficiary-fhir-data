data "aws_caller_identity" "current" {}

data "aws_kms_key" "bucket_cmk" {
  key_id = "alias/bfd-insights-bb2-cmk"
}

data "aws_s3_bucket" "main" {
  bucket = "bfd-insights-bb2-${local.account_id}"
}

locals {
  full_name  = "bfd-insights-${var.project}-${var.firehose_name}"
  account_id = data.aws_caller_identity.current.account_id
  table      = var.table_name
}

# Firehose delivery stream
resource "aws_kinesis_firehose_delivery_stream" "kinesis_firehose_stream" {
  name        = local.full_name
  destination = "extended_s3"

  tags = var.tags

  extended_s3_configuration {
    bucket_arn          = data.aws_s3_bucket.main.arn
    buffer_interval     = var.buffer_interval
    buffer_size         = var.buffer_size # 128
    compression_format  = "GZIP"
    error_output_prefix = "databases/${var.project}/events_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
    kms_key_arn         = data.aws_kms_key.bucket_cmk.arn
    prefix              = "databases/${var.project}/events-${var.firehose_name}/dt=!{timestamp:YYYY/MM/dd/HH}/"
    role_arn            = "arn:aws:iam::${local.account_id}:role/bfd-insights-${var.project}-events"
    s3_backup_mode      = "Disabled"

    cloudwatch_logging_options {
      enabled = false
    }

    data_format_conversion_configuration {
      enabled = false

      input_format_configuration {
        deserializer {

          open_x_json_ser_de {
            case_insensitive                         = true
            column_to_json_key_mappings              = {}
            convert_dots_in_json_keys_to_underscores = false
          }
        }
      }

      output_format_configuration {
        serializer {

          parquet_ser_de {
            block_size_bytes              = 268435456
            compression                   = "SNAPPY"
            enable_dictionary_compression = false
            max_padding_bytes             = 0
            page_size_bytes               = 1048576
            writer_version                = "V1"
          }
        }
      }

      schema_configuration {
        database_name = var.project
        region        = "us-east-1"
        role_arn      = "arn:aws:iam::${local.account_id}:role/bfd-insights-${var.project}-events"
        table_name    = local.table
        version_id    = "LATEST"
      }
    }

    processing_configuration {
      enabled = true

      processors {
        type = "Lambda"

        parameters {
          parameter_name  = "LambdaArn"
          parameter_value = "arn:aws:lambda:us-east-1:${local.account_id}:function:${var.project}-kinesis-firehose-cloudwatch-logs-processor-python:$LATEST"
        }
      }
    }
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }

}