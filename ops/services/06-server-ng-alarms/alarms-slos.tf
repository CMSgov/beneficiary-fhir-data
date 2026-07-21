locals {
  # Ensures that prod and prod-sbx/sandbox always have a valid alarm alert destination, as the
  # application of this Terraservice will fail-fast otherwise
  slos_high_alert_topic_path = "/bfd/${local.service}/sns_topics/slos/high_alert"
  slos_alert_topic_path      = "/bfd/${local.service}/sns_topics/slos/alert"
  slos_warning_topic_path    = "/bfd/${local.service}/sns_topics/slos/warning"
  slos_env_sns = contains(["prod", "prod-sbx", "sandbox"], local.env) ? {
    high_alert = local.ssm_config[local.slos_high_alert_topic_path]
    alert      = local.ssm_config[local.slos_alert_topic_path]
    warning    = local.ssm_config[local.slos_warning_topic_path]
    } : {
    # In the event this module is being applied in a non-critical environment (i.e. an ephemeral
    # environment/test) these lookups will ensure that an empty configuration will be returned
    # instead of an error if no configuration is available.
    high_alert = lookup(local.ssm_config, local.slos_high_alert_topic_path, null)
    alert      = lookup(local.ssm_config, local.slos_alert_topic_path, null)
    warning    = lookup(local.ssm_config, local.slos_warning_topic_path, null)
  }
  # Use Terraform's "splat" operator to automatically return either an empty list, if no SNS topic
  # was retrieved (data.aws_sns_topic.sns.length == 0) or a list with 1 element that is the ARN of
  # the SNS topic. Functionally equivalent to [for o in data.aws_sns_topic.sns : o.arn]
  slos_high_alert_arn = data.aws_sns_topic.slos_high_alert_sns[*].arn
  slos_alert_arn      = data.aws_sns_topic.slos_alert_sns[*].arn
  slos_warning_arn    = data.aws_sns_topic.slos_warning_sns[*].arn

  http500_log_insights_query = <<-EOF
fields @timestamp, @message, status, operation, request_id
| filter status = 500
| sort @timestamp desc
| limit 200
EOF
  http500_log_insights_query_url = join("", [
    "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#",
    "logsV2:logs-insights$3FqueryDetail$3D",
    urlencode(jsonencode({
      editorString = local.http500_log_insights_query
      start        = "-3600"
      end          = "0"
      timeType     = "RELATIVE"
      unit         = "seconds"
      source       = [local.server_access_log_group_name]
    }))
  ])

  slos_metrics = {
    all_latency                         = "http-requests/latency/all"
    all_responses_count                 = "http-requests/count/all"
    all_http500s_count                  = "http-requests/count/500-responses"
    availability_success_count          = "availability/success"
    availability_failure_count          = "availability/failure"
  }

  error_slo_configs = {
    slo_http500_percent_1hr_alert = {
      type          = "alert"
      period        = 1 * 60 * 60
      threshold     = "10"
      alarm_actions = local.slos_high_alert_arn
    }
    slo_http500_percent_1hr_warning = {
      type          = "warning"
      period        = 1 * 60 * 60
      threshold     = "1"
      alarm_actions = local.slos_warning_arn
    }
    slo_http500_percent_24hr_alert = {
      type          = "alert"
      period        = 24 * 60 * 60
      threshold     = "0.01"
      alarm_actions = local.slos_high_alert_arn
    }
    slo_http500_percent_24hr_warning = {
      type          = "warning"
      period        = 24 * 60 * 60
      threshold     = "0.001"
      alarm_actions = local.slos_warning_arn
    }
    slo_http500_percent_1hr_alert_min_req_1000 = {
      type          = "alert"
      period        = 1 * 60 * 60
      threshold     = "50"
      min_requests  = 1000
      alarm_actions = local.slos_high_alert_arn
    }
  }

  availability_slo_failure_sum_configs = {
    slo_availability_failures_sum_5m_alert = {
      type          = "alert"
      period        = 60 * 5
      threshold     = "3"
      alarm_actions = local.slos_high_alert_arn
    }
    slo_availability_failures_sum_5m_warning = {
      type          = "warning"
      period        = 60 * 5
      threshold     = "1"
      alarm_actions = local.slos_warning_arn
    }
  }

}

data "aws_sns_topic" "slos_high_alert_sns" {
  count = local.slos_env_sns.high_alert != null ? 1 : 0
  name  = local.slos_env_sns.high_alert
}

data "aws_sns_topic" "slos_alert_sns" {
  count = local.slos_env_sns.alert != null ? 1 : 0
  name  = local.slos_env_sns.alert
}

data "aws_sns_topic" "slos_warning_sns" {
  count = local.slos_env_sns.warning != null ? 1 : 0
  name  = local.slos_env_sns.warning
}


resource "aws_cloudwatch_metric_alarm" "slo_all_latency_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-all-latency-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "10000"

  alarm_description = join("", [
    "All endpoint response mean 15 minute latency exceeded ALERT threshold of 10 seconds for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.all_latency
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_all_latency_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-all-latency-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "5000"

  alarm_description = join("", [
    "All endpoint response mean 15 minute latency exceeded WARNING threshold of 5 seconds for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.all_latency
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_http500_count_percent" {
  for_each = local.error_slo_configs

  alarm_name          = "${local.alarm_name_prefix}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "HTTP 500 error rate over ${each.value.period / (60 * 60)} hour(s) exceeded ",
    "${upper(each.value.type)} threshold ${each.value.threshold}% for ${local.target_service} in ",
    "${local.env}. ",
    "Logs query: ${local.http500_log_insights_query_url}"
  ])

  metric_query {
    id          = "e1"
    expression  = "IF(m1>${lookup(each.value, "min_requests", 0)}, m2/m1*100, 0)"
    label       = "Error Rate"
    return_data = "true"
  }

  metric_query {
    id = "m1"

    metric {
      metric_name = local.slos_metrics.all_responses_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  metric_query {
    id = "m2"

    metric {
      metric_name = local.slos_metrics.all_http500s_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  alarm_actions = each.value.alarm_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_http500_any_count_5m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-http500-any-count-5m-alert"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  period              = "300"
  statistic           = "Sum"
  threshold           = "1"

  alarm_description = join("", [
    "At least one HTTP 500 response occurred in the last 5 minutes for ",
    "${local.target_service} in ${local.env}. ",
    "Logs query: ${local.http500_log_insights_query_url}"
  ])

  metric_name = local.slos_metrics.all_http500s_count
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_availability_failures_sum" {
  for_each = local.availability_slo_failure_sum_configs

  alarm_name          = "${local.alarm_name_prefix}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  period              = each.value.period
  statistic           = "Sum"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "The sum of failed availability checks exceeded or was equal to ${upper(each.value.type)} ",
    "SLO threshold of ${each.value.threshold} failures in ${each.value.period / 60} minute(s) ",
    "for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.availability_failure_count
  namespace   = local.namespace

  # alarm_actions = each.value.alarm_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

