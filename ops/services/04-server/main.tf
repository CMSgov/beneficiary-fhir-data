terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/04-server"
  subnet_layers        = !var.greenfield ? ["app", "dmz"] : ["public", "private"]
}

locals {
  service = "server"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  app_subnets              = !var.greenfield ? module.terraservice.subnets_map["app"] : module.terraservice.subnets_map["private"]
  dmz_subnets              = !var.greenfield ? module.terraservice.subnets_map["dmz"] : module.terraservice.subnets_map["public"]
  azs                      = keys(module.terraservice.default_azs)

  app_subnet_ids = local.app_subnets[*].id
  dmz_subnet_ids = local.dmz_subnets[*].id

  name_prefix = !var.greenfield ? "bfd-${local.env}-${local.service}-ecs" : "bfd-${local.env}-${local.service}"

  green_state = "green"
  blue_state  = "blue"
}
