
data "aws_iam_policy_document" "sns_allow_s3_publish" {
  for_each = local.partners
  statement {
    sid     = "AllowS3Publish"
    effect  = "Allow"
    actions = ["SNS:Publish"]
    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }
    resources = [aws_sns_topic.partner_bucket_events[each.value].arn]
    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [local.partner_buckets[each.value].arn]
    }
  }
}

data "aws_iam_policy_document" "sqs_allow_sns_send" {
  for_each = local.partners
  statement {
    sid     = "AllowSnsSendMessage"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]
    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
    resources = [aws_sqs_queue.partner_bucket_events[each.value].arn]
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.partner_bucket_events[each.value].arn]
    }
  }
}
resource "aws_sns_topic" "partner_bucket_events" {
  for_each = local.partners
  name     = "${local.name_prefix}-${each.value}-bucket-events"

  tags = merge(local.default_tags, {
    Env     = local.env
    Service = local.service
    Partner = each.key
  })
}

resource "aws_sns_topic_policy" "partner_bucket_events" {
  for_each = local.partners
  arn      = aws_sns_topic.partner_bucket_events[each.value].arn
  policy   = data.aws_iam_policy_document.sns_allow_s3_publish[each.value].json
}

resource "aws_sqs_queue" "partner_bucket_events_dlq" {
  for_each = local.partners
  name     = "${local.name_prefix}-${each.value}-bucket-events-dlq"

  tags = merge(local.default_tags, {
    Env     = local.env
    Service = local.service
    Partner = each.key
  })
}

resource "aws_sqs_queue" "partner_bucket_events" {
  for_each = local.partners
  name     = "${local.name_prefix}-${each.value}-bucket-events"
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.partner_bucket_events_dlq[each.value].arn
    maxReceiveCount     = 5
  })

  tags = merge(local.default_tags, {
    Env     = local.env
    Service = local.service
    Partner = each.key
  })
}

resource "aws_sns_topic_subscription" "partner_bucket_events_to_sqs" {
  for_each  = local.partners
  topic_arn = aws_sns_topic.partner_bucket_events[each.value].arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.partner_bucket_events[each.value].arn
}

resource "aws_sqs_queue_policy" "partner_bucket_events" {
  for_each  = local.partners
  queue_url = aws_sqs_queue.partner_bucket_events[each.value].id
  policy    = data.aws_iam_policy_document.sqs_allow_sns_send[each.value].json
}
