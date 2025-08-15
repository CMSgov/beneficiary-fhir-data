data "aws_iam_policy_document" "service_assume_role" {
  for_each = toset(["transfer", "lambda"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "sftp_server_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.sftp_server.arn}:*"]
  }
}

resource "aws_iam_policy" "sftp_server_logs" {
  name = "${local.sftp_full_name}-logs"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.sftp_full_name} SFTP Server to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.sftp_server_logs.json
}

data "aws_iam_policy_document" "sftp_server_kms" {
  statement {
    sid = "AllowEncryptionUsingMasterKeys"
    actions = [
      "kms:Encrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "sftp_server_kms" {
  name = "${local.sftp_full_name}-kms"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.sftp_full_name} SFTP Server to use the KMS Master Key to ",
    "encrypt"
  ])
  policy = data.aws_iam_policy_document.sftp_server_kms.json
}

resource "aws_iam_role" "sftp_server" {
  name                  = "${local.sftp_full_name}-logs"
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for the ${local.sftp_full_name} Transfer Server to write logs"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["transfer"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "sftp_server" {
  for_each = {
    logs = aws_iam_policy.sftp_server_logs.arn
    kms  = aws_iam_policy.sftp_server_kms.arn
  }

  role       = aws_iam_role.sftp_server.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "sftp_user" {
  statement {
    sid = "AllowEncryptionAndDecryptionOfS3Files"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [
      local.env_key_arn
    ]
  }

  statement {
    sid       = "AllowListingOfUserFolder"
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [module.bucket_eft.bucket.arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${local.inbound_sftp_s3_home_dir}/*", local.inbound_sftp_s3_home_dir]
    }
  }

  statement {
    sid = "HomeDirObjectAccess"
    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:DeleteObjectVersion",
      "s3:GetObjectVersion",
      "s3:GetObjectACL",
      "s3:PutObjectACL",
    ]
    resources = ["${module.bucket_eft.bucket.arn}/${local.inbound_sftp_s3_home_dir}*"]
  }
}

resource "aws_iam_policy" "sftp_user" {
  name = "${local.sftp_full_name}-sftp-user"
  path = local.iam_path
  description = join("", [
    "Allows the ${local.inbound_sftp_user_username} SFTP user to access their restricted portion ",
    "of the ${module.bucket_eft.bucket.id} S3 bucket when using SFTP"
  ])
  policy = data.aws_iam_policy_document.sftp_user.json
}

resource "aws_iam_role" "sftp_user" {
  name                  = "${local.sftp_full_name}-sftp-user"
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for the ${local.inbound_sftp_user_username} SFTP user"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["transfer"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "sftp_user" {
  for_each = {
    all = aws_iam_policy.sftp_user.arn
  }

  role       = aws_iam_role.sftp_user.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "partner_bucket_access" {
  for_each = local.eft_partners_config

  statement {
    sid       = "AllowListingOfPartnerHomePath"
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [module.bucket_eft.bucket.arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${each.value.bucket_home_path}/*"]
    }
  }

  statement {
    sid = "AllowPartnerAccessToHomePath"
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
      "s3:PutObjectVersionAcl",
    ]
    resources = ["${module.bucket_eft.bucket.arn}/${each.value.bucket_home_path}/*"]
  }

  statement {
    sid = "AllowEncryptionAndDecryptionOfS3Files"
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

resource "aws_iam_policy" "partner_bucket_access" {
  for_each = local.eft_partners_config

  path = local.iam_path
  name = "${local.full_name}-${each.key}-bucket-access-policy"
  description = join("", [
    "Allows ${each.key} to access their specific EFT data when this policy's corresponding IAM ",
    "role is assumed by ${each.key}"
  ])
  policy = data.aws_iam_policy_document.partner_bucket_access[each.key].json
}

data "aws_iam_policy_document" "partner_bucket_assume" {
  for_each = local.eft_partners_config

  dynamic "statement" {
    for_each = { for idx, assumer_arn in each.value.bucket_iam_assumer_arns : idx => assumer_arn }

    content {
      sid     = "AllowAssumeRole${statement.key}"
      actions = ["sts:AssumeRole"]

      principals {
        type        = "AWS"
        identifiers = [statement.value]
      }
    }
  }
}

resource "aws_iam_role" "partner_bucket_access" {
  for_each = local.eft_partners_config

  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  name                 = "${local.full_name}-${each.key}-bucket-access-role"
  description = join("", [
    "Role granting cross-account permissions to partner-specific folder for ${each.key} within ",
    "the ${module.bucket_eft.bucket.id} EFT bucket when role is assumed"
  ])
  assume_role_policy    = data.aws_iam_policy_document.partner_bucket_assume[each.key].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "partner_bucket_access" {
  for_each = merge([
    for partner, _ in local.eft_partners_config
    : {
      "${partner}-all" = { partner = partner, policy = aws_iam_policy.partner_bucket_access[partner].arn }
    }
  ]...)

  role       = aws_iam_role.partner_bucket_access[each.value.partner].name
  policy_arn = each.value.policy
}

data "aws_iam_policy_document" "isp_bcda_bucket_access" {
  count = length(local.bcda_isp_bucket_assumer_arns) > 0 ? 1 : 0

  statement {
    sid       = "AllowListingOfBCDAHomePath"
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [module.bucket_eft.bucket.arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${local.eft_partners_config["bcda"].bucket_home_path}/*"]
    }
  }

  statement {
    sid = "AllowRestrictedAccessToBCDAHomePath"
    actions = [
      "s3:AbortMultipartUpload",
      "s3:PutObject",
      "s3:PutObjectAcl",
      "s3:PutObjectVersionAcl"
    ]
    resources = [
      "${module.bucket_eft.bucket.arn}/${local.eft_partners_config["bcda"].bucket_home_path}/*"
    ]
  }

  statement {
    sid = "AllowEncryptionAndDecryptionOfS3Files"
    actions = [
      "kms:Encrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "isp_bcda_bucket_access" {
  count = length(local.bcda_isp_bucket_assumer_arns) > 0 ? 1 : 0

  name = "${local.full_name}-isp-to-bcda-bucket-access-policy"
  path = local.iam_path
  description = join("", [
    "Allows ISP to access the BCDA inbound path when this policy's corresponding IAM ",
    "role is assumed by ISP"
  ])
  policy = one(data.aws_iam_policy_document.isp_bcda_bucket_access[*].json)
}

data "aws_iam_policy_document" "isp_bcda_bucket_access_assume" {
  count = length(local.bcda_isp_bucket_assumer_arns) > 0 ? 1 : 0

  statement {
    sid     = "AllowAssumeRole"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "AWS"
      identifiers = local.bcda_isp_bucket_assumer_arns
    }
  }
}

resource "aws_iam_role" "isp_bcda_bucket_access" {
  count = length(local.bcda_isp_bucket_assumer_arns) > 0 ? 1 : 0

  name = "${local.full_name}-isp-to-bcda-bucket-access-role"
  path = "/"
  description = join("", [
    "Role granting cross-account permissions to partner-specific folder for ISP to BCDA folder in ",
    "path within the ${module.bucket_eft.bucket.id} EFT bucket when role is assumed"
  ])

  assume_role_policy = one(data.aws_iam_policy_document.isp_bcda_bucket_access_assume[*].json)

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "isp_bcda_bucket_access" {
  for_each = length(local.bcda_isp_bucket_assumer_arns) > 0 ? {
    all = one(aws_iam_policy.isp_bcda_bucket_access[*].arn)
  } : {}

  role       = one(aws_iam_role.isp_bcda_bucket_access[*].name)
  policy_arn = each.value.policy
}

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
        "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${hierarchy}/common/*",
        "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${hierarchy}/${local.service}/*",
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

data "aws_iam_policy_document" "sftp_outbound_transfer_sqs_dlq" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  statement {
    sid       = "AllowSendingMessages"
    actions   = ["sqs:GetQueueUrl", "sqs:SendMessage"]
    resources = [one(aws_sqs_queue.sftp_outbound_transfer_dlq[*].arn)]
  }
}

resource "aws_iam_policy" "sftp_outbound_transfer_sqs_dlq" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-sqs-dlq"
  path = local.iam_path
  description = join("", [
    "Allows the ${local.outbound_lambda_full_name} to push events into its DLQ upon any failures"
  ])
  policy = one(data.aws_iam_policy_document.sftp_outbound_transfer_sqs_dlq[*].json)
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
    logs    = one(aws_iam_policy.sftp_outbound_transfer_logs[*].arn),
    ssm     = one(aws_iam_policy.sftp_outbound_transfer_ssm[*].arn),
    kms     = one(aws_iam_policy.sftp_outbound_transfer_kms[*].arn),
    s3      = one(aws_iam_policy.sftp_outbound_transfer_s3[*].arn),
    sqs_dlq = one(aws_iam_policy.sftp_outbound_transfer_sqs_dlq[*].arn)
    sns     = one(aws_iam_policy.sftp_outbound_transfer_sns[*].arn)
    vpc     = data.aws_iam_policy.lambda_vpc_access.arn
  } : {}

  role       = one(aws_iam_role.sftp_outbound_transfer[*].name)
  policy_arn = each.value
}
