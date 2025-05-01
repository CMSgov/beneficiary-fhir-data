locals {
  slos_alert_topic_path   = "/bfd/${local.service}/sns_topics/slos/alert"
  slos_warning_topic_path = "/bfd/${local.service}/sns_topics/slos/warning"
  slos_env_sns = contains(["prod", "prod-sbx"], local.env) ? {
    alert   = local.ssm_config[local.slos_alert_topic_path]
    warning = local.ssm_config[local.slos_warning_topic_path]
    } : {
    # In the event this module is being applied in a non-critical environment (i.e. an ephemeral
    # environment/test) these lookups will ensure that an empty configuration will be returned
    # instead of an error if no configuration is available.
    alert   = lookup(local.ssm_config, local.slos_alert_topic_path, null)
    warning = lookup(local.ssm_config, local.slos_warning_topic_path, null)
  }
  # Use Terraform's "splat" operator to automatically return either an empty list, if no SNS topic
  # was retrieved (data.aws_sns_topic.sns.length == 0) or a list with 1 element that is the ARN of
  # the SNS topic. Functionally equivalent to [for o in data.aws_sns_topic.sns : o.arn]
  slos_alert_arn   = data.aws_sns_topic.slos_alert_sns[*].arn
  slos_warning_arn = data.aws_sns_topic.slos_warning_sns[*].arn

  rda_pipeline_latency_alert = {
    period       = "300"
    eval_periods = "1"
    threshold    = "28800000"
    datapoints   = "1"
    metrics = [
      { sink_name = "FissClaimRdaSink", claim_type = "fiss" },
      { sink_name = "McsClaimRdaSink", claim_type = "mcs" },
    ]
  }
}

data "aws_sns_topic" "slos_alert_sns" {
  count = local.slos_env_sns.alert != null ? 1 : 0
  name  = local.slos_env_sns.alert
}

data "aws_sns_topic" "slos_warning_sns" {
  count = local.slos_env_sns.warning != null ? 1 : 0
  name  = local.slos_env_sns.warning
}

# Creates alarms for FissClaimRdaSink.extract.latency.millis.max and
# McsClaimRdaSink.extract.latency.millis.max.
resource "aws_cloudwatch_metric_alarm" "latency" {
  count               = length(local.rda_pipeline_latency_alert.metrics)
  alarm_name          = "${local.alarms_prefix}-max-${local.rda_pipeline_latency_alert.metrics[count.index].claim_type}-claim-latency-exceeded"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.rda_pipeline_latency_alert.eval_periods
  period              = local.rda_pipeline_latency_alert.period
  statistic           = "Maximum"
  threshold           = local.rda_pipeline_latency_alert.threshold
  alarm_description = join("", [
    "${local.rda_pipeline_latency_alert.metrics[count.index].claim_type} claim processing is ",
    "falling behind (max latency exceeded) in APP-ENV: bfd-${local.env}",
    "\n\n",
    "${local.dashboard_message_fragment}"
  ])

  metric_name = "${local.rda_pipeline_latency_alert.metrics[count.index].sink_name}.change.latency.millis.avg"
  namespace   = local.metrics_namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = local.pipeline_messages_datasetfailed.datapoints
  treat_missing_data  = "notBreaching"
}
