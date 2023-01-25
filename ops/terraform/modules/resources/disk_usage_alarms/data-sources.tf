data "aws_caller_identity" "current" {}

data "aws_sns_topic" "cloudwatch_alarms_alert" {
  # TODO: Replace this with the non-temporary variant of the CloudWatch alarms SNS topic in BFD-2244
  name = "bfd-${var.env}-cloudwatch-alarms-alert-testing"
}

data "aws_sns_topic" "cloudwatch_alarms_ok" {
  # TODO: Replace this with the non-temporary variant of the CloudWatch alarms SNS topic in BFD-2244
  name = "bfd-${var.env}-cloudwatch-alarms-ok-testing"
}

data "archive_file" "this" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/manage_disk_usage_alarms.py"
  output_path = "${path.module}/lambda_src/manage_disk_usage_alarms.zip"
}
