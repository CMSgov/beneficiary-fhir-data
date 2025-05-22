locals {
  # The below IAM Roles may not exist in an account that has not enabled AutoScaling or RDS
  # AutoScaling, so we need to enable their policy fragements dynamically
  autoscaling_iam_role_arn     = one(data.aws_iam_roles.autoscaling.arns)
  rds_autoscaling_iam_role_arn = one(data.aws_iam_roles.rds_autoscaling.arns)
  cpm_iam_role_arn             = one(data.aws_iam_roles.cpm.arns)
}

data "aws_iam_roles" "autoscaling" {
  name_regex  = "AWSServiceRoleForAutoScaling"
  path_prefix = "/aws-service-role/autoscaling.amazonaws.com/"
}

data "aws_iam_roles" "rds_autoscaling" {
  name_regex  = "AWSServiceRoleForApplicationAutoScaling_RDSCluster"
  path_prefix = "/aws-service-role/rds.application-autoscaling.amazonaws.com/"
}

data "aws_iam_roles" "cpm" {
  name_regex = "aws-cpm-assume-roleV2"
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

# Key policy statements specific to the alt region data keys. Specifically, these statements allow the CPM service to
# continue being able to backup and restore data in the event of a failover. If/when we start managing our own backups,
# this can be deprecated.
data "aws_iam_policy_document" "data_alt" {
  count = local.cpm_iam_role_arn != null ? 1 : 0

  # Allow CPM to use the key to encrypt/decrypt data
  statement {
    sid    = "AllowCpmKeyUsage"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.cpm_iam_role_arn]
    }
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
      "kms:ListAliases",
      "kms:ListKeys",
    ]
    resources = ["*"]
  }

  # Allow CPM to create and revoke grants for the key
  statement {
    sid    = "AllowCpmGrantUsage"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = [local.cpm_iam_role_arn]
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

data "aws_iam_policy_document" "primary_data_key_policy_combined" {
  source_policy_documents = concat(
    [
      data.aws_iam_policy_document.default_kms_key_policy.json,
      data.aws_iam_policy_document.data.json
    ],
    data.aws_iam_policy_document.autoscaling[*].json,
    data.aws_iam_policy_document.rds_autoscaling[*].json
  )
}

data "aws_iam_policy_document" "alt_data_key_policy_combined" {
  source_policy_documents = concat(
    [
      data.aws_iam_policy_document.default_kms_key_policy.json,
      data.aws_iam_policy_document.data.json,
    ],
    data.aws_iam_policy_document.autoscaling[*].json,
    data.aws_iam_policy_document.rds_autoscaling[*].json,
    data.aws_iam_policy_document.data_alt[*].json
  )
}

# Environment data keys for the current region. Note: During a failover event, the alt keys and aliases will need to be
# imported (`terraform import aws_kms_key.data["test"] <key_arn>`, terraform import aws_kms_alias.data["prod"]
# <key_arn>, etc..).Alternatively you can import by  enclosing the resource address in single quotes imported
# (`terraform import 'aws_kms_key.data["test"]' <key_arn>`,
# terraform import 'aws_kms_alias.data["prod"]' <key_arn>, etc..).
resource "aws_kms_key" "data" {
  count = local.is_ephemeral_env ? 0 : 1

  description                        = "Data key for the ${local.region} ${local.env} environment."
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = local.kms_default_deletion_window_days

  policy = data.aws_iam_policy_document.primary_data_key_policy_combined.json

  lifecycle {
    prevent_destroy = true
  }
}

# key aliases for data protection
resource "aws_kms_alias" "data" {
  count = local.is_ephemeral_env ? 0 : 1

  name          = "alias/bfd-${local.env}-cmk"
  target_key_id = one(aws_kms_key.data[*].arn)
}

# Alt region keys. Note: These keys were originally created by the CPM team during the initial setup of BFD. We imported
# them into our terraform state (12/2023) so we can manage them going forward.
resource "aws_kms_key" "data_alt" {
  provider = aws.secondary
  count    = local.is_ephemeral_env ? 0 : 1

  description                        = "Data key for ${local.dr_region} ${local.env} environment"
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = local.kms_default_deletion_window_days

  policy = data.aws_iam_policy_document.alt_data_key_policy_combined.json

  lifecycle {
    prevent_destroy = true
  }
}

# alias
resource "aws_kms_alias" "data_alt" {
  provider = aws.secondary
  count    = local.is_ephemeral_env ? 0 : 1

  name          = "alias/bfd-${local.env}-cmk"
  target_key_id = one(aws_kms_key.data_alt[*].arn)
}
