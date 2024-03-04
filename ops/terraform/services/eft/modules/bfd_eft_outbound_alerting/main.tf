locals {
  env        = terraform.workspace
  service    = "eft"
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name

  lambda_metrics_namespace = "AWS/Lambda"
  sns_metrics_namespace    = "AWS/SNS"

  alarms_raw_config = {
    lambda_errors = {
      breach_topic   = lookup(var.ssm_config, "/bfd/${local.service}/outbound/o11y/lambda_errors/alarms/breach_topic", null)
      ok_topic       = lookup(var.ssm_config, "/bfd/${local.service}/outbound/o11y/lambda_errors/alarms/ok_topic", null)
      log_group_name = "/aws/lambda/${var.outbound_lambda_name}"
    }
    sns_failures = {
      breach_topic = lookup(var.ssm_config, "/bfd/${local.service}/outbound/o11y/sns_failures/alarms/breach_topic", null)
      ok_topic     = lookup(var.ssm_config, "/bfd/${local.service}/outbound/o11y/sns_failures/alarms/ok_topic", null)
      per_topic = {
        for topic_name in var.outbound_sns_topic_names :
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
      alarm_name       = "bfd-${local.service}-${var.outbound_lambda_name}-errors"
      log_group_name   = local.alarms_raw_config.lambda_errors.log_group_name
      log_group_url    = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#logsV2:log-groups/log-group/${replace(local.alarms_raw_config.lambda_errors.log_group_name, "/", "$252F")}"
      queue_url        = "https://us-east-1.console.aws.amazon.com/sqs/v3/home?region=us-east-1#/queues/${urlencode(data.aws_sqs_queue.outbound_lambda_dlq.url)}"
    }
    sns_failures = {
      breach_topic_arn = try(data.aws_sns_topic.breach_topics["sns_failures"].arn, null)
      ok_topic_arn     = try(data.aws_sns_topic.ok_topics["sns_failures"].arn, null)
      per_topic = {
        for topic_name in var.outbound_sns_topic_names :
        topic_name => {
          alarm_name     = "${topic_name}-sns-delivery-failures"
          log_group_name = local.alarms_raw_config.sns_failures.per_topic[topic_name].log_group_name
          log_group_url  = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#logsV2:log-groups/log-group/${replace(local.alarms_raw_config.sns_failures.per_topic[topic_name].log_group_name, "/", "$252F")}"
        }
      }
    }
  }
  slack_webhook_ssm_path = var.ssm_config["/bfd/${local.service}/outbound/o11y/slack_notifier/webhook_ssm_path"]
  slack_webhook          = nonsensitive(data.aws_ssm_parameter.slack_webhook.value)
}

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = local.alarms_config.lambda_errors.alarm_name
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"
  datapoints_to_alarm = "1"
  treat_missing_data  = "notBreaching"

  alarm_description = join("", [
    "The ${var.outbound_lambda_name} has failed to transfer a file in ${local.env}. View the ",
    "linked CloudWatch Log Group for more details on the failure, and inspect the failing event ",
    "in the linked DLQ",
    "\n\n",
    "* CloudWatch Log Group: <${local.alarms_config.lambda_errors.log_group_url}|${local.alarms_config.lambda_errors.log_group_name}>",
    "* Dead Letter Queue: <${local.alarms_config.lambda_errors.queue_url}|${var.outbound_lambda_dlq_name}>",
  ])

  metric_name = "Errors"
  namespace   = local.lambda_metrics_namespace
  dimensions = {
    FunctionName = var.outbound_lambda_name
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
    "\n\n",
    "* CloudWatch Log Group: <${each.value.log_group_url}|${each.value.log_group_name}>",
  ])

  metric_name = "NumberOfNotificationsFailed"
  namespace   = local.sns_metrics_namespace
  dimensions = {
    TopicName = each.key
  }

  alarm_actions = local.alarms_config.sns_failures.breach_topic_arn != null ? [local.alarms_config.sns_failures.breach_topic_arn] : null
  ok_actions    = local.alarms_config.sns_failures.ok_topic_arn != null ? [local.alarms_config.sns_failures.ok_topic_arn] : null
}
