data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-cmk"
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

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}