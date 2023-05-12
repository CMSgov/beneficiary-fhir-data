locals {

  default_tags = {
    Environment    = local.data_env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/base"
  }

  established_envs = ["test", "prod-sbx", "prod"]
  env              = terraform.workspace
  is_ephemeral_env = !(contains(local.established_envs, local.env))

  seed_env = local.is_ephemeral_env ? reverse(split("-", local.env))[0] : ""
  data_env = local.is_ephemeral_env ? local.seed_env : local.env
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
