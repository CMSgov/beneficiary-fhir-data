locals {
  alarm_actions = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_actions    = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]

  http_500 = {
    period       = "300"
    eval_periods = "1"
    threshold    = "0.0"
  }

  http_latency_4s = {
    period       = "900"
    eval_periods = "1"
    threshold    = "4000.0"
    ext_stat     = "p90"
  }

  http_latency_6s = {
    period       = "3600"
    eval_periods = "1"
    threshold    = "6000.0"
    ext_stat     = "p99"
  }

  mct_query_time = {
    period       = "900"
    eval_periods = "1"
    threshold    = "6000.0"
    ext_stat     = "p99"
  }
}

# HTTP 500 Errors
resource "aws_cloudwatch_metric_alarm" "http-500" {
  alarm_name          = "${var.app}-${var.env}-http-500"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.http_500.eval_periods
  period              = local.http_500.period
  statistic           = "Maximum"
  threshold           = local.http_500.threshold
  alarm_description   = "HTTP 500 Errors Detected within ${local.http_500.threshold} seconds in APP-ENV: ${var.app}-${var.env} over ${local.http_500.period} seconds"


  metric_name = "http-requests/count/http-500"
  namespace   = "bfd-${var.env}/bfd-server"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

# EOB Slow Response > 4s p90
resource "aws_cloudwatch_metric_alarm" "http-requests-latency-fhir-eob-4s" {
  alarm_name          = "${var.app}-${var.env}-http-requests-latency-fhir-eob-4s"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.http_latency_4s.eval_periods
  period              = local.http_latency_4s.period
  extended_statistic  = local.http_latency_4s.ext_stat
  threshold           = local.http_latency_4s.threshold
  alarm_description   = "HTTP EOB Request Latency ${local.http_latency_4s.ext_stat} exceeds ${local.http_latency_4s.threshold} seconds in APP-ENV: ${var.app}-${var.env} over ${local.http_latency_4s.period} seconds"

  metric_name = "http-requests/latency/fhir/eob"
  namespace   = "bfd-${var.env}/bfd-server"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

# EOB Slow Response > 6s p99 over 1h
resource "aws_cloudwatch_metric_alarm" "http-requests-latency-fhir-eob-6s" {
  alarm_name          = "${var.app}-${var.env}-http-requests-latency-fhir-eob-6s"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.http_latency_6s.eval_periods
  period              = local.http_latency_6s.period
  extended_statistic  = local.http_latency_6s.ext_stat
  threshold           = local.http_latency_6s.threshold
  alarm_description   = "HTTP EOB Request Latency ${local.http_latency_6s.ext_stat} exceeds ${local.http_latency_6s.threshold} seconds in APP-ENV: ${var.app}-${var.env} over ${local.http_latency_6s.period} seconds"

  metric_name = "http-requests/latency/fhir/eob"
  namespace   = "bfd-${var.env}/bfd-server"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}

# MCT Specific - EOB Slow Response > 6s p99 over 15m
resource "aws_cloudwatch_metric_alarm" "mct-query-duration" {
  alarm_name          = "${var.app}-${var.env}-mct-query-duration"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.mct_query_time.eval_periods
  period              = local.mct_query_time.period
  extended_statistic  = local.mct_query_time.ext_stat
  threshold           = local.mct_query_time.threshold
  alarm_description   = "HTTP EOB Request Latency ${local.mct_query_time.ext_stat} exceeds ${local.mct_query_time.threshold} seconds for MCT in APP-ENV: ${var.app}-${var.env} over ${local.mct_query_time.period} seconds"

  metric_name = "http-requests/latency/mct"
  namespace   = "bfd-${var.env}/bfd-server"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions

  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"
}
