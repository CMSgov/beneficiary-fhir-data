locals {
  env = terraform.workspace

  region = data.aws_region.current.name

  app = "bfd-server"

  victor_ops_sns         = "bfd-${local.env}-cloudwatch-alarms"
  bfd_test_slack_sns     = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-test"
  bfd_warnings_slack_sns = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-warnings"
  default_alert_ok_sns   = "bfd-${local.env}-cloudwatch-ok"
  # Each established environment has a different destination for which alarm notifications should
  # route to. The below map maps each particular SNS (destination) to a particular type of SLO
  # alarm.
  topic_names_by_env = {
    prod = {
      alert      = local.victor_ops_sns
      warning    = local.bfd_warnings_slack_sns
      alert_ok   = local.default_alert_ok_sns
      warning_ok = null
    }
    prod-sbx = {
      alert      = local.bfd_test_slack_sns
      warning    = local.bfd_test_slack_sns
      alert_ok   = null
      warning_ok = null
    }
    test = {
      alert      = local.bfd_test_slack_sns
      warning    = local.bfd_test_slack_sns
      alert_ok   = null
      warning_ok = null
    }
  }
  # In the event this module is being applied in a non-established environment (i.e. an ephemeral
  # environment) this lookup will ensure that an empty configuration will be returned
  env_sns = lookup(local.topic_names_by_env, local.env, {
    alert      = null
    warning    = null
    alert_ok   = null
    warning_ok = null
  })
  # The following trys and coalesces ensure two things: the operator is able to override the
  # SNS topic/destination of each alarm type, and that if no destination is specified (either
  # explicitly such as with the OK SNS topics in prod-sbx/test or through the environment being
  # ephemeral) that Terraform does not raise an error and instead the SNS topic is empty
  alert_sns_name      = try(coalesce(var.alert_sns_override, local.env_sns.alert), null)
  warning_sns_name    = try(coalesce(var.warning_sns_override, local.env_sns.warning), null)
  alert_ok_sns_name   = try(coalesce(var.alert_ok_sns_override, local.env_sns.alert_ok), null)
  warning_ok_sns_name = try(coalesce(var.warning_ok_sns_override, local.env_sns.warning_ok), null)
  # Use Terraform's "splat" operator to automatically return either an empty list, if no
  # SNS topic was retrieved (data.aws_sns_topic.sns.length == 0) or a list with 1 element that
  # is the ARN of the SNS topic. Functionally equivalent to [for o in data.aws_sns_topic.sns : o.arn]
  alert_arn      = data.aws_sns_topic.alert_sns[*].arn
  warning_arn    = data.aws_sns_topic.warning_sns[*].arn
  alert_ok_arn   = data.aws_sns_topic.alert_ok_sns[*].arn
  warning_ok_arn = data.aws_sns_topic.warning_ok_sns[*].arn

  namespace = "bfd-${local.env}/${local.app}"
  metrics = {
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

  partners = {
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

  all_partners = merge(local.partners.bulk, local.partners.non_bulk)

  error_slo_configs = {
    slo_http500_percent_1hr_alert = {
      type      = "alert"
      period    = 1 * 60 * 60
      threshold = "10"
    }
    slo_http500_percent_1hr_warning = {
      type      = "warning"
      period    = 1 * 60 * 60
      threshold = "1"
    }
    slo_http500_percent_24hr_alert = {
      type      = "alert"
      period    = 24 * 60 * 60
      threshold = "0.01"
    }
    slo_http500_percent_24hr_warning = {
      type      = "warning"
      period    = 24 * 60 * 60
      threshold = "0.001"
    }
  }

  availability_slo_failure_sum_configs = {
    slo_availability_failures_sum_5m_alert = {
      type      = "alert"
      period    = 60 * 5
      threshold = "3"
    }
    slo_availability_failures_sum_5m_warning = {
      type      = "warning"
      period    = 60 * 5
      threshold = "1"
    }
  }

  availability_slo_uptime_percent_configs = {
    # CloudWatch Alarms have an upper limit of 1 day periods. Unfortunately, our SLO for
    # availability is on a monthly-basis rather than daily, so this alarm does not exactly model
    # that SLO. However, these alarms do give us more immediate and actionable feedback when the BFD
    # Server is falling over, so there is some upside. The thresholds for warning and alert have
    # been modified slightly with the significantly smaller period in-mind, and any failing checks
    # at all (between 100% and 99.8% uptime per-day) will be caught by the other availability alarm
    # TODO: Determine some method of alarming on the agreed-upon monthly availability SLO
    slo_availability_uptime_percent_24hr_warning = {
      type      = "warning"
      period    = 24 * 60 * 60
      threshold = "99.8"
    }
    slo_availability_uptime_percent_24hr_alert = {
      type      = "alert"
      period    = 24 * 60 * 60
      threshold = "99"
    }
  }

  slo_dashboard_url          = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards:name=bfd-${local.env}-server-slos"
  default_dashboard_url      = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards:name=bfd-${local.env}-server"
  dashboard_message_fragment = <<-EOF
View the relevant CloudWatch dashboards below for more information:

* <${local.slo_dashboard_url}|bfd-${local.env}-server-slos>
    * This dashboard visualizes SLOs along with ASG instance count and CPU utilization 
* <${local.default_dashboard_url}|bfd-${local.env}-server>
    * This dashboard visualizes data such as request count and latency per-endpoint and per-partner, and more
  EOF 
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${local.env}-slo-coverage-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "260"

  alarm_description = join("", [
    "/v*/fhir/Coverage response mean 15 minute latency exceeded ALERT SLO threshold of 260ms for ",
    "${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${local.env}-slo-coverage-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "180"

  alarm_description = join("", [
    "/v*/fhir/Coverage response mean 15 minute latency exceeded WARNING SLO threshold of 180ms ",
    "for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_alert" {
  # Only create per-partner alarms for partners that have dimensioned metrics in the current
  # environment. This set intersection returns a set of partners that have corresponding metrics
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-coverage-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded ALERT SLO threshold of ",
    "${local.partners.bulk[each.key].timeout_ms} ms for partner ${each.key} for ${local.app} in ",
    "${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-coverage-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded WARNING SLO threshold of 1440 ",
    "ms for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-coverage-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "2050"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded ALERT SLO threshold of ",
    "2050 ms for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-coverage-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded WARNING SLO threshold of ",
    "1440 ms for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${local.env}-slo-eob-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "440"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 440 ms for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${local.env}-slo-eob-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "310"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 310 ms for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_latency_per_kb_mean_15m_alert" {
  alarm_name          = "${local.app}-${local.env}-slo-eob-with-resources-latency-per-kb-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "450"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with resources returned mean 15 minute latency per KB ",
    "exceeded ALERT SLO threshold of 450 ms/KB for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_latency_per_kb_mean_15m_warning" {
  alarm_name          = "${local.app}-${local.env}-slo-eob-with-resources-latency-per-kb-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "320"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with resources returned mean 15 minute latency per KB ",
    "exceeded WARNING SLO threshold of 320 ms/KB for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-eob-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_bulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-eob-with-resources-bulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute BULK latency ",
    "per KB exceeded WARNING SLO threshold of 480 ms/KB for partner ${each.key} for ${local.app} ",
    "in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-eob-with-resources-nonbulk-latency-per-kb-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "690"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute NON-BULK ",
    "latency per KB exceeded ALERT SLO threshold of 690 ms for partner ${each.key} for ",
    "${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-eob-with-resources-nonbulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute NON-BULK ",
    "latency per KB exceeded WARNING SLO threshold of 480 ms for partner ${each.key} for ",
    "${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-patient-no-contract-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded ALERT SLO ",
    "threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ${each.key} for ",
    "${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-patient-no-contract-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "585"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded WARNING SLO ",
    "threshold of 585 ms for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-patient-no-contract-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "740"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded ALERT ",
    "SLO threshold of 740 ms for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-patient-no-contract-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "520"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded WARNING ",
    "SLO threshold of 520 ms for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${local.env}-slo-patient-by-contract-count-4000-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "40000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response mean 15 minute latency exceeded ALERT ",
    "SLO threshold of 40 seconds for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${local.env}-slo-patient-by-contract-count-4000-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "40000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response mean 15 minute latency exceeded WARNING ",
    "SLO threshold of 40 seconds for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.all_partners)),
    toset(keys(data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-patient-by-contract-count-4000-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.all_partners[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded ALERT ",
    "SLO threshold of ${local.all_partners[each.key].timeout_ms} ms for partner ${each.key} for ",
    "${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.all_partners)),
    toset(keys(data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-patient-by-contract-count-4000-latency-99p-15m-warning-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = "44000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded WARNING ",
    "SLO threshold of 44 seconds for partner ${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${local.env}-slo-claim-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "700"

  alarm_description = join("", [
    "/v*/fhir/Claim response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 700 ms for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.claim_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${local.env}-slo-claim-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "600"

  alarm_description = join("", [
    "/v*/fhir/Claim response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 600 ms for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.claim_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claim_resources_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-claim-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Claim responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.claim_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claim_resources_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.alert_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${local.env}-slo-claimresponse-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "1100"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 1100 ms for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.claimresponse_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  
  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${local.env}-slo-claimresponse-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "950"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 1000 ms for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.claimresponse_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claimresponse_resources_latency"].result))
  )

  alarm_name                            = "${local.app}-${local.env}-slo-claimresponse-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator                   = "GreaterThanThreshold"
  evaluation_periods                    = "1"
  period                                = "900"
  extended_statistic                    = "p99"
  evaluate_low_sample_count_percentiles = "ignore"
  threshold                             = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.claimresponse_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claimresponse_resources_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.alert_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_http500_count_percent" {
  for_each = local.error_slo_configs

  alarm_name          = "${local.app}-${local.env}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "Percent HTTP 500 (error) responses over ${each.value.period / (60 * 60)} hour(s) exceeded ",
    "${upper(each.value.type)} SLO threshold of ${each.value.threshold}% for ${local.app} in ",
    "${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
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
      metric_name = local.metrics.all_responses_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  metric_query {
    id = "m2"

    metric {
      metric_name = local.metrics.all_http500s_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  alarm_actions = each.value.type == "alert" ? local.alert_arn : local.warning_arn
  ok_actions    = each.value.type == "alert" ? local.alert_ok_arn : local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_availability_failures_sum" {
  for_each = local.availability_slo_failure_sum_configs

  alarm_name          = "${local.app}-${local.env}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  period              = each.value.period
  statistic           = "Sum"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "The sum of failed availability checks exceeded or was equal to ${upper(each.value.type)} ",
    "SLO threshold of ${each.value.threshold} failures in ${each.value.period / 60} minute(s) ",
    "for ${local.app} in ${local.env} environment.",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_name = local.metrics.availability_failure_count
  namespace   = local.namespace

  alarm_actions = each.value.type == "alert" ? local.alert_arn : local.warning_arn
  ok_actions    = each.value.type == "alert" ? local.alert_ok_arn : local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_availability_uptime_percent" {
  for_each = local.availability_slo_uptime_percent_configs

  alarm_name          = "${local.app}-${local.env}-${replace(each.key, "_", "-")}"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "The alarm transitioned to the ALARM state due to one of the following occurring:\n",
    "* Percent uptime over ${each.value.period / (24 * 60 * 60)} day(s) dropped below ",
    "${upper(each.value.type)} SLO threshold of ${each.value.threshold}% for ${local.app} in ",
    "${local.env} environment.\n",
    "* No data was reported by the availability checker Jenkins pipeline; the pipeline may have ",
    "stopped running",
    "\n\n${local.dashboard_message_fragment}"
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
      metric_name = local.metrics.availability_success_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  metric_query {
    id = "m2"

    metric {
      metric_name = local.metrics.availability_failure_count
      namespace   = local.namespace
      period      = each.value.period
      stat        = "Sum"
      unit        = "Count"
    }
  }

  alarm_actions = each.value.type == "alert" ? local.alert_arn : local.warning_arn
  ok_actions    = each.value.type == "alert" ? local.alert_ok_arn : local.warning_ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "breaching"
}
