data "aws_iam_policy_document" "events_logs" {
  count = local.apply_events_resources ? 1 : 0

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${one(aws_cloudwatch_log_group.events[*].arn)}:*"]
  }
}

resource "aws_iam_policy" "events_logs" {
  count = local.apply_events_resources ? 1 : 0

  name = "${local.events_lambda_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.events_lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = one(data.aws_iam_policy_document.events_logs[*].json)
}

data "aws_iam_policy_document" "events_ssm" {
  count = local.apply_events_resources ? 1 : 0

  statement {
    actions = ["ssm:GetParameter"]
    resources = [
      "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.service}/sensitive/db/username",
      "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${local.service}/sensitive/db/password"
    ]
  }
}

resource "aws_iam_policy" "events_ssm" {
  count = local.apply_events_resources ? 1 : 0

  name        = "${local.events_lambda_full_name}-ssm"
  path        = local.iam_path
  description = "Grants permission for the ${local.events_lambda_full_name} Lambda to get relevant SSM parameters"
  policy      = one(data.aws_iam_policy_document.events_ssm[*].json)
}

data "aws_iam_policy_document" "events_kms" {
  count = local.apply_events_resources ? 1 : 0

  statement {
    sid = "AllowEncryptionAndDecryptionOfMasterKeys"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "events_kms" {
  count = local.apply_events_resources ? 1 : 0

  name = "${local.events_lambda_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permission for the ${local.events_lambda_full_name} Lambda to decrypt config KMS ",
    "keys and encrypt and decrypt master KMS keys for ${local.env}"
  ])

  policy = one(data.aws_iam_policy_document.events_kms[*].json)
}

data "aws_iam_policy_document" "events_sqs" {
  count = local.apply_events_resources ? 1 : 0

  statement {
    sid       = "AllowUsageOfInvokeQueue"
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
    resources = aws_sqs_queue.events[*].arn
  }

  statement {
    sid       = "AllowSendMessageToDLQ"
    actions   = ["sqs:SendMessage"]
    resources = aws_sqs_queue.events_dlq[*].arn
  }
}

resource "aws_iam_policy" "events_sqs" {
  count = local.apply_events_resources ? 1 : 0

  name        = "${local.events_lambda_full_name}-sqs"
  path        = local.iam_path
  description = "Grants permission for the ${local.events_lambda_full_name} to use SQS for invocation and dead-letters"
  policy      = one(data.aws_iam_policy_document.events_sqs[*].json)
}


resource "aws_iam_role" "events" {
  count = local.apply_events_resources ? 1 : 0

  name                  = local.events_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.events_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "events" {
  for_each = local.apply_events_resources ? {
    logs = one(aws_iam_policy.events_logs[*].arn)
    ssm  = one(aws_iam_policy.events_ssm[*].arn)
    kms  = one(aws_iam_policy.events_kms[*].arn)
    sqs  = one(aws_iam_policy.events_sqs[*].arn)
    vpc  = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
  } : {}

  role       = one(aws_iam_role.events[*].name)
  policy_arn = each.value
}
