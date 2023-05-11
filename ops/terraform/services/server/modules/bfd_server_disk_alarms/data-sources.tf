data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}

# TODO: Consolidate the two data sources (and related resources in main.tf) with similar resources
# in server-load
data "aws_launch_template" "template" {
  name = "bfd-${local.env}-fhir"
}

data "aws_autoscaling_group" "asg" {
  name = "${data.aws_launch_template.template.name}-${data.aws_launch_template.template.latest_version}"
}

data "aws_sns_topic" "alarms_action_sns" {
  count = local.alarm_action_sns != null ? 1 : 0
  name  = local.alarm_action_sns
}

data "aws_sns_topic" "alarms_ok_sns" {
  count = local.alarms_ok_sns != null ? 1 : 0
  name  = local.alarms_ok_sns
}

data "archive_file" "this" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/manage_disk_usage_alarms.py"
  output_path = "${path.module}/lambda_src/manage_disk_usage_alarms.zip"
}
