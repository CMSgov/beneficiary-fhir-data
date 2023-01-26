data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}

# TODO: Consolidate the two data sources (and related resources in main.tf) with similar resources
# in server-load
data "aws_launch_template" "template" {
  name = "bfd-${var.env}-fhir"
}

data "aws_autoscaling_group" "asg" {
  name = "${data.aws_launch_template.template.name}-${data.aws_launch_template.template.latest_version}"
}

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
