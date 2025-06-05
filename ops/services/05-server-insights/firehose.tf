# Firehose Data Stream
resource "aws_kinesis_firehose_delivery_stream" "firehose_ingester" {
  name        = "${local.full_name}-firehose-ingester"
  destination = "extended_s3"

  extended_s3_configuration {
    bucket_arn          = data.aws_s3_bucket.bfd_insights_bucket.arn
    buffering_interval  = 300
    buffering_size      = 128
    error_output_prefix = "databases/${module.database.name}/${module.glue_table_api_requests.name}_errors/!{firehose:error-output-type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/"
    kms_key_arn         = data.aws_kms_key.kms_key.arn
    prefix              = "databases/${module.database.name}/${module.glue_table_api_requests.name}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/"
    role_arn            = aws_iam_role.iam_role_firehose.arn
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
          parameter_value = "${resource.aws_lambda_function.lambda_function_format_firehose_logs.arn}:$LATEST"
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
        role_arn      = resource.aws_iam_role.iam_role_firehose.arn
        table_name    = module.glue_table_api_requests.name
      }
    }
  }

  server_side_encryption {
    enabled  = true
    key_type = "AWS_OWNED_CMK"
  }
}

data "aws_cloudwatch_log_group" "server_access" {
  name = "/aws/ecs/bfd-${local.env}-cluster/${local.target_service}/${local.target_service}/access"
}

# TODO: Only here to ensure legacy server logs continue to be ingested; remove
resource "aws_cloudwatch_log_subscription_filter" "cloudwatch_access_log_subscription" {
  name = "${local.full_name}-access-log-subscription"
  # Set the log group name so that if we use an environment ending in "-dev", it will get logs from
  # the "real" log group for that environment. So we could make an environment "prod-sbx-dev" that
  # we can use for development, and it will read from the "prod-sbx" environment.
  log_group_name  = "/bfd/${replace(local.env, "-dev", "")}/bfd-server/access.json"
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.firehose_ingester.arn
  role_arn        = aws_iam_role.iam_role_cloudwatch_logs.arn
}

resource "aws_cloudwatch_log_subscription_filter" "ecs_access_log" {
  name            = "${local.full_name}-ecs-access-subscription"
  log_group_name  = data.aws_cloudwatch_log_group.server_access.name
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.firehose_ingester.arn
  role_arn        = aws_iam_role.iam_role_cloudwatch_logs.arn
}
