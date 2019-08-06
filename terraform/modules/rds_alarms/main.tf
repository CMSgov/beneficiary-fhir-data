##
#
# NOTE: This module is for defining RDS CloudWatch alarms
#
##

resource "aws_cloudwatch_metric_alarm" "rds_high_cpu" {
  count               = "${var.alarm_rds_high_cpu_enable}"
  alarm_name          = "${var.rds_name}-rds-high-cpu"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_rds_high_cpu_eval_periods}"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_high_cpu_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_rds_high_cpu_threshold}"

  alarm_description = "RDS - CPU Utilization is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "rds_free_storage" {
  count               = "${var.alarm_rds_free_storage_enable}"
  alarm_name          = "${var.rds_name}-rds-free-storage"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_rds_free_storage_eval_periods}"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_free_storage_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_rds_free_storage_threshold}"

  alarm_description = "RDS - Free storage space is low for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "rds_write_latency" {
  count               = "${var.alarm_rds_write_latency_enable}"
  alarm_name          = "${var.rds_name}-rds-write-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_rds_write_latency_eval_periods}"
  metric_name         = "WriteLatency"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_write_latency_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_rds_write_latency_threshold}"

  alarm_description = "RDS - Write latency is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "rds_read_latency" {
  count               = "${var.alarm_rds_read_latency_enable}"
  alarm_name          = "${var.rds_name}-rds-read-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_rds_read_latency_eval_periods}"
  metric_name         = "ReadLatency"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_read_latency_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_rds_read_latency_threshold}"

  alarm_description = "RDS - Read latency is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "rds_swap_usage" {
  count               = "${var.alarm_rds_swap_usage_enable}"
  alarm_name          = "${var.rds_name}-rds-swap-usage"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "${var.alarm_rds_swap_usage_eval_periods}"
  metric_name         = "SwapUsage"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_swap_usage_period}"
  statistic           = "Sum"
  threshold           = "${var.alarm_rds_swap_usage_threshold}"

  alarm_description = "RDS - Swap Usage is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "rds_disk_queue_depth" {
  count               = "${var.alarm_rds_disk_queue_depth_enable}"
  alarm_name          = "${var.rds_name}-rds-disk-queue-depth"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_rds_disk_queue_depth_eval_periods}"
  metric_name         = "DiskQueueDepth"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_disk_queue_depth_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_rds_disk_queue_depth_threshold}"

  alarm_description = "RDS - Disk queue depth is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "rds_free_memory" {
  count               = "${var.alarm_rds_free_memory_enable}"
  alarm_name          = "${var.rds_name}-rds-free-memory"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_rds_free_memory_eval_periods}"
  metric_name         = "FreeableMemory"
  namespace           = "AWS/RDS"
  period              = "${var.alarm_rds_free_memory_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_rds_free_memory_threshold}"

  alarm_description = "RDS - Free memory is low for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}
