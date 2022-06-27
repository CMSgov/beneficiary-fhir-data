# Current Account
data "aws_caller_identity" "current" {}

# BFD Analysts IAM Group
data "aws_iam_group" "bfd-analysts" {
  group_name = "bfd-insights-analysts"
}

# AssumeRole Policy Document
data "aws_iam_policy_document" "trust_rel_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["logs.us-east-1.amazonaws.com"]
    }
  }
}


# Firehose Ingestion

# CloudWatch Role
resource "aws_iam_role" "cloudwatch_role" {
  name               = "${local.full_name}-cloudwatch-logs-role"
  description        = "Allows access to the BFD Insights Firehose Delivery Stream and Export to S3"
  assume_role_policy = data.aws_iam_policy_document.trust_rel_assume_role_policy.json

  inline_policy {
    name = "${local.full_name}-cloudwatch-logs-policy"

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action   = ["firehose:*"]
          Effect   = "Allow"
          Resource = ["arn:aws:firehose:us-east-1:${data.aws_caller_identity.current.account_id}:deliverystream/${local.full_name}-firehose"]
        }
      ]
    })
  }
}

# Firehose Policy
resource "aws_iam_policy" "firehose_policy" {
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
          Effect   = "Allow"
          Resource = "arn:aws:glue:us-east-1:${data.aws_caller_identity.current.account_id}:table/${module.database.name}/${aws_glue_catalog_table.api-requests-table.name}"
          Sid      = "GetGlueTable"
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
resource "aws_iam_role" "firehose_role" {
  name                  = "${local.full_name}-firehose-role"
  description           = ""
  path                  = "/"
  force_detach_policies = false
  managed_policy_arns   = [
    aws_iam_policy.firehose_policy.arn,
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
            Action   = "lambda:InvokeFunction"
            Effect   = "Allow"
            Resource = [
              "arn:aws:lambda:us-east-1:577373831711:function:${local.full_name}-cw-to-flattened-json",
              "arn:aws:lambda:us-east-1:577373831711:function:${local.full_name}-cw-to-flattened-json:$LATEST"
            ]
            Sid      = "InvokeCW2Json"
          },
        ]
        Version = "2012-10-17"
      }
    )
  }
}

# Lambda Role
resource "aws_iam_role" "firehose-lambda-role" {
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
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": "logs:CreateLogGroup",
              "Resource": "arn:aws:logs:us-east-1:577373831711:*"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "logs:CreateLogStream",
                  "logs:PutLogEvents"
              ],
              "Resource": [
                  "arn:aws:logs:us-east-1:577373831711:log-group:/aws/lambda/${local.full_name}-cw-to-flattened-json:*"
              ]
          }
      ]
    })
  }
}


# Glue

# Glue Role
resource "aws_iam_role" "glue-role" {
  name                 = "${local.full_name}-glue-role"
  description          = "Allow the Glue service to access the BFD Insights S3 and Glue resources"
  max_session_duration = 3600
  managed_policy_arns = [
    aws_iam_policy.bfd-insights-bfd-glue-role-s3-access.arn,
    data.aws_iam_policy.athena-full-access.arn,
    data.aws_iam_policy.glue-service-role.arn,
  ]
  assume_role_policy   = jsonencode(
    {
      Statement = [
        {
          Action    = "sts:AssumeRole"
          Effect    = "Allow"
          Principal = {
            Service = "glue.amazonaws.com"
          }
          Sid       = "assume"
        },
      ]
      Version   = "2012-10-17"
    }
  )
}

data "aws_iam_policy" "athena-full-access" {
  name = "AmazonAthenaFullAccess"
}

data "aws_iam_policy" "glue-service-role" {
  name = "AWSGlueServiceRole"
}

# Access Policy for S3 Access
resource "aws_iam_policy" "bfd-insights-bfd-glue-role-s3-access" {
  name = "${local.full_name}-glue-s3-access"
  description = "Allow Glue Role to access insights S3 bucket"
  policy = jsonencode(
    {
      "Statement": [
          {
              "Action": [
                  "s3:ListBucket",
                  "s3:HeadBucket",
                  "s3:GetObject*",
                  "s3:GetBucketLocation"
              ],
              "Effect": "Allow",
              "Resource": [
                  "arn:aws:s3:::${data.aws_s3_bucket.bfd-insights-bucket.id}/*",
                  "arn:aws:s3:::${data.aws_s3_bucket.bfd-insights-bucket.id}"
              ],
              "Sid": "s3BFDResources"
          },
          {
              "Action": [
                  "s3:ListBucketMultipartUploads",
                  "s3:ListBucket",
                  "s3:HeadBucket",
                  "s3:GetBucketLocation"
              ],
              "Effect": "Allow",
              "Resource": data.aws_s3_bucket.bfd-insights-bucket.arn,
              "Sid": "s3Buckets"
          },
          {
              "Action": [
                  "s3:PutObject*",
                  "s3:ListMultipartUploadParts",
                  "s3:GetObject*",
                  "s3:DeleteObject*",
                  "s3:AbortMultipartUpload"
              ],
              "Effect": "Allow",
              "Resource": "${data.aws_s3_bucket.bfd-insights-bucket.arn}/databases/*",
              "Sid": "s3Objects"
          },
          {
              "Action": [
                  "kms:ReEncrypt*",
                  "kms:GenerateDataKey*",
                  "kms:Encrypt",
                  "kms:DescribeKey",
                  "kms:Decrypt"
              ],
              "Effect": "Allow",
              "Resource": data.aws_kms_key.kms_key.arn
              "Sid": "CMK"
          }
      ],
      "Version": "2012-10-17"
    }
  )
}
