data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}

data "aws_sns_topic" "cloudwatch_alarms_alert" {
  # TODO: Replace this with the non-temporary variant of the CloudWatch alarms SNS topic in BFD-2244
  name = "bfd-${var.env}-cloudwatch-alarms-alert-testing"
}

data "aws_sns_topic" "cloudwatch_alarms_slack_bfd_notices" {
  name = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-notices"
}

data "archive_file" "this" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/cw_alarms_slack_notifier.py"
  output_path = "${path.module}/lambda-src/cw_alarms_slack_notifier.zip"
}
