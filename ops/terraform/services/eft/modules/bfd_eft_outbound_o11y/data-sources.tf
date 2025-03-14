data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_sns_topic" "breach_topics" {
  for_each = { for k, v in local.alarms_raw_config : k => v if v.breach_topic != null }

  name = each.value.breach_topic
}

data "aws_sns_topic" "ok_topics" {
  for_each = { for k, v in local.alarms_raw_config : k => v if v.ok_topic != null }

  name = each.value.ok_topic
}

data "aws_sqs_queue" "outbound_lambda_dlq" {
  name = var.outbound_lambda_dlq_name
}

data "aws_lambda_function" "outbound_lambda" {
  function_name = var.outbound_lambda_name
}

data "aws_ssm_parameter" "slack_webhook" {
  name            = local.slack_webhook_ssm_path
  with_decryption = true
}

data "archive_file" "slack_notifier_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.slack_notifier_lambda_src}/${local.slack_notifier_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.slack_notifier_lambda_src}/${local.slack_notifier_lambda_src}.py")
    filename = "${local.slack_notifier_lambda_src}.py"
  }
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
