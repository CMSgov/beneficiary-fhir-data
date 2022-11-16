# Current Account
data "aws_caller_identity" "current" {}

# BFD Analysts IAM Group
data "aws_iam_group" "iam-group-bfd-analysts" {
  group_name = "bfd-insights-analysts"
}


# Firehose Ingestion

# CloudWatch Role
resource "aws_iam_role" "iam-role-cloudwatch-logs" {
  name        = "${local.full_name}-cloudwatch-logs-role"
  description = "Allows access to the BFD Insights Firehose Delivery Stream and Export to S3"
  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "logs.us-east-1.amazonaws.com"
          }
          Sid = ""
        },
      ]
      Version = "2012-10-17"
    }
  )
  inline_policy {
    name = "${local.full_name}-cloudwatch-logs-policy"

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action   = ["firehose:*"]
          Effect   = "Allow"
          Resource = ["arn:aws:firehose:us-east-1:${data.aws_caller_identity.current.account_id}:deliverystream/${local.full_name}-firehose-ingester"]
        }
      ]
    })
  }
}

# Firehose Policy
resource "aws_iam_policy" "iam-policy-firehose" {
  description = "Allow firehose delivery to insights S3 bucket"
  name        = "${local.full_name}-firehose-to-s3-policy"
  policy = jsonencode(
    {
      Statement = [
        {
          Action = [
            "glue:GetTable",
            "glue:GetTableVersion",
            "glue:GetTableVersions",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:glue:us-east-1:${data.aws_caller_identity.current.account_id}:table/${module.database.name}/${module.glue-table-api-requests.name}",
            "arn:aws:glue:us-east-1:${data.aws_caller_identity.current.account_id}:database/${module.database.name}",
            "arn:aws:glue:us-east-1:${data.aws_caller_identity.current.account_id}:catalog"
          ]
          Sid = "GetGlueTable"
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
            data.aws_s3_bucket.bfd-insights-bucket.arn,
            "${data.aws_s3_bucket.bfd-insights-bucket.arn}/*",
          ]
          Sid = "GetS3Bucket"
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
            data.aws_kms_key.kms_key.arn
          ],
          Sid = "UseKMSKey"
        },
        {
          Action = [
            "logs:PutLogEvents",
          ]
          Effect = "Allow"
          Resource = [
            "arn:aws:logs:us-east-1:577373831711:log-group:/aws/kinesisfirehose/${local.full_name}-firehose:log-stream:*",
          ]
          Sid = "PutLogEvents"
        },
      ]
      Version = "2012-10-17"
    }
  )
}

# Firehose Role
resource "aws_iam_role" "iam-role-firehose" {
  name                  = "${local.full_name}-firehose-role"
  description           = ""
  path                  = "/"
  force_detach_policies = false
  managed_policy_arns = [
    aws_iam_policy.iam-policy-firehose.arn,
  ]
  max_session_duration = 3600
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
  inline_policy {
    name = "${local.full_name}-invoke-cw-to-flattened-json"
    policy = jsonencode(
      {
        Statement = [
          {
            Action = "lambda:InvokeFunction"
            Effect = "Allow"
            Resource = [
              "arn:aws:lambda:us-east-1:577373831711:function:${local.full_name}-cw-to-flattened-json",
              "arn:aws:lambda:us-east-1:577373831711:function:${local.full_name}-cw-to-flattened-json:$LATEST"
            ]
            Sid = "InvokeCW2Json"
          },
        ]
        Version = "2012-10-17"
      }
    )
  }
}

# Lambda Role
resource "aws_iam_role" "iam-role-firehose-lambda" {
  name                  = "${local.full_name}-firehose-lambda-role"
  description           = "Allow Lambda to create and write to its log group"
  path                  = "/service-role/"
  max_session_duration  = 3600
  force_detach_policies = false
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
  inline_policy {
    name = "${local.full_name}-lambda-policy"
    policy = jsonencode({
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Effect" : "Allow",
          "Action" : "logs:CreateLogGroup",
          "Resource" : "arn:aws:logs:us-east-1:577373831711:*"
        },
        {
          "Effect" : "Allow",
          "Action" : [
            "logs:CreateLogStream",
            "logs:PutLogEvents"
          ],
          "Resource" : [
            "arn:aws:logs:us-east-1:577373831711:log-group:/aws/lambda/${local.full_name}-cw-to-flattened-json:*"
          ]
        },
        {
          "Effect" : "Allow",
          "Action" : [
            "firehose:PutRecordBatch"
          ],
          "Resource" : [
            "arn:aws:firehose:us-east-1:577373831711:deliverystream/${local.full_name}-firehose-ingester"
          ]
        }
      ]
    })
  }
}


# Glue

# Role for Glue to assume with S3 permissions
data "aws_iam_role" "iam-role-glue" {
  name = "bfd-insights-bfd-glue-role"
}
