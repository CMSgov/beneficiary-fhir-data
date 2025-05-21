resource "aws_kms_key" "config" {
  count = local.is_ephemeral_env ? 0 : 1

  policy                             = data.aws_iam_policy_document.default_kms_key_policy.json
  description                        = "${local.env} primary config; used for sensitive SSM configuration"
  multi_region                       = true
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "config" {
  count = local.is_ephemeral_env ? 0 : 1

  name          = "alias/bfd-${local.env}-config-cmk"
  target_key_id = one(aws_kms_key.config[*].arn)
}

resource "aws_kms_replica_key" "config_alt" {
  provider = aws.secondary
  count    = local.is_ephemeral_env ? 0 : 1

  policy                             = data.aws_iam_policy_document.default_kms_key_policy.json
  description                        = "${local.env} config replica; used for sensitive SSM configuration"
  primary_key_arn                    = one(aws_kms_key.config[*].arn)
  bypass_policy_lockout_safety_check = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "config_alt" {
  provider = aws.secondary
  count    = local.is_ephemeral_env ? 0 : 1

  name          = "alias/bfd-${local.env}-config-cmk"
  target_key_id = one(aws_kms_replica_key.config_alt[*].arn)
}
