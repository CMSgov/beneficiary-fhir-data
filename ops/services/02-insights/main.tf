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
  relative_module_root = "ops/services/02-insights"
}

locals {
  service = "insights"

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
}

# Temporary placeholder for Insights Bucket structure to enable applying server-insights.
# TODO: Make this Terraservice more useful
module "bucket_insights_bfd" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = "bfd-${local.env}-insights-bfd"
  force_destroy      = local.is_ephemeral_env

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bucket"
}
