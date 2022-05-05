locals {
  tags     = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database = "bfd"
  project  = "bfd"
  table    = "beneficiaries"
  full_name   = "bfd-insights-${local.database}-${local.table}"
  account_id  = data.aws_caller_identity.current.account_id
}

module "bucket" {
  source      = "../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
}

module "database" {
  source          = "../../modules/database"
  database        = local.database
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
}

module "workgroup" {
  source          = "../../modules/workgroup"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  name            = local.database
  tags            = local.tags
}

module "glue_jobs" {
  source          = "../../modules/jobs"
  project         = local.project
  tags            = local.tags

  buckets         = [
    {bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn}
  ]
}

resource "aws_glue_crawler" "glue_crawler" {
  name          = local.project
  database_name = local.database
  role          = "bfd-insights/bfd-insights-${local.project}-glue-role"
  s3_target {
    path = "s3://${module.bucket.id}/databases/${local.project}"
  }

  configuration = jsonencode({
    CrawlerOutput = {
      Partitions = {
        AddOrUpdateBehavior = "InheritFromTable"
      }
    }
    Grouping = {
      TableGroupingPolicy     = "CombineCompatibleSchemas"
      TableLevelConfiguration = 4
    }
    Version = 1
  })

  schema_change_policy {
    delete_behavior = "DEPRECATE_IN_DATABASE"
    update_behavior = "UPDATE_IN_DATABASE"
  }
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
    name        = "bfd-insights-bfd-beneficiaries"
    path        = "/bfd-insights/"
    policy      = jsonencode(
        {
            Statement = [
                {
                    Action   = [
                        "glue:GetTable",
                        "glue:GetTableVersion",
                        "glue:GetTableVersions",
                    ]
                    Effect   = "Allow"
                    Resource = "*"
                    Sid      = ""
                },
                {
                    Action   = [
                        "s3:AbortMultipartUpload",
                        "s3:GetBucketLocation",
                        "s3:GetObject",
                        "s3:ListBucket",
                        "s3:ListBucketMultipartUploads",
                        "s3:PutObject",
                    ]
                    Effect   = "Allow"
                    Resource = [
                        "arn:aws:s3:::bfd-insights-bfd-577373831711",
                        "arn:aws:s3:::bfd-insights-bfd-577373831711/*",
                    ]
                    Sid      = ""
                },
                {
                    Action   = [
                        "kms:Encrypt",
                        "kms:Decrypt",
                        "kms:ReEncrypt*",
                        "kms:GenerateDataKey*",
                        "kms:DescribeKey",
                    ]
                    Effect   = "Allow"
                    Resource = [
                        "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea",
                    ]
                },
                {
                    Action   = [
                        "logs:PutLogEvents",
                    ]
                    Effect   = "Allow"
                    Resource = [
                        "arn:aws:logs:us-east-1:577373831711:log-group:/aws/kinesisfirehose/bfd-insights-bfd-beneficiaries:log-stream:*",
                    ]
                    Sid      = ""
                },
                {
                    Action   = [
                        "kinesis:DescribeStream",
                        "kinesis:GetShardIterator",
                        "kinesis:GetRecords",
                        "kinesis:ListShards",
                    ]
                    Effect   = "Allow"
                    Resource = "arn:aws:kinesis:us-east-1:577373831711:stream/bfd-insights-bfd-beneficiaries"
                    Sid      = ""
                },
            ]
            Version   = "2012-10-17"
        }
    )
    tags        = {}
    tags_all    = {}
}

resource "aws_iam_role" "firehose" {
    assume_role_policy    = jsonencode(
        {
            Statement = [
                {
                    Action    = "sts:AssumeRole"
                    Effect    = "Allow"
                    Principal = {
                        Service = "firehose.amazonaws.com"
                    }
                    Sid       = ""
                },
            ]
            Version   = "2012-10-17"
        }
    )
    force_detach_policies = false
    managed_policy_arns   = [
        "arn:aws:iam::577373831711:policy/bfd-insights/bfd-insights-bfd-beneficiaries",
    ]
    max_session_duration  = 3600
    name                  = "bfd-insights-bfd-beneficiaries"
    path                  = "/"
    tags                  = {
        "application" = "bfd-insights"
        "business"    = "OEDA"
        "project"     = "bfd"
    }
    tags_all              = {
        "application" = "bfd-insights"
        "business"    = "OEDA"
        "project"     = "bfd"
    }

    inline_policy {
        name   = "bfd-transform-lambda"
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
                Version   = "2012-10-17"
            }
        )
    }
}

resource "aws_iam_role_policy_attachment" "main" {
    policy_arn = "arn:aws:iam::577373831711:policy/bfd-insights/bfd-insights-bfd-beneficiaries"
    role       = "bfd-insights-bfd-beneficiaries"
}

resource "aws_kinesis_firehose_delivery_stream" "main" {
    arn            = "arn:aws:firehose:us-east-1:577373831711:deliverystream/bfd-insights-bfd-beneficiaries"
    destination    = "extended_s3"
    destination_id = "destinationId-000000000001"
    name           = "bfd-insights-bfd-beneficiaries"
    tags           = {
        "application" = "bfd-insights"
        "business"    = "OEDA"
        "project"     = "bfd"
    }
    tags_all       = {
        "application" = "bfd-insights"
        "business"    = "OEDA"
        "project"     = "bfd"
    }
    version_id     = "2"

    extended_s3_configuration {
        bucket_arn          = "arn:aws:s3:::bfd-insights-bfd-577373831711"
        buffer_interval     = 60
        buffer_size         = 5
        compression_format  = "GZIP"
        error_output_prefix = "databases/bfd/beneficiaries_errors/!{firehose:error-output-type}/!{timestamp:yyyy-MM-dd}/"
        kms_key_arn         = "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea"
        prefix              = "databases/bfd/beneficiaries/dt=!{timestamp:yyyy-MM-dd}/"
        role_arn            = "arn:aws:iam::577373831711:role/bfd-insights-bfd-beneficiaries"
        s3_backup_mode      = "Disabled"

        cloudwatch_logging_options {
            enabled = false
        }

        processing_configuration {
            enabled = true

            processors {
                type = "Lambda"

                parameters {
                    parameter_name  = "LambdaArn"
                    parameter_value = "arn:aws:lambda:us-east-1:577373831711:function:bfd-transform:$LATEST"
                }
            }
        }
    }

    server_side_encryption {
        enabled  = true
        key_type = "AWS_OWNED_CMK"
    }
}

resource "aws_cloudwatch_log_subscription_filter" "poc_access_json" {
  name            = "poc_access_json"
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
