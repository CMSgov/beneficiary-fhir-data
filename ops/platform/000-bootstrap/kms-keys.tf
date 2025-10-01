locals {
  # The below IAM Roles may not exist in an account that has not enabled AutoScaling or RDS
  # AutoScaling, so we need to enable their policy fragements dynamically
  autoscaling_iam_role_arn         = one(data.aws_iam_roles.autoscaling.arns)
  rds_autoscaling_iam_role_arn     = one(data.aws_iam_roles.rds_autoscaling.arns)
  kms_default_deletion_window_days = 30
}

data "aws_iam_roles" "autoscaling" {
  name_regex  = "AWSServiceRoleForAutoScaling"
  path_prefix = "/aws-service-role/autoscaling.amazonaws.com/"
}

data "aws_iam_roles" "rds_autoscaling" {
  name_regex  = "AWSServiceRoleForApplicationAutoScaling_RDSCluster"
  path_prefix = "/aws-service-role/rds.application-autoscaling.amazonaws.com/"
}

data "aws_iam_policy_document" "autoscaling" {
  count = local.autoscaling_iam_role_arn != null ? 1 : 0

  # Allow our asg's to use the key to encrypt/decrypt data
  statement {
    sid    = "AllowAsgKeyUsage"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.autoscaling_iam_role_arn]
    }
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey",
      "kms:GenerateDataKeyWithoutPlaintext",
      "kms:DescribeKey",
    ]
    resources = ["*"]
  }

  # Allow our asg's to create and revoke grants for the key
  statement {
    sid    = "AllowAsgGrantUsage"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.autoscaling_iam_role_arn]
    }
    actions = [
      "kms:CreateGrant",
      "kms:ListGrants",
      "kms:RevokeGrant",
    ]
    condition {
      test     = "Bool"
      variable = "kms:GrantIsForAWSResource"
      values   = ["true"]
    }
    resources = ["*"]
  }
}

data "aws_iam_policy_document" "rds_autoscaling" {
  count = local.rds_autoscaling_iam_role_arn != null ? 1 : 0

  # Allow RDS app autoscaling to use the key to encrypt/decrypt data, specifically for Performance
  # Insights
  statement {
    sid    = "AllowRdsKeyUsage"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.rds_autoscaling_iam_role_arn]
    }
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey",
      "kms:GenerateDataKeyWithoutPlaintext",
      "kms:DescribeKey",
    ]
    resources = ["*"]
  }

  # Allow RDS app autoscaling to create and revoke grants for the key, specifically for Performance
  # Insights
  statement {
    sid    = "AllowRdsGrantUsage"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.rds_autoscaling_iam_role_arn]
    }
    actions = [
      "kms:CreateGrant",
      "kms:ListGrants",
      "kms:RevokeGrant",
    ]
    condition {
      test     = "Bool"
      variable = "kms:GrantIsForAWSResource"
      values   = ["true"]
    }
    resources = ["*"]
  }
}

