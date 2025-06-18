data "aws_iam_policy_document" "slack_alerter_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.slack_alerter.arn}:*"]
  }
}

resource "aws_iam_policy" "slack_alerter_logs" {
  name = "${local.slack_lambda_full_name}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.slack_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = data.aws_iam_policy_document.slack_alerter_logs.json
}

data "aws_iam_policy_document" "slack_alerter_kms" {
  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.kms_key_arn]
  }
}

resource "aws_iam_policy" "slack_alerter_kms" {
  name        = "${local.slack_lambda_full_name}-kms-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.slack_lambda_full_name} to use the ${local.kms_key_alias} CMK"
  policy      = data.aws_iam_policy_document.slack_alerter_kms.json
}

data "aws_iam_policy_document" "slack_alerter_sqs" {
  statement {
    sid       = "AllowUsageOfInvokeQueue"
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
    resources = [aws_sqs_queue.slack_alerter_invoke.arn]
  }

  statement {
    sid       = "AllowSendMessageToDLQ"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.slack_alerter_dlq.arn]
  }
}

resource "aws_iam_policy" "slack_alerter_sqs" {
  name        = "${local.slack_lambda_full_name}-sqs-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.slack_lambda_full_name} to use SQS for invocation and dead-letters"
  policy      = data.aws_iam_policy_document.slack_alerter_sqs.json
}

data "aws_iam_policy_document" "lambda_assume_slack_alerter" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "slack_alerter" {
  name                  = "${local.slack_lambda_full_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.slack_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume_slack_alerter.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "slack_alerter" {
  for_each = {
    logs = aws_iam_policy.slack_alerter_logs.arn
    kms  = aws_iam_policy.slack_alerter_kms.arn
    sqs  = aws_iam_policy.slack_alerter_sqs.arn
  }

  role       = aws_iam_role.slack_alerter.name
  policy_arn = each.value
}
