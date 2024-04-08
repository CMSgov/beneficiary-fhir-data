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

  name_prefix                 = "${local.app}-${local.env}-500-errors"
  alert_lambda_scheduler_name = "${local.name_prefix}-errors-alerter-scheduler"
  alert_lambda_scheduler_src  = "${replace(local.app, "-", "_")}_errors_alerter_scheduler"
  alerting_lambda_name        = "${local.name_prefix}-errors-alerter"
  alerting_lambda_src         = "${replace(local.app, "-", "_")}_errors_alerter"

  # We construct this manually (instead of through a data resource) as ephemeral environments may
  # not have any Log Group and thus will fail to apply otherwise
  access_json_log_group_name   = "/bfd/${local.env}/${local.app}/access.json"
  messages_json_log_group_name = "/bfd/${local.env}/${local.app}/messages.json"

  all_http500s_count_metric = "http-requests/count/500-responses"
}

resource "aws_cloudwatch_metric_alarm" "this" {
  alarm_name          = "${local.name_prefix}-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "60"
  statistic           = "Sum"
  threshold           = "0"

  alarm_description = "A 500 error was encountered in ${local.env} ${local.app}"

  metric_name = local.all_http500s_count_metric
  namespace   = local.namespace

  alarm_actions = [aws_sns_topic.this.arn]

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_scheduler_schedule_group" "this" {
  name = "${local.name_prefix}-lambda-schedules"
}

resource "aws_sns_topic" "this" {
  name              = local.name_prefix
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "this" {
  arn = aws_sns_topic.this.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "Allow CloudWatch to Send Alarm Actions to SNS"
        Effect    = "Allow"
        Principal = { Service = "cloudwatch.amazonaws.com" }
        Action    = "SNS:Publish"
        Resource  = aws_sns_topic.this.arn
      }
    ]
  })
}

resource "aws_lambda_permission" "this" {
  statement_id   = "${local.alert_lambda_scheduler_name}-allow-sns"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.alert_lambda_scheduler.function_name
  principal      = "sns.amazonaws.com"
  source_arn     = aws_sns_topic.this.arn
  source_account = local.account_id
}

resource "aws_sns_topic_subscription" "this" {
  topic_arn = aws_sns_topic.this.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.alert_lambda_scheduler.arn
}

resource "aws_lambda_function" "alert_lambda_scheduler" {
  function_name = local.alert_lambda_scheduler_name

  description = join("", [
    "Invoked when the ${aws_cloudwatch_metric_alarm.this.alarm_name} transitions to a new state ",
    "in ${local.env}. This Lambda either creates or removes EventBridge cron schedules that ",
    "invoke the ${local.alerting_lambda_name} Lambda"
  ])

  tags = {
    Name = local.alert_lambda_scheduler_name
  }

  kms_key_arn      = local.kms_key_id
  filename         = data.archive_file.alert_lambda_scheduler_src.output_path
  source_code_hash = data.archive_file.alert_lambda_scheduler_src.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "${local.alert_lambda_scheduler_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = 300

  environment {
    variables = {
      BFD_ENVIRONMENT             = local.env
      EVENTBRIDGE_SCHEDULES_GROUP = aws_scheduler_schedule_group.this.name
      RECURRING_SCHEDULE_RATE_STR = local.alerter_lambda_rate
      SCHEDULER_ROLE_ARN          = aws_iam_role.scheduler_assume_role.arn
      ALERTER_LAMBDA_ARN          = aws_lambda_function.alerting_lambda.arn
    }
  }

  role = aws_iam_role.lambda_roles[local.alert_lambda_scheduler_name].arn
}

resource "aws_lambda_function" "alerting_lambda" {
  function_name = local.alerting_lambda_name

  description = join("", [
    "Invoked whenever schedules in the ${aws_scheduler_schedule_group.this.name} group execute, ",
    "this Lambda uses Log Insights to post alerts to Slack indicating the number of 500 errors ",
    "that have occurred in the past ${local.alerter_lambda_lookback} seconds in ${local.env}'s ",
    "${local.app}"
  ])

  tags = {
    Name = local.alerting_lambda_name
  }

  kms_key_arn      = local.kms_key_id
  filename         = data.archive_file.alerting_lambda_src.output_path
  source_code_hash = data.archive_file.alerting_lambda_src.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "${local.alerting_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
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

  role = aws_iam_role.lambda_roles[local.alerting_lambda_name].arn
}
