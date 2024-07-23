resource "aws_cloudwatch_dashboard" "bfd-pipeline-dashboard" {
  count          = local.is_ephemeral_env ? 0 : 1
  dashboard_name = "bfd-pipeline-${local.env}"
  dashboard_body = templatefile("${path.module}/dashboard.json.tftpl",
  { dashboard_namespace = "bfd-${local.env}/bfd-pipeline" })
}

resource "aws_cloudwatch_log_metric_filter" "pipeline-messages-error-count" {
  count          = local.is_ephemeral_env ? 0 : 1
  name           = "bfd-${local.env}/bfd-pipeline/messages/count/error"
  pattern        = "[datetime, env, java_thread, level = \"ERROR\", java_class, message]"
  log_group_name = local.log_groups.messages

  metric_transformation {
    name      = "messages/count/error"
    namespace = local.this-metric-ns
    # namespace     = "bfd-${local.env}/bfd-pipeline"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "pipeline-messages-datasetfailed-count" {
  count          = local.is_ephemeral_env ? 0 : 1
  name           = "bfd-${local.env}/bfd-pipeline/messages/count/datasetfailed"
  pattern        = "[datetime, env, java_thread, level = \"ERROR\", java_class, message = \"*Data set failed with an unhandled error*\"]"
  log_group_name = local.log_groups.messages

  metric_transformation {
    name      = "messages/count/datasetfailed"
    namespace = local.this-metric-ns
    # namespace     = "bfd-${local.env}/bfd-pipeline"
    value         = "1"
    default_value = "0"
  }
}
## BFD-3520
locals {
  init-fail-pattern   = "%failed=[1-9]%"
  this-metric-ns      = "bfd-${local.env}/bfd-pipeline"
  init-fail-filter-nm = "bfd-${local.env}/bfd-pipeline/ec2/init-count/fail"
  init-fail-metric-nm = "cloudinit/count/fail"
  init-fail-alarm-nm  = "bfd-${local.env}-pipeline-init-failure"
}

resource "aws_cloudwatch_log_metric_filter" "pipeline-init-fail-count" {
  count          = local.is_ephemeral_env ? 0 : 1
  name           = local.init-fail-filter-nm
  pattern        = local.init-fail-pattern
  log_group_name = local.log_groups.cloudinit_out

  metric_transformation {
    name          = local.init-fail-metric-nm
    namespace     = local.this-metric-ns
    value         = "1"
    default_value = "0"
  }
}
resource "aws_cloudwatch_metric_alarm" "pipeline-init-fail" {
  alarm_name          = local.init-fail-alarm-nm
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"

  metric_name = local.init-fail-filter-nm
  namespace   = local.this-metric-ns
  period      = 60

  statistic = "Maximum"
  threshold = "1"

  alarm_actions = local.notice_alarm_actions

  actions_enabled   = true
  alarm_description = "Triggers when failed count for startup steps is positive"
}

##

# CloudWatch metric alarms
resource "aws_cloudwatch_metric_alarm" "pipeline-messages-error" {
  count               = local.is_ephemeral_env ? 0 : 1
  alarm_name          = "bfd-${local.env}-pipeline-messages-error"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.pipeline_messages_error.eval_periods
  period              = local.pipeline_messages_error.period
  statistic           = "Sum"
  threshold           = local.pipeline_messages_error.threshold
  alarm_description   = "Pipeline errors detected over ${local.pipeline_messages_error.eval_periods} evaluation periods of ${local.pipeline_messages_error.period} seconds in APP-ENV: bfd-${local.env}"

  metric_name = "messages/count/error"
  namespace   = local.this-metric-ns
  # namespace     = "bfd-${local.env}/bfd-pipeline"

  alarm_actions = local.notice_alarm_actions

  datapoints_to_alarm = local.pipeline_messages_error.datapoints
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "pipeline-messages-datasetfailed" {
  count               = local.is_ephemeral_env ? 0 : 1
  alarm_name          = "bfd-${local.env}-pipeline-messages-datasetfailed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.pipeline_messages_datasetfailed.eval_periods
  period              = local.pipeline_messages_datasetfailed.period
  statistic           = "Sum"
  threshold           = local.pipeline_messages_datasetfailed.threshold
  alarm_description   = "Data set processing failed, pipeline has shut down in APP-ENV: bfd-${local.env}"

  metric_name = "messages/count/datasetfailed"
  namespace   = local.this-metric-ns
  # namespace     = "bfd-${local.env}/bfd-pipeline"

  alarm_actions = local.notice_alarm_actions

  datapoints_to_alarm = local.pipeline_messages_datasetfailed.datapoints
  treat_missing_data  = "notBreaching"
}

# Creates alarms for FissClaimRdaSink.extract.latency.millis.max and 
# McsClaimRdaSink.extract.latency.millis.max.
resource "aws_cloudwatch_metric_alarm" "pipeline-max-claim-latency-exceeded" {
  count               = length(local.rda_pipeline_latency_alert.metrics)
  alarm_name          = "bfd-${local.env}-pipeline-max-${local.rda_pipeline_latency_alert.metrics[count.index].claim_type}-claim-latency-exceeded"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.rda_pipeline_latency_alert.eval_periods
  period              = local.rda_pipeline_latency_alert.period
  statistic           = "Maximum"
  threshold           = local.rda_pipeline_latency_alert.threshold
  alarm_description   = "${local.rda_pipeline_latency_alert.metrics[count.index].claim_type} claim processing is falling behind (max latency exceeded) in APP-ENV: bfd-${local.env}"

  metric_name = "${local.rda_pipeline_latency_alert.metrics[count.index].sink_name}.change.latency.millis.avg"
  namespace   = local.this-metric-ns
  # namespace     = "bfd-${local.env}/bfd-pipeline"

  alarm_actions = local.notice_alarm_actions

  datapoints_to_alarm = local.pipeline_messages_datasetfailed.datapoints
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "pipeline-log-availability-1hr" {
  alarm_name          = "bfd-${local.env}-pipeline-log-availability-1hr"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = local.pipeline_log_availability.eval_periods
  period              = local.pipeline_log_availability.period
  statistic           = "Sum"
  threshold           = local.pipeline_log_availability.threshold

  alarm_description = join("", [
    "Pipeline logs have not been submitted to CloudWatch in 1 hour, pipeline has likely shutdown ",
    "in APP-ENV: bfd-${local.env}"
  ])

  metric_name = "IncomingLogEvents"
  namespace   = "AWS/Logs"

  dimensions = {
    LogGroupName = "/bfd/${local.env}/bfd-pipeline/messages.txt"
  }

  alarm_actions = local.log_availability_alarm_actions

  datapoints_to_alarm = local.pipeline_log_availability.datapoints
  treat_missing_data  = "notBreaching"
}
