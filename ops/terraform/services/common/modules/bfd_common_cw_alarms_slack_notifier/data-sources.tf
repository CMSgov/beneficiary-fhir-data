data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}

data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env}-cmk"
}

data "archive_file" "this" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/cw_alarms_slack_notifier.py"
  output_path = "${path.module}/lambda-src/cw_alarms_slack_notifier.zip"
}

data "aws_ssm_parameter" "victor_ops_url" {
  name            = "/bfd/${local.env}/common/sensitive/victor_ops_url"
  with_decryption = true
}
