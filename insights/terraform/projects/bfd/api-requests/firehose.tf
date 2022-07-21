# Firehose Data Stream
resource "aws_kinesis_firehose_delivery_stream" "firehose-ingester" {
  name           = "${local.full_name}-firehose-ingester"
  destination    = "extended_s3"

  extended_s3_configuration {
    bucket_arn          = data.aws_s3_bucket.bfd-insights-bucket.arn
    buffer_interval     = 60
    buffer_size         = 128
    error_output_prefix = "databases/${module.database.name}/${module.glue-table-api-history.name}_errors/!{firehose:error-output-type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    kms_key_arn         = data.aws_kms_key.kms_key.arn
    prefix              = "databases/${module.database.name}/${module.glue-table-api-history.name}/firehose:year=!{timestamp:yyyy};month=!{timestamp:MM}/day:!{timestamp:dd}/"
    role_arn            = aws_iam_role.iam-role-firehose.arn
    s3_backup_mode      = "Disabled"
    compression_format  = "GZIP"

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
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }
}

# CloudWatch Log Subscription
resource "aws_cloudwatch_log_subscription_filter" "cloudwatch-access-log-subscription" {
  name            = "${local.full_name}-access-log-subscription"
  # Set the log group name so that if we use an environment ending in "-dev", it will get logs from
  # the "real" log group for that environment. So we could make an environment "prod-sbx-dev" that
  # we can use for development, and it will read from the "prod-sbx" environment.
  log_group_name  = "/bfd/${replace(local.environment, "-dev", "")}/bfd-server/access.json"
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.firehose-ingester.arn
  role_arn        = aws_iam_role.iam-role-cloudwatch-logs.arn
}
