locals {
  events_queue_roles = jsondecode(
    nonsensitive(
      lookup(local.ssm_config, "/bfd/${local.service}/queue/authorized_roles_json", "[]")
    )
  )
}

resource "aws_sqs_queue" "events" {
  count = local.is_prod ? 1 : 0

  name = local.name_prefix

  fifo_queue                 = false
  sqs_managed_sse_enabled    = false
  max_message_size           = 1048576
  message_retention_seconds  = 4 * 24 * 60 * 60 # 4 days
  receive_wait_time_seconds  = 0
  visibility_timeout_seconds = 30
}

data "aws_iam_policy_document" "events_sqs" {
  count = local.is_prod ? 1 : 0

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
  count = local.is_prod ? 1 : 0

  queue_url = one(aws_sqs_queue.events[*].url)
  policy    = one(data.aws_iam_policy_document.events_sqs[*].json)
}
