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

  service              = local.service
  relative_module_root = "ops/services/03-adot"
  subnet_layers = ["private"]
}

locals {
  service      = "adot"

  region       = module.terraservice.region
  env          = module.terraservice.env
  full_name    = "bfd-${local.env}-${local.service}"
  subnets = module.terraservice.subnets_map["private"]
  name_prefix  = "bfd-${local.env}-${local.service}"
  vpc          = module.terraservice.vpc
}
