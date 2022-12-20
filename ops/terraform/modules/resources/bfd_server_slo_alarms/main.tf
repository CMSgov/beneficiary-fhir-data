locals {
  app = "bfd-server"

  alert_arn   = var.alert_notification_arn == null ? [] : [var.alert_notification_arn]
  warning_arn = var.warning_notification_arn == null ? [] : [var.warning_notification_arn]
  ok_arn      = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]

  namespace = "bfd-${var.env}/${local.app}"
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
      threshold = "90"
    }
    slo_http500_percent_1hr_warning = {
      type      = "warning"
      period    = 1 * 60 * 60
      threshold = "50"
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
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-coverage-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "260"

  alarm_description = join("", [
    "/v*/fhir/Coverage response mean 15 minute latency exceeded ALERT SLO threshold of 260ms for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-coverage-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "180"

  alarm_description = join("", [
    "/v*/fhir/Coverage response mean 15 minute latency exceeded WARNING SLO threshold of 180ms ",
    "for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

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

  alarm_name          = "${local.app}-${var.env}-slo-coverage-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded ALERT SLO threshold of ",
    "${local.partners.bulk[each.key].timeout_ms} ms for partner ${each.key} for ${local.app} in ",
    "${var.env} environment"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-coverage-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded WARNING SLO threshold of 1440 ",
    "ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-coverage-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "2050"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded ALERT SLO threshold of ",
    "2050 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["coverage_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-coverage-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded WARNING SLO threshold of ",
    "1440 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.coverage_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["coverage_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-eob-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "440"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 440 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-eob-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "310"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 310 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_latency_per_kb_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-eob-with-resources-latency-per-kb-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "450"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with resources returned mean 15 minute latency per KB ",
    "exceeded ALERT SLO threshold of 450 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_latency_per_kb_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-eob-with-resources-latency-per-kb-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "320"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response with resources returned mean 15 minute latency per KB ",
    "exceeded WARNING SLO threshold of 320 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-eob-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_bulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-eob-with-resources-bulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute BULK latency ",
    "per KB exceeded WARNING SLO threshold of 480 ms/KB for partner ${each.key} for ${local.app} ",
    "in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-eob-with-resources-nonbulk-latency-per-kb-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "690"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute NON-BULK ",
    "latency per KB exceeded ALERT SLO threshold of 690 ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-eob-with-resources-nonbulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit responses with resources returned 99% 15 minute NON-BULK ",
    "latency per KB exceeded WARNING SLO threshold of 480 ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.eob_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["eob_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "80"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response mean 15 minute latency exceeded ALERT SLO ",
    "threshold of 80 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "60"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response mean 15 minute latency exceeded WARNING SLO ",
    "threshold of 60 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded ALERT SLO ",
    "threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "585"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded WARNING SLO ",
    "threshold of 585 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "740"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded ALERT ",
    "SLO threshold of 740 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.non_bulk)),
    toset(keys(data.external.client_ssls_by_partner["patient_no_contract_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "520"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded WARNING ",
    "SLO threshold of 520 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_no_contract_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_no_contract_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-patient-by-contract-count-4000-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "40000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response mean 15 minute latency exceeded ALERT ",
    "SLO threshold of 40 seconds for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-patient-by-contract-count-4000-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "40000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response mean 15 minute latency exceeded WARNING ",
    "SLO threshold of 40 seconds for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.all_partners)),
    toset(keys(data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-patient-by-contract-count-4000-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = local.all_partners[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded ALERT ",
    "SLO threshold of ${local.all_partners[each.key].timeout_ms} ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.all_partners)),
    toset(keys(data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-patient-by-contract-count-4000-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "44000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded WARNING ",
    "SLO threshold of 44 seconds for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.patient_contract_count_4000_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["patient_contract_count_4000_latency"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-claim-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "700"

  alarm_description = join("", [
    "/v*/fhir/Claim response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 700 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claim_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-claim-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "600"

  alarm_description = join("", [
    "/v*/fhir/Claim response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 600 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claim_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_with_resources_latency_per_kb_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-claim-with-resources-latency-per-kb-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "700"

  alarm_description = join("", [
    "/v*/fhir/Claim response with resources returned mean 15 minute latency per KB ",
    "exceeded ALERT SLO threshold of 700 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claim_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_with_resources_latency_per_kb_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-claim-with-resources-latency-per-kb-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "600"

  alarm_description = join("", [
    "/v*/fhir/Claim response with resources returned mean 15 minute latency per KB ",
    "exceeded WARNING SLO threshold of 600 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claim_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claim_resources_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-claim-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/Claim responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claim_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claim_resources_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claim_with_resources_bulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claim_resources_latency_by_kb"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-claim-with-resources-bulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "700"

  alarm_description = join("", [
    "/v*/fhir/Claim responses with resources returned 99% 15 minute BULK latency ",
    "per KB exceeded WARNING SLO threshold of 480 ms/KB for partner ${each.key} for ${local.app} ",
    "in ${var.env} environment"
  ])

  metric_name = local.metrics.claim_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claim_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_no_resources_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-claimresponse-no-resources-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "1100"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with no resources returned mean 15 minute latency ",
    "exceeded ALERT SLO threshold of 1100 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claimresponse_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_no_resources_latency_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-claimresponse-no-resources-latency-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "1000"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with no resources returned mean 15 minute latency ",
    "exceeded WARNING SLO threshold of 1000 ms for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claimresponse_no_resources_latency
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_with_resources_latency_per_kb_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-claimresponse-with-resources-latency-per-kb-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "1100"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with resources returned mean 15 minute latency per KB ",
    "exceeded ALERT SLO threshold of 1100 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claimresponse_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_with_resources_latency_per_kb_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-claimresponse-with-resources-latency-per-kb-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "1000"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse response with resources returned mean 15 minute latency per KB ",
    "exceeded WARNING SLO threshold of 1000 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claimresponse_resources_latency_by_kb
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_with_resources_bulk_latency_99p_15m_alert" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claimresponse_resources_latency"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-claimresponse-with-resources-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = local.partners.bulk[each.key].timeout_ms

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse responses with resources returned 99% 15 minute BULK latency ",
    "exceeded ALERT SLO threshold of ${local.partners.bulk[each.key].timeout_ms} ms for partner ",
    "${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = local.metrics.claimresponse_resources_latency
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claimresponse_resources_latency"].result[each.key]
  }

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_claimresponse_with_resources_bulk_latency_per_kb_99p_15m_warning" {
  for_each = setintersection(
    toset(keys(local.partners.bulk)),
    toset(keys(data.external.client_ssls_by_partner["claimresponse_resources_latency_by_kb"].result))
  )

  alarm_name          = "${local.app}-${var.env}-slo-claimresponse-with-resources-bulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = "p99"
  threshold           = "1100"

  alarm_description = join("", [
    "/v*/fhir/ClaimResponse responses with resources returned 99% 15 minute BULK latency ",
    "per KB exceeded WARNING SLO threshold of 480 ms/KB for partner ${each.key} for ${local.app} ",
    "in ${var.env} environment"
  ])

  metric_name = local.metrics.claimresponse_resources_latency_by_kb
  namespace   = local.namespace
  dimensions = {
    client_ssl = data.external.client_ssls_by_partner["claimresponse_resources_latency_by_kb"].result[each.key]
  }

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_http500_count_percent" {
  for_each = local.error_slo_configs

  alarm_name          = "${local.app}-${var.env}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  threshold           = each.value.threshold

  alarm_description = join("", [
    "Percent HTTP 500 (error) responses over ${each.value.period / (60 * 60)} hour(s) exceeded ",
    "${upper(each.value.type)} SLO threshold of ${each.value.threshold}% for ${local.app} in ",
    "${var.env} environment"
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
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}