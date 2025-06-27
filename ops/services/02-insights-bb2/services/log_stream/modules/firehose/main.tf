module "terraservice" {
  source               = "../../../terraform-modules/bfd/bfd-terraservice"
  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/02-insights-bb2"
}

locals {
  env                 = module.terraservice.env
  insights_bucket_env = var.insights_env != "" ? var.insights_env : local.env
  bucket_param        = "/bfd/${local.insights_bucket_env}/insights/nonsensitive/bfd-insights-${var.project}"
  env_key_alias       = module.terraservice.env_key_alias
  full_name           = "bfd-insights-${var.project}-${var.name}"
  account_id          = data.aws_caller_identity.current.account_id
  table               = var.table_name
}

data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "bucket_name_param" {
  name = local.bucket_param
}

data "aws_s3_bucket" "bucket" {
  bucket = data.aws_ssm_parameter.bucket_name_param.value
}

# Firehose delivery stream
resource "aws_kinesis_firehose_delivery_stream" "kinesis_firehose_stream" {
  name        = local.full_name
  destination = "extended_s3"

  extended_s3_configuration {
    bucket_arn          = data.aws_s3_bucket.bucket.arn
    buffering_interval  = var.firehose_s3_buffer_interval
    buffering_size      = var.firehose_s3_buffer_size
    compression_format  = "GZIP"
    error_output_prefix = "databases/events_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
    kms_key_arn         = local.env_key_alias
    prefix              = "databases/events-${var.name}/dt=!{timestamp:YYYY/MM/dd/HH}/"
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
        region        = var.region
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
          parameter_value = "arn:aws:lambda:${var.region}:${local.account_id}:function:${var.lambda_name}:$LATEST"
        }
      }
    }
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }

}
