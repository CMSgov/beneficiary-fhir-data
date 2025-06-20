## Glue Role

resource "aws_iam_role" "glue_role" {
  name        = "bfd-insights-${var.project}-glue-role"
  description = "Allow the Glue service to access the Insights buckets" 
  tags        = var.tags
  path        = "/bfd-insights/"

  assume_role_policy = <<-POLICY
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "glue.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": "assume"
      }
    ]
  }
  POLICY
}

## Find and create policies for the role

data "aws_iam_policy" "glue_service" {
  arn  = "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole"
}

data "aws_iam_policy" "athena_service" {
  arn  = "arn:aws:iam::aws:policy/AmazonAthenaFullAccess"
}

data "aws_iam_policy_document" "s3_access" {
  statement {
    sid = "testData"
    actions = [
      "s3:GetBucketLocation",
      "s3:HeadBucket",
      "s3:ListBucket",
      "s3:GetObject*"
    ]
    resources = ["arn:aws:s3:::awsglue-datasets", "arn:aws:s3:::awsglue-datasets/*"]
  }

  statement {
    sid = "s3Buckets"
    actions = [
      "s3:GetBucketLocation",
      "s3:HeadBucket",
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads",
    ]
    resources = var.buckets[*].bucket
  }

  statement {
    sid = "s3Objects"
    actions = [
      "s3:ListMultipartUploadParts",
      "s3:AbortMultipartUpload",
      "s3:GetObject*",
      "s3:PutObject*",
      "s3:DeleteObject*"
    ]
    resources = formatlist("%s/%s", var.buckets[*].bucket, "*") 
  }

  statement {
    sid = "CMK"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey"
    ]
    resources = var.buckets[*].cmk
  }
}

resource "aws_iam_policy" "s3_access" {
  name    = "bfd-insights-${var.project}-glue-role-s3-access"
  path    = "/bfd-insights/"
  policy  = data.aws_iam_policy_document.s3_access.json
}


## Attach policies to the role

locals {
  glue_role_policies = [
    data.aws_iam_policy.glue_service.arn, 
    aws_iam_policy.s3_access.arn,
    data.aws_iam_policy.athena_service.arn
  ]
}

resource "aws_iam_role_policy_attachment" "glue_role_attach" {
  count       = length(local.glue_role_policies)
  role        = aws_iam_role.glue_role.name
  policy_arn  = local.glue_role_policies[count.index]
}
