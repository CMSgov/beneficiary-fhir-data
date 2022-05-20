locals {
  tags       = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database   = "bfd"
  project    = "bfd"
  table      = "api-requests"
  full_name  = "bfd-insights-${local.database}-${local.table}"
  account_id = data.aws_caller_identity.current.account_id
}

module "bucket" {
  source      = "../../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
}

module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  tags       = local.tags
}

module "workgroup" {
  source     = "../../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
  tags       = local.tags
}

module "glue_jobs" {
  source  = "../../../modules/jobs"
  project = local.project
  tags    = local.tags

  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn }
  ]
}

data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd_analysts" {
  group_name = "bfd-insights-analysts"
}

resource "aws_iam_group_policy" "poc" {
  name   = "foo"
  group  = data.aws_iam_group.bfd_analysts.group_name
  policy = module.bucket.iam_full_policy_body
}

resource "aws_iam_policy" "firehose" {
  description = "Allow firehose delivery to bfd-insights-bfd-577373831711"
  name        = "bfd-insights-bfd-api-requests"
  path        = "/bfd-insights/"
  policy = jsonencode(
    {
      Statement = [
        {
          Action = [
            "glue:GetTable",
            "glue:GetTableVersion",
            "glue:GetTableVersions",
          ]
          Effect   = "Allow"
          Resource = "*"
          Sid      = ""
        },
        {
          Action = [
            "s3:AbortMultipartUpload",
            "s3:GetBucketLocation",
            "s3:GetObject",
            "s3:ListBucket",
            "s3:ListBucketMultipartUploads",
            "s3:PutObject",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:s3:::bfd-insights-bfd-577373831711",
            "arn:aws:s3:::bfd-insights-bfd-577373831711/*",
          ]
          Sid = ""
        },
        {
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea",
          ]
        },
        {
          Action = [
            "logs:PutLogEvents",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:logs:us-east-1:577373831711:log-group:/aws/kinesisfirehose/bfd-insights-bfd-api-requests:log-stream:*",
          ]
          Sid = ""
        },
        {
          Action = [
            "kinesis:DescribeStream",
            "kinesis:GetShardIterator",
            "kinesis:GetRecords",
            "kinesis:ListShards",
          ]
          Effect   = "Allow"
          Resource = "arn:aws:kinesis:us-east-1:577373831711:stream/bfd-insights-bfd-api-requests"
          Sid      = ""
        },
      ]
      Version = "2012-10-17"
    }
  )
  tags     = {}
  tags_all = {}
}

resource "aws_iam_role" "firehose" {
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "firehose.amazonaws.com"
          }
          Sid = ""
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = false
  managed_policy_arns = [
    "arn:aws:iam::577373831711:policy/bfd-insights/bfd-insights-bfd-api-requests",
  ]
  max_session_duration = 3600
  name                 = "bfd-insights-bfd-api-requests"
  path                 = "/"
  tags = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }
  tags_all = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }

  inline_policy {
    name = "bfd-transform-lambda"
    policy = jsonencode(
      {
        Statement = [
          {
            Action   = "lambda:InvokeFunction"
            Effect   = "Allow"
            Resource = "arn:aws:lambda:us-east-1:577373831711:function:bfd-transform:$LATEST"
            Sid      = "VisualEditor0"
          },
        ]
        Version = "2012-10-17"
      }
    )
  }
  inline_policy {
    name = "invoke-bfd-cw-to-flattened-json"
    policy = jsonencode(
      {
        Statement = [
          {
            Action   = "lambda:InvokeFunction"
            Effect   = "Allow"
            Resource = "arn:aws:lambda:us-east-1:577373831711:function:bfd-cw-to-flattened-json:$LATEST"
            Sid      = "VisualEditor0"
          },
        ]
        Version = "2012-10-17"
      }
    )
  }
}

resource "aws_iam_role_policy_attachment" "main" {
  policy_arn = "arn:aws:iam::577373831711:policy/bfd-insights/bfd-insights-bfd-api-requests"
  role       = "bfd-insights-bfd-api-requests"
}

