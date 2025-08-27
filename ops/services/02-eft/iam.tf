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
