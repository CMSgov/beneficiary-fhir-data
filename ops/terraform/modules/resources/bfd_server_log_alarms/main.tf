locals {
  victor_ops_sns         = "bfd-${var.env}-cloudwatch-alarms"
  bfd_test_slack_sns     = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-test"
  bfd_warnings_slack_sns = "bfd-${var.env}-cloudwatch-alarms-slack-bfd-warnings"
  default_ok_sns         = "bfd-${var.env}-cloudwatch-ok"
  topic_names_by_env = {
    prod = {
      alert  = local.victor_ops_sns
      notify = local.bfd_warnings_slack_sns
      ok     = local.default_ok_sns
    }
    prod-sbx = {
      alert  = local.victor_ops_sns
      notify = local.bfd_warnings_slack_sns
      ok     = null
    }
    test = {
      alert  = null # test is expected to not always be running, so alerts could be false positives
      notify = local.bfd_test_slack_sns
      ok     = null
    }
  }
  env_sns = lookup(local.topic_names_by_env, var.env, {
    alert  = null
    notify = null
    ok     = null
  })
  alert_sns_name  = try(coalesce(var.alert_sns_override, local.env_sns.alert), null)
  notify_sns_name = try(coalesce(var.notify_sns_override, local.env_sns.notify), null)
  ok_sns_name     = try(coalesce(var.ok_sns_override, local.env_sns.ok), null)
  alert_arn       = try([data.aws_sns_topic.alert_sns[0].arn], [])
  notify_arn      = try([data.aws_sns_topic.notify_sns[0].arn], [])
  ok_arn          = try([data.aws_sns_topic.ok_sns[0].arn], [])

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

  alarm_actions = local.alert_arn
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
  alarm_actions = local.notify_arn

  # NOTE: alarm has no meaningful transition from 'In alarm' to 'OK'
  ok_actions = []

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}
