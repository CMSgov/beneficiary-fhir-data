##
# CloudWatch SNS Topics Resources for Alarms for environments
locals {
  env                              = var.env
  is_prod                          = substr(var.env, 0, 4) == "prod"
  victor_ops_url                   = data.aws_ssm_parameter.victor_ops_url.value
  kms_master_key_id                = data.aws_kms_key.master_key.id
  enable_victor_ops                = local.is_prod # only wake people up for prod alarms
  cloudwatch_sns_topic_policy_spec = <<-EOF
{
  "Version": "2008-10-17",
  "Id": "__default_policy_ID",
  "Statement": [
    {
        "Sid": "Allow_Publish_Alarms",
        "Effect": "Allow",
        "Principal":
        {
            "Service": [
                "cloudwatch.amazonaws.com"
            ]
        },
        "Action": "sns:Publish",
        "Resource": "%s"
    },
    {
      "Sid": "__default_statement_ID",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "SNS:GetTopicAttributes",
        "SNS:SetTopicAttributes",
        "SNS:AddPermission",
        "SNS:RemovePermission",
        "SNS:DeleteTopic",
        "SNS:Subscribe",
        "SNS:ListSubscriptionsByTopic",
        "SNS:Publish",
        "SNS:Receive"
      ],
      "Resource": "%s",
      "Condition": {
        "StringEquals": {
          "AWS:SourceOwner": "${local.account_id}"
        }
      }
    }
  ]
}
EOF
}

resource "aws_sns_topic" "cloudwatch_alarms" {
  name              = "bfd-${local.env}-cloudwatch-alarms"
  display_name      = "BFD Cloudwatch Alarm. Created by Terraform."
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms" {
  arn    = aws_sns_topic.cloudwatch_alarms.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms.arn, aws_sns_topic.cloudwatch_alarms.arn)
}

resource "aws_sns_topic_subscription" "alarm" {
  count                  = local.enable_victor_ops ? 1 : 0
  protocol               = "https"
  topic_arn              = aws_sns_topic.cloudwatch_alarms.arn
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

resource "aws_sns_topic_subscription" "slack_webhook" {
  count     = local.enable_victor_ops ? 1 : 0
  topic_arn = aws_sns_topic.cloudwatch_alarms.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.this["bfd_alerts"].arn
}

resource "aws_sns_topic" "cloudwatch_ok" {
  name         = "bfd-${local.env}-cloudwatch-ok"
  display_name = "BFD Cloudwatch OK notifications. Created by Terraform."

  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "cloudwatch_ok" {
  arn    = aws_sns_topic.cloudwatch_ok.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_ok.arn, aws_sns_topic.cloudwatch_ok.arn)
}

resource "aws_sns_topic_subscription" "ok" {
  count                  = local.enable_victor_ops ? 1 : 0
  topic_arn              = aws_sns_topic.cloudwatch_ok.arn
  protocol               = "https"
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_notices" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-notices"
  display_name      = "BFD Cloudwatch Alarms notices to #bfd-notices. Created by Terraform."
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_notices" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_notices.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_notices.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_notices.arn)
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_test" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-test"
  display_name      = "BFD Cloudwatch Alarms alerts to #bfd-internal-alerts. Created by Terraform."
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_test" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_test.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_test.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_test.arn)
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_warnings" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-warnings"
  display_name      = "BFD Cloudwatch Alarms alerts to #bfd-warnings. Created by Terraform."
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_warnings" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings.arn)
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_alerts" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-alerts"
  display_name      = "BFD Cloudwatch Alarms alerts to #bfd-alerts. Created by Terraform."
  kms_master_key_id = local.kms_master_key_id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_alerts" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts.arn)
}
