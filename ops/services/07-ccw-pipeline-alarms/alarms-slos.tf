locals {
  slos_alert_topic_path   = "/bfd/${local.service}/sns_topics/slos/alert"
  slos_warning_topic_path = "/bfd/${local.service}/sns_topics/slos/warning"
  slos_env_sns = contains(["prod", "prod-sbx", "sandbox"], local.env) ? {
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

  data_load_ingestion_time_slo_configs = {
    slo_ingestion_time_warning = {
      metric_name = "CcwRifLoadJob.dataset_processing.active.duration"
      type        = "warning"
      period      = 60
      threshold   = 24 * 60 * 60 * 1000 # 24 hours in ms
    }
    slo_ingestion_time_alert = {
      metric_name = "CcwRifLoadJob.dataset_processing.active.duration"
      type        = "alert"
      period      = 60
      threshold   = 36 * 60 * 60 * 1000 # 36 hours in ms
    }
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

resource "aws_cloudwatch_metric_alarm" "slo_data_load_ingestion_time" {
  for_each = local.data_load_ingestion_time_slo_configs

  alarm_name          = "${local.alarms_prefix}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanThreshold"
  datapoints_to_alarm = 1
  evaluation_periods  = 1
  threshold           = each.value.threshold
  treat_missing_data  = "ignore"

  alarm_actions = each.value.type == "warning" ? local.slos_warning_arn : local.slos_alert_arn

  alarm_description = join("", [
    "CCW Pipeline in ${local.env} environment failed to load data within a ",
    "${each.value.threshold / 60 / 60 / 1000} hour period",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_query {
    expression = join("", [
      "SELECT MAX(\"${each.value.metric_name}\")",
      " FROM SCHEMA(\"${local.metrics_namespace}\", data_set_timestamp,is_synthetic)",
      " WHERE is_synthetic = 'false'",
    ])
    id          = "active_dataset_duration"
    label       = "Active Dataset Processing Duration"
    period      = 60
    return_data = true
  }
}

# Weekend data load availability is implemented by the 'manifests-verifier' Lambda
