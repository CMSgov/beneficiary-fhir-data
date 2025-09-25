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
  relative_module_root = "ops/services/05-server-insights"
}

locals {
  service        = "server-insights"
  target_service = "server"

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
  azs                      = keys(module.terraservice.default_azs)

  project              = "bfd"
  full_name            = !var.greenfield ? "bfd-insights-${local.project}-${local.env}" : "bfd-${local.env}-insights-${local.project}"
  full_name_underscore = replace(local.full_name, "-", "_")
  database             = local.full_name
  tags = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = local.project
    environment = local.env
  }
}
