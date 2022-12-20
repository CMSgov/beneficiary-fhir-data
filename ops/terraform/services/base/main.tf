locals {
  env              = terraform.workspace
  is_ephemeral_env = !(contains(local.established_envs, local.env))

  established_envs = [
    "test",
    "mgmt",
    "prod-sbx",
    "prod"
  ]

  default_tags = {
    Environment    = local.env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/base"
  }
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
