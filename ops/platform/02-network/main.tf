terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.52.0" # TODO: Replace with "~> 6" when ECS is fixed
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  service              = local.service
  relative_module_root = "ops/platform/02-network"
}

locals {
  service = "network"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags
  kms_key_arn  = module.terraservice.key_arn
  ssm_config   = module.terraservice.ssm_config
}
