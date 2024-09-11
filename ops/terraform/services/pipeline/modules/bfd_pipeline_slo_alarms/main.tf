locals {
  region = data.aws_region.current.name

  app = "bfd-pipeline"

  env = terraform.workspace

  metric_namespace = "bfd-${local.env}/${local.app}"

  victor_ops_sns         = "bfd-${local.env}-cloudwatch-alarms"
  bfd_test_slack_sns     = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-test"
  bfd_warnings_slack_sns = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-warnings"
  default_alert_ok_sns   = "bfd-${local.env}-cloudwatch-ok"
  # Each established environment has a different destination for which alarm notifications should
  # route to. The below map maps each particular SNS (destination) to a particular type of SLO
  # alarm.
  topic_names_by_env = {
    prod = {
      alert      = local.victor_ops_sns
      warning    = local.bfd_warnings_slack_sns
      alert_ok   = null
      warning_ok = null
    }
    prod-sbx = {
      alert      = local.bfd_test_slack_sns
      warning    = local.bfd_test_slack_sns
      alert_ok   = null
      warning_ok = null
    }
    test = {
      alert      = local.bfd_test_slack_sns
      warning    = local.bfd_test_slack_sns
      alert_ok   = null
      warning_ok = null
    }
  }
  # In the event this module is being applied in a non-established environment (i.e. an ephemeral
  # environment) this lookup will ensure that an empty configuration will be returned
  env_sns = lookup(local.topic_names_by_env, local.env, {
    alert      = null
    warning    = null
    alert_ok   = null
    warning_ok = null
  })
  # The following trys and coalesces ensure two things: the operator is able to override the
  # SNS topic/destination of each alarm type, and that if no destination is specified (either
  # explicitly such as with the OK SNS topics in prod-sbx/test or through the environment being
  # ephemeral) that Terraform does not raise an error and instead the SNS topic is empty
  alert_sns_name      = try(coalesce(var.alert_sns_override, local.env_sns.alert), null)
  warning_sns_name    = try(coalesce(var.warning_sns_override, local.env_sns.warning), null)
  alert_ok_sns_name   = try(coalesce(var.alert_ok_sns_override, local.env_sns.alert_ok), null)
  warning_ok_sns_name = try(coalesce(var.warning_ok_sns_override, local.env_sns.warning_ok), null)
  # Use Terraform's "splat" operator to automatically return either an empty list, if no
  # SNS topic was retrieved (data.aws_sns_topic.sns.length == 0) or a list with 1 element that
  # is the ARN of the SNS topic. Functionally equivalent to [for o in data.aws_sns_topic.sns : o.arn]
  alert_arn      = data.aws_sns_topic.alert_sns[*].arn
  warning_arn    = data.aws_sns_topic.warning_sns[*].arn
  alert_ok_arn   = data.aws_sns_topic.alert_ok_sns[*].arn
  warning_ok_arn = data.aws_sns_topic.warning_ok_sns[*].arn

  tz_to_utc_hour_offset = {
    "EDT" = 4
    "EST" = 5
  }

  current_eastern_tz         = data.external.edt_or_est.result.timezone
  eastern_seconds_utc_offset = local.tz_to_utc_hour_offset[local.current_eastern_tz] * 60 * 60

  dashboard_url              = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards:name=bfd-${local.env}-pipeline"
  dashboard_message_fragment = <<-EOF
View the relevant CloudWatch dashboard below for more information:

* <${local.dashboard_url}|bfd-${local.env}-pipeline>
    * This dashboard visualizes SLOs and other important Pipeline metrics
  EOF

  data_load_ingestion_time_slo_configs = {
    slo_ingestion_time_warning = {
      type      = "warning"
      period    = 60
      threshold = 24 * 60 * 60 # 24 hours
    }
    slo_ingestion_time_alert = {
      type      = "alert"
      period    = 60
      threshold = 36 * 60 * 60 # 36 hours
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "slo_load_exceeds_9am_est" {
  alarm_name          = "${local.app}-${local.env}-load-exceeds-9am-est"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  datapoints_to_alarm = 1
  threshold           = 1
  treat_missing_data  = "ignore"

  alarm_actions = local.alert_arn
  ok_actions    = local.alert_ok_arn

  alarm_description = join("", [
    "BFD Pipeline in ${local.env} environment failed to load data prior to this Monday 9 AM ",
    "EST/EDT",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_query {
    id          = "m1"
    return_data = false

    metric {
      metric_name = "time/data-fully-loaded-repeating"
      namespace   = local.metric_namespace
      period      = 60
      stat        = "Maximum"
    }
  }

  metric_query {
    id          = "m2"
    return_data = false

    metric {
      metric_name = "time/data-first-available-repeating"
      namespace   = local.metric_namespace
      period      = 60
      stat        = "Maximum"
    }
  }

  metric_query {
    expression  = "(e1 - (DAY(e1) + 5) * 86400 - (HOUR(e1) + 7) * 3600 - (${local.eastern_seconds_utc_offset} + (MINUTE(e1) * 60)) + 604800)"
    id          = "e4"
    label       = "Unix Timestamp of Week's Current Monday 9 AM ET"
    return_data = false
  }

  metric_query {
    expression  = "EPOCH(m1)"
    id          = "e1"
    label       = "Unix Time"
    return_data = false
  }

  metric_query {
    expression  = "FILL(m1, REPEAT)"
    id          = "e7"
    label       = "Filled Fully Loaded"
    return_data = false
  }

  metric_query {
    expression  = "FILL(m2, REPEAT)"
    id          = "e9"
    label       = "Filled Time Available"
    return_data = false
  }

  metric_query {
    # Breaking this down:
    # 1. e9 > e7 - If the Pipeline is currently loading something
    # 2. e4 > e9 - If the Pipeline was started prior to the upcoming Monday
    # 3. e1 > e4 - If the current time (when the Alarm evaluates) exceeds Monday at 9 AM; basically,
    #    is current time after current Monday 9 AM ET?
    # If all are true, this means that an ongoing load did not finish prior to the current Monday at
    # 9 AM ET, and so the SLO has been broken
    expression  = "IF(e9 > e7 && e4 > e9 && e1 > e4, 1, 0)"
    id          = "e3"
    label       = "Has ongoing load exceeded Monday 9 AM EST/EDT?"
    return_data = true
  }
}

resource "aws_cloudwatch_metric_alarm" "slo_data_load_ingestion_time" {
  for_each = local.data_load_ingestion_time_slo_configs

  alarm_name          = "${local.app}-${local.env}-${replace(each.key, "_", "-")}"
  comparison_operator = "GreaterThanThreshold"
  datapoints_to_alarm = 1
  evaluation_periods  = 1
  threshold           = each.value.threshold
  treat_missing_data  = "ignore"

  alarm_actions = each.value.type == "warning" ? local.warning_arn : local.alert_arn
  ok_actions    = each.value.type == "warning" ? local.warning_ok_arn : local.alert_ok_arn

  alarm_description = join("", [
    "BFD Pipeline in ${local.env} environment failed to load data within a ",
    "${each.value.threshold / 60 / 60} hour period",
    "\n\n${local.dashboard_message_fragment}"
  ])

  metric_query {
    id          = "m1"
    return_data = false

    metric {
      metric_name = "time/data-first-available-repeating"
      namespace   = local.metric_namespace
      period      = 60
      stat        = "Maximum"
    }
  }

  metric_query {
    id          = "m2"
    return_data = false

    metric {
      metric_name = "time/data-fully-loaded-repeating"
      namespace   = local.metric_namespace
      period      = 60
      stat        = "Maximum"
    }
  }

  metric_query {
    expression  = "EPOCH(m1)"
    id          = "e1"
    label       = "Expression1"
    return_data = false
  }

  metric_query {
    expression  = "FILL(m1, REPEAT)"
    id          = "e2"
    label       = "Expression2"
    return_data = false
  }

  metric_query {
    expression  = "FILL(m2, REPEAT)"
    id          = "e3"
    label       = "Expression3"
    return_data = false
  }

  metric_query {
    expression  = "IF(e2>e3, e1-e2, 0)"
    id          = "e4"
    label       = "Ongoing Data Load Time"
    return_data = true
  }
}