resource "aws_kinesis_firehose_delivery_stream" "main" {
  arn            = "arn:aws:firehose:us-east-1:577373831711:deliverystream/bfd-insights-bfd-api-requests"
  destination    = "extended_s3"
  destination_id = "destinationId-000000000001"
  name           = "bfd-insights-bfd-api-requests"
  tags = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }
  tags_all = {
    "application" = "bfd-insights"
    "business"    = "OEDA"
    "project"     = "bfd"
  }
  version_id = "10"

  extended_s3_configuration {
    bucket_arn          = "arn:aws:s3:::bfd-insights-bfd-577373831711"
    buffer_interval     = 60
    buffer_size         = 128
    compression_format  = "GZIP"
    error_output_prefix = "databases/bfd/api_requests_errors/!{firehose:error-output-type}/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    kms_key_arn         = "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea"
    prefix              = "databases/bfd/test_api_requests/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    role_arn            = "arn:aws:iam::577373831711:role/bfd-insights-bfd-api-requests"
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
        region        = "us-east-1"
        role_arn      = "arn:aws:iam::577373831711:role/bfd-insights-bfd-api-requests"
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
          parameter_value = "arn:aws:lambda:us-east-1:577373831711:function:bfd-cw-to-flattened-json:$LATEST"
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
  destination_arn = "arn:aws:firehose:us-east-1:${local.account_id}:deliverystream/${local.full_name}"
  role_arn        = aws_iam_role.cloudwatch_role.arn
}

data "aws_iam_policy_document" "trust_rel_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["logs.us-east-1.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cloudwatch_role" {
  name               = "${local.full_name}-cwl2firehose-role"
  assume_role_policy = data.aws_iam_policy_document.trust_rel_assume_role_policy.json

  inline_policy {
    name = "${local.full_name}-cwl2firehose-policy"

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action   = ["firehose:*"]
          Effect   = "Allow"
          Resource = ["arn:aws:firehose:us-east-1:${local.account_id}:*"]
        },
      ]
    })
  }
}

data "archive_file" "zip_the_python_code" {
  type        = "zip"
  source_dir  = "${path.module}/lambda_src/"
  output_path = "${path.module}/lambda_src/bfd-cw-to-flattened-json.zip"
}

resource "aws_lambda_function" "bfd-cw-to-flattened-json" {
  architectures = [
    "x86_64",
  ]
  description                    = "Extracts and flattens JSON messages from CloudWatch log subscriptions."
  function_name                  = "bfd-cw-to-flattened-json"
  filename                       = "${path.module}/lambda_src/bfd-cw-to-flattened-json.zip"
  source_code_hash               = filebase64sha256("${path.module}/lambda_src/bfd-cw-to-flattened-json.zip")
  handler                        = "bfd-cw-to-flattened-json.lambda_handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = "arn:aws:iam::577373831711:role/service-role/bfd-transform-role-rlenc44a"
  runtime                        = "python3.8"
  tags = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }
  tags_all = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }
  timeout = 300

  ephemeral_storage {
    size = 512
  }

  timeouts {}

  tracing_config {
    mode = "PassThrough"
  }
}

resource "aws_s3_bucket" "bfd-insights-bfd-app-logs" {
    bucket                      = "bfd-insights-bfd-app-logs"
    hosted_zone_id              = "Z3AQBSTGFYJSTF"
    object_lock_enabled         = false
    policy                      = jsonencode(
        {
            Statement = [
                {
                    Action    = "s3:GetBucketAcl"
                    Effect    = "Allow"
                    Principal = {
                        Service = "logs.us-east-1.amazonaws.com"
                    }
                    Resource  = "arn:aws:s3:::bfd-insights-bfd-app-logs"
                },
                {
                    Action    = "s3:PutObject"
                    Condition = {
                        StringEquals = {
                            "s3:x-amz-acl" = "bucket-owner-full-control"
                        }
                    }
                    Effect    = "Allow"
                    Principal = {
                        Service = "logs.us-east-1.amazonaws.com"
                    }
                    Resource  = "arn:aws:s3:::bfd-insights-bfd-app-logs/*"
                },
                {
                    Action    = "s3:*"
                    Condition = {
                        Bool = {
                            "aws:SecureTransport" = "false"
                        }
                    }
                    Effect    = "Deny"
                    Principal = "*"
                    Resource  = [
                        "arn:aws:s3:::bfd-insights-bfd-app-logs",
                        "arn:aws:s3:::bfd-insights-bfd-app-logs/*",
                    ]
                    Sid       = "AllowSSLRequestsOnly"
                },
            ]
            Version   = "2012-10-17"
        }
    )
    request_payer               = "BucketOwner"
    tags                        = {}
    tags_all                    = {}

    grant {
        id          = "c393fda6bb2079d44a0284a5164e871a4555039d6c6f973c9e5db5f4d7d76b1a"
        permissions = [
            "FULL_CONTROL",
        ]
        type        = "CanonicalUser"
    }

    server_side_encryption_configuration {
        rule {
            bucket_key_enabled = false

            apply_server_side_encryption_by_default {
                sse_algorithm = "AES256"
            }
        }
    }

    versioning {
        enabled    = false
        mfa_delete = false
    }
}
