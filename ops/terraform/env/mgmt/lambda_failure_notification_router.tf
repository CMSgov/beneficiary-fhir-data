locals {
  notification_router_lambda_name = "lambda_failure_notification_router"
  do_not_notify_for_param_name    = "/bfd/mgmt/common/nonsensitive/alarm/bfd-mgmt-lambda-error/do_not_notify_for"
  send_alert_to_param             = "/bfd/mgmt/common/nonsensitive/alarm/bfd-mgmt-lambda-error/send_alert_to"
}

resource "aws_ssm_parameter" "do_not_notify_list_param" {
  name  = local.do_not_notify_for_param_name
  type  = "StringList"
  value = local.notification_router_lambda_name

  lifecycle {
    ignore_changes = ["value"]
  }
}

resource "aws_ssm_parameter" "send_alert_to_param" {
  name  = local.send_alert_to_param
  type  = "String"
  value = "{}"

  lifecycle {
    ignore_changes = ["value"]
  }
}

data "archive_file" "zip_notification_router_lambda" {
  type        = "zip"
  source_file = "./${local.notification_router_lambda_name}.py"
  output_path = "./${local.notification_router_lambda_name}.zip"
}

resource "aws_iam_role" "notification_router_lambda_role" {
  name               = "${local.notification_router_lambda_name}-role"
  description        = "Role for the ${local.notification_router_lambda_name} Lambda"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume.json
}

resource "aws_iam_policy" "notification_router_lambda" {
  name = "${local.notification_router_lambda_name}-policy"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Sid    = "AllowPublishingToSNS",
      Effect = "Allow",
      Action = ["sns:Publish"],
      Resource = [data.aws_sns_topic.internal_alert_slack.arn,
        data.aws_sns_topic.bfd_alerts_slack.arn,
        data.aws_sns_topic.bfd_notices_slack.arn,
        data.aws_sns_topic.bfd_warnings_slack.arn,
      data.aws_sns_topic.victor_ops.arn]
      },
      {
        Sid      = "AllowListingOfLambdaFunctions",
        Effect   = "Allow",
        Action   = ["lambda:ListFunctions"],
        Resource = "*"

      },
      {
        Sid      = "AllowToReadSSMParameters",
        Effect   = "Allow",
        Action   = ["ssm:GetParameter"],
        Resource = "arn:aws:ssm:${local.region}:${local.account_id}:parameter/*"
      },
      {
        Sid    = "AllowToPublishToEncryptedSNS",
        Effect = "Allow",
        Action = ["kms:GenerateDataKey",
        "kms:Decrypt"],
        Resource = data.aws_kms_key.test_cmk.arn
      },
      {
        Sid    = "AllowListingTagsOnCloudWatchAlarms",
        Effect = "Allow",
        Action = ["cloudwatch:ListTagsForResource",
        "cloudwatch:GetMetricData"],
        Resource = "*"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "notification_router_lambda_basic_permission" {
  role       = aws_iam_role.notification_router_lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_policy_attachment" "nofitication_router_lambda_custom_permission" {
  name       = "${local.notification_router_lambda_name}_custom_permission"
  policy_arn = aws_iam_policy.notification_router_lambda.arn
  roles      = [aws_iam_role.notification_router_lambda_role.name]
}

resource "aws_lambda_function" "notification_router" {
  function_name    = local.notification_router_lambda_name
  runtime          = "python3.12"
  filename         = data.archive_file.zip_notification_router_lambda.output_path
  source_code_hash = data.archive_file.zip_notification_router_lambda.output_base64sha256
  role             = aws_iam_role.notification_router_lambda_role.arn
  handler          = "${local.notification_router_lambda_name}.handler"

  timeout = 30

  environment {
    variables = {
      DEFAULT_SNS_TOPIC_ARN   = "${data.aws_sns_topic.internal_alert_slack.arn}"
      BFD_INTERNAL            = "${data.aws_sns_topic.internal_alert_slack.arn}"
      BFD_ALERTS              = "${data.aws_sns_topic.bfd_alerts_slack.arn}"
      BFD_NOTICES             = "${data.aws_sns_topic.bfd_notices_slack.arn}"
      BFD_WARNINGS            = "${data.aws_sns_topic.bfd_warnings_slack.arn}"
      VICTOR_OPS              = "${data.aws_sns_topic.victor_ops.arn}"
      DO_NOT_NOTIFY_FOR_PARAM = "${aws_ssm_parameter.do_not_notify_list_param.name}"
      SEND_ALERT_TO_PARAM     = "${aws_ssm_parameter.send_alert_to_param.name}"
    }
  }
}

resource "aws_cloudwatch_log_metric_filter" "notification_router" {
  name           = "${local.notification_router_lambda_name}-metric-filter"
  log_group_name = "/aws/lambda/${aws_lambda_function.notification_router.function_name}"
  pattern        = "\"${aws_lambda_function.notification_router.function_name}-Error\""

  metric_transformation {
    namespace = "LambdaNotificationRouterMetricFilter"
    name      = "Error"
    value     = 1
  }

  depends_on = [aws_lambda_function.notification_router]
}

resource "aws_cloudwatch_metric_alarm" "notification_router" {
  alarm_name          = local.notification_router_lambda_name
  namespace           = "LambdaNotificationRouterMetricFilter"
  comparison_operator = "GreaterThanThreshold"
  statistic           = "Sum"
  metric_name         = "Error"
  threshold           = 0
  period              = 60
  evaluation_periods  = 1

  alarm_actions = [data.aws_sns_topic.internal_alert_slack.arn]
}

resource "aws_cloudwatch_event_rule" "alarm_state_change" {
  name = "lambda-failure-alarm-events"
  event_pattern = jsonencode({
    "source" : ["aws.cloudwatch"],
    "detail-type" : ["CloudWatch Alarm State Change"],
    "detail" : {
      "alarmName" : [aws_cloudwatch_metric_alarm.lambda_errors.alarm_name],
      "state" : {
        "value" : ["ALARM"]
      },
    }
  })
}

resource "aws_cloudwatch_event_target" "route_alarm_to_lambda" {
  rule      = aws_cloudwatch_event_rule.alarm_state_change.name
  target_id = "lambda-failure-notification-router"
  arn       = aws_lambda_function.notification_router.arn
}

resource "aws_lambda_permission" "notification_router" {
  function_name = aws_lambda_function.notification_router.function_name
  statement_id  = "AllowCloudWatchFunctionInvocation"
  action        = "lambda:InvokeFunction"
  principal     = "events.amazonaws.com"
  #source_arn    = "arn:aws:events:us-east-1:577373831711:rule/lambda-failure-alarm-events"
  source_arn = aws_cloudwatch_event_rule.alarm_state_change.arn
}
