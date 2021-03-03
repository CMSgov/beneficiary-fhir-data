##
#
# NOTE: This module is for defining RDS CloudWatch alarms
#
##

locals {
  alarm_actions = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_actions    = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_high_cpu" {
  count               = var.high_cpu == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-high-cpu"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.high_cpu.eval_periods
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = var.high_cpu.period
  statistic           = "Average"
  threshold           = var.high_cpu.threshold

  alarm_description = "RDS - CPU Utilization is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_free_storage" {
  count               = var.free_storage == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-free-storage"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = var.free_storage.eval_periods
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = var.free_storage.period
  statistic           = "Average"
  threshold           = var.free_storage.threshold

  alarm_description = "RDS - Free storage space is low for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_write_latency" {
  count               = var.write_latency == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-write-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.write_latency.eval_periods
  metric_name         = "WriteLatency"
  namespace           = "AWS/RDS"
  period              = var.write_latency.period
  statistic           = "Average"
  threshold           = var.write_latency.threshold

  alarm_description = "RDS - Write latency is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_read_latency" {
  count               = var.read_latency == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-read-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.read_latency.eval_periods
  metric_name         = "ReadLatency"
  namespace           = "AWS/RDS"
  period              = var.read_latency.period
  statistic           = "Average"
  threshold           = var.read_latency.threshold

  alarm_description = "RDS - Read latency is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_swap_usage" {
  count               = var.swap_usage == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-swap-usage"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = var.swap_usage.eval_periods
  metric_name         = "SwapUsage"
  namespace           = "AWS/RDS"
  period              = var.swap_usage.period
  statistic           = "Sum"
  threshold           = var.swap_usage.threshold

  alarm_description = "RDS - Swap Usage is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = "${var.rds_name}"
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_disk_queue_depth" {
  count               = var.disk_queue_depth == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-disk-queue-depth"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.disk_queue_depth.eval_periods
  metric_name         = "DiskQueueDepth"
  namespace           = "AWS/RDS"
  period              = var.disk_queue_depth.period
  statistic           = "Average"
  threshold           = var.disk_queue_depth.threshold

  alarm_description = "RDS - Disk queue depth is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_free_memory" {
  count               = var.free_memory == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-free-memory"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = var.free_memory.eval_periods
  metric_name         = "FreeableMemory"
  namespace           = "AWS/RDS"
  period              = var.free_memory.period
  statistic           = "Average"
  threshold           = var.free_memory.threshold

  alarm_description = "RDS - Free memory is low for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "rds_replica_lag" {
  count               = var.replica_lag == null ? 0 : 1
  alarm_name          = "${var.rds_name}-rds-replica-lag"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.replica_lag.eval_periods
  metric_name         = "ReplicaLag"
  namespace           = "AWS/RDS"
  period              = var.replica_lag.period
  statistic           = "Average"
  threshold           = var.replica_lag.threshold

  alarm_description = "RDS - Replica Lag is high for ${var.rds_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    DBInstanceIdentifier = var.rds_name
  }

  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}
