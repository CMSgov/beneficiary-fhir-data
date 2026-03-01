data "aws_iam_policy_document" "partner_bucket_assume" {
  for_each = local.partners_config

  dynamic "statement" {
    for_each = {
      for idx, assumer_arn in each.value.bucket_iam_assumer_arns : idx => assumer_arn
    }
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

data "aws_iam_policy_document" "partner_bucket_access" {
  for_each = local.partners_config

  statement {
    sid     = "AllowPartnersBenePrefsS3List${each.key}"
    actions = [
      "s3:ListBucket"
    ]
    resources = [
      module.buckets[each.key].bucket.arn
    ]
  }

  statement {
    sid     = "AllowPartnersBenePrefsS3ReadWrite${each.key}"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
    ]
    resources = [
        "${module.buckets[each.key].bucket.arn}${each.value.bucket_home_path}/*"
    ]
  }
}

resource "aws_iam_policy" "partner_bucket_access" {
  for_each = local.partners_config

  name        = "${local.name_prefix}-${each.key}-bucket-access-policy"
  path        = local.iam_path
  description = "Policy granting cross-account access to ${each.key} beneficiary prefs bucket when role is assumed"
  policy = data.aws_iam_policy_document.partner_bucket_access[each.key].json
}

resource "aws_iam_role" "partner_bucket_access" {
  for_each = local.partners_config

  name                  = "${local.name_prefix}-${each.key}-bucket-access-role"
  description           = "Cross-account role for ${each.key} to access bene-prefs bucket"
  path                  = local.iam_path
  assume_role_policy    = data.aws_iam_policy_document.partner_bucket_assume[each.key].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "partner_bucket_access" {
  for_each = local.partners_config

  role       = aws_iam_role.partner_bucket_access[each.key].name
  policy_arn = aws_iam_policy.partner_bucket_access[each.key].arn
}
