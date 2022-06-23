data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd-analysts" {
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
          Resource = ["arn:aws:firehose:us-east-1:${data.aws_caller_identity.current.account_id}:deliverystream/${local.full_name}-firehose"]
        },
      ]
    })
  }
}

resource "aws_iam_policy" "firehose_policy" {
  description = "Allow firehose delivery to insights S3 bucket"
  name        = "${local.full_name}-api-requests"
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
            data.aws_s3_bucket.bfd-insights-bucket.arn,
            "${data.aws_s3_bucket.bfd-insights-bucket.arn}/*",
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
            data.aws_kms_key.kms_key.arn
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
        # {
        #   Action = [
        #     "kinesis:DescribeStream",
        #     "kinesis:GetShardIterator",
        #     "kinesis:GetRecords",
        #     "kinesis:ListShards",
        #   ]
        #   Effect   = "Allow"
        #   Resource = "arn:aws:kinesis:us-east-1:577373831711:stream/bfd-insights-bfd-api-requests"
        #   Sid      = ""
        # },
      ]
      Version = "2012-10-17"
    }
  )
  tags     = {}
  tags_all = {}
}

resource "aws_iam_role" "firehose_role" {
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
    aws_iam_policy.firehose_policy.arn,
  ]
  max_session_duration = 3600
  name                 = "${local.full_name}-api-requests"
  path                 = "/"
  # tags = {
  #   "application" = "bfd-insights"
  #   "business"    = "OEDA"
  #   "project"     = "bfd"
  # }
  # tags_all = {
  #   "application" = "bfd-insights"
  #   "business"    = "OEDA"
  #   "project"     = "bfd"
  # }

  # inline_policy {
  #   name = "${local.full_name}-transform-lambda"
  #   policy = jsonencode(
  #     {
  #       Statement = [
  #         {
  #           Action   = "lambda:InvokeFunction"
  #           Effect   = "Allow"
  #           Resource = "arn:aws:lambda:us-east-1:577373831711:function:bfd-transform:$LATEST"
  #           Sid      = "VisualEditor0"
  #         },
  #       ]
  #       Version = "2012-10-17"
  #     }
  #   )
  # }
  inline_policy {
    name = "${local.full_name}-invoke-cw-to-flattened-json"
    policy = jsonencode(
      {
        Statement = [
          {
            Action   = "lambda:InvokeFunction"
            Effect   = "Allow"
            Resource = "arn:aws:lambda:us-east-1:577373831711:function:${local.full_name}-cw-to-flattened-json"
            Sid      = "VisualEditor0"
          },
        ]
        Version = "2012-10-17"
      }
    )
  }
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
          # {
          #     "Effect": "Allow",
          #     "Action": [
          #         "logs:CreateLogStream",
          #         "logs:PutLogEvents"
          #     ],
          #     "Resource": [
          #         "arn:aws:logs:us-east-1:577373831711:log-group:/aws/lambda/bfd-transform:*"
          #     ]
          # },
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
  max_session_duration = 3600
  name                 = "${local.full_name}-transform-role-rlenc44a"
  path                 = "/service-role/"
  tags                 = {}
  tags_all             = {}
}

resource "aws_iam_role" "glue-role" {
  name                 = "${local.full_name}-glue-role"
  description          = "Allow the Glue service to access the Insights buckets"
  # tags                 = local.tags
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

# TODO: Move to common?
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
              "Resource": data.aws_s3_bucket.bfd-app-logs.arn,
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
              "Resource": [
                "${data.aws_s3_bucket.bfd-app-logs.arn}/*",
                "${data.aws_s3_bucket.bfd-insights-bucket.arn}/databases/*"
              ],
              "Sid": "s3Objects"
          },
          {
              "Action": [
                  "s3:ListBucketMultipartUploads",
                  "s3:ListBucket",
                  "s3:HeadBucket",
                  "s3:GetBucketLocation"
              ],
              "Effect": "Allow",
              "Resource": data.aws_s3_bucket.bfd-app-logs.arn,
              "Sid": "s3BucketsAppLogs"
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
              "Resource": "${data.aws_s3_bucket.bfd-app-logs.arn}/*",
              "Sid": "s3ObjectsAppLogs"
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
              # "arn:aws:kms:us-east-1:577373831711:key/9bfd6886-7124-4229-931a-4a30ce61c0ea",
              "Sid": "CMK"
          }
      ],
      "Version": "2012-10-17"
    }
  )
}
