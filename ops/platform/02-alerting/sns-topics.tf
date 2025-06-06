locals {
  splunk_on_call_url = local.ssm_config["/bfd/alerting/splunk_on_call_subscription_url"]

  splunk_incident_topic = "${local.name_prefix}-splunk-on-call-incident"
  slack_channel_to_topic = {
    for channel in local.slack_channels
    : channel => nonsensitive(local.ssm_config["/bfd/alerting/slack/${channel}/topic"])
  }
}

data "aws_iam_policy_document" "topic_template" {
  statement {
    sid       = "AllowAlarmPublish"
    actions   = ["SNS:Publish"]
    resources = ["%s"]

    principals {
      type        = "Service"
      identifiers = ["cloudwatch.amazonaws.com", "events.amazonaws.com"]
    }
  }

  statement {
    sid = "AllowAccountUsage"
    actions = [
      "SNS:GetTopicAttributes",
      "SNS:SetTopicAttributes",
      "SNS:AddPermission",
      "SNS:RemovePermission",
      "SNS:DeleteTopic",
      "SNS:Subscribe",
      "SNS:ListSubscriptionsByTopic",
      "SNS:Publish",
      "SNS:Receive"
    ]
    resources = ["%s"]

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceOwner"
      values   = [local.account_id]
    }
  }
}

resource "aws_cloudwatch_log_group" "splunk_incident_success" {
  name         = "sns/${local.region}/${local.account_id}/${local.splunk_incident_topic}"
  kms_key_id   = local.kms_key_arn
  skip_destroy = true
}

resource "aws_cloudwatch_log_group" "splunk_incident_failure" {
  name         = "sns/${local.region}/${local.account_id}/${local.splunk_incident_topic}/Failure"
  kms_key_id   = local.kms_key_arn
  skip_destroy = true
}

resource "aws_sns_topic" "splunk_incident" {
  depends_on = [
    aws_cloudwatch_log_group.splunk_incident_success,
    aws_cloudwatch_log_group.splunk_incident_failure
  ]

  name              = local.splunk_incident_topic
  display_name      = "Messages sent to this Topic will open/close a Splunk On Call Incident"
  kms_master_key_id = local.kms_key_arn

  sqs_success_feedback_sample_rate = 100
  sqs_success_feedback_role_arn    = aws_iam_role.splunk_topic.arn
  sqs_failure_feedback_role_arn    = aws_iam_role.splunk_topic.arn
}

resource "aws_sns_topic_policy" "splunk_incident" {
  arn = aws_sns_topic.splunk_incident.arn
  policy = format(
    data.aws_iam_policy_document.topic_template.json,
    aws_sns_topic.splunk_incident.arn,
    aws_sns_topic.splunk_incident.arn
  )
}

resource "aws_sns_topic_subscription" "splunk_incident" {
  protocol               = "https"
  topic_arn              = aws_sns_topic.splunk_incident.arn
  endpoint               = local.splunk_on_call_url
  endpoint_auto_confirms = true
}

resource "aws_cloudwatch_log_group" "slack_success" {
  for_each = local.slack_channel_to_topic

  name         = "sns/${local.region}/${local.account_id}/${each.value}"
  kms_key_id   = local.kms_key_arn
  skip_destroy = true
}

resource "aws_cloudwatch_log_group" "slack_failure" {
  for_each = local.slack_channel_to_topic

  name         = "sns/${local.region}/${local.account_id}/${each.value}/Failure"
  kms_key_id   = local.kms_key_arn
  skip_destroy = true
}

resource "aws_sns_topic" "slack" {
  for_each = local.slack_channel_to_topic
  depends_on = [
    aws_cloudwatch_log_group.slack_success,
    aws_cloudwatch_log_group.slack_failure
  ]

  name              = each.value
  display_name      = "Invokes a Lambda that sends the contents of the notification to #${each.key}"
  kms_master_key_id = local.kms_key_arn

  sqs_success_feedback_sample_rate = 100
  sqs_success_feedback_role_arn    = aws_iam_role.slack_topic[each.key].arn
  sqs_failure_feedback_role_arn    = aws_iam_role.slack_topic[each.key].arn
}

resource "aws_sns_topic_policy" "slack" {
  for_each = local.slack_channel_to_topic

  arn = aws_sns_topic.slack[each.key].arn
  policy = format(
    data.aws_iam_policy_document.topic_template.json,
    aws_sns_topic.slack[each.key].arn,
    aws_sns_topic.slack[each.key].arn
  )
}
