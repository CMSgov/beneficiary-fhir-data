locals {
  config_key_name = "bfd-platform-config-cmk"
}

resource "aws_kms_key" "config" {
  policy                             = data.aws_iam_policy_document.default_kms_key_policy.json
  description                        = "platform primary config CMK; used for configuration encryption"
  multi_region                       = true
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "config" {
  name          = "alias/${local.config_key_name}"
  target_key_id = one(aws_kms_key.config[*].arn)
}

resource "aws_kms_replica_key" "config_alt" {
  provider = aws.secondary

  description                        = "platform replica config CMK; used for configuration encryption"
  primary_key_arn                    = one(aws_kms_key.config[*].arn)
  bypass_policy_lockout_safety_check = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "config_alt" {
  provider = aws.secondary

  name          = "alias/${local.config_key_name}"
  target_key_id = one(aws_kms_replica_key.config_alt[*].arn)
}
