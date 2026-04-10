terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  service              = local.service
  relative_module_root = "ops/services/000-bootstrap"
  lookup_kms_key       = false
}

locals {
  service = "bootstrap"

  env = "platform"

  region               = module.terraservice.region
  account_id           = module.terraservice.account_id
  default_tags         = module.terraservice.default_tags
  iam_path             = module.terraservice.default_iam_path
  permissions_boundary = module.terraservice.default_permissions_boundary_arn
  bfd_version          = module.terraservice.bfd_version

  envs_per_acc = {
    non-prod = ["test", "platform"]
    prod     = ["sandbox", "prod", "platform"]
  }
  envs = local.envs_per_acc[local.account_type]
}
