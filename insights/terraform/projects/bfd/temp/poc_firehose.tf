resource "aws_kinesis_firehose_delivery_stream" "main" {
  destination    = "extended_s3"
  destination_id = "destinationId-000000000001"
  name           = "bfd-insights-bfd-poc-${local.table}"
  tags = local.tags
  tags_all = local.tags
  version_id = "2"

  extended_s3_configuration {
    bucket_arn          = local.external.s3_insights_arn
    buffer_interval     = 60
    buffer_size         = 128
    compression_format  = "GZIP"
    error_output_prefix = "databases/bfd/api_requests_errors/!{firehose:error-output-type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    kms_key_arn         = local.external.kms_arn
    prefix              = "databases/bfd/test_api_requests/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    role_arn            = "arn:aws:iam::577373831711:role/bfd-insights-bfd-api-requests" # aws_iam_role.firehose.arn
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
        database_name = "bfd"
        region        = local.region
        role_arn      = aws_iam_role.firehose.arn
        table_name    = "bfd-test-api-requests"
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

resource "aws_cloudwatch_log_subscription_filter" "bfd-test-access-log-subscription" {
  name            = "bfd-test-access-log-subscription"
  log_group_name  = "/bfd/test/bfd-server/access.json"
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.main.arn
  # destination_arn = "arn:aws:firehose:us-east-1:${local.account_id}:deliverystream/${local.full_name}"
  role_arn        = aws_iam_role.cloudwatch_role.arn # "arn:aws:iam::577373831711:role/bfd-insights-bfd-api-requests-cwl2firehose-role"
}
