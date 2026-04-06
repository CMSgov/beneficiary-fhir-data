locals {
  s3logs_firehose_name = "${local.name_prefix}-s3logs"
}

module "bucket_s3logs" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = local.s3logs_firehose_name
  force_destroy      = local.is_ephemeral_env
}

resource "aws_cloudwatch_log_group" "s3logs" {
  name              = "/aws/kinesisfirehose/${local.s3logs_firehose_name}"
  kms_key_id        = local.env_key_arn
  retention_in_days = local.ten_year_retention_days
  skip_destroy      = true
}

resource "aws_kinesis_firehose_delivery_stream" "s3logs" {
  depends_on = [aws_iam_role_policy_attachment.s3logs_firehose]

  destination = "extended_s3"
  name        = local.s3logs_firehose_name

  server_side_encryption {
    enabled  = true
    key_arn  = local.env_key_arn
    key_type = "CUSTOMER_MANAGED_CMK"
  }

  extended_s3_configuration {
    bucket_arn          = module.bucket_s3logs.bucket.arn
    buffering_interval  = 300
    buffering_size      = 128
    compression_format  = "UNCOMPRESSED"
    custom_time_zone    = "UTC"
    error_output_prefix = "errors/"
    file_extension      = ".json"
    role_arn            = aws_iam_role.s3logs_firehose.arn
    s3_backup_mode      = "Disabled"

    cloudwatch_logging_options {
      enabled         = true
      log_group_name  = aws_cloudwatch_log_group.s3logs.name
      log_stream_name = "DestinationDelivery"
    }

    dynamic_partitioning_configuration {
      enabled        = true
      retry_duration = 300
    }

    processing_configuration {
      enabled = true

      processors {
        type = "RecordDeAggregation"

        parameters {
          parameter_name  = "SubRecordType"
          parameter_value = "JSON"
        }
      }

      processors {
        type = "MetadataExtraction"

        parameters {
          parameter_name  = "JsonParsingEngine"
          parameter_value = "JQ-1.6"
        }

        parameters {
          parameter_name = "MetadataExtractionQuery"
          # This is actually a jq object expression which is JSON-like, but not quite real JSON
          parameter_value = "{${join(",", sort(
            [
              for k, v in {
                level      = "(.[\"log.level\"] // \"unknown\")"
                cert_alias = "(.[\"mdc.certificateAlias\"] // \"unknown\")"
                resource   = "(.resource // \"unknown\")"
              } : "${k}:${v}"
            ]
          ))}}"
        }
      }

      processors {
        type = "Decompression"

        parameters {
          parameter_name  = "CompressionFormat"
          parameter_value = "GZIP"
        }
      }

      processors {
        type = "CloudWatchLogProcessing"

        parameters {
          parameter_name  = "DataMessageExtraction"
          parameter_value = "true"
        }
      }
    }

    prefix = "${join("/", [
      # Normally a map would be used, but the order is important here and maps have no ordering.
      for tupl in [
        ["year", { filter = "timestamp", value = "yyyy" }],
        ["month", { filter = "timestamp", value = "MM" }],
        ["day", { filter = "timestamp", value = "dd" }],
        ["hour", { filter = "timestamp", value = "hh" }],
        ["level", { filter = "partitionKeyFromQuery", value = "level" }],
        ["cert_alias", { filter = "partitionKeyFromQuery", value = "cert_alias" }],
        ["resource", { filter = "partitionKeyFromQuery", value = "resource" }]
      ] : "${tupl[0]}=!{${tupl[1].filter}:${tupl[1].value}}"
    ])}/"
  }
}

resource "aws_cloudwatch_log_subscription_filter" "s3logs" {
  depends_on = [aws_iam_role_policy_attachment.s3logs_cw]

  name            = "${local.name_prefix}-message-log-subscription"
  log_group_name  = aws_cloudwatch_log_group.server_messages.name
  filter_pattern  = ""
  destination_arn = aws_kinesis_firehose_delivery_stream.s3logs.arn
  role_arn        = aws_iam_role.s3logs_cw.arn
}
