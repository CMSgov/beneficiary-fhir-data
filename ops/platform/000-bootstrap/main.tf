terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/000-bootstrap"
  lookup_kms_key       = false
}

locals {
  service = "bootstrap"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags

  envs_per_acc = {
    non-prod = ["test", "platform"]
    prod     = ["sandbox", "prod-sbx", "platform"]
  }
  envs = local.envs_per_acc[local.account_type]
}
