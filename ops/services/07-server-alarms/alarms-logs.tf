locals {
  logs_alert_topic_path   = "/bfd/${local.service}/sns_topics/logs/alert"
  logs_warning_topic_path = "/bfd/${local.service}/sns_topics/logs/warning"
  logs_env_sns = contains(["prod", "prod-sbx", "sandbox"], local.env) ? {
    alert   = local.ssm_config[local.logs_alert_topic_path]
    warning = local.ssm_config[local.logs_warning_topic_path]
    } : {
    # In the event this module is being applied in a non-critical environment (i.e. an ephemeral
    # environment/test) these lookups will ensure that an empty configuration will be returned
    # instead of an error if no configuration is available.
    alert   = lookup(local.ssm_config, local.logs_alert_topic_path, null)
    warning = lookup(local.ssm_config, local.logs_warning_topic_path, null)
  }
  # Use Terraform's "splat" operator to automatically return either an empty list, if no SNS topic
  # was retrieved (data.aws_sns_topic.sns.length == 0) or a list with 1 element that is the ARN of
  # the SNS topic. Functionally equivalent to [for o in data.aws_sns_topic.sns : o.arn]
  logs_alert_arn   = data.aws_sns_topic.logs_alert_sns[*].arn
  logs_warning_arn = data.aws_sns_topic.logs_warning_sns[*].arn
}

data "aws_sns_topic" "logs_alert_sns" {
  count = local.logs_env_sns.alert != null ? 1 : 0
  name  = local.logs_env_sns.alert
}

data "aws_sns_topic" "logs_warning_sns" {
  count = local.logs_env_sns.warning != null ? 1 : 0
  name  = local.logs_env_sns.warning
}

resource "aws_cloudwatch_metric_alarm" "server_log_availability_1hr" {
  alarm_name          = "${local.alarm_name_prefix}-log-availability-1hr"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 1
  period              = 1 * 60 * 60 # 1 hour
  statistic           = "Sum"
  threshold           = 0

  alarm_description = join("", [
    "${local.target_service} logs have not been submitted to CloudWatch in 1 hour,",
    " ${local.target_service} has possibly shutdown in ${local.env}.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = "IncomingLogEvents"
  namespace   = "AWS/Logs"

  dimensions = {
    LogGroupName = data.aws_cloudwatch_log_group.server_access.name
  }

  alarm_actions = local.logs_alert_arn

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}

# NOTE: alarm is triggered when gov.cms.bfd.server.war QueryLoggingListener encounters an
#       unknown query, signaling that the application is missing a required pattern.
resource "aws_cloudwatch_metric_alarm" "server_query_logging_listener_warning" {
  alarm_name          = "${local.alarm_name_prefix}-query-logging-listener-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  period              = 1 * 60 # 1 Minute
  statistic           = "Sum"
  threshold           = 1

  alarm_description = join("", [
    "${local.target_service} QueryLoggingListener has encountered an unknown query ",
    "in APP-ENV: bfd-${local.env}"
  ])

  metric_name = "query-logging-listener/count/warning"
  namespace   = local.namespace

  # TODO: Re-enable when query logging listener errors are addressed
  # alarm_actions = local.logs_warning_arn

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "samhsa_mismatch_error" {
  alarm_name          = "${local.alarm_name_prefix}-samhsa-mismatch-error"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = 1
  evaluation_periods  = 1
  period              = 60
  statistic           = "Sum"

  alarm_description = "${local.target_service} has encountered a SAMHSA 2.0 filter mismatch error in ${local.env}"

  metric_name = "samhsa-mismatch/count/error"
  namespace   = local.namespace

  # TODO: Re-enable when SAMHSA mismatch errors are fixed
  # alarm_actions = local.logs_warning_arn

  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"
}
