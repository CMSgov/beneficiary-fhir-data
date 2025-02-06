locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  env        = terraform.workspace
  service    = "server"
  app        = "bfd-${local.service}"
  namespace  = "bfd-${local.env}/${local.app}"
  kms_key_id = data.aws_kms_key.cmk.arn

  nonsensitive_common_map = zipmap(
    data.aws_ssm_parameters_by_path.nonsensitive_common.names,
    nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values)
  )
  nonsensitive_common_config = {
    for key, value in local.nonsensitive_common_map
    : split("/", key)[5] => value
  }
  nonsensitive_service_map = zipmap(
    data.aws_ssm_parameters_by_path.nonsensitive_service.names,
    nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_service.values)
  )
  nonsensitive_service_config = {
    for key, value in local.nonsensitive_service_map
    : split("/", key)[5] => value
  }

  alerter_lambda_rate       = local.nonsensitive_service_config["500_errors_alerter_rate"]
  alerter_lambda_lookback   = local.nonsensitive_service_config["500_errors_alerter_log_lookback_seconds"]
  alerter_lambda_slack_hook = local.nonsensitive_service_config["500_errors_alerter_slack_webhook"]

  name_prefix         = "${local.app}-${local.env}-500-errors"
  alerter_lambda_name = "${local.name_prefix}-alerter"
  alerter_lambda_src  = "${replace(local.app, "-", "_")}_errors_alerter"

  # We construct this manually (instead of through a data resource) as ephemeral environments may
  # not have any Log Group and thus will fail to apply otherwise
  access_json_log_group_name   = "/bfd/${local.env}/${local.app}/access.json"
  messages_json_log_group_name = "/bfd/${local.env}/${local.app}/messages.json"
}

resource "aws_scheduler_schedule_group" "this" {
  name = "${local.name_prefix}-lambda-schedules"
}

resource "aws_scheduler_schedule" "this" {
  group_name          = aws_scheduler_schedule_group.this.name
  name                = "bfd-${local.env}-${local.service}-run-error-alerter-every-${replace(local.alerter_lambda_rate, " ", "-")}"
  schedule_expression = "rate(${local.alerter_lambda_rate})"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.alerter_lambda.arn
    role_arn = aws_iam_role.scheduler_assume_role.arn
  }
}

resource "aws_lambda_function" "alerter_lambda" {
  function_name = local.alerter_lambda_name

  description = join("", [
    "Invoked whenever schedules in the ${aws_scheduler_schedule_group.this.name} group execute, ",
    "this Lambda uses Log Insights to post alerts to Slack indicating the number of 500 errors ",
    "that have occurred in the past ${local.alerter_lambda_lookback} seconds in ${local.env}'s ",
    "${local.app}"
  ])

  tags = {
    Name = local.alerter_lambda_name
  }

  kms_key_arn      = local.kms_key_id
  filename         = data.archive_file.alerter_lambda_src.output_path
  source_code_hash = data.archive_file.alerter_lambda_src.output_base64sha256
  architectures    = ["arm64"]
  handler          = "${local.alerter_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.11"
  timeout          = 300

  environment {
    variables = {
      BFD_ENVIRONMENT         = local.env
      LOG_LOOKBACK_SECONDS    = local.alerter_lambda_lookback
      ACCESS_JSON_LOG_GROUP   = local.access_json_log_group_name
      MESSAGES_JSON_LOG_GROUP = local.messages_json_log_group_name
      SLACK_WEBHOOK           = data.aws_ssm_parameter.alerter_slack_webhook.value
    }
  }

  role = aws_iam_role.alerter_lambda_role.arn
}

