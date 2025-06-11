data "aws_iam_policy_document" "run_locust_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.run_locust.arn}:*"]
  }
}

resource "aws_iam_policy" "run_locust_logs" {
  name = "${local.run_locust_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_locust_lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.run_locust_logs.json
}

data "aws_iam_policy_document" "run_locust_ssm" {
  statement {
    sid     = "AllowAccessToUsedParams"
    actions = ["ssm:GetParameter"]
    resources = [
      for param in [
        "sensitive/db/username",
        "sensitive/db/password",
        "sensitive/cert/key",
        "sensitive/cert/pem_data",
      ]
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.service}/${param}"
    ]
  }
}

resource "aws_iam_policy" "run_locust_ssm" {
  name = "${local.run_locust_lambda_full_name}-ssm"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_locust_lambda_full_name} Lambda to get SSM parameters"
  ])
  policy = data.aws_iam_policy_document.run_locust_ssm.json
}

data "aws_iam_policy_document" "run_locust_kms" {
  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "run_locust_kms" {
  name = "${local.run_locust_lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_locust_lambda_full_name} Lambda to use the ",
    "${local.env_key_alias} key"
  ])
  policy = data.aws_iam_policy_document.run_locust_kms.json
}

data "aws_iam_policy_document" "run_locust_s3" {
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
    resources = [
      "${module.bucket_athena.bucket.arn}",
      "${module.bucket_athena.bucket.arn}/databases/${local.locust_stats_db_name}/${local.locust_stats_table}/*",
      "${module.bucket_athena.bucket.arn}/query_results/*"
    ]
  }
}

resource "aws_iam_policy" "run_locust_s3" {
  name = "${local.run_locust_lambda_full_name}-s3"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_locust_lambda_full_name} Lambda to interact with the ",
    "${module.bucket_athena.bucket.bucket} S3 Bucket"
  ])
  policy = data.aws_iam_policy_document.run_locust_s3.json
}

data "aws_iam_policy_document" "run_locust_athena" {
  statement {
    sid = "AllowAthenaQueries"
    actions = [
      "athena:StartQueryExecution",
      "athena:GetQueryResults",
      "athena:GetWorkGroup",
      "athena:StopQueryExecution",
      "athena:GetQueryExecution"
    ]
    resources = [aws_athena_workgroup.locust_stats.arn]
  }
}

resource "aws_iam_policy" "run_locust_athena" {
  name = "${local.run_locust_lambda_full_name}-athena"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_locust_lambda_full_name} Lambda to execute Athena ",
    "queries against the ${aws_athena_workgroup.locust_stats.name} Workgroup"
  ])
  policy = data.aws_iam_policy_document.run_locust_athena.json
}

data "aws_iam_policy_document" "run_locust_glue" {
  statement {
    sid = "AllowGlueOperations"
    actions = [
      "glue:GetTable",
      "glue:GetPartitions"
    ]
    resources = [
      "arn:aws:glue:${local.region}:${local.account_id}:catalog",
      "arn:aws:glue:${local.region}:${local.account_id}:database/${local.locust_stats_db_name}",
      "arn:aws:glue:${local.region}:${local.account_id}:table/${local.locust_stats_db_name}/${local.locust_stats_table}"
    ]
  }
}

resource "aws_iam_policy" "run_locust_glue" {
  name = "${local.run_locust_lambda_full_name}-glue"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.run_locust_lambda_full_name} Lambda to get Glue Table and ",
    "Partitions"
  ])
  policy = data.aws_iam_policy_document.run_locust_glue.json
}

data "aws_iam_policy" "lambda_vps_access_role" {
  name = "AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role" "run_locust" {
  name                  = "${local.run_locust_lambda_full_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.run_locust_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "run_locust" {
  for_each = {
    logs       = aws_iam_policy.run_locust_logs.arn
    ssm        = aws_iam_policy.run_locust_ssm.arn
    kms        = aws_iam_policy.run_locust_kms.arn
    s3         = aws_iam_policy.run_locust_s3.arn
    athena     = aws_iam_policy.run_locust_athena.arn
    glue       = aws_iam_policy.run_locust_glue.arn
    vpc_access = data.aws_iam_policy.lambda_vps_access_role.arn
  }

  role       = aws_iam_role.run_locust.name
  policy_arn = each.value
}
