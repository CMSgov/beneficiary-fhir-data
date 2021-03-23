##
#
# NOTE: This module is for defining EFS CloudWatch alarms
#
##

resource "aws_cloudwatch_metric_alarm" "burst_credit_balance_too_low" {
  alarm_name          = "${var.app}-${var.env}-efs_check_burst_credit_balance_too_low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "BurstCreditBalance"
  namespace           = "AWS/EFS"
  period              = "600"
  statistic           = "Average"

  dimensions = {
    FileSystemId = var.filesystem_id
  }

  alarm_description = "Average burst credit balance over last 10 minutes too low, expect a significant performance drop soon in: ${var.app}-${var.env}"

  threshold          = var.burst_credit_balance_threshold
  unit               = "Count"
  treat_missing_data = "notBreaching"

  alarm_actions = [var.cloudwatch_notification_arn]
  ok_actions    = [var.cloudwatch_notification_arn]
}

resource "aws_cloudwatch_metric_alarm" "percent_io_limit_too_high" {
  alarm_name          = "${var.app}-${var.env}-efs-io_limit_too_high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "3"
  metric_name         = "PercentIOLimit"
  namespace           = "AWS/EFS"
  period              = "600"
  statistic           = "Maximum"

  dimensions = {
    FileSystemId = var.filesystem_id
  }

  alarm_description = "I/O limit has been reached, consider using Max I/O performance mode in: ${var.app}-${var.env}"

  threshold          = var.percent_io_limit_threshold
  unit               = "Count"
  treat_missing_data = "notBreaching"

  alarm_actions = [var.cloudwatch_notification_arn]
  ok_actions    = [var.cloudwatch_notification_arn]
}

resource "aws_cloudwatch_metric_alarm" "client_connections" {
  alarm_name          = "${var.app}-${var.env}-efs_client_connections_too_high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "3"
  metric_name         = "ClientConnections"
  namespace           = "AWS/EFS"
  period              = "600"
  statistic           = "Sum"

  dimensions = {
    FileSystemId = var.filesystem_id
  }

  alarm_description = "Number of Client Connections has surpassed the threshold in: ${var.app}-${var.env}"

  threshold          = var.client_connections
  unit               = "Count"
  treat_missing_data = "notBreaching"

  alarm_actions = [var.cloudwatch_notification_arn]
  ok_actions    = [var.cloudwatch_notification_arn]
}

