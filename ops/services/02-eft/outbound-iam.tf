data "aws_iam_policy_document" "sftp_outbound_transfer_logs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${one(aws_cloudwatch_log_group.sftp_outbound_transfer[*].arn)}:*"]
  }
}

resource "aws_iam_policy" "sftp_outbound_transfer_logs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.outbound_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])
  policy = one(data.aws_iam_policy_document.sftp_outbound_transfer_logs[*].json)
}

data "aws_iam_policy_document" "sftp_outbound_transfer_ssm" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = flatten([
      for hierarchy in local.ssm_hierarchy_roots :
      [
        "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${hierarchy}/${local.env}/common/*",
        "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${hierarchy}/${local.env}/${local.service}/*",
      ]
    ])
  }
}

resource "aws_iam_policy" "sftp_outbound_transfer_ssm" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = "${local.outbound_lambda_full_name}-ssm"
  path        = local.iam_path
  description = "Permissions to get parameters from the appropriate hierarchies"
  policy      = one(data.aws_iam_policy_document.sftp_outbound_transfer_ssm[*].json)
}

data "aws_iam_policy_document" "sftp_outbound_transfer_kms" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

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

resource "aws_iam_policy" "sftp_outbound_transfer_kms" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = "${local.outbound_lambda_full_name}-kms"
  path        = local.iam_path
  description = "Permissions to encrypt and decrypt master KMS keys for ${local.env}"
  policy      = one(data.aws_iam_policy_document.sftp_outbound_transfer_kms[*].json)
}

data "aws_iam_policy_document" "sftp_outbound_transfer_s3" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    sid       = "AllowListingOfBucket"
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [module.bucket_eft.bucket.arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values = [
        for partner in local.eft_partners_with_outbound_enabled :
        "${module.bucket_eft.bucket.arn}/${local.eft_partners_config[partner].outbound.pending_path}/*"
      ]
    }
  }

  statement {
    sid = "AllowAccessToOutboundPaths"
    actions = [
      "s3:AbortMultipartUpload",
      "s3:DeleteObject",
      "s3:DeleteObjectVersion",
      "s3:GetObject",
      "s3:GetObjectAcl",
      "s3:GetObjectVersion",
      "s3:GetObjectVersionAcl",
      "s3:PutObject",
      "s3:PutObjectAcl",
      "s3:PutObjectVersionAcl"
    ]
    resources = [
      for partner in local.eft_partners_with_outbound_enabled :
      "${module.bucket_eft.bucket.arn}/${local.eft_partners_config[partner].outbound.pending_path}/*"
    ]
  }
}

resource "aws_iam_policy" "sftp_outbound_transfer_s3" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-s3-policy"
  path = local.iam_path
  description = join("", [
    "Allows the ${local.outbound_lambda_full_name} to manipulate objects within the ",
    "${local.full_name} S3 bucket",
  ])
  policy = one(data.aws_iam_policy_document.sftp_outbound_transfer_s3[*].json)
}

data "aws_iam_policy_document" "sftp_outbound_transfer_sqs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    sid       = "AllowUsageOfInvokeQueue"
    actions   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
    resources = aws_sqs_queue.sftp_outbound_transfer_invoke[*].arn
  }

  statement {
    sid       = "AllowSendMessageToDLQ"
    actions   = ["sqs:SendMessage"]
    resources = aws_sqs_queue.sftp_outbound_transfer_dlq[*].arn
  }
}

resource "aws_iam_policy" "sftp_outbound_transfer_sqs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = "${local.outbound_lambda_full_name}-sqs-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.outbound_lambda_full_name} to use SQS for invocation and dead-letters"
  policy      = one(data.aws_iam_policy_document.sftp_outbound_transfer_sqs[*].json)
}

data "aws_iam_policy_document" "sftp_outbound_transfer_sns" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    sid     = "AllowSendingMessages"
    actions = ["SNS:Publish"]
    resources = flatten([
      module.topic_outbound_notifs[*].topic.arn,
      [
        for partner in local.eft_partners_with_outbound_notifs :
        module.topic_outbound_partner_notifs[partner].topic.arn
      ]
    ])
  }
}

resource "aws_iam_policy" "sftp_outbound_transfer_sns" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-sns"
  path = local.iam_path
  description = join("", [
    "Allows the ${local.outbound_lambda_full_name} to publish status notifications to the ",
    "${local.outbound_notifs_topic_prefix} SNS Topic and partner-specific Topics"
  ])
  policy = one(data.aws_iam_policy_document.sftp_outbound_transfer_sns[*].json)
}

resource "aws_iam_role" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name                  = local.outbound_lambda_full_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.outbound_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  force_detach_policies = true
}

data "aws_iam_policy" "lambda_vpc_access" {
  name = "AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy_attachment" "sftp_outbound_transfer" {
  for_each = length(local.eft_partners_with_outbound_enabled) > 0 ? {
    logs = one(aws_iam_policy.sftp_outbound_transfer_logs[*].arn),
    ssm  = one(aws_iam_policy.sftp_outbound_transfer_ssm[*].arn),
    kms  = one(aws_iam_policy.sftp_outbound_transfer_kms[*].arn),
    s3   = one(aws_iam_policy.sftp_outbound_transfer_s3[*].arn),
    sqs  = one(aws_iam_policy.sftp_outbound_transfer_sqs[*].arn)
    sns  = one(aws_iam_policy.sftp_outbound_transfer_sns[*].arn)
    vpc  = data.aws_iam_policy.lambda_vpc_access.arn
  } : {}

  role       = one(aws_iam_role.sftp_outbound_transfer[*].name)
  policy_arn = each.value
}
