locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name

  topic_arn_placeholder = "%TOPIC_ARN%"
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "topic_template" {
  statement {
    sid = "AllowAccountUsage"
    actions = [
      "SNS:GetTopicAttributes",
      "SNS:SetTopicAttributes",
      "SNS:AddPermission",
      "SNS:RemovePermission",
      "SNS:DeleteTopic",
      "SNS:Subscribe",
      "SNS:ListSubscriptionsByTopic",
      "SNS:Publish",
      "SNS:Receive"
    ]
    resources = [local.topic_arn_placeholder]

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceOwner"
      values   = [local.account_id]
    }
  }
}

data "aws_iam_policy_document" "combined" {
  source_policy_documents = concat(
    [data.aws_iam_policy_document.topic_template.json],
    coalesce(var.additional_topic_policy_docs, [])
  )
}

resource "aws_cloudwatch_log_group" "success" {
  name         = "sns/${local.region}/${local.account_id}/${var.topic_name}"
  kms_key_id   = var.kms_key_arn
  skip_destroy = true
}

resource "aws_cloudwatch_log_group" "failure" {
  name         = "sns/${local.region}/${local.account_id}/${var.topic_name}/Failure"
  kms_key_id   = var.kms_key_arn
  skip_destroy = true
}

resource "aws_sns_topic" "this" {
  depends_on = [
    aws_cloudwatch_log_group.success,
    aws_cloudwatch_log_group.failure
  ]

  name              = var.topic_name
  display_name      = var.topic_description
  kms_master_key_id = var.kms_key_arn

  application_success_feedback_sample_rate = var.application_sample_rate
  application_success_feedback_role_arn    = aws_iam_role.this.arn
  application_failure_feedback_role_arn    = aws_iam_role.this.arn

  http_success_feedback_sample_rate = var.http_sample_rate
  http_success_feedback_role_arn    = aws_iam_role.this.arn
  http_failure_feedback_role_arn    = aws_iam_role.this.arn

  lambda_success_feedback_sample_rate = var.lambda_sample_rate
  lambda_success_feedback_role_arn    = aws_iam_role.this.arn
  lambda_failure_feedback_role_arn    = aws_iam_role.this.arn

  sqs_success_feedback_sample_rate = var.sqs_sample_rate
  sqs_success_feedback_role_arn    = aws_iam_role.this.arn
  sqs_failure_feedback_role_arn    = aws_iam_role.this.arn

  firehose_success_feedback_sample_rate = var.firehose_sample_rate
  firehose_success_feedback_role_arn    = aws_iam_role.this.arn
  firehose_failure_feedback_role_arn    = aws_iam_role.this.arn
}

resource "aws_sns_topic_policy" "this" {
  arn    = aws_sns_topic.this.arn
  policy = replace(data.aws_iam_policy_document.combined.json, local.topic_arn_placeholder, aws_sns_topic.this.arn)
}
