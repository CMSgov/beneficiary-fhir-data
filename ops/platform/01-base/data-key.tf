locals {
  data_key_name = "bfd-platform-data-cmk"
}

resource "aws_kms_key" "data" {
  policy                             = data.aws_iam_policy_document.default_kms_key_policy.json
  description                        = "platform primary; used for general encryption"
  multi_region                       = true
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "data" {
  name          = "alias/${local.data_key_name}"
  target_key_id = one(aws_kms_key.data[*].arn)
}

resource "aws_kms_replica_key" "data_alt" {
  provider = aws.secondary

  description                        = "platform replica; used for general encryption"
  primary_key_arn                    = one(aws_kms_key.data[*].arn)
  bypass_policy_lockout_safety_check = false

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "data_alt" {
  provider = aws.secondary

  name          = "alias/${local.data_key_name}"
  target_key_id = one(aws_kms_replica_key.data_alt[*].arn)
}


