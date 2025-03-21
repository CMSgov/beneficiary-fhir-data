module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/server"
}

locals {
  service    = "server"
  region     = data.aws_region.current.name
  account_id = data.aws_caller_identity.current.account_id

  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  seed_env           = module.terraservice.seed_env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release
  ssm_config         = module.terraservice.ssm_config

  azs            = ["us-east-1a", "us-east-1b", "us-east-1c"]
  app_subnet_ids = [for _, v in data.aws_subnet.app_subnets : v.id]
  dmz_subnet_ids = [for _, v in data.aws_subnet.dmz_subnets : v.id]

  kms_key_alias        = nonsensitive(local.ssm_config["/bfd/common/kms_key_alias"])
  kms_config_key_alias = nonsensitive(local.ssm_config["/bfd/common/kms_config_key_alias"])

  # TODO: Remove "ecs" from the name prefix once we accept this as the new server service
  name_prefix = "bfd-${local.env}-${local.service}-ecs"

  green_state = "green"
  blue_state  = "blue"

  cloudtamer_iam_path = "/delegatedadmin/developer/"
}
