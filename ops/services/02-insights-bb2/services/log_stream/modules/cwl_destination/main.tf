data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  full_name  = "bfd-insights-${var.project}-${var.firehose_name}"
  env        = terraform.workspace
}

# CWL destination
resource "aws_cloudwatch_log_destination" "cwl_destination" {
  name       = "${local.full_name}-firehose-destination"
  role_arn   = aws_iam_role.cwl2firehose_role.arn
  target_arn = "arn:aws:firehose:${var.region}:${local.account_id}:deliverystream/${local.full_name}"
}

data "aws_iam_policy_document" "trust_rel_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["logs.${var.region}.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cwl2firehose_role" {
  name               = "${local.full_name}-cwl2firehose-role"
  assume_role_policy = data.aws_iam_policy_document.trust_rel_assume_role_policy.json

  inline_policy {
    name = "${local.full_name}-cwl2firehose-policy"

    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Action   = ["firehose:*"]
          Effect   = "Allow"
          Resource = ["arn:aws:firehose:${var.region}:${local.account_id}:*"]
        },
      ]
    })
  }
}

# CWL destination policy
data "aws_iam_policy_document" "cwl_destination_policy_doc" {
  statement {
    effect = "Allow"
    principals {
      type = "AWS"
      identifiers = [
        data.aws_ssm_parameter.bb2_account_id.value,
        data.aws_ssm_parameter.bb2_legacy_account_id.value
      ]
    }
    actions = [
      "logs:PutSubscriptionFilter",
    ]
    resources = [
      aws_cloudwatch_log_destination.cwl_destination.arn,
    ]
  }
}
resource "aws_cloudwatch_log_destination_policy" "cwl_destination_policy" {
  destination_name = aws_cloudwatch_log_destination.cwl_destination.name
  access_policy    = data.aws_iam_policy_document.cwl_destination_policy_doc.json
}

data "aws_ssm_parameter" "bb2_account_id" {
  name = "/bb2/${local.env}/common/sensitive/account_id"
}

data "aws_ssm_parameter" "bb2_legacy_account_id" {
  name = "/bb2/${local.env}/common/sensitive/legacy_account_id"
}
