data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd_analysts" {
  group_name = "bfd-insights-analysts"
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

# TODO: Work out the policies required
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

resource "aws_iam_role" "bfd-transform-role-rlenc44a" {
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "lambda.amazonaws.com"
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = false
  managed_policy_arns = [
    "arn:aws:iam::577373831711:policy/service-role/AWSLambdaBasicExecutionRole-53dca3ce-b863-43c9-8603-1895a319a671",
  ]
  max_session_duration = 3600
  name                 = "bfd-transform-role-rlenc44a"
  path                 = "/service-role/"
  tags                 = {}
  tags_all             = {}
}

# TODO: "bfd-insights/bfd-insights-bfd-glue-role" does not seem to be in terraform

