locals {
  events_queue_roles = jsondecode(
    nonsensitive(
      lookup(
        local.ssm_config,
        "/bfd/${local.service}/queue/authorized_roles_json",
        jsonencode(["arn:aws:iam::${local.account_id}:root"])
      )
    )
  )
}

resource "aws_sqs_queue" "events" {
  count = local.apply_events_resources ? 1 : 0

  name = local.name_prefix

  fifo_queue                 = false
  sqs_managed_sse_enabled    = false
  max_message_size           = 1048576
  message_retention_seconds  = 4 * 24 * 60 * 60 # 4 days
  receive_wait_time_seconds  = 0
  visibility_timeout_seconds = 60
}

data "aws_iam_policy_document" "events_queue_policy" {
  count = local.apply_events_resources ? 1 : 0

  statement {
    sid       = "AllowPrincipalsToSendMessages"
    actions   = ["sqs:SendMessage"]
    resources = aws_sqs_queue.events[*].arn

    principals {
      type        = "AWS"
      identifiers = local.events_queue_roles
    }
  }
}

resource "aws_sqs_queue_policy" "events" {
  count = local.apply_events_resources ? 1 : 0

  queue_url = one(aws_sqs_queue.events[*].url)
  policy    = one(data.aws_iam_policy_document.events_queue_policy[*].json)
}

resource "aws_sqs_queue" "events_dlq" {
  count = local.apply_events_resources ? 1 : 0

  name                      = "${local.name_prefix}-dlq"
  kms_master_key_id         = local.env_key_arn
  message_retention_seconds = 14 * 24 * 60 * 60 # 14 days, in seconds, which is the maximum
}


resource "aws_sqs_queue_redrive_allow_policy" "events" {
  count = local.apply_events_resources ? 1 : 0

  queue_url = one(aws_sqs_queue.events_dlq[*].id)
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue",
    sourceQueueArns   = aws_sqs_queue.events[*].arn
  })
}

resource "aws_sqs_queue_redrive_policy" "events" {
  count = local.apply_events_resources ? 1 : 0

  queue_url = one(aws_sqs_queue.events[*].id)
  redrive_policy = jsonencode({
    deadLetterTargetArn = one(aws_sqs_queue.events_dlq[*].arn)
    maxReceiveCount     = 4
  })
}
