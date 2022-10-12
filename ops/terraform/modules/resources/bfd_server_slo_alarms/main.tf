locals {
  app = "bfd-server"

  alert_arn   = var.alert_notification_arn == null ? [] : [var.alert_notification_arn]
  warning_arn = var.warning_notification_arn == null ? [] : [var.warning_notification_arn]
  ok_arn      = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]

  namespace = "bfd-${var.env}/${local.app}"
  metrics = {
    coverage_latency                    = "http-requests/latency/coverageAll"
    eob_latency                         = "http-requests/latency/eobAll"
    eob_no_resources_latency            = "http-requests/latency/eobAllNoResources"
    eob_latency_by_kb                   = "http-requests/latency-by-kb/eobAll"
    patient_no_contract_latency         = "http-requests/latency/patientNotByContract"
    patient_contract_count_4000_latency = "http-requests/latency/patientByContractCount4000"
    all_error_rate                      = "http-requests/count-500"
  }

  bulk_partners = [
    "bcda",
    "dpc",
    "abd2d"
  ]

  non_bulk_partners = [
    "bb"
  ]

  partner_timeouts_ms = {
    ab2d = (300 * 1000) / 2
    bcda = (45 * 1000) / 2
    dpc  = (30 * 1000) / 2
    bb   = (120 * 1000) / 2
  }

  ext_stat_99p = "p99"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_latency_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-coverage-latency-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "260"

  alarm_description = "/v*/fhir/Coverage response mean 15 minute latency exceeded ALERT SLO threshold of 260ms for ${local.app} in ${var.env} environment"

  metric_name = "${local.metrics.coverage_latency}/all"
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

  alarm_description = "/v*/fhir/Coverage response mean 15 minute latency exceeded WARNING SLO threshold of 180ms for ${local.app} in ${var.env} environment"

  metric_name = "${local.metrics.coverage_latency}/all"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_alert" {
  for_each = local.bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-coverage-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "${local.partner_timeouts_ms[each.key]}"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded ALERT SLO threshold of ",
    "${local.partner_timeouts_ms[each.key]} ms for partner ${each.key} for ${local.app} ",
    "in ${var.env} environment"
  ])

  metric_name = "${local.metrics.coverage_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_bulk_latency_99p_15m_warning" {
  for_each = local.bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-coverage-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute BULK latency exceeded WARNING SLO threshold of 1440 ",
    "ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.coverage_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_alert" {
  for_each = local.non_bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-coverage-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "2050"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded ALERT SLO threshold of ",
    "2050 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.coverage_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_coverage_nonbulk_latency_99p_15m_warning" {
  for_each = local.non_bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-coverage-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "1440"

  alarm_description = join("", [
    "/v*/fhir/Coverage response 99% 15 minute NON-BULK latency exceeded WARNING SLO threshold of ",
    "1440 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.coverage_latency}/${each.key}"
  namespace   = local.namespace

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

  metric_name = "${local.metrics.eob_no_resources_latency}/all"
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

  metric_name = "${local.metrics.eob_no_resources_latency}/all"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_latency_per_kb_mean_15m_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-eob-latency-per-kb-mean-15m-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "450"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response mean 15 minute latency per KB exceeded ALERT SLO ",
    "threshold of 450 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.eob_latency_by_kb}/all"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_latency_per_kb_mean_15m_warning" {
  alarm_name          = "${local.app}-${var.env}-slo-eob-latency-per-kb-mean-15m-warning"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  statistic           = "Average"
  threshold           = "320"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response mean 15 minute latency per KB exceeded WARNING SLO ",
    "threshold of 320 ms/KB for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.eob_latency_by_kb}/all"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_bulk_latency_99p_15m_alert" {
  for_each = local.bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-eob-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "${local.partner_timeouts_ms[each.key]}"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response 99% 15 minute BULK latency exceeded ALERT SLO ",
    "threshold of ${local.partner_timeouts_ms[each.key]} ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.eob_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_bulk_latency_per_kb_99p_15m_warning" {
  for_each = local.bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-eob-bulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response 99% 15 minute BULK latency per KB exceeded WARNING SLO ",
    "threshold of ${local.partner_timeouts_ms[each.key]} ms/KB for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.eob_latency_by_kb}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_nonbulk_latency_per_kb_99p_15m_alert" {
  for_each = local.non_bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-eob-nonbulk-latency-per-kb-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "690"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response 99% 15 minute NON-BULK latency per KB exceeded ALERT ",
    "SLO threshold of 690 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.eob_latency_by_kb}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_eob_nonbulk_latency_per_kb_99p_15m_warning" {
  for_each = local.non_bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-eob-nonbulk-latency-per-kb-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "480"

  alarm_description = join("", [
    "/v*/fhir/ExplanationOfBenefit response 99% 15 minute NON-BULK latency per KB exceeded ",
    "WARNING SLO threshold of 480 ms for partner ${each.key} for ${local.app} in ${var.env} ",
    "environment"
  ])

  metric_name = "${local.metrics.eob_latency_by_kb}/${each.key}"
  namespace   = local.namespace

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

  metric_name = "${local.metrics.patient_no_contract_latency}/all"
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

  metric_name = "${local.metrics.patient_no_contract_latency}/all"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_alert" {
  for_each = local.bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-bulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "${local.partner_timeouts_ms[each.key]}"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded ALERT SLO ",
    "threshold of ${local.partner_timeouts_ms[each.key]} ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.patient_no_contract_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_bulk_latency_99p_15m_warning" {
  for_each = local.bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-bulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "110"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute BULK latency exceeded WARNING SLO ",
    "threshold of 110 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.patient_no_contract_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_alert" {
  for_each = local.non_bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-nonbulk-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "160"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded ALERT ",
    "SLO threshold of 160 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.patient_no_contract_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_no_contract_nonbulk_latency_99p_15m_warning" {
  for_each = local.non_bulk_partners

  alarm_name          = "${local.app}-${var.env}-slo-patient-no-contract-nonbulk-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "110"

  alarm_description = join("", [
    "/v*/fhir/Patient (not by contract) response 99% 15 minute NON-BULK latency exceeded WARNING ",
    "SLO threshold of 110 ms for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.patient_no_contract_latency}/${each.key}"
  namespace   = local.namespace

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

  metric_name = "${local.metrics.patient_contract_count_4000_latency}/all"
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

  metric_name = "${local.metrics.patient_contract_count_4000_latency}/all"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_alert" {
  for_each = concat(local.bulk_partners, local.non_bulk_partners)

  alarm_name          = "${local.app}-${var.env}-slo-patient-by-contract-count-4000-latency-99p-15m-alert-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "${local.partner_timeouts_ms[each.key]}"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded ALERT ",
    "SLO threshold of ${local.partner_timeouts_ms[each.key]} ms for partner ${each.key} for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.patient_contract_count_4000_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_patient_by_contract_count_4000_latency_99p_15m_warning" {
  for_each = concat(local.bulk_partners, local.non_bulk_partners)

  alarm_name          = "${local.app}-${var.env}-slo-patient-by-contract-count-4000-latency-99p-15m-warning-${each.key}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "900"
  extended_statistic  = local.ext_stat_99p
  threshold           = "44000"

  alarm_description = join("", [
    "/v*/fhir/Patient (by contract, count 4000) response 99% 15 minute latency exceeded WARNING ",
    "SLO threshold of 44 seconds for partner ${each.key} for ${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.patient_contract_count_4000_latency}/${each.key}"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_http500_count_mean_1hr_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-http500-count-mean-1hr-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "3600"
  statistic           = "Average"
  threshold           = "0.0001"

  alarm_description = join("", [
    "Percent HTTP 500 (error) responses over 1 hour exceeded ALERT SLO threshold of 0.01% for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.all_error_rate}/all"
  namespace   = local.namespace

  alarm_actions = local.alert_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

resource "aws_cloudwatch_metric_alarm" "slo_http500_count_mean_1hr_alert" {
  alarm_name          = "${local.app}-${var.env}-slo-http500-count-mean-1hr-alert"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  period              = "3600"
  statistic           = "Average"
  threshold           = "0.00001"

  alarm_description = join("", [
    "Percent HTTP 500 (error) responses over 1 hour exceeded WARNING SLO threshold of 0.001% for ",
    "${local.app} in ${var.env} environment"
  ])

  metric_name = "${local.metrics.all_error_rate}/all"
  namespace   = local.namespace

  alarm_actions = local.warning_arn
  ok_actions    = local.ok_arn

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}