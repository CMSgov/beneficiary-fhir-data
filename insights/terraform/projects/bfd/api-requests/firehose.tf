# Firehose Data Stream
resource "aws_kinesis_firehose_delivery_stream" "bfd-firehose" {
  name           = "${local.full_name}-firehose"
  destination    = "extended_s3"

  extended_s3_configuration {
    bucket_arn          = data.aws_s3_bucket.bfd-insights-bucket.arn
    buffer_interval     = 60
    buffer_size         = 128
    compression_format  = "GZIP"
    error_output_prefix = "databases/${module.database.name}/${module.api-requests-table.name}_errors/!{firehose:error-output-type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    kms_key_arn         = data.aws_kms_key.kms_key.arn
    prefix              = "databases/${module.database.name}/${module.api-requests-table.name}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    role_arn            = aws_iam_role.firehose_role.arn
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
          orc_ser_de {
            block_size_bytes                        = 268435456
            bloom_filter_columns                    = []
            bloom_filter_false_positive_probability = 0.05
            compression                             = "SNAPPY"
            dictionary_key_threshold                = 0
            enable_padding                          = false
            format_version                          = "V0_12"
            padding_tolerance                       = 0.05
            row_index_stride                        = 10000
            stripe_size_bytes                       = 67108864
          }
        }
      }

      schema_configuration {
        database_name = local.database
        region        = local.region
        role_arn      = aws_iam_role.firehose_role.arn
        table_name    = module.api-requests-table.name
        version_id    = "LATEST"
      }
    }

    processing_configuration {
      enabled = true

      processors {
        type = "Lambda"

        parameters {
          parameter_name  = "LambdaArn"
          parameter_value = "${resource.aws_lambda_function.bfd-cw-to-flattened-json.arn}:$LATEST"
        }
      }
    }
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }
}

# CloudWatch Log Subscription
resource "aws_cloudwatch_log_subscription_filter" "bfd-access-log-subscription" {
  name            = "${local.full_name}-access-log-subscription"
  log_group_name  = "/bfd/${local.environment}/bfd-server/access.json"
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.bfd-firehose.arn
  role_arn        = aws_iam_role.cloudwatch_role.arn
}
