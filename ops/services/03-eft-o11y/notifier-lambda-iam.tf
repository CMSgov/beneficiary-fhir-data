data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "slack_notifier_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.slack_notifier.arn}:*"]
  }
}

resource "aws_iam_policy" "slack_notifier_logs" {
  name = "${local.slack_notifier_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.slack_notifier_lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.slack_notifier_logs.json
}

resource "aws_iam_role" "slack_notifier" {
  name                  = local.slack_notifier_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.slack_notifier_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "slack_notifier" {
  for_each = {
    logs = aws_iam_policy.slack_notifier_logs.arn
  }

  role       = aws_iam_role.slack_notifier.name
  policy_arn = each.value
}
