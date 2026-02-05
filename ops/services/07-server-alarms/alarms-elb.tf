locals {
  # Ensures that prod and prod-sbx/sandbox always have a valid alarm alert destination, as the
  # application of this Terraservice will fail-fast otherwise. In the event this module is being
  # applied in a non-critical environment (i.e. an ephemeral environment/test) the lookup will
  # ensure that an empty configuration will be returned instead of an error if no configuration is
  # available.
  elb_high_alert_path  = "/bfd/${local.service}/sns_topics/elb/high_alert"
  elb_high_alert_topic = contains(["prod", "prod-sbx", "sandbox"], local.env) ? local.ssm_config[local.elb_high_alert_path] : lookup(local.ssm_config, local.elb_high_alert_path, null)
  elb_high_alert_arn   = data.aws_sns_topic.elb_high_alert_sns[*].arn
}

data "aws_sns_topic" "elb_high_alert_sns" {
  count = local.elb_high_alert_topic != null ? 1 : 0
  name  = local.elb_high_alert_topic
}

data "aws_lb" "main" {
  name = "bfd-${local.env}-${local.target_service}-nlb"
}

data "aws_lb_target_group" "tg_0" {
  name = "bfd-${local.env}-${local.target_service}-tg-0"
}

data "aws_lb_target_group" "tg_1" {
  name = "bfd-${local.env}-${local.target_service}-tg-1"
}

resource "aws_cloudwatch_metric_alarm" "elb_healthy_hosts" {
  alarm_name          = "${local.alarm_name_prefix}-elb-healthy-hosts"
  comparison_operator = "LessThanThreshold"
  datapoints_to_alarm = 1
  evaluation_periods  = 1
  threshold           = 1
  treat_missing_data  = "breaching"

  alarm_description = join("", [
    "No healthy hosts were reported in the past minute in any Target Groups within the",
    " ${data.aws_lb.main.name} NLB. This indicates that the ${local.target_service} is not able to",
    " serve traffic in the ${local.env} environment.",
    "\n\n${local.default_dashboard_message_fragment}"
  ])

  metric_query {
    id          = "m1"
    label       = "tg-0 host count"
    return_data = false

    metric {
      dimensions = {
        "LoadBalancer" = "${data.aws_lb.main.arn_suffix}"
        "TargetGroup"  = "${data.aws_lb_target_group.tg_0.arn_suffix}"
      }
      metric_name = "HealthyHostCount"
      namespace   = "AWS/NetworkELB"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    id          = "m2"
    label       = "tg-1 host count"
    return_data = false

    metric {
      dimensions = {
        "LoadBalancer" = "${data.aws_lb.main.arn_suffix}"
        "TargetGroup"  = "${data.aws_lb_target_group.tg_1.arn_suffix}"
      }
      metric_name = "HealthyHostCount"
      namespace   = "AWS/NetworkELB"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    expression  = "MAX([m1, m2])"
    id          = "e1"
    label       = "MAX(hosts in all tgs)"
    return_data = true
  }

  alarm_actions = local.elb_high_alert_arn
}
