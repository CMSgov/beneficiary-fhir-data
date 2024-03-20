# NOTE: These resources are defined to support a contingency that should be
# activated in the event that AWS Lambda cannot satisfy the EFT Outbound
# requirements within the allotted 15m timeout.
resource "aws_sqs_queue" "sftp_outbound_transfer_contingency" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0
  name  = "${local.outbound_lambda_full_name}-contingency"
}

resource "aws_sns_topic_subscription" "sftp_outbound_transfer_contingency" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

  topic_arn = aws_sns_topic.outbound_pending_s3_notifs[each.key].arn
  protocol  = "sqs"
  endpoint  = one(aws_sqs_queue.sftp_outbound_transfer_contingency[*].arn)
}

resource "aws_sqs_queue_policy" "sftp_outbound_transfer_sqs_contingency" {
  count     = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0
  queue_url = one(aws_sqs_queue.sftp_outbound_transfer_contingency[*].id)
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Id" : "${local.outbound_lambda_full_name}-contingency",
      "Statement" : [
        {
          "Effect" : "Allow",
          "Principal" : {
            "Service" : "sns.amazonaws.com"
          },
          "Action" : "SQS:SendMessage",
          Resource = [one(aws_sqs_queue.sftp_outbound_transfer_contingency[*].arn)]
          "Condition" : { "StringLike" : { "aws:SourceArn" : [for partner, data in aws_sns_topic.outbound_pending_s3_notifs : data.arn] } }
        }
      ]
    }
  )
}
