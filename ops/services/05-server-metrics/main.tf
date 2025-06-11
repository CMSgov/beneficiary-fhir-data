terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/05-server-metrics"
}

locals {
  service = "server-metrics"

  region           = module.terraservice.region
  account_id       = module.terraservice.account_id
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  is_ephemeral_env = module.terraservice.is_ephemeral_env
  ssm_config       = module.terraservice.ssm_config

  target_service = "server"
  namespace      = !var.greenfield ? "bfd-${local.env}/${local.target_service}/ecs" : "bfd-${local.env}/${local.target_service}"
}
