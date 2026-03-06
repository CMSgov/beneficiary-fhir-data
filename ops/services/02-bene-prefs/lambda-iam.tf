data "aws_iam_policy_document" "lambda_cloudwatch" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.this.arn}:*"]
  }
}

data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "lambda_cloudwatch" {
  name = "${local.lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.lambda_cloudwatch.json
}

data "aws_iam_policy_document" "lambda_ssm" {
  statement {
    sid     = "AllowAccessToUsedParams"
    actions = ["ssm:GetParameter"]
    resources = flatten([[
      for param in [
        "sensitive/idr_account",
        "sensitive/idr_database",
        "sensitive/idr_schema",
        "sensitive/idr_username",
        "sensitive/idr_private_key",
        "sensitive/idr_warehouse",
      ]
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/idr-pipeline/${param}"
    ], [for k in module.buckets : k.ssm_bucket_name[0].arn]])
  }
}

resource "aws_iam_policy" "lambda_ssm" {
  name = "${local.lambda_full_name}-ssm"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to get SSM parameters"
  ])
  policy = data.aws_iam_policy_document.lambda_ssm.json
}

data "aws_iam_policy_document" "lambda_kms" {
  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "lambda_kms" {
  name = "${local.lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to use the ",
    "${local.env_key_alias} key"
  ])
  policy = data.aws_iam_policy_document.lambda_kms.json
}


data "aws_iam_policy_document" "lambda_dynamodb" {
  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["dynamodb:*"]
    resources = [aws_dynamodb_table.this[0].arn]
  }
}

resource "aws_iam_policy" "lambda_dynamodb" {
  name = "${local.lambda_full_name}-dynamodb"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to use the ",
    "${aws_dynamodb_table.this[0].name} table"
  ])
  policy = data.aws_iam_policy_document.lambda_dynamodb.json
}

data "aws_iam_policy_document" "lambda_s3" {
  statement {
    sid = "AllowS3BucketAccess"
    actions = [
      "s3:PutObject",
      "s3:PutObjectAcl",
      "s3:AbortMultipartUpload",
      "s3:GetBucketLocation",
      "s3:GetObject",
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads",
      "s3:ListMultipartUploadParts"
    ]
    resources = flatten([for k in module.buckets : [k.bucket.arn, "${k.bucket.arn}/*"]])
  }
}

resource "aws_iam_policy" "lambda_s3" {
  name = "${local.lambda_full_name}-s3"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to interact with the ",
    "${local.service} partner S3 Buckets"
  ])
  policy = data.aws_iam_policy_document.lambda_s3.json
}

data "aws_iam_policy" "lambda_vpc_access_role" {
  name = "AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role" "lambda" {
  name                  = local.lambda_full_name
  path                  = local.iam_path
  description           = "TODO"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "lambda" {
  for_each = {
    cloudwatch = aws_iam_policy.lambda_cloudwatch.arn
    dynamodb   = aws_iam_policy.lambda_dynamodb.arn
    kms        = aws_iam_policy.lambda_kms.arn
    s3         = aws_iam_policy.lambda_s3.arn
    ssm        = aws_iam_policy.lambda_ssm.arn
    vpc_access = data.aws_iam_policy.lambda_vpc_access_role.arn
  }

  role       = aws_iam_role.lambda.name
  policy_arn = each.value
}
