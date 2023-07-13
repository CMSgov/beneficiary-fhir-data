data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = local.nonsensitive_common_config["kms_key_alias"]
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "archive_file" "alert_lambda_scheduler_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.alert_lambda_scheduler_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.alert_lambda_scheduler_src}.py")
    filename = "${local.alert_lambda_scheduler_src}.py"
  }
}
