# Environment data keys for the current region. Note: During a failover event, the alt keys and aliases will need to be
# imported (`terraform import aws_kms_key.data_keys["test"] <key_arn>`, terraform import aws_kms_alias.data_keys["prod"]
# <key_arn>, etc..).Alternatively you can import by  enclosing the resource address in single quotes imported
# (`terraform import 'aws_kms_key.data_keys["test"]' <key_arn>`,
# terraform import 'aws_kms_alias.data_keys["prod"]' <key_arn>, etc..).
resource "aws_kms_key" "data_keys" {
  for_each = toset(concat(local.established_envs, ["mgmt"]))

  description                        = "Data key for the ${local.region} ${each.key} environment."
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = local.kms_default_deletion_window_days

  policy = data.aws_iam_policy_document.primary_data_key_policy_combined.json

  # Ensure mgmt definition doesn't conflict with identical default_tags with conditional
  tags = each.key != "mgmt" ? {
    Environment = each.key
    stack       = each.key
  } : {}

  lifecycle {
    prevent_destroy = true
  }
}

# key aliases for data protection
resource "aws_kms_alias" "data_keys" {
  for_each = toset(concat(local.established_envs, ["mgmt"]))

  name          = "alias/bfd-${each.key}-cmk"
  target_key_id = aws_kms_key.data_keys[each.key].arn
}

# Alt region keys. Note: These keys were originally created by the CPM team during the initial setup of BFD. We imported
# them into our terraform state (12/2023) so we can manage them going forward.
resource "aws_kms_key" "data_keys_alt" {
  provider = aws.alt
  for_each = toset(local.established_envs)

  description                        = "Data key for ${local.alt_region} ${each.key} environment"
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  deletion_window_in_days            = local.kms_default_deletion_window_days

  policy = data.aws_iam_policy_document.alt_data_key_policy_combined.json

  tags = {
    Environment = each.key
    stack       = each.key
  }

  lifecycle {
    prevent_destroy = true
  }
}

# alias
resource "aws_kms_alias" "data_keys_alt" {
  provider = aws.alt
  for_each = toset(local.established_envs)

  name          = "alias/bfd-${each.key}-cmk"
  target_key_id = aws_kms_key.data_keys_alt[each.key].arn
}

data "aws_iam_policy_document" "primary_data_key_policy_combined" {
  source_policy_documents = [
    local.kms_default_key_policy.json,
    data.aws_iam_policy_document.data_keys.json
  ]
}

data "aws_iam_policy_document" "alt_data_key_policy_combined" {
  source_policy_documents = [
    local.kms_default_key_policy.json,
    data.aws_iam_policy_document.data_keys.json,
    data.aws_iam_policy_document.data_keys_alt.json
  ]
}

# Key policy statements used by *both* the primary and alt region data keys. Please note: `resources = ["*"]` may seem
# like an overly permissive statement, but it's actually required for the key to be usable. This *only* applies to kms
# key policies not iam policies. See the following on key policy statements for more info:
#   - https://docs.aws.amazon.com/kms/latest/developerguide/key-policy-overview.html#key-policy-elements
data "aws_iam_policy_document" "data_keys" {
  # Allow our asg's to use the key to encrypt/decrypt data
  statement {
    sid    = "AllowAsgKeyUsage"
    effect = "Allow"
    principals {
      type = "AWS"
      identifiers = [
        "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling"
      ]
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
      type = "AWS"
      identifiers = [
        "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling"
      ]
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

  # Allow RDS app autoscaling to use the key to encrypt/decrypt data, specifically for Performance
  # Insights
  statement {
    sid    = "AllowRdsKeyUsage"
    effect = "Allow"
    principals {
      type = "AWS"
      identifiers = [
        "arn:aws:iam::${local.account_id}:role/aws-service-role/rds.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_RDSCluster"
      ]
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
      type = "AWS"
      identifiers = [
        "arn:aws:iam::${local.account_id}:role/aws-service-role/rds.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_RDSCluster"
      ]
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

  # Allow cloudwatch to use the key to decrypt data
  statement {
    sid    = "AllowCloudWatchKeyUsage"
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["cloudwatch.amazonaws.com","events.amazonaws.com"]
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
data "aws_iam_policy_document" "data_keys_alt" {
  # Allow CPM to use the key to encrypt/decrypt data
  statement {
    sid    = "AllowCpmKeyUsage"
    effect = "Allow"
    principals {
      type = "AWS"
      identifiers = [
        "arn:aws:iam::${local.account_id}:role/aws-cpm-assume-roleV2"
      ]
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
      type = "AWS"
      identifiers = [
        "arn:aws:iam::${local.account_id}:role/aws-cpm-assume-roleV2"
      ]
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
