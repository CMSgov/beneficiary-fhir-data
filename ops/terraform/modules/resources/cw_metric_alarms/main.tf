locals {
  alarm_actions       = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_actions          = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]
}

# HTTP 500 Errors
resource "aws_cloudwatch_metric_alarm" "http-500" {
  count = var.create_cw_alarms ? 1 : 0

  alarm_name                = "${var.app}-${var.env}-http-500"
  comparison_operator       = "GreaterThanOrEqualToThreshold"
  evaluation_periods        = "1"
  period                    = "300"
  statistic                 = "Maximum"
  threshold                 = "0.0"
  alarm_description         = "HTTP 500 Errors detected in APP-ENV: ${var.app}-${var.env} over a 5m period."

  metric_name               = "http-requests/count/http-500"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}

# EOB Slow Response > 4s p90
resource "aws_cloudwatch_metric_alarm" "http-requests-latency-fhir-eob-4s" {
  count = var.create_cw_alarms ? 1 : 0

  alarm_name                = "${var.app}-${var.env}-http-requests-latency-fhir-eob"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = "1"
  period                    = "900"
  extended_statistic        = "p90"
  threshold                 = "4000.0"
  alarm_description         = "HTTP EOB Request Latency exceeds 4s (p90) in APP-ENV: ${var.app}-${var.env} over a 15m period."

  metric_name               = "http-requests/latency/fhir/eob"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}

# EOB Slow Response > 6s p99 over 1h
resource "aws_cloudwatch_metric_alarm" "http-requests-latency-fhir-eob-6s" {
  count = var.create_cw_alarms ? 1 : 0

  alarm_name                = "${var.app}-${var.env}-http-requests-latency-fhir-eob"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = "1"
  period                    = "3600"
  extended_statistic        = "p99"
  threshold                 = "6000.0"
  alarm_description         = "HTTP EOB Request Latency exceeds 6s (p99) in APP-ENV: ${var.app}-${var.env} over a 1h period."

  metric_name               = "http-requests/latency/fhir/eob"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}

# MCT Specific - EOB Slow Response > 6s p99 over 15m
resource "aws_cloudwatch_metric_alarm" "mct-query-duration" {
  count = var.create_cw_alarms ? 1 : 0

  alarm_name                = "${var.app}-${var.env}-mct-query-duration"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = "1"
  period                    = "3600"
  extended_statistic        = "p99"
  threshold                 = "6000.0"
  alarm_description         = "HTTP EOB Request Latency exceeds 6s (p99) in APP-ENV: ${var.app}-${var.env} over a 1h period."

  metric_name               = "mct-query-duration"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}