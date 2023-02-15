locals {
  alarm_arn        = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  notification_arn = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_arn           = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]

  server_log_availability = {
    period       = 1 * 60 * 60 # 1 hour 
    eval_periods = 1
    threshold    = 0
    datapoints   = 1
  }
}

resource "aws_cloudwatch_metric_alarm" "server-log-availability-1hr" {
  alarm_name          = "bfd-${var.env}-server-log-availability-1hr"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = local.server_log_availability.eval_periods
  period              = local.server_log_availability.period
  statistic           = "Sum"
  threshold           = local.server_log_availability.threshold

  alarm_description = join("", [
    "BFD Server logs have not been submitted to CloudWatch in 1 hour, server has likely shutdown ",
    "in APP-ENV: bfd-${var.env}"
  ])

  metric_name = "IncomingLogEvents"
  namespace   = "AWS/Logs"

  dimensions = {
    LogGroupName = "/bfd/${var.env}/bfd-server/access.json"
  }

  alarm_actions = local.alarm_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = local.server_log_availability.datapoints
  treat_missing_data  = "notBreaching"
}

# NOTE: alarm is triggered when gov.cms.bfd.server.war QueryLoggingListener encounters an
#       unknown query, signaling that the application is missing a required pattern.
resource "aws_cloudwatch_metric_alarm" "server-query-logging-listener-warning" {
  alarm_name          = "bfd-${var.env}-server-query-logging-listener-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 1 * 60 # 1 Minute
  statistic           = "Sum"
  threshold           = 1

  alarm_description = join("", [
    "BFD Server QueryLoggingListener has encountered an unknown query ",
    "in APP-ENV: bfd-${var.env}"
  ])

  metric_name = "query-logging-listener/count/warning"
  namespace   = "bfd-${var.env}/bfd-server"

  dimensions = {
    LogGroupName = "/bfd/${var.env}/bfd-server/messages.json"
  }

  # NOTE: alarm should always be a low severity notification
  alarm_actions = local.notification_arn

  # NOTE: alarm has no meaningful transition from 'In alarm' to 'OK'
  ok_actions = []

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}
