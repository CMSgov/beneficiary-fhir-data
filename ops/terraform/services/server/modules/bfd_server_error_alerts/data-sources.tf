data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = local.nonsensitive_common_config["kms_key_alias"]
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_service" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}

data "aws_ssm_parameter" "alerter_slack_webhook" {
  name            = "/bfd/mgmt/common/sensitive/slack_webhook_${local.alerter_lambda_slack_hook}"
  with_decryption = true
}

data "archive_file" "alerter_lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.alerter_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.alerter_lambda_src}.py")
    filename = "${local.alerter_lambda_src}.py"
  }
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
