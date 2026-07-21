terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.52.0" # TODO: Replace with "~> 6" when ECS is fixed
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  service              = local.service
  relative_module_root = "ops/services/05-server-ng-metrics"
}

locals {
  service = "server-ng-metrics"

  region           = module.terraservice.region
  account_id       = module.terraservice.account_id
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  is_ephemeral_env = module.terraservice.is_ephemeral_env
  ssm_config       = module.terraservice.ssm_config

  target_service = "server-ng"
  namespace      = "bfd-${local.env}/${local.target_service}"
}
