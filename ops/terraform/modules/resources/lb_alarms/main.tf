## ELB v1 and v2 CloudWatch alarms
#

locals {
  alarm_actions = var.alarm_notification_arn == null ? [] : [var.alarm_notification_arn]
  ok_actions    = var.ok_notification_arn == null ? [] : [var.ok_notification_arn]
}

resource "aws_cloudwatch_metric_alarm" "healthy_hosts" {
  count               = var.healthy_hosts == null ? 0 : 1
  alarm_name          = "${var.load_balancer_name}-healthy-hosts"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = var.healthy_hosts.eval_periods
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ELB"
  period              = var.healthy_hosts.period
  statistic           = "Average"
  threshold           = var.healthy_hosts.threshold

  alarm_description = "No healthy hosts available for ${var.load_balancer_name} in APP-ENV: ${var.app}-${var.env}"

  dimensions = {
    LoadBalancerName = var.load_balancer_name
  }

  # we should always have a measure of the number of healthy hosts - alert if not
  treat_missing_data = "breaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}


# Classic ELB metrics
#

resource "aws_cloudwatch_metric_alarm" "clb_spillover_count" {
  count               = var.clb_spillover_count == null ? 0 : 1
  alarm_name          = "${var.load_balancer_name}-spillover-count"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.clb_spillover_count.eval_periods
  metric_name         = "SpilloverCount"
  namespace           = "AWS/ELB"
  period              = var.clb_spillover_count.period
  statistic           = "Maximum"

  dimensions = {
    LoadBalancerName = var.load_balancer_name
  }

  threshold         = var.clb_spillover_count.threshold
  unit              = "Count"
  alarm_description = "Spillover alarm for ELB ${var.load_balancer_name} in APP-ENV: ${var.app}-${var.env}"

  # a missing spillover count means that we haven't spillover - that's good! don't alert.
  treat_missing_data = "notBreaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "clb_clb_surge_queue_length" {
  count               = var.clb_surge_queue_length == null ? 0 : 1
  alarm_name          = "${var.load_balancer_name}-surge-queue-length"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.clb_surge_queue_length.eval_periods
  metric_name         = "SurgeQueueLength"
  namespace           = "AWS/ELB"
  period              = var.clb_surge_queue_length.period
  statistic           = "Maximum"

  dimensions = {
    LoadBalancerName = var.load_balancer_name
  }

  threshold         = var.clb_surge_queue_length.threshold
  unit              = "Count"
  alarm_description = "Surge queue exceeded for ELB ${var.load_balancer_name} in APP-ENV: ${var.app}-${var.env}"

  # an undefined surge queue length is good - we haven't had to queue any requests recently, so don't alert
  treat_missing_data = "notBreaching"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions
}


## ALB metrics
#

resource "aws_cloudwatch_metric_alarm" "alb_alb_high_latency" {
  count               = var.alb_high_latency == null ? 0 : 1
  alarm_name          = "${var.load_balancer_name}-high-latency"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.alb_high_latency.eval_periods
  metric_name         = "Latency"
  namespace           = "AWS/ELB"
  period              = var.alb_high_latency.period
  statistic           = "Average"

  dimensions = {
    LoadBalancerName = var.load_balancer_name
  }

  threshold         = var.alb_high_latency.threshold
  unit              = "Seconds"
  alarm_description = "High latency for ALB ${var.load_balancer_name} in APP-ENV: ${var.app}-${var.env}"

  # "Missing data" means that we haven't had any measure of latency - alert if we don't
  treat_missing_data = "breaching"
  alarm_actions      = local.alarm_actions
  ok_actions         = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "alb_status_4xx" {
  count               = var.alb_status_4xx == null ? 0 : 1
  alarm_name          = "${var.load_balancer_name}-status-4xx"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.alb_status_4xx.eval_periods
  metric_name         = "HTTPCode_Target_4XX_Count"
  namespace           = "AWS/ELB"
  period              = var.alb_status_4xx.period
  statistic           = "Sum"

  dimensions = {
    LoadBalancerName = var.load_balancer_name
  }

  threshold         = var.alb_status_4xx.threshold
  unit              = "Count"
  alarm_description = "HTTP Backend 4xx response codes exceeded for ALB ${var.load_balancer_name} in APP-ENV: ${var.app}-${var.env}"

  treat_missing_data = "notBreaching"

  alarm_actions = local.alarm_actions
  ok_actions    = local.ok_actions
}

resource "aws_cloudwatch_metric_alarm" "alb_rate_of_5xx" {
  count               = var.alb_rate_of_5xx == null ? 0 : 1
  alarm_name          = "${var.load_balancer_name}-rate-of-5xx"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = var.alb_rate_of_5xx.eval_periods
  threshold           = var.alb_rate_of_5xx.threshold
  alarm_description   = "HTTP 5xx response codes rate exceeded for ALB ${var.load_balancer_name} in APP-ENV: ${var.app}-${var.env}"
  treat_missing_data  = "notBreaching"
  alarm_actions       = local.alarm_actions
  ok_actions          = local.ok_actions

  metric_query {
    id          = "e1"
    expression  = "error_sum/request_sum*100"
    label       = "Error Rate"
    return_data = "true"
  }

  metric_query {
    id = "request_sum"

    metric {
      metric_name = "RequestCount"
      namespace   = "AWS/ApplicationELB"
      period      = var.alb_rate_of_5xx.period
      stat        = "Sum"
      unit        = "Count"

      dimensions = {
        LoadBalancer = var.load_balancer_name
      }
    }
  }

  metric_query {
    id = "error_sum"

    metric {
      metric_name = "HTTPCode_ELB_5XX_Count"
      namespace   = "AWS/ApplicationELB"
      period      = var.alb_rate_of_5xx.period
      stat        = "Sum"
      unit        = "Count"

      dimensions = {
        LoadBalancer = var.load_balancer_name
      }
    }
  }
}
