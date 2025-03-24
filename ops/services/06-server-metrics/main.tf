module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = "server-metrics"
  relative_module_root = "ops/services/server-metrics"
}

locals {
  service            = module.terraservice.service
  region             = module.terraservice.region
  account_id         = module.terraservice.account_id
  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  seed_env           = module.terraservice.seed_env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release
  ssm_config         = module.terraservice.ssm_config

  target_service = "server"
}