# Key policy statements used by *both* the primary and alt region data keys. Please note: `resources = ["*"]` may seem
# like an overly permissive statement, but it's actually required for the key to be usable. This *only* applies to kms
# key policies not iam policies. See the following on key policy statements for more info:
#   - https://docs.aws.amazon.com/kms/latest/developerguide/key-policy-overview.html#key-policy-elements
data "aws_iam_policy_document" "data" {
  # Allow cloudwatch to use the key to decrypt data
  statement {
    sid    = "AllowCloudWatchKeyUsage"
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["cloudwatch.amazonaws.com"]
    }
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*",
    ]
    resources = ["*"]
  }

  # Allow CloudFront to use the key to decrypt data
  statement {
    sid    = "AllowCloudfrontKeyUsage"
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*",
    ]
    resources = ["*"]
  }

  statement {
    sid    = "AllowSNSKeyUsage"
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*",
    ]
    resources = ["*"]
  }

  statement {
    sid    = "AllowSQSKeyUsage"
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["sqs.amazonaws.com"]
    }
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey*",
    ]
    resources = ["*"]
  }

  # Allow cloudwatch logs to use the key to encrypt/decrypt data
  statement {
    sid    = "AllowCloudWatchLogsKeyUsage"
    effect = "Allow"
    principals {
      type = "Service"
      identifiers = [
        "logs.${local.region}.amazonaws.com"
      ]
    }
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt",
      "kms:GenerateDataKey",
      "kms:DescribeKey",
    ]
    resources = ["*"]
  }

  # Allow S3 to work with encrypted queues and topics
  statement {
    sid    = "AllowS3KeyUsage"
    effect = "Allow"
    principals {
      type = "Service"
      identifiers = [
        "s3.amazonaws.com"
      ]
    }
    actions = [
      "kms:GenerateDataKey",
      "kms:Decrypt",
    ]
    resources = ["*"]
  }

  statement {
    sid    = "AllowEventsKeyUsage"
    effect = "Allow"
    principals {
      type = "Service"
      identifiers = [
        "events.amazonaws.com"
      ]
    }
    actions = [
      "kms:GenerateDataKey*",
      "kms:Encrypt",
      "kms:Decrypt"
    ]
    resources = ["*"]
  }

  # Allow ECS Fargate to generate a data key and describe the key
  statement {
    sid    = "AllowECSFargateKeyUsage"
    effect = "Allow"
    principals {
      type = "Service"
      identifiers = [
        "fargate.amazonaws.com"
      ]
    }
    actions = [
      "kms:GenerateDataKeyWithoutPlaintext",
      "kms:DescribeKey",
    ]
    resources = ["*"]
  }

  # Allow ECS Fargate to create grants for a given key
  statement {
    sid    = "AllowECSFargateKeyGrants"
    effect = "Allow"
    principals {
      type = "Service"
      identifiers = [
        "fargate.amazonaws.com"
      ]
    }
    condition {
      test     = "ForAllValues:StringEquals"
      variable = "kms:GrantOperations"
      values   = ["Decrypt"]
    }
    actions   = ["kms:CreateGrant"]
    resources = ["*"]
  }
}

# Define a default key policy doc that allows our root account and admins to manage and delegate access to the key. This
# policy statement must be included in every kms key policy, whether you use this doc or not. Without it, our account
# and account admins will not be able to manage the key. See the following for more info on default key policies:
#  - https://docs.aws.amazon.com/kms/latest/developerguide/key-policies.html#key-policy-default
data "aws_iam_policy_document" "default_kms_key_policy" {
  statement {
    sid    = "AllowKeyManagement"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${local.account_id}:root"]
    }
    actions   = ["kms:*"]
    resources = ["*"]
  }
}

data "aws_iam_policy_document" "combined" {
  source_policy_documents = concat(
    [
      data.aws_iam_policy_document.default_kms_key_policy.json,
      data.aws_iam_policy_document.data.json
    ],
    data.aws_iam_policy_document.autoscaling[*].json,
    data.aws_iam_policy_document.rds_autoscaling[*].json
  )
}

# Environment keys for the current region. Note: During disaster recovery, the alt keys and aliases
# will need to be imported (`tofu import aws_kms_key.data_keys["test"] <key_arn>`, tofu import
# aws_kms_alias.data_keys["prod"] <key_arn>, etc..).Alternatively you can import by enclosing the
# resource address in single quotes imported (`tofu import 'aws_kms_key.data_keys["test"]'
# <key_arn>`, tofu import 'aws_kms_alias.data_keys["prod"]' <key_arn>, etc..).
resource "aws_kms_key" "primary" {
  for_each = toset(local.envs)

  description                        = "Primary key for ${local.region} ${each.key}."
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = local.kms_default_deletion_window_days

  policy = data.aws_iam_policy_document.combined.json

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "primary" {
  for_each = toset(local.envs)

  name          = "alias/bfd-${each.key}-cmk"
  target_key_id = aws_kms_key.primary[each.key].arn
}

resource "aws_kms_key" "secondary" {
  provider = aws.secondary
  for_each = toset(local.envs)

  description                        = "Secondary key for ${local.region} ${each.key}. Used in DR scenarios."
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = local.kms_default_deletion_window_days

  policy = data.aws_iam_policy_document.combined.json

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "secondary" {
  provider = aws.secondary
  for_each = toset(local.envs)

  name          = "alias/bfd-${each.key}-cmk"
  target_key_id = aws_kms_key.secondary[each.key].arn
}
