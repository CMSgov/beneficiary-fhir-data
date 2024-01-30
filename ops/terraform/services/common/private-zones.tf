## VPC Private Local Zone for CNAME Records
locals {
  vpc_id = data.aws_vpc.main.id
  public = false
}

module "local_zone" {
  count      = local.is_ephemeral_env ? 0 : 1
  source     = "./modules/bfd_common_private_zones"
  env_config = { env = local.env, vpc_id = data.aws_vpc.main.id }
}

