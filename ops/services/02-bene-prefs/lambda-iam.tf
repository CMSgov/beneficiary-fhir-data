data "aws_iam_policy_document" "lambda_cloudwatch" {
  count = local.conditional_count

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.this[0].arn}:*"]
  }
}

data "aws_iam_policy_document" "lambda_assume" {
  count = local.conditional_count

  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "lambda_cloudwatch" {
  count = local.conditional_count

  name = "${local.lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.lambda_cloudwatch[0].json
}

data "aws_iam_policy_document" "lambda_ssm" {
  count = local.conditional_count

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
  count = local.conditional_count

  name = "${local.lambda_full_name}-ssm"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to get SSM parameters"
  ])
  policy = data.aws_iam_policy_document.lambda_ssm[0].json
}

data "aws_iam_policy_document" "lambda_kms" {
  count = local.conditional_count

  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "lambda_kms" {
  count = local.conditional_count

  name = "${local.lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to use the ",
    "${local.env_key_alias} key"
  ])
  policy = data.aws_iam_policy_document.lambda_kms[0].json
}


data "aws_iam_policy_document" "lambda_dynamodb" {
  count = local.conditional_count

  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["dynamodb:*"]
    resources = [aws_dynamodb_table.this[0].arn]
  }
}

resource "aws_iam_policy" "lambda_dynamodb" {
  count = local.conditional_count

  name = "${local.lambda_full_name}-dynamodb"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to use the ",
    "${aws_dynamodb_table.this[0].name} table"
  ])
  policy = data.aws_iam_policy_document.lambda_dynamodb[0].json
}

data "aws_iam_policy_document" "lambda_s3" {
  count = local.conditional_count

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
  count = local.conditional_count

  name = "${local.lambda_full_name}-s3"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.lambda_full_name} Lambda to interact with the ",
    "${local.service} partner S3 Buckets"
  ])
  policy = data.aws_iam_policy_document.lambda_s3[0].json
}

data "aws_iam_policy" "lambda_vpc_access_role" {
  count = local.conditional_count

  name = "AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role" "lambda" {
  count = local.conditional_count

  name                  = local.lambda_full_name
  path                  = local.iam_path
  description           = "Queries for beneficiary preferences from IDR and generates files in a format expected by partners"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume[0].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "lambda" {
  for_each = { for k, v in {
    cloudwatch = try(aws_iam_policy.lambda_cloudwatch[0].arn, "")
    dynamodb   = try(aws_iam_policy.lambda_dynamodb[0].arn, "")
    kms        = try(aws_iam_policy.lambda_kms[0].arn, "")
    s3         = try(aws_iam_policy.lambda_s3[0].arn, "")
    ssm        = try(aws_iam_policy.lambda_ssm[0].arn, "")
    vpc_access = try(data.aws_iam_policy.lambda_vpc_access_role[0].arn, "")
  } : k => v if length(v) > 0 }

  role       = aws_iam_role.lambda[0].name
  policy_arn = each.value
}
