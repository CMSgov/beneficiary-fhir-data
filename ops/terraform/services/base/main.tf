module "terraservice" {
  source = "../_modules/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/base"
}

locals {
  account_id       = data.aws_caller_identity.current.account_id
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  seed_env         = module.terraservice.seed_env
  is_ephemeral_env = module.terraservice.is_ephemeral_env

  kms_key_alias = "alias/bfd-${local.seed_env}-config-cmk"
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
  # The KMS key ID/ARN is dependent on whether the current env is ephemeral or not: if it is, then
  # this Terraservice will use the seed env's config CMK instead of creating one specific to the
  # environment, otherwise, if the env is established, this Terraservice will encrypt all sensitive
  # SSM configuration with the primary key defined within this Terraservice
  ## BFD-3089
  ## Simplifying local definition due to changes below
  ## TODO: remove commentary after validation
  # kms_key_id = local.is_ephemeral_env ? one(data.aws_kms_key.cmk[*].arn) : one(aws_kms_key.primary[*].arn)
  kms_key_id = data.aws_kms_key.cmk.arn
  
  # Normal precedence. Values stored in YAML files.
  yaml_env = local.is_ephemeral_env ? "ephemeral" : local.env
  yaml     = data.external.yaml.result
}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  ## BFD-3089
  ## remove count structure
  ## TODO: remove comment after verification
  # count = local.is_ephemeral_env ? 1 : 0

  key_id = local.kms_key_alias
}

data "external" "yaml" {
  program = ["${path.module}/scripts/tf-decrypt-shim.sh"]
  query = {
    seed_env    = local.yaml_env
    env         = local.env
    kms_key_arn = local.kms_key_id
  }
}
## BFD-3089
## The following four (4) resources are now defined in MGMT for SDLC environments
## These resource definitions appear superfluous and have been commented out
## TODO: remove comment block entirely once validated
##
# resource "aws_kms_key" "primary" {
#   count = !local.is_ephemeral_env ? 1 : 0

#   policy                             = local.key_policy_template
#   description                        = "${local.kms_key_alias} primary; used for sensitive SSM configuration"
#   multi_region                       = true
#   enable_key_rotation                = true
#   bypass_policy_lockout_safety_check = false
# }

# resource "aws_kms_alias" "primary" {
#   count = !local.is_ephemeral_env ? 1 : 0

#   name          = local.kms_key_alias
#   target_key_id = one(aws_kms_key.primary[*].arn)
# }

# resource "aws_kms_replica_key" "secondary" {
#   provider = aws.secondary

#   count = !local.is_ephemeral_env ? 1 : 0

#   policy                             = local.key_policy_template
#   description                        = "${local.kms_key_alias} replica; used for sensitive SSM configuration"
#   primary_key_arn                    = one(aws_kms_key.primary[*].arn)
#   bypass_policy_lockout_safety_check = false
# }

# resource "aws_kms_alias" "secondary" {
#   provider = aws.secondary

#   count = !local.is_ephemeral_env ? 1 : 0

#   name          = local.kms_key_alias
#   target_key_id = one(aws_kms_replica_key.secondary[*].arn)
# }
