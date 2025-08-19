terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = true
  service              = local.service
  relative_module_root = "ops/services/03-eft-o11y"
}

locals {
  service        = "eft-o11y"
  target_service = "eft"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = nonsensitive(module.terraservice.ssm_config)
  env_key_alias            = module.terraservice.env_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  azs                      = keys(module.terraservice.default_azs)

  full_name = "bfd-${local.env}-${local.service}"

  lambda_metrics_namespace = "AWS/Lambda"
  sns_metrics_namespace    = "AWS/SNS"

  outbound_sns_topic_names = concat([local.eft_outputs.outbound_bfd_sns_topic_name], local.eft_outputs.outbound_partner_sns_topic_names)
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

  slack_notifier_lambda_name = "bfd-${local.env}-${local.service}-outbound-slack-notifier"
  slack_notifier_lambda_src  = "outbound_slack_notifier"
  slack_webhook_ssm_path     = local.ssm_config["/bfd/${local.service}/outbound/slack_notifier/webhook_ssm_path"]
  slack_webhook              = nonsensitive(data.aws_ssm_parameter.slack_webhook.value)
}

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = local.alarms_config.lambda_errors.alarm_name
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "10"
  period              = "60"
  statistic           = "Sum"
  threshold           = "1"
  datapoints_to_alarm = "3"
  treat_missing_data  = "notBreaching"

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

resource "aws_lambda_permission" "slack_notifier_allow_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.slack_notifier.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = local.eft_outputs.outbound_bfd_sns_topic_arn
}

resource "aws_sns_topic_subscription" "sns_to_slack_notifier" {
  topic_arn = local.eft_outputs.outbound_bfd_sns_topic_arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.slack_notifier.arn
}

data "archive_file" "slack_notifier_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.slack_notifier_lambda_src}/${local.slack_notifier_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.slack_notifier_lambda_src}/${local.slack_notifier_lambda_src}.py")
    filename = "${local.slack_notifier_lambda_src}.py"
  }
}

resource "aws_lambda_function" "slack_notifier" {
  function_name = local.slack_notifier_lambda_name

  description = join("", [
    "Invoked when the ${local.eft_outputs.outbound_bfd_sns_topic_name} sends a notification. This Lambda posts ",
    "the contents of the notification to the configured Slack channel"
  ])

  kms_key_arn      = local.env_key_arn
  filename         = data.archive_file.slack_notifier_src.output_path
  source_code_hash = data.archive_file.slack_notifier_src.output_base64sha256
  architectures    = ["arm64"]
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python313-arm64:18"]
  handler          = "${local.slack_notifier_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 300

  tags = {
    Name = local.slack_notifier_lambda_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT   = local.env
      SLACK_WEBHOOK_URL = local.slack_webhook
    }
  }

  role = one(aws_iam_role.slack_notifier[*].arn)
}
