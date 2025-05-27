module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/05-server-metrics"
}

locals {
  service = "server-metrics"

  region             = module.terraservice.region
  account_id         = module.terraservice.account_id
  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  seed_env           = module.terraservice.seed_env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release
  ssm_config         = module.terraservice.ssm_config

  target_service = "server"
  # TODO: Remove ecs suffix part when server is fully migrated to Fargate
  namespace = "bfd-${local.env}/${local.target_service}/ecs"
}
