locals {
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  seed_env         = module.terraservice.seed_env
  is_ephemeral_env = module.terraservice.is_ephemeral_env
  kms_key_alias    = "alias/bfd-${local.seed_env}-cmk"
}

module "terraservice" {
  source = "../_modules/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/base"
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
