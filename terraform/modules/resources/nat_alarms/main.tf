##
#
# NOTE: This module is for defining CloudWatch alarms
#
##

resource "aws_cloudwatch_metric_alarm" "nat_error_port_alloc" {
  count               = "${var.alarm_nat_error_port_alloc_enable}"
  alarm_name          = "${var.nat_gw_name}-nat-error-port-alloc"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "${var.alarm_nat_error_port_alloc_eval_periods}"
  metric_name         = "ErrorPortAllocation"
  namespace           = "AWS/NATGateway"
  period              = "${var.alarm_nat_error_port_alloc_period}"
  statistic           = "Sum"
  threshold           = "${var.alarm_nat_error_port_alloc_threshold}"

  alarm_description = "NAT GATEWAY - Port allocation error count is high for ${var.nat_gw_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.nat_gw_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "nat_packets_drop_count" {
  count               = "${var.alarm_nat_packets_drop_count_enable}"
  alarm_name          = "${var.nat_gw_name}-nat-packets-drop-count"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "${var.alarm_nat_packets_drop_count_eval_periods}"
  metric_name         = "PacketsDropCount"
  namespace           = "AWS/NATGateway"
  period              = "${var.alarm_nat_packets_drop_count_period}"
  statistic           = "Sum"
  threshold           = "${var.alarm_nat_packets_drop_count_threshold}"

  alarm_description = "NAT GATEWAY - Packets drop count is high for ${var.nat_gw_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.nat_gw_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}
