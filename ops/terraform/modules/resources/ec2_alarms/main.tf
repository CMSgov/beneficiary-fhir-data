##
#
# NOTE: This module is for defining EC2 CloudWatch alarms
#
##

resource "aws_cloudwatch_metric_alarm" "status_check_failed" {
  count               = "${var.alarm_status_check_failed_enable}"
  alarm_name          = "${var.app}-${var.env}-status_check_failed"
  comparison_operator = "GreaterThanOrEqualToThreshold"

  evaluation_periods = "${var.alarm_status_check_failed_eval_periods}"
  metric_name        = "StatusCheckFailed"
  namespace          = "AWS/EC2"
  period             = "${var.alarm_status_check_failed_period}"
  statistic          = "Sum"

  dimensions {
    AutoScalingGroupName = "${var.asg_name}"
  }

  alarm_description = "Both instance and system status checks have FAILED for ${var.asg_name} ASG in APP-ENV: ${var.app}-${var.env}"

  threshold          = "${var.alarm_status_check_failed_threshold}"
  unit               = "Count"
  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "status_check_failed_instance" {
  count               = "${var.alarm_status_check_failed_instance_enable}"
  alarm_name          = "${var.app}-${var.env}-status_check_instance"
  comparison_operator = "GreaterThanOrEqualToThreshold"

  evaluation_periods = "${var.alarm_status_check_failed_instance_eval_periods}"
  metric_name        = "StatusCheckFailed_Instance"
  namespace          = "AWS/EC2"
  period             = "${var.alarm_status_check_failed_instance_period}"
  statistic          = "Sum"

  dimensions {
    AutoScalingGroupName = "${var.asg_name}"
  }

  alarm_description = "Instance status check has FAILED for ${var.asg_name} ASG in APP-ENV: ${var.app}-${var.env}"

  threshold          = "${var.alarm_status_check_failed_instance_threshold}"
  unit               = "Count"
  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "status_check_failed_system" {
  count               = "${var.alarm_status_check_failed_system_enable}"
  alarm_name          = "${var.app}-${var.env}-status_check_system"
  comparison_operator = "GreaterThanOrEqualToThreshold"

  evaluation_periods = "${var.alarm_status_check_failed_system_eval_periods}"
  metric_name        = "StatusCheckFailed_System"
  namespace          = "AWS/EC2"
  period             = "${var.alarm_status_check_failed_system_period}"
  statistic          = "Sum"

  dimensions {
    AutoScalingGroupName = "${var.asg_name}"
  }

  alarm_description = "System status check has FAILED for ${var.asg_name} ASG in APP-ENV: ${var.app}-${var.env}"

  threshold          = "${var.alarm_status_check_failed_system_threshold}"
  unit               = "Count"
  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}
