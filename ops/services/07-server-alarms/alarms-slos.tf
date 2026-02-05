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

  slos_metrics = {
    coverage_latency                      = "http-requests/latency/coverage-all"
    eob_resources_latency                 = "http-requests/latency/eob-all-with-resources"
    eob_no_resources_latency              = "http-requests/latency/eob-all-no-resources"
    eob_resources_latency_by_kb           = "http-requests/latency-by-kb/eob-all-with-resources"
    patient_no_contract_latency           = "http-requests/latency/patient-not-by-contract"
    patient_contract_count_4000_latency   = "http-requests/latency/patient-by-contract-count-4000"
    claim_no_resources_latency            = "http-requests/latency/claim-all-no-resources"
    claim_resources_latency               = "http-requests/latency/claim-all-with-resources"
    claim_resources_latency_by_kb         = "http-requests/latency-by-kb/claim-all-with-resources"
    claimresponse_no_resources_latency    = "http-requests/latency/claimresponse-all-no-resources"
    claimresponse_resources_latency       = "http-requests/latency/claimresponse-all-with-resources"
    claimresponse_resources_latency_by_kb = "http-requests/latency-by-kb/claimresponse-all-with-resources"
    all_responses_count                   = "http-requests/count/all"
    all_http500s_count                    = "http-requests/count/500-responses"
    availability_success_count            = "availability/success"
    availability_failure_count            = "availability/failure"
  }

  # Per-environment "client_ssl_regex" have not been moved to SSM configuration as they will be
  # removed entirely when mTLS is moved to the ALB. The Server will expose the partner as a new MDC
  # attribute, rendering these regexes obsolete
  slos_partners = {
    bulk = {
      ab2d = {
        timeout_ms = (300 * 1000) / 2
        client_ssl_regex = {
          prod_sbx = ".*ab2d-sbx-client.*"
          prod     = ".*ab2d-prod-client.*"
        }
      }
      bcda = {
        timeout_ms = (45 * 1000) / 2
        client_ssl_regex = {
          prod_sbx = ".*bcda-sbx-client.*"
          prod     = ".*bcda-prod-client.*"
        }
      }
      dpc = {
        timeout_ms = (30 * 1000) / 2
        client_ssl_regex = {
          prod_sbx = ".*dpc-prod-sbx-client.*"
          # jq requires escaped characters be escaped with 2 backslashes
          prod = ".*dpc\\\\.prod\\\\.client.*"
        }
      }
    }
    non_bulk = {
      bb = {
        timeout_ms = (120 * 1000) / 2
        client_ssl_regex = {
          prod_sbx = ".*BlueButton.*"
          prod     = ".*BlueButton.*"
        }
      }
    }
  }

  slos_all_partners = merge(local.slos_partners.bulk, local.slos_partners.non_bulk)

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

  availability_slo_uptime_percent_configs = {
    # CloudWatch Alarms have an upper limit of 1 day periods. Unfortunately, our SLO for
    # availability is on a monthly-basis rather than daily, so this alarm does not exactly model
    # that SLO. However, these alarms do give us more immediate and actionable feedback when the BFD
    # Server is falling over, so there is some upside. The thresholds for warning and alert have
    # been modified slightly with the significantly smaller period in-mind, and any failing checks
    # at all (between 100% and 99.8% uptime per-day) will be caught by the other availability alarm
    slo_availability_uptime_percent_24hr_warning = {
      type          = "warning"
      period        = 24 * 60 * 60
      threshold     = "99.8"
      alarm_actions = local.slos_warning_arn
    }
    slo_availability_uptime_percent_24hr_alert = {
      type          = "alert"
      period        = 24 * 60 * 60
      threshold     = "99"
      alarm_actions = local.slos_alert_arn
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

data "external" "client_ssls_by_partner" {
  for_each = local.slos_metrics

  program = [
    "bash",
    "${path.module}/scripts/get-partner-client-ssl.sh",
    each.value,
    local.namespace,
    jsonencode({
      for partner, config in local.slos_all_partners :
      partner => lookup(config.client_ssl_regex, replace(local.env, "-", "_"), null)
    })
  ]
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-coverage-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "260"

  alarm_description = join("", [
    "/v*/fhir/Coverage response mean 15 minute latency exceeded ALERT SLO threshold of 260ms for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.coverage_latency
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-coverage-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "180"

  alarm_description = join("", [
    "/v*/fhir/Coverage response mean 15 minute latency exceeded WARNING SLO threshold of 180ms ",
    "for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.coverage_latency
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_alert" {
  # Only create per-partner alarms for partners that have dimensioned metrics in the current
  # environment. This set intersection returns a set of partners that have corresponding metrics
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-coverage-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.slos_partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded ALERT SLO threshold of ",
    "${local.slos_partners.bulk[each.key].timeout_ms} ms for partner ${each.key} for",
    " ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-coverage-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded WARNING SLO threshold of 1440 ",
    "ms for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-coverage-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "2050"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded ALERT SLO threshold of ",
    "2050 ms for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-coverage-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded WARNING SLO threshold of ",
    "1440 ms for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-eob-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "440"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 440 ms for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-eob-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "310"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 310 ms for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_latency_per_kb_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-eob-with-resources-latency-per-kb-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "450"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with resources returned mean 15 minute latency per KB ",
    "exceeded ALERT SLO threshold of 450 ms/KB for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_latency_per_kb_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-eob-with-resources-latency-per-kb-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "320"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with resources returned mean 15 minute latency per KB ",
    "exceeded WARNING SLO threshold of 320 ms/KB for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-eob-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.slos_partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.slos_partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_bulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-eob-with-resources-bulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute BULK latency ",
    "per KB exceeded WARNING SLO threshold of 480 ms/KB for partner ${each.key} for ${local.target_service} ",
    "in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-eob-with-resources-nonbulk-latency-per-kb-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "690"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute NON-BULK ",
    "latency per KB exceeded ALERT SLO threshold of 690 ms for partner ${each.key} for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-eob-with-resources-nonbulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute NON-BULK ",
    "latency per KB exceeded WARNING SLO threshold of 480 ms for partner ${each.key} for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-patient-no-contract-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.slos_partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded ALERT SLO ",
    "threshold of ${local.slos_partners.bulk[each.key].timeout_ms} ms for partner ${each.key} for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-patient-no-contract-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "585"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded WARNING SLO ",
    "threshold of 585 ms for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-patient-no-contract-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "740"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded ALERT ",
    "SLO threshold of 740 ms for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-patient-no-contract-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "520"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded WARNING ",
    "SLO threshold of 520 ms for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-patient-by-contract-count-4000-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "40000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response mean 15 minute latency exceeded ALERT ",
    "SLO threshold of 40 seconds for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_contract_count_4000_latency
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-patient-by-contract-count-4000-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "40000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response mean 15 minute latency exceeded WARNING ",
    "SLO threshold of 40 seconds for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_contract_count_4000_latency
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_all_partners)),
    toset(keys(data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-patient-by-contract-count-4000-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.slos_all_partners[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded ALERT ",
    "SLO threshold of ${local.slos_all_partners[each.key].timeout_ms} ms for partner ${each.key} for ",
    "${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_contract_count_4000_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.slos_all_partners)),
    toset(keys(data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-patient-by-contract-count-4000-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "44000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded WARNING ",
    "SLO threshold of 44 seconds for partner ${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.patient_contract_count_4000_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result[each.key]
  }

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-claim-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "700"

  alarm_description = join("", [
    "/v*/fhir/Claim response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 700 ms for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.claim_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-claim-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "600"

  alarm_description = join("", [
    "/v*/fhir/Claim response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 600 ms for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.claim_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claim_resources_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-claim-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.slos_partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Claim responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.slos_partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.claim_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claim_resources_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.alarm_name_prefix}-slo-claimresponse-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "1100"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 1100 ms for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.claimresponse_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.slos_alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.alarm_name_prefix}-slo-claimresponse-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "950"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 1000 ms for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.claimresponse_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.slos_warning_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.slos_partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claimresponse_resources_latency"].result))
  )

  alarm_name                            = "${local.alarm_name_prefix}-slo-claimresponse-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.slos_partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.slos_partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.target_service} in ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_name = local.slos_metrics.claimresponse_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claimresponse_resources_latency"].result[each.key]
  }

  alarm_actions = local.slos_alert_arn

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
    "Percent HTTP 500 (error) responses over ${each.value.period / (60 * 60)} hour(s) exceeded ",
    "${upper(each.value.type)} SLO threshold of ${each.value.threshold}% for ${local.target_service} in ",
    "${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_query {
    id          = "e1"
    expression  = "m2/m1*100"
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

  # TODO: Intentionally disabled for until availability solution is refactored out of Jenkins
  # alarm_actions = each.value.alarm_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_availability_uptime_percent" {
  for_each = local.availability_slo_uptime_percent_configs

  alarm_name          = "${local.alarm_name_prefix}-${replace(each.key, "_", "-")}"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "The alarm transitioned to the ALARM state due to one of the following occurring:\n",
    "* Percent uptime over ${each.value.period / (24 * 60 * 60)} day(s) dropped below ",
    "${upper(each.value.type)} SLO threshold of ${each.value.threshold}% for ${local.target_service} in ",
    "${local.env} environment.\n",
    "* No data was reported by the availability checker Jenkins pipeline; the pipeline may have ",
    "stopped running",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_query {
    id          = "e1"
    expression  = "100*(m1/(m1+m2))"
    label       = "% Uptime"
    return_data = "true"
  }

  metric_query {
    id = "m1"

    metric {
      metric_name = local.slos_metrics.availability_success_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  metric_query {
    id = "m2"

    metric {
      metric_name = local.slos_metrics.availability_failure_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  # TODO: Intentionally disabled for until availability solution is refactored out of Jenkins
  # alarm_actions = each.value.alarm_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "breaching"
}
