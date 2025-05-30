module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/05-server"
  subnet_layers        = ["app", "dmz"]
}

locals {
  service = "server"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  seed_env                 = module.terraservice.seed_env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  latest_bfd_release       = module.terraservice.latest_bfd_release
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_config_key_alias     = module.terraservice.env_config_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  env_config_key_arns      = module.terraservice.env_config_key_arns
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  app_subnets              = module.terraservice.subnets_map["app"]
  dmz_subnets              = module.terraservice.subnets_map["dmz"]
  azs                      = keys(module.terraservice.default_azs)

  app_subnet_ids = local.app_subnets[*].id
  dmz_subnet_ids = local.dmz_subnets[*].id

  # TODO: Remove "ecs" from the name prefix once we accept this as the new server service
  name_prefix = "bfd-${local.env}-${local.service}-ecs"

  green_state = "green"
  blue_state  = "blue"
}
