locals {
  slack_lambda_name      = "slack-alerter"
  slack_lambda_full_name = "${local.name_prefix}-${local.slack_lambda_name}"
  slack_lambda_src       = replace(local.slack_lambda_name, "-", "_")

  topic_to_webhook_map = merge(
    {
      for channel in local.slack_channels
      : nonsensitive(local.ssm_config["/bfd/alerting/slack/${channel}/topic"]) => local.ssm_config["/bfd/alerting/slack/${channel}/webhook"]
    },
    # We want alerts sent to Splunk On Call to also send a detailed summary to #bfd-alerts. So, we
    # manually add a destination for notifications originating from the Splunk SNS Topic to go to
    # the #bfd-alerts channel
    {
      "${aws_sns_topic.splunk_incident.name}" = local.ssm_config["/bfd/alerting/slack/bfd-alerts/webhook"]
    }
  )
  topic_to_channel_map = merge(
    {
      for channel in local.slack_channels
      : nonsensitive(local.ssm_config["/bfd/alerting/slack/${channel}/topic"]) => channel
    },
    {
      "${aws_sns_topic.splunk_incident.name}" = "bfd-alerts"
    }
  )
}

data "aws_iam_policy_document" "slack_alerter_invoke" {
  statement {
    sid       = "AllowSNSSendMessage"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.slack_alerter_invoke.arn]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    condition {
      test     = "ForAnyValue:ArnEquals"
      variable = "aws:SourceArn"
      values   = concat(values(aws_sns_topic.slack)[*].arn, [aws_sns_topic.splunk_incident.arn])
    }
  }
}

resource "aws_sqs_queue" "slack_alerter_invoke" {
  name              = "${local.slack_lambda_full_name}-sqs"
  kms_master_key_id = local.kms_key_arn
}

resource "aws_sqs_queue_policy" "slack_alerter_invoke" {
  queue_url = aws_sqs_queue.slack_alerter_invoke.id
  policy    = data.aws_iam_policy_document.slack_alerter_invoke.json
}

resource "aws_sqs_queue" "slack_alerter_dlq" {
  name                      = "${local.slack_lambda_full_name}-dlq"
  kms_master_key_id         = local.kms_key_arn
  message_retention_seconds = 14 * 24 * 60 * 60 # 14 days, in seconds, which is the maximum
}

resource "aws_sqs_queue_redrive_allow_policy" "slack_alerter" {
  queue_url = aws_sqs_queue.slack_alerter_dlq.id
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue",
    sourceQueueArns   = [aws_sqs_queue.slack_alerter_invoke.arn]
  })
}

resource "aws_sqs_queue_redrive_policy" "slack_alerter" {
  queue_url = aws_sqs_queue.slack_alerter_invoke.id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.slack_alerter_dlq.arn
    maxReceiveCount     = 4
  })
}

resource "aws_sns_topic_subscription" "slack_alerter_slack_queues" {
  for_each = toset(local.slack_channels)

  topic_arn = aws_sns_topic.slack[each.key].arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.slack_alerter_invoke.arn
}

resource "aws_sns_topic_subscription" "slack_alerter_splunk_queue" {
  topic_arn = aws_sns_topic.splunk_incident.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.slack_alerter_invoke.arn
}

data "archive_file" "slack_alerter_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.slack_lambda_name}/${local.slack_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.slack_lambda_name}/${local.slack_lambda_src}.py")
    filename = "${local.slack_lambda_src}.py"
  }
}

resource "aws_cloudwatch_log_group" "slack_alerter" {
  name         = "/aws/lambda/${local.slack_lambda_full_name}"
  kms_key_id   = local.kms_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "slack_alerter" {
  function_name = local.slack_lambda_full_name

  description = join("", [
    "Invoked whenever Alerts of any kind are sent to the Slack or Splunk On Call SNS Topics to ",
    "send messages to specific Slack channels based upon SSM parameter configuration."
  ])

  kms_key_arn      = local.kms_key_arn
  filename         = data.archive_file.slack_alerter_src.output_path
  source_code_hash = data.archive_file.slack_alerter_src.output_base64sha256
  architectures    = ["arm64"]
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python313-arm64:7"]
  handler          = "${local.slack_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 30

  logging_config {
    log_group  = aws_cloudwatch_log_group.slack_alerter.name
    log_format = "Text"
  }

  tags = {
    Name = local.slack_lambda_full_name
  }

  environment {
    variables = {
      TOPIC_TO_WEBHOOK_MAP = jsonencode(local.topic_to_webhook_map)
      TOPIC_TO_CHANNEL_MAP = jsonencode(local.topic_to_channel_map)
    }
  }

  role = aws_iam_role.slack_alerter.arn
}

resource "aws_lambda_event_source_mapping" "slack_alerter" {
  depends_on = [aws_iam_role_policy_attachment.slack_alerter]

  event_source_arn = aws_sqs_queue.slack_alerter_invoke.arn
  function_name    = aws_lambda_function.slack_alerter.function_name
  batch_size       = 1
}

resource "aws_lambda_function_event_invoke_config" "slack_aleter" {
  depends_on = [aws_iam_role_policy_attachment.slack_alerter]

  function_name          = aws_lambda_function.slack_alerter.function_name
  maximum_retry_attempts = 2

  destination_config {
    on_failure {
      destination = aws_sqs_queue.slack_alerter_dlq.arn
    }
  }
}

resource "aws_lambda_permission" "slack_alerter" {
  statement_id  = "AllowExecutionFromQueue"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.slack_alerter.function_name
  principal     = "sqs.amazonaws.com"
  source_arn    = aws_sqs_queue.slack_alerter_invoke.arn
}


resource "aws_cloudwatch_event_rule" "guardduty_runtime_health" {
  name        = "${local.name_prefix}-guardduty-runtime-health-status"
  state       = "DISABLED" # TODO: Disabled until BFD-4379 makes these alerts more useful
  description = "Capture events indicating a runtime agent is no longer sending telemtry"
  event_pattern = jsonencode({
    "source" : ["aws.guardduty"],
    "detail-type" : ["GuardDuty Runtime Protection Unhealthy"]
  })
}
resource "aws_cloudwatch_event_target" "guardduty_runtime_health" {
  rule = aws_cloudwatch_event_rule.guardduty_runtime_health.name
  arn  = aws_sns_topic.slack["bfd-warnings"].arn
}
