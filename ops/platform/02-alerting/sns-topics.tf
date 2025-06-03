locals {
  splunk_on_call_url = local.ssm_config["/bfd/alerting/splunk_on_call_subscription_url"]
}

data "aws_iam_policy_document" "topic_template" {
  statement {
    sid       = "AllowAlarmPublish"
    actions   = ["SNS:Publish"]
    resources = ["%s"]

    principals {
      type        = "Service"
      identifiers = ["cloudwatch.amazonaws.com"]
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

resource "aws_sns_topic" "splunk_incident" {
  name              = "${local.name_prefix}-splunk-on-call-incident"
  display_name      = "Messages sent to this Topic will open/close a Splunk On Call Incident"
  kms_master_key_id = local.kms_key_arn
}

resource "aws_sns_topic_policy" "splunk_incident" {
  arn = aws_sns_topic.splunk_incident.arn
  policy = format(
    data.aws_iam_policy_document.topic_template.json,
    aws_sns_topic.splunk_incident.arn,
    aws_sns_topic.splunk_incident.arn
  )
}

# resource "aws_sns_topic_subscription" "splunk_incident" {
#   protocol               = "https"
#   topic_arn              = aws_sns_topic.splunk_incident.arn
#   endpoint               = local.splunk_on_call_url
#   endpoint_auto_confirms = true
# }

# resource "aws_sns_topic_subscription" "slack_webhook" {
#   count     = local.enable_victor_ops ? 1 : 0
#   topic_arn = aws_sns_topic.cloudwatch_alarms.arn
#   protocol  = "lambda"
#   endpoint  = aws_lambda_function.this["bfd_alerts"].arn
# }

resource "aws_sns_topic" "slack" {
  for_each = toset(local.slack_channels)

  name              = nonsensitive(local.ssm_config["/bfd/alerting/slack/${each.key}/topic"])
  display_name      = "Invokes a Lambda that sends the contents of the notification to #${each.key}"
  kms_master_key_id = local.kms_key_arn
}

resource "aws_sns_topic_policy" "slack" {
  for_each = toset(local.slack_channels)

  arn = aws_sns_topic.slack[each.key].arn
  policy = format(
    data.aws_iam_policy_document.topic_template.json,
    aws_sns_topic.slack[each.key].arn,
    aws_sns_topic.slack[each.key].arn
  )
}
