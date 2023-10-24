data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}

data "aws_sns_topic" "cloudwatch_alarms_slack_bfd_notices" {
  name = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-notices"
}

data "aws_sns_topic" "cloudwatch_alarms_slack_bfd_test" {
  name = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-test"
}

data "aws_sns_topic" "cloudwatch_alarms_slack_bfd_warnings" {
  name = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-warnings"
}

data "aws_sns_topic" "cloudwatch_alarms_slack_bfd_alerts" {
  name = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-alerts"
}

data "archive_file" "this" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/cw_alarms_slack_notifier.py"
  output_path = "${path.module}/lambda-src/cw_alarms_slack_notifier.zip"
}
