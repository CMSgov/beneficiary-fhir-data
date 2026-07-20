data "aws_iam_policy_document" "checker_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.checker.arn}:*"]
  }
}

resource "aws_iam_policy" "checker_logs" {
  name = "${local.checker_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.checker_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.checker_logs.json
}

data "aws_iam_policy_document" "checker_describe_logs" {
  statement {
    sid       = "AllowDescribeLogGroups"
    actions   = ["logs:DescribeLogGroups"]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "checker_describe_logs" {
  name = "${local.checker_lambda_full_name}-describe-log-groups"
  path = local.iam_path
  description = join("", [
    "Permissions for ${local.checker_lambda_full_name} Lambda to list CloudWatch Log Groups ",
    "and evaluate retention settings"
  ])
  policy = data.aws_iam_policy_document.checker_describe_logs.json
}

data "aws_iam_policy_document" "checker_publish_alerts" {
  count = length(data.aws_sns_topic.checker_alert_topic) > 0 ? 1 : 0

  statement {
    sid       = "AllowSnsPublishAlerts"
    actions   = ["sns:Publish"]
    resources = [one(data.aws_sns_topic.checker_alert_topic[*].arn)]
  }
}

resource "aws_iam_policy" "checker_publish_alerts" {
  count = length(data.aws_sns_topic.checker_alert_topic) > 0 ? 1 : 0

  name = "${local.checker_lambda_full_name}-publish-alerts"
  path = local.iam_path
  description = join("", [
    "Permissions for ${local.checker_lambda_full_name} Lambda to publish non-compliance alerts ",
    "to the configured SNS topic"
  ])
  policy = one(data.aws_iam_policy_document.checker_publish_alerts[*].json)
}

data "aws_iam_policy_document" "assume_lambda" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "checker" {
  name                  = local.checker_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.checker_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.assume_lambda.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "checker" {
  for_each = {
    logs          = aws_iam_policy.checker_logs.arn
    describe_logs = aws_iam_policy.checker_describe_logs.arn
  }

  role       = aws_iam_role.checker.name
  policy_arn = each.value
}

resource "aws_iam_role_policy_attachment" "checker_publish_alerts" {
  count = length(data.aws_sns_topic.checker_alert_topic) > 0 ? 1 : 0

  role       = aws_iam_role.checker.name
  policy_arn = one(aws_iam_policy.checker_publish_alerts[*].arn)
}
data "aws_iam_policy_document" "checker_kms" {
  statement {
    sid       = "AllowKmsDecryptForLogGroup"
    actions   = ["kms:Decrypt", "kms:GenerateDataKey", "kms:DescribeKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "checker_kms" {
  name = "${local.checker_lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Permissions for ${local.checker_lambda_full_name} Lambda to decrypt CloudWatch Log Group "
  ])
  policy = data.aws_iam_policy_document.checker_kms.json
}

resource "aws_iam_role_policy_attachment" "checker_kms" {
  role       = aws_iam_role.checker.name
  policy_arn = aws_iam_policy.checker_kms.arn
}

resource "aws_kms_grant" "checker_kms" {
  name              = "${local.checker_lambda_full_name}-kms"
  key_id            = local.env_key_arn
  grantee_principal = aws_iam_role.checker.arn
  operations        = ["Decrypt", "GenerateDataKey", "DescribeKey"]
}

resource "aws_kms_grant" "checker_platform_kms" {
  name              = "${local.checker_lambda_full_name}-platform-kms"
  key_id            = module.terraservice.platform_key_arn
  grantee_principal = aws_iam_role.checker.arn
  operations        = ["Decrypt", "GenerateDataKey", "DescribeKey"]
}

data "aws_iam_policy_document" "invoke_checker" {
  statement {
    sid       = "AllowInvokeLogRetentionCheckerLambda"
    actions   = ["lambda:InvokeFunction"]
    resources = [aws_lambda_function.checker.arn]
  }
}

resource "aws_iam_policy" "invoke_checker" {
  name = "${local.checker_lambda_full_name}-scheduler-assumee-allow-lambda-invoke"
  path = local.iam_path
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.checker_lambda_full_name} Lambda"
  ])
  policy = data.aws_iam_policy_document.invoke_checker.json
}

data "aws_iam_policy_document" "assume_scheduler" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["scheduler.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "scheduler_assume_role" {
  name                 = "${local.name_prefix}-lrc-scheduler-assume"
  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  description = join("", [
    "Role for EventBridge Scheduler to assume allowing permissions to invoke the ",
    "${local.checker_lambda_full_name} Lambda"
  ])
  assume_role_policy    = data.aws_iam_policy_document.assume_scheduler.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "invoke_checker_to_scheduler_assume_role" {
  role       = aws_iam_role.scheduler_assume_role.name
  policy_arn = aws_iam_policy.invoke_checker.arn
}