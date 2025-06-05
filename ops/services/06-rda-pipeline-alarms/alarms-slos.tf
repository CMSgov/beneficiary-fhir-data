locals {
  slos_topic_paths = {
    high_alert = "/bfd/${local.service}/sns_topics/msgs/high_alert"
    alert      = "/bfd/${local.service}/sns_topics/msgs/alert"
    warning    = "/bfd/${local.service}/sns_topics/msgs/warning"
  }
  slos_topic_names = {
    for k, v in local.msgs_topic_paths
    # In the event this module is being applied in a non-critical environment (i.e. an ephemeral
    # environment/test) using lookup() will ensure that an empty configuration will be returned
    # instead of an error if no configuration is available.
    : k => contains(["prod", "prod-sbx", "sandbox"], local.env) ? nonsensitive(local.ssm_config[v]) : nonsensitive(lookup(local.ssm_config, v, sensitive(null)))
  }
  slos_topic_arns = merge(
    { for k, _ in local.msgs_topic_paths : k => null },
    { for k, v in data.aws_sns_topic.msgs_actions : k => [v.arn] }
  )

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

data "aws_sns_topic" "slos_actions" {
  for_each = { for k, v in local.slos_topic_names : k => v if v != null }
  name     = each.value
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

  alarm_actions = local.slos_topic_arns["warning"]

  datapoints_to_alarm = local.rda_pipeline_latency_alert.datapoints
  treat_missing_data  = "notBreaching"
}
