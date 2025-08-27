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

