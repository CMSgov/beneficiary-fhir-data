locals {
  msgs_topic_paths = {
    high_alert = "/bfd/${local.service}/sns_topics/msgs/high_alert"
    alert      = "/bfd/${local.service}/sns_topics/msgs/alert"
    warning    = "/bfd/${local.service}/sns_topics/msgs/warning"
  }
  msgs_topic_names = {
    for k, v in local.msgs_topic_paths
    # In the event this module is being applied in a non-critical environment (i.e. an ephemeral
    # environment/test) using lookup() will ensure that an empty configuration will be returned
    # instead of an error if no configuration is available.
    : k => contains(["prod", "prod-sbx", "sandbox"], local.env) ? nonsensitive(local.ssm_config[v]) : nonsensitive(lookup(local.ssm_config, v, sensitive(null)))
  }
  msgs_topic_arns = merge(
    { for k, _ in local.msgs_topic_paths : k => null },
    { for k, v in data.aws_sns_topic.msgs_actions : k => [v.arn] }
  )
}

data "aws_sns_topic" "msgs_actions" {
  for_each = { for k, v in local.msgs_topic_names : k => v if v != null }
  name     = each.value
}

resource "aws_cloudwatch_metric_alarm" "errors" {
  alarm_name          = "${local.alarms_prefix}-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "RDA Pipeline errors detected in ${local.env}\n\n${local.dashboard_message_fragment}"

  metric_name = "messages/count/error"
  namespace   = local.metrics_namespace

  alarm_actions = local.msgs_topic_arns["alert"]

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "smoketest_failures" {
  alarm_name          = "${local.alarms_prefix}-smoketest-failures"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_description = join("", [
    "RDA Pipeline smoketest failures detected in ${local.env}. RDA Pipeline is likely shutdown.",
    "\n\n",
    "${local.dashboard_message_fragment}"
  ])

  metric_name = "messages/count/smoketest-failure"
  namespace   = local.metrics_namespace

  alarm_actions = local.msgs_topic_arns["alert"]

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "log_availability_1hr" {
  alarm_name          = "${local.alarms_prefix}-log-availability-1hr"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 1
  period              = 1 * 60 * 60
  statistic           = "Sum"
  threshold           = 0

  alarm_description = join("", [
    "RDA Pipeline logs have not been submitted to CloudWatch in 1 hour, pipeline has likely shutdown ",
    "in APP-ENV: bfd-${local.env}",
    "\n\n",
    "${local.dashboard_message_fragment}"
  ])

  metric_name = "IncomingLogEvents"
  namespace   = "AWS/Logs"

  dimensions = {
    LogGroupName = data.aws_cloudwatch_log_group.messages.name
  }

  alarm_actions = local.msgs_topic_arns["alert"]

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}
