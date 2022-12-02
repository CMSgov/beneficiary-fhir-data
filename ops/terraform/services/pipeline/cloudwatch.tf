resource "aws_cloudwatch_dashboard" "bfd-pipeline-dashboard" {
  dashboard_name = "bfd-pipeline-${local.env}"
  dashboard_body = templatefile("${path.module}/dashboard.json.tftpl",
  { dashboard_namespace = "bfd-${local.env}/bfd-pipeline" })
}

resource "aws_cloudwatch_log_metric_filter" "pipeline-messages-error-count" {
  name           = "bfd-${local.env}/bfd-pipeline/messages/count/error"
  pattern        = "[datetime, env, java_thread, level = \"ERROR\", java_class, message]"
  log_group_name = local.log_groups.messages

  metric_transformation {
    name          = "messages/count/error"
    namespace     = "bfd-${local.env}/bfd-pipeline"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "pipeline-messages-datasetfailed-count" {
  name           = "bfd-${local.env}/bfd-pipeline/messages/count/datasetfailed"
  pattern        = "[datetime, env, java_thread, level = \"ERROR\", java_class, message = \"*Data set failed with an unhandled error*\"]"
  log_group_name = local.log_groups.messages

  metric_transformation {
    name          = "messages/count/datasetfailed"
    namespace     = "bfd-${local.env}/bfd-pipeline"
    value         = "1"
    default_value = "0"
  }
}

# CloudWatch metric alarms
resource "aws_cloudwatch_metric_alarm" "pipeline-messages-error" {
  alarm_name          = "bfd-${local.env}-pipeline-messages-error"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.pipeline_messages_error.eval_periods
  period              = local.pipeline_messages_error.period
  statistic           = "Sum"
  threshold           = local.pipeline_messages_error.threshold
  alarm_description   = "Pipeline errors detected over ${local.pipeline_messages_error.eval_periods} evaluation periods of ${local.pipeline_messages_error.period} seconds in APP-ENV: bfd-${local.env}"

  metric_name = "messages/count/error"
  namespace   = "bfd-${local.env}/bfd-pipeline"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions

  datapoints_to_alarm = local.pipeline_messages_error.datapoints
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "pipeline-messages-datasetfailed" {
  alarm_name          = "bfd-${local.env}-pipeline-messages-datasetfailed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.pipeline_messages_datasetfailed.eval_periods
  period              = local.pipeline_messages_datasetfailed.period
  statistic           = "Sum"
  threshold           = local.pipeline_messages_datasetfailed.threshold
  alarm_description   = "Data set processing failed, pipeline has shut down in APP-ENV: bfd-${local.env}"

  metric_name = "messages/count/datasetfailed"
  namespace   = "bfd-${local.env}/bfd-pipeline"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions

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

  metric_name = "${local.rda_pipeline_latency_alert.metrics[count.index].sink_name}.extract.latency.millis.max"
  namespace   = "bfd-${local.env}/bfd-pipeline"

  # TODO: Address in BFD-2146 with info/notice escalations.
  # alarm_actions =
  # ok_actions    =

  datapoints_to_alarm = local.pipeline_messages_datasetfailed.datapoints
  treat_missing_data  = "notBreaching"
}
