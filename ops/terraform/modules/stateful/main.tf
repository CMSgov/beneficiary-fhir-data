## 
# Stateful resources for an environment and associated KMS needed by both stateful and stateless resources

locals {
  account_id = data.aws_caller_identity.current.account_id
  azs        = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env        = var.env
  env_config = { env = local.env, vpc_id = data.aws_vpc.main.id, zone_id = module.local_zone.zone_id }
}

data "aws_caller_identity" "current" {}
