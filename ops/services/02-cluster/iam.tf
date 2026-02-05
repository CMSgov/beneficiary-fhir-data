data "aws_iam_policy_document" "eventbridge_logs" {
  count = local.is_ephemeral_env ? 0 : 1

  statement {
    sid    = "EventBridgeToCloudWatchLogs"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }

    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]

    resources = [
      "${aws_cloudwatch_log_group.ecs_events[0].arn}:*"
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [local.account_id]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_event_rule.ecs_events[0].arn]
    }
  }
}
