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
  established_envs = [
    "test",
    "prod-sbx",
    "prod"
  ]

  kms_config_keys_envs = toset(concat(local.established_envs, [local.env]))
  kms_key_id           = aws_kms_key.primary_config[local.env].arn


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

  yaml = data.external.yaml.result
  common_sensitive = {
    for key, value in local.yaml
    : key => value if contains(split("/", key), "common") && value != "UNDEFINED"
  }
  cpm_sensitive = {
    for key, value in local.yaml
    : key => value if contains(split("/", key), "cpm") && value != "UNDEFINED"
  }
  jenkins_sensitive = {
    for key, value in local.yaml
    : key => value if contains(split("/", key), "jenkins") && value != "UNDEFINED"
  }
  quicksight_sensitive = {
    for key, value in local.yaml
    : key => value if contains(split("/", key), "quicksight") && value != "UNDEFINED"
  }
}

resource "aws_kms_key" "primary_config" {
  for_each = local.kms_config_keys_envs

  policy                             = local.key_policy_template
  description                        = "${each.value} primary config; used for sensitive SSM configuration"
  multi_region                       = true
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
}

resource "aws_kms_alias" "primary_config" {
  for_each = local.kms_config_keys_envs

  name          = "alias/bfd-${each.key}-config-cmk"
  target_key_id = aws_kms_key.primary_config[each.key].arn
}

resource "aws_kms_replica_key" "secondary_config" {
  for_each = local.kms_config_keys_envs

  provider = aws.secondary

  policy                             = local.key_policy_template
  description                        = "${each.value} config replica; used for sensitive SSM configuration"
  primary_key_arn                    = aws_kms_key.primary_config[each.value].arn
  bypass_policy_lockout_safety_check = false
}

resource "aws_kms_alias" "secondary_config" {
  for_each = local.kms_config_keys_envs

  provider = aws.secondary

  name          = "alias/bfd-${each.key}-config-cmk"
  target_key_id = aws_kms_replica_key.secondary_config[each.key].arn
}
##

resource "aws_ssm_parameter" "common_sensitive" {
  for_each = local.common_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

resource "aws_ssm_parameter" "cpm_sensitive" {
  for_each = local.cpm_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

resource "aws_ssm_parameter" "jenkins_sensitive" {
  for_each = local.jenkins_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

resource "aws_ssm_parameter" "quicksight_sensitive" {
  for_each = local.quicksight_sensitive

  key_id    = local.kms_key_id
  name      = each.key
  overwrite = true
  type      = "SecureString"
  value     = each.value
}

# BFD-3384
# CloudWatch Log Group for the "CI - Update OPS Infrastructure" Terraform plan/apply logs
resource "aws_cloudwatch_log_group" "gha_ci_ops_infra" {
  name       = "/bfd/${local.env}/gha/ci-ops-infra"
  kms_key_id = aws_kms_key.primary_config[local.env].arn
}
