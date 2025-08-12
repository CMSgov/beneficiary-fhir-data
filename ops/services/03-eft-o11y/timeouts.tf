resource "aws_cloudwatch_metric_alarm" "lambda_timeouts" {
  alarm_name          = "${local.alarms_config.lambda_errors.alarm_name}-timeout"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  # `period` is expressed in minutes, calculated here for previous 15 minutes to catch **all** lambda timeouts
  period    = 60 * 15
  statistic = "Maximum"
  # `threshold` expressed in milliseconds
  threshold           = data.aws_lambda_function.outbound_lambda.timeout * 1000
  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"

  alarm_description = join("", [
    "The ${var.outbound_lambda_name} has timed out in ${local.env}. View the ",
    "linked CloudWatch Log Group for more details on the failure, and inspect the failing event ",
    "in the linked DLQ",
    "\n",
    "\n* CloudWatch Log Group: <${local.alarms_config.lambda_errors.log_group_url}|${local.alarms_config.lambda_errors.log_group_name}>",
    "\n* Dead Letter Queue: <${local.alarms_config.lambda_errors.queue_url}|${var.outbound_lambda_dlq_name}>",
  ])

  metric_name = "Duration"
  namespace   = local.lambda_metrics_namespace
  dimensions = {
    FunctionName = var.outbound_lambda_name
  }

  alarm_actions = local.alarms_config.lambda_errors.breach_topic_arn != null ? [local.alarms_config.lambda_errors.breach_topic_arn] : null
  ok_actions    = local.alarms_config.lambda_errors.ok_topic_arn != null ? [local.alarms_config.lambda_errors.ok_topic_arn] : null
}
