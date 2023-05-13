locals {

  default_tags = {
    Environment    = local.seed_env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/base"
  }

  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", local.env))])

  is_ephemeral_env = local.env != local.seed_env
  kms_key_alias    = "alias/bfd-${local.seed_env}-cmk"
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
