data "aws_sns_topic" "breach_topics" {
  for_each = { for k, v in local.alarms_raw_config : k => v if v.breach_topic != null }

  name = each.value.breach_topic
}

data "aws_sns_topic" "ok_topics" {
  for_each = { for k, v in local.alarms_raw_config : k => v if v.ok_topic != null }

  name = each.value.ok_topic
}

data "aws_sqs_queue" "outbound_lambda_dlq" {
  name = local.eft_outputs.outbound_lambda_dlq_name
}

data "aws_lambda_function" "outbound_lambda" {
  function_name = local.eft_outputs.outbound_lambda_name
}

data "aws_ssm_parameter" "slack_webhook" {
  name            = local.slack_webhook_ssm_path
  with_decryption = true
}
