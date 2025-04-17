module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = "server"
  relative_module_root = "ops/services/server"
}

locals {
  service                  = module.terraservice.service
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

  azs            = ["us-east-1a", "us-east-1b", "us-east-1c"]
  app_subnet_ids = [for _, v in data.aws_subnet.app_subnets : v.id]
  dmz_subnet_ids = [for _, v in data.aws_subnet.dmz_subnets : v.id]

  # TODO: Remove "ecs" from the name prefix once we accept this as the new server service
  name_prefix = "bfd-${local.env}-${local.service}-ecs"

  green_state = "green"
  blue_state  = "blue"
}
