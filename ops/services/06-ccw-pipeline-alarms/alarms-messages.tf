locals {
  msgs_alert_topic_path   = "/bfd/${local.service}/sns_topics/msgs/alert"
  msgs_warning_topic_path = "/bfd/${local.service}/sns_topics/msgs/warning"
  msgs_env_sns = contains(["prod", "prod-sbx", "sandbox"], local.env) ? {
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
  alarm_description   = "CCW Pipeline errors detected in ${local.env}"

  metric_name = "messages/count/error"
  namespace   = local.metrics_namespace

  alarm_actions = local.msgs_warning_arn

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "dataset_failed" {
  alarm_name          = "${local.alarms_prefix}-dataset-failed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Data set processing failed, CCW Pipeline has shut down in ${local.env}"

  metric_name = "messages/count/datasetfailed"
  namespace   = local.metrics_namespace

  alarm_actions = local.msgs_warning_arn

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}
