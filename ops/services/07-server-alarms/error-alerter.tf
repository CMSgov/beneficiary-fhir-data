locals {
  alerter_lambda_rate           = local.ssm_config["/bfd/${local.service}/error_alerter/rate"]
  alerter_lambda_lookback       = local.ssm_config["/bfd/${local.service}/error_alerter/log_lookback_seconds"]
  alerter_lambda_slack_hook_ssm = local.ssm_config["/bfd/${local.service}/error_alerter/slack_webhook_ssm"]

  # TODO: Remove "-ecs"
  alerter_name_prefix      = "bfd-${local.env}-${local.target_service}-ecs"
  alerter_lambda_name      = "error-alerter"
  alerter_lambda_full_name = "${local.alerter_name_prefix}-${local.alerter_lambda_name}"
  alerter_lambda_src       = replace(local.alerter_lambda_name, "-", "_")
}

data "aws_ssm_parameter" "alerter_slack_webhook" {
  name            = local.alerter_lambda_slack_hook_ssm
  with_decryption = true
}

data "archive_file" "alerter_lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.alerter_lambda_src}/${local.alerter_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.alerter_lambda_src}/${local.alerter_lambda_src}.py")
    filename = "${local.alerter_lambda_src}.py"
  }
}

resource "aws_scheduler_schedule_group" "alerter" {
  name = "${local.alerter_lambda_full_name}-lambda-schedules"
}

resource "aws_scheduler_schedule" "alerter" {
  group_name          = aws_scheduler_schedule_group.alerter.name
  name                = "${local.alarm_name_prefix}-run-${local.alerter_lambda_name}-every-${replace(local.alerter_lambda_rate, " ", "-")}"
  schedule_expression = "rate(${local.alerter_lambda_rate})"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.alerter.arn
    role_arn = aws_iam_role.scheduler_assume_role.arn
  }
}

resource "aws_cloudwatch_log_group" "alerter" {
  name         = "/aws/lambda/${local.alerter_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "alerter" {
  function_name = local.alerter_lambda_full_name

  description = join("", [
    "Invoked whenever schedules in the ${aws_scheduler_schedule_group.alerter.name} group execute, ",
    "this Lambda uses Log Insights to post alerts to Slack indicating the number of 500 errors ",
    "that have occurred in the past ${local.alerter_lambda_lookback} seconds in ${local.env}'s ",
    "${local.target_service}"
  ])

  tags = {
    Name = local.alerter_lambda_full_name
  }

  kms_key_arn      = local.env_key_arn
  filename         = data.archive_file.alerter_lambda_src.output_path
  source_code_hash = data.archive_file.alerter_lambda_src.output_base64sha256
  architectures    = ["arm64"]
  handler          = "${local.alerter_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 300

  environment {
    variables = {
      BFD_ENVIRONMENT         = local.env
      LOG_LOOKBACK_SECONDS    = local.alerter_lambda_lookback
      ACCESS_JSON_LOG_GROUP   = data.aws_cloudwatch_log_group.server_access.name
      MESSAGES_JSON_LOG_GROUP = data.aws_cloudwatch_log_group.server_messages.name
      SLACK_WEBHOOK           = data.aws_ssm_parameter.alerter_slack_webhook.value
    }
  }

  role = aws_iam_role.alerter.arn
}

