locals {
  account_id = data.aws_caller_identity.current.account_id
  env        = "mgmt"
  default_tags = {
    Environment    = local.env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/env/mgmt/base_config"
  }

  kms_key_alias = "alias/bfd-${local.env}-config-cmk"

  key_policy_template = jsonencode(
    {
      Version = "2012-10-17",
      Id      = "config-key-policy",
      Statement = [
        # This policy statement is the default key policy allowing the account principal to delegate
        # permissions, via IAM policies, to other principals within the AWS Account. Due to this
        # delegation, it is not necessary to include additional IAM User/Group-specific policy
        # statements within this key policy, as that permission delegation can be handled via
        # distinct IAM Policies attached to those entities. See
        # https://docs.aws.amazon.com/kms/latest/developerguide/key-policy-default.html#key-policy-default-allow-root-enable-iam
        {
          Sid    = "Enable IAM User Permissions",
          Effect = "Allow",
          Principal = {
            AWS = "arn:aws:iam::${local.account_id}:root"
          },
          Action   = "kms:*",
          Resource = "*"
        }
      ]
    }
  )

  kms_key_id = aws_kms_key.primary.arn
}

resource "aws_kms_key" "primary" {
  policy                             = local.key_policy_template
  description                        = "${local.kms_key_alias} primary; used for sensitive SSM configuration"
  multi_region                       = true
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
}

resource "aws_kms_alias" "primary" {
  name          = local.kms_key_alias
  target_key_id = aws_kms_key.primary.arn
}

resource "aws_kms_replica_key" "secondary" {
  provider = aws.secondary

  policy                             = local.key_policy_template
  description                        = "${local.kms_key_alias} replica; used for sensitive SSM configuration"
  primary_key_arn                    = aws_kms_key.primary.arn
  bypass_policy_lockout_safety_check = false
}

resource "aws_kms_alias" "secondary" {
  provider = aws.secondary

  name          = local.kms_key_alias
  target_key_id = aws_kms_replica_key.secondary.arn
}
