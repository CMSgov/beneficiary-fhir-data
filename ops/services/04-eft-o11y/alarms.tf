locals {
  lambda_metrics_namespace = "AWS/Lambda"
  sns_metrics_namespace    = "AWS/SNS"

  alarms_raw_config = {
    lambda_errors = {
      breach_topic   = lookup(local.ssm_config, "/bfd/${local.service}/outbound/alarms/lambda_errors/breach_topic", null)
      ok_topic       = lookup(local.ssm_config, "/bfd/${local.service}/outbound/alarms/lambda_errors/ok_topic", null)
      log_group_name = "/aws/lambda/${local.eft_outputs.outbound_lambda_name}"
    }
    sns_failures = {
      breach_topic = lookup(local.ssm_config, "/bfd/${local.service}/outbound/alarms/sns_failures/breach_topic", null)
      ok_topic     = lookup(local.ssm_config, "/bfd/${local.service}/outbound/alarms/sns_failures/ok_topic", null)
      per_topic = {
        for topic_name in local.outbound_sns_topic_names :
        topic_name => {
          log_group_name = "sns/${local.region}/${local.account_id}/${topic_name}/Failure"
        }
      }
    }
  }
  alarms_config = {
    lambda_errors = {
      breach_topic_arn = try(data.aws_sns_topic.breach_topics["lambda_errors"].arn, null)
      ok_topic_arn     = try(data.aws_sns_topic.ok_topics["lambda_errors"].arn, null)
      alarm_name       = "${local.eft_outputs.outbound_lambda_name}-errors"
      log_group_name   = local.alarms_raw_config.lambda_errors.log_group_name
      log_group_url    = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#logsV2:log-groups/log-group/${replace(local.alarms_raw_config.lambda_errors.log_group_name, "/", "$252F")}"
      queue_url        = "https://${local.region}.console.aws.amazon.com/sqs/v3/home?region=${local.region}#/queues/${urlencode(data.aws_sqs_queue.outbound_lambda_dlq.url)}"
    }
    sns_failures = {
      breach_topic_arn = try(data.aws_sns_topic.breach_topics["sns_failures"].arn, null)
      ok_topic_arn     = try(data.aws_sns_topic.ok_topics["sns_failures"].arn, null)
      per_topic = {
        for topic_name in local.outbound_sns_topic_names :
        topic_name => {
          alarm_name     = "${topic_name}-sns-delivery-failures"
          log_group_name = local.alarms_raw_config.sns_failures.per_topic[topic_name].log_group_name
          log_group_url  = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#logsV2:log-groups/log-group/${replace(local.alarms_raw_config.sns_failures.per_topic[topic_name].log_group_name, "/", "$252F")}"
        }
      }
    }
  }
}

data "aws_sns_topic" "breach_topics" {
  for_each = { for k, v in local.alarms_raw_config : k => v if v.breach_topic != null }

  name = each.value.breach_topic
}

data "aws_sns_topic" "ok_topics" {
  for_each = { for k, v in local.alarms_raw_config : k => v if v.ok_topic != null }

  name = each.value.ok_topic
}

data "aws_sqs_queue" "outbound_lambda_dlq" {
  name = local.eft_outputs.outbound_lambda_dlq_name
}

data "aws_lambda_function" "outbound_lambda" {
  function_name = local.eft_outputs.outbound_lambda_name
}

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = local.alarms_config.lambda_errors.alarm_name
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "10"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"
  datapoints_to_alarm = "3"
  treat_missing_data  = "ignore"

  alarm_description = join("", [
    "The ${local.eft_outputs.outbound_lambda_name} has failed 3 times in 10 minutes in ${local.env}. View the ",
    "linked CloudWatch Log Group for more details on the failures, and inspect the failing events ",
    "in the linked DLQ",
    "\n",
    "\n* CloudWatch Log Group: <${local.alarms_config.lambda_errors.log_group_url}|${local.alarms_config.lambda_errors.log_group_name}>",
    "\n* Dead Letter Queue: <${local.alarms_config.lambda_errors.queue_url}|${local.eft_outputs.outbound_lambda_dlq_name}>",
  ])

  metric_name = "Errors"
  namespace   = local.lambda_metrics_namespace
  dimensions = {
    FunctionName = local.eft_outputs.outbound_lambda_name
  }

  alarm_actions = local.alarms_config.lambda_errors.breach_topic_arn != null ? [local.alarms_config.lambda_errors.breach_topic_arn] : null
  ok_actions    = local.alarms_config.lambda_errors.ok_topic_arn != null ? [local.alarms_config.lambda_errors.ok_topic_arn] : null
}

resource "aws_cloudwatch_metric_alarm" "sns_failures" {
  for_each = local.alarms_config.sns_failures.per_topic

  alarm_name          = each.value.alarm_name
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"
  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"

  alarm_description = join("", [
    "The ${each.key} SNS Topic has failed to deliver a status notification to a subscriber in ",
    "${local.env}. View the linked CloudWatch Log Group for more details",
    "\n",
    "\n* CloudWatch Log Group: <${each.value.log_group_url}|${each.value.log_group_name}>",
  ])

  metric_name = "NumberOfNotificationsFailed"
  namespace   = local.sns_metrics_namespace
  dimensions = {
    TopicName = each.key
  }

  alarm_actions = local.alarms_config.sns_failures.breach_topic_arn != null ? [local.alarms_config.sns_failures.breach_topic_arn] : null
  ok_actions    = local.alarms_config.sns_failures.ok_topic_arn != null ? [local.alarms_config.sns_failures.ok_topic_arn] : null
}

resource "aws_cloudwatch_metric_alarm" "lambda_timeouts" {
  alarm_name          = "${local.alarms_config.lambda_errors.alarm_name}-timeout"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  # `period` is expressed in minutes, calculated here for previous 15 minutes to catch **all** lambda timeouts
  period    = 60 * 15
  statistic = "Maximum"
  # `threshold` expressed in milliseconds
  threshold           = data.aws_lambda_function.outbound_lambda.timeout * 1000
  datapoints_to_alarm = 1
  treat_missing_data  = "notBreaching"

  alarm_description = join("", [
    "The ${local.eft_outputs.outbound_lambda_name} has timed out in ${local.env}. View the ",
    "linked CloudWatch Log Group for more details on the failure, and inspect the failing event ",
    "in the linked DLQ",
    "\n",
    "\n* CloudWatch Log Group: <${local.alarms_config.lambda_errors.log_group_url}|${local.alarms_config.lambda_errors.log_group_name}>",
    "\n* Dead Letter Queue: <${local.alarms_config.lambda_errors.queue_url}|${local.eft_outputs.outbound_lambda_dlq_name}>",
  ])

  metric_name = "Duration"
  namespace   = local.lambda_metrics_namespace
  dimensions = {
    FunctionName = local.eft_outputs.outbound_lambda_name
  }

  alarm_actions = local.alarms_config.lambda_errors.breach_topic_arn != null ? [local.alarms_config.lambda_errors.breach_topic_arn] : null
  ok_actions    = local.alarms_config.lambda_errors.ok_topic_arn != null ? [local.alarms_config.lambda_errors.ok_topic_arn] : null
}
