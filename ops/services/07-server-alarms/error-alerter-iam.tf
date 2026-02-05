data "aws_iam_policy_document" "alerter_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.alerter.arn}:*"]
  }
}

resource "aws_iam_policy" "alerter_logs" {
  name = "${local.alerter_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.alerter_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.alerter_logs.json
}

data "aws_iam_policy_document" "alerter_query_log" {
  statement {
    sid     = "AllowAccessLogStartQuery"
    actions = ["logs:StartQuery"]
    resources = [
      data.aws_cloudwatch_log_group.server_access.arn,
      "${data.aws_cloudwatch_log_group.server_access.arn}:log-stream:*"
    ]
  }

  statement {
    sid     = "AllowAccessLogGetQueryResults"
    actions = ["logs:GetQueryResults"]
    # GetQueryResults does not support resource-level restrictions.
    # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_amazoncloudwatchlogs.html
    resources = ["*"]
  }
}

resource "aws_iam_policy" "alerter_query_log" {
  name = "${local.alerter_lambda_full_name}-query-logs"
  path = local.iam_path
  description = join("", [
    "Permissions for ${local.alerter_lambda_full_name} to start and retrieve the results of a Log ",
    "Insights query against the ${local.target_service} JSON Access CloudWatch Log Group"
  ])
  policy = data.aws_iam_policy_document.alerter_query_log.json
}

data "aws_iam_policy_document" "invoke_alerter" {
  statement {
    sid       = "AllowInvokeErrorAlerterLambda"
    actions   = ["lambda:InvokeFunction"]
    resources = [aws_lambda_function.alerter.arn]
  }
}

resource "aws_iam_policy" "invoke_alerter" {
  name = "${local.alerter_name_prefix}-scheduler-assumee-allow-lambda-invoke"
  path = local.iam_path
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.alerter_lambda_full_name} Lambda"
  ])
  policy = data.aws_iam_policy_document.invoke_alerter.json
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

resource "aws_iam_role" "alerter" {
  name                  = local.alerter_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.alerter_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.assume_lambda.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "alerter" {
  for_each = {
    logs      = aws_iam_policy.alerter_logs.arn
    query_log = aws_iam_policy.alerter_query_log.arn
  }

  role       = aws_iam_role.alerter.name
  policy_arn = each.value
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
  name                 = "${local.alerter_name_prefix}-scheduler-assume"
  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  description = join("", [
    "Role for EventBridge Scheduler to assume allowing permissions to invoke the ",
    "${local.alerter_lambda_full_name} Lambda"
  ])
  assume_role_policy    = data.aws_iam_policy_document.assume_scheduler.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "invoke_alerter_to_scheduler_assume_role" {
  role       = aws_iam_role.scheduler_assume_role.name
  policy_arn = aws_iam_policy.invoke_alerter.arn
}
