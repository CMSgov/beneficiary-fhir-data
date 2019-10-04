locals {
  alarm_actions       = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_actions          = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]
}

# HTTP 500 Errors
resource "aws_cloudwatch_metric_alarm" "http-500" {
  count                     = var.http_500 == null ? 0 : 1

  alarm_name                = "${var.app}-${var.env}-http-500"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = var.http_500.eval_periods
  period                    = var.http_500.period
  statistic                 = "Maximum"
  threshold                 = var.http_500.threshold
  alarm_description         = "HTTP 500 Errors Detected within ${var.http_500.threshold} seconds in APP-ENV: ${var.app}-${var.env} over ${var.http_500.period} seconds"


  metric_name               = "http-requests/count/http-500"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}

# EOB Slow Response > 4s p90
resource "aws_cloudwatch_metric_alarm" "http-requests-latency-fhir-eob-4s" {
  count                     = var.http_latency_4s == null ? 0 : 1

  alarm_name                = "${var.app}-${var.env}-http-requests-latency-fhir-eob"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = var.http_latency_4s.eval_periods
  period                    = var.http_latency_4s.period
  extended_statistic        = var.http_latency_4s.ext_stat
  threshold                 = var.http_latency_4s.threshold
  alarm_description         = "HTTP EOB Request Latency ${var.http_latency_4s.ext_stat} exceeds ${var.http_latency_4s.threshold} seconds in APP-ENV: ${var.app}-${var.env} over ${var.http_latency_4s.period} seconds"

  metric_name               = "http-requests/latency/fhir/eob"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}

# EOB Slow Response > 6s p99 over 1h
resource "aws_cloudwatch_metric_alarm" "http-requests-latency-fhir-eob-6s" {
  count                     = var.http_latency_6s == null ? 0 : 1

  alarm_name                = "${var.app}-${var.env}-http-requests-latency-fhir-eob"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = var.http_latency_6s.eval_periods
  period                    = var.http_latency_6s.period
  extended_statistic        = var.http_latency_6s.ext_stat
  threshold                 = var.http_latency_6s.threshold
  alarm_description         = "HTTP EOB Request Latency ${var.http_latency_6s.ext_stat} exceeds ${var.http_latency_6s.threshold} seconds in APP-ENV: ${var.app}-${var.env} over ${var.http_latency_6s.period} seconds"

  metric_name               = "http-requests/latency/fhir/eob"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}

# MCT Specific - EOB Slow Response > 6s p99 over 15m
resource "aws_cloudwatch_metric_alarm" "mct-query-duration" {
  count                     = var.mct_query_time == null ? 0 : 1

  alarm_name                = "${var.app}-${var.env}-mct-query-duration"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = var.mct_query_time.eval_periods
  period                    = var.mct_query_time.period
  extended_statistic        = var.mct_query_time.ext_stat
  threshold                 = var.mct_query_time.threshold
  alarm_description         = "HTTP EOB Request Latency ${var.mct_query_time.ext_stat} exceeds ${var.mct_query_time.threshold} seconds for MCT in APP-ENV: ${var.app}-${var.env} over ${var.mct_query_time.period} seconds"

  metric_name               = "http-requests/latency/mct"
  namespace                 = "bfd-${var.env}/bfd-server"

  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions

  datapoints_to_alarm                   = "1"
  treat_missing_data                    = "notBreaching"
}
