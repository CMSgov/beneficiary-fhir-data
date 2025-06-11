data "aws_iam_policy_document" "service_assume_role" {
  for_each = toset(["lambda", "scheduler"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "verifier_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.verifier.arn}:*"]
  }
}

resource "aws_iam_policy" "verifier_logs" {
  name = "${local.verifier_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.verifier_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = data.aws_iam_policy_document.verifier_logs.json
}

data "aws_iam_policy_document" "verifier_ssm" {
  statement {
    actions = ["ssm:GetParameter"]
    resources = [
      "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.target_service}/sensitive/db/username",
      "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.target_service}/sensitive/db/password"
    ]
  }
}

resource "aws_iam_policy" "verifier_ssm" {
  name        = "${local.verifier_lambda_full_name}-ssm"
  path        = local.iam_path
  description = "Permissions for the ${local.verifier_lambda_full_name} Lambda to get relevant SSM parameters"
  policy      = data.aws_iam_policy_document.verifier_ssm.json
}

data "aws_iam_policy_document" "verifier_rds" {
  statement {
    sid       = "AllowDescribeCluster"
    actions   = ["rds:DescribeDBClusters"]
    resources = [data.aws_rds_cluster.main.arn]
  }
}

resource "aws_iam_policy" "verifier_rds" {
  name        = "${local.verifier_lambda_full_name}-rds"
  path        = local.iam_path
  description = "Permissions for the ${local.verifier_lambda_full_name} Lambda to describe the ${data.aws_rds_cluster.main.cluster_identifier} cluster"
  policy      = data.aws_iam_policy_document.verifier_rds.json
}

data "aws_iam_policy_document" "verifier_kms" {
  statement {
    sid = "AllowEncryptionAndDecryptionOfMasterKeys"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "verifier_kms" {
  name = "${local.verifier_lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.verifier_lambda_full_name} Lambda to decrypt config KMS keys and encrypt ",
    "and decrypt master KMS keys for ${local.env}"
  ])

  policy = data.aws_iam_policy_document.verifier_kms.json
}

data "aws_iam_policy_document" "verifier_s3" {
  statement {
    actions   = ["s3:ListBucket"]
    resources = [data.aws_s3_bucket.ccw_pipeline.arn]
  }
}

resource "aws_iam_policy" "verifier_s3" {
  name        = "${local.verifier_lambda_full_name}-s3"
  path        = local.iam_path
  description = "Permissions for the ${local.verifier_lambda_full_name} Lambda to list objects in the ${data.aws_s3_bucket.ccw_pipeline.bucket} Bucket"
  policy      = data.aws_iam_policy_document.verifier_s3.json
}

data "aws_iam_policy_document" "verifier_sns" {
  statement {
    sid       = "AllowPublish"
    actions   = ["SNS:Publish"]
    resources = data.aws_sns_topic.verifier_alert_topic[*].arn
  }
}

resource "aws_iam_policy" "verifier_sns" {
  name        = "${local.verifier_lambda_full_name}-sns"
  path        = local.iam_path
  description = "Permissions for the ${local.verifier_lambda_full_name} Lambda to publish to the configured SNS Topic(s)"
  policy      = data.aws_iam_policy_document.verifier_sns.json
}

resource "aws_iam_role" "verifier" {
  name                  = local.verifier_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.verifier_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  force_detach_policies = true
}

data "aws_iam_policy" "lambda_vpc_execution" {
  name = "AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy_attachment" "verifier" {
  for_each = {
    logs = aws_iam_policy.verifier_logs.arn
    ssm  = aws_iam_policy.verifier_ssm.arn
    rds  = aws_iam_policy.verifier_rds.arn
    kms  = aws_iam_policy.verifier_kms.arn
    s3   = aws_iam_policy.verifier_s3.arn
    sns  = aws_iam_policy.verifier_sns.arn
    vpc  = data.aws_iam_policy.lambda_vpc_execution.arn
  }

  role       = aws_iam_role.verifier.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "verifier_scheduler_lambda" {
  statement {
    actions   = ["lambda:InvokeFunction"]
    resources = [aws_lambda_function.verifier.arn]
  }
}

resource "aws_iam_policy" "verifier_scheduler_lambda" {
  name = "${local.verifier_lambda_full_name}-scheduler-allow-lambda-invoke"
  path = local.iam_path
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.verifier_lambda_full_name} Lambda"
  ])

  policy = data.aws_iam_policy_document.verifier_scheduler_lambda.json
}

resource "aws_iam_role" "verifier_scheduler" {
  name                 = "${local.verifier_lambda_full_name}-scheduler"
  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  description = join("", [
    "Role for EventBridge Scheduler allowing permissions to invoke the ",
    "${local.verifier_lambda_full_name} Lambda"
  ])
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["scheduler"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "verifier_scheduler" {
  role       = aws_iam_role.verifier_scheduler.name
  policy_arn = aws_iam_policy.verifier_scheduler_lambda.arn
}
