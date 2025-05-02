locals {
  msgs_alert_topic_path   = "/bfd/${local.service}/sns_topics/msgs/alert"
  msgs_warning_topic_path = "/bfd/${local.service}/sns_topics/msgs/warning"
  msgs_env_sns = contains(["prod", "prod-sbx"], local.env) ? {
    alert   = local.ssm_config[local.msgs_alert_topic_path]
    warning = local.ssm_config[local.msgs_warning_topic_path]
    } : {
    # In the event this module is being applied in a non-critical environment (i.e. an ephemeral
    # environment/test) these lookups will ensure that an empty configuration will be returned
    # instead of an error if no configuration is available.
    alert   = lookup(local.ssm_config, local.msgs_alert_topic_path, null)
    warning = lookup(local.ssm_config, local.msgs_warning_topic_path, null)
  }
  # Use Terraform's "splat" operator to automatically return either an empty list, if no SNS topic
  # was retrieved (data.aws_sns_topic.sns.length == 0) or a list with 1 element that is the ARN of
  # the SNS topic. Functionally equivalent to [for o in data.aws_sns_topic.sns : o.arn]
  msgs_alert_arn   = data.aws_sns_topic.msgs_alert_sns[*].arn
  msgs_warning_arn = data.aws_sns_topic.msgs_warning_sns[*].arn
}

data "aws_sns_topic" "msgs_alert_sns" {
  count = local.msgs_env_sns.alert != null ? 1 : 0
  name  = local.msgs_env_sns.alert
}

data "aws_sns_topic" "msgs_warning_sns" {
  count = local.msgs_env_sns.warning != null ? 1 : 0
  name  = local.msgs_env_sns.warning
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

  alarm_actions = local.msgs_alert_arn

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
    LogGroupName = "/bfd/${local.env}/bfd-pipeline-rda/messages.txt"
  }

  alarm_actions = local.msgs_alert_arn

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}
