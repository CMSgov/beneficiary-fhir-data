# Flexible BFD server alarm to handle different configurations for different partners
#

resource "aws_cloudwatch_metric_alarm" "bfd-server-alarm" {
  alarm_name        = "bfd-${var.env}-server-${var.alarm_config.alarm_name}"
  alarm_description = ""

  metric_name = "${var.alarm_config.metric_prefix}/${var.alarm_config.partner_name}"
  namespace   = "bfd-${var.env}/bfd-server"

  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.alarm_config.eval_periods
  period              = var.alarm_config.period
  statistic           = var.alarm_config.statistic
  extended_statistic  = var.alarm_config.ext_statistic
  threshold           = var.alarm_config.threshold
  datapoints_to_alarm = var.alarm_config.datapoints
  treat_missing_data  = "notBreaching"

  alarm_actions = var.alarm_config.alarm_notify_arn == null ? [] : [var.alarm_config.alarm_notify_arn]
  ok_actions    = var.alarm_config.ok_notify_arn == null ? [] : [var.alarm_config.ok_notify_arn]
}
