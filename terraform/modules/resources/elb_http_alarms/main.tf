##
#
# NOTE: This module is for defining ELB - HTTP CloudWatch alarms
#
##

resource "aws_cloudwatch_metric_alarm" "healthy_hosts" {
  count               = "${var.alarm_elb_no_backend_enable}"
  alarm_name          = "${var.load_balancer_name}-elb-no-backend"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "${var.alarm_elb_no_backend_eval_periods}"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_elb_no_backend_period}"
  statistic           = "Average"
  threshold           = "${var.alarm_elb_no_backend_threshold}"

  alarm_description = "No backends available for ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  # We should always have a measure of the number of healthy hosts - alert if not
  treat_missing_data = "breaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "high_latency" {
  count               = "${var.alarm_elb_high_latency_enable}"
  alarm_name          = "elb-${var.load_balancer_name}-high-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_elb_high_latency_eval_periods}"
  metric_name         = "Latency"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_elb_high_latency_period}"
  statistic           = "Average"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  threshold         = "${var.alarm_elb_high_latency_threshold}"
  unit              = "Seconds"
  alarm_description = "High latency for ELB ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  # "Missing data" means that we haven't had any measure of latency - alert if we don't
  treat_missing_data = "breaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "spillover_count" {
  count               = "${var.alarm_elb_spillover_count_enable}"
  alarm_name          = "elb-${var.load_balancer_name}-spillover-count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_elb_spillover_count_eval_periods}"
  metric_name         = "SpilloverCount"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_elb_spillover_count_period}"
  statistic           = "Maximum"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  threshold         = "${var.alarm_elb_spillover_count_threshold}"
  unit              = "Count"
  alarm_description = "Spillover alarm for ELB ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  # A missing spillover count means that we haven't spillover - that's good! Don't alert.
  treat_missing_data = "notBreaching"
  alarm_actions      = ["${var.cloudwatch_notification_arn}"]
  ok_actions         = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "surge_queue_exceeded" {
  count               = "${var.alarm_elb_surge_queue_length_enable}"
  alarm_name          = "elb-${var.load_balancer_name}-surge-queue-length"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_elb_surge_queue_length_eval_periods}"
  metric_name         = "SurgeQueueLength"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_elb_surge_queue_length_period}"
  statistic           = "Maximum"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  threshold         = "${var.alarm_elb_surge_queue_length_threshold}"
  unit              = "Count"
  alarm_description = "Surge queue exceeded for ELB ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  # An undefined surge queue length is good - we haven't had to queue any requests recently, so
  # don't alert
  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "httpcode_backend_4xx" {
  count               = "${var.alarm_backend_4xx_enable}"
  alarm_name          = "elb-${var.load_balancer_name}-httpcode-backend-4xx"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_backend_4xx_eval_periods}"
  metric_name         = "HTTPCode_Backend_4XX"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_backend_4xx_period}"
  statistic           = "Sum"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  threshold         = "${var.alarm_backend_4xx_threshold}"
  unit              = "Count"
  alarm_description = "HTTP Backend 4xx response codes exceeded for ELB ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "httpcode_backend_5xx" {
  count               = "${var.alarm_backend_5xx_enable}"
  alarm_name          = "elb-${var.load_balancer_name}-httpcode-backend-5xx"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_backend_5xx_eval_periods}"
  metric_name         = "HTTPCode_Backend_5XX"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_backend_5xx_period}"
  statistic           = "Sum"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  threshold         = "${var.alarm_backend_5xx_threshold}"
  unit              = "Count"
  alarm_description = "HTTP Backend 5xx response codes exceeded for ELB ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}

resource "aws_cloudwatch_metric_alarm" "httpcode_elb_5xx" {
  count               = "${var.alarm_elb_5xx_enable}"
  alarm_name          = "elb-${var.load_balancer_name}-httpcode-elb-5xx"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "${var.alarm_elb_5xx_eval_periods}"
  metric_name         = "HTTPCode_ELB_5XX"
  namespace           = "AWS/ELB"
  period              = "${var.alarm_elb_5xx_period}"
  statistic           = "Sum"

  dimensions {
    LoadBalancerName = "${var.load_balancer_name}"
  }

  threshold         = "${var.alarm_elb_5xx_threshold}"
  unit              = "Count"
  alarm_description = "HTTP ELB 5xx response codes exceeded for ELB ${var.load_balancer_name} in ${var.vpc_name} in APP-ENV: ${var.app}-${var.env}"

  treat_missing_data = "notBreaching"

  alarm_actions = ["${var.cloudwatch_notification_arn}"]
  ok_actions    = ["${var.cloudwatch_notification_arn}"]
}
