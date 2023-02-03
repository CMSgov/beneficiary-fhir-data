# Firehose Data Stream
resource "aws_kinesis_firehose_delivery_stream" "firehose-ingester" {
  name        = "${local.full_name}-firehose-ingester"
  destination = "extended_s3"

  extended_s3_configuration {
    bucket_arn          = data.aws_s3_bucket.bfd-insights-bucket.arn
    buffer_interval     = 300
    buffer_size         = 128
    error_output_prefix = "databases/${module.database.name}/${module.glue-table-api-requests.name}_errors/!{firehose:error-output-type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/"
    kms_key_arn         = data.aws_kms_key.kms_key.arn
    prefix              = "databases/${module.database.name}/${module.glue-table-api-requests.name}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/"
    role_arn            = aws_iam_role.iam-role-firehose.arn
    s3_backup_mode      = "Disabled"
    compression_format  = "UNCOMPRESSED" # Must be UNCOMPRESSED when format_conversion is turned on

    cloudwatch_logging_options {
      enabled = false
    }

    processing_configuration {
      enabled = true

      processors {
        type = "Lambda"

        parameters {
          parameter_name  = "LambdaArn"
          parameter_value = "${resource.aws_lambda_function.lambda-function-format-firehose-logs.arn}:$LATEST"
        }
      }
    }

    data_format_conversion_configuration {
      input_format_configuration {
        deserializer {
          hive_json_ser_de {}
        }
      }

      output_format_configuration {
        serializer {
          parquet_ser_de {
            compression = "SNAPPY"
          }
        }
      }

      schema_configuration {
        database_name = local.database
        role_arn      = resource.aws_iam_role.iam-role-firehose.arn
        table_name    = module.glue-table-api-requests.name
      }
    }
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }
}

# CloudWatch Log Subscription
resource "aws_cloudwatch_log_subscription_filter" "cloudwatch-access-log-subscription" {
  name = "${local.full_name}-access-log-subscription"
  # Set the log group name so that if we use an environment ending in "-dev", it will get logs from
  # the "real" log group for that environment. So we could make an environment "prod-sbx-dev" that
  # we can use for development, and it will read from the "prod-sbx" environment.
  log_group_name  = "/bfd/${replace(local.environment, "-dev", "")}/bfd-server/access.json"
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.firehose-ingester.arn
  role_arn        = aws_iam_role.iam-role-cloudwatch-logs.arn
}
