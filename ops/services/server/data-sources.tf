data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_kms_alias" "env_cmk" {
  name = local.kms_key_alias
}

data "aws_kms_alias" "env_config_cmk" {
  name = local.kms_config_key_alias
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

data "aws_subnet" "app_subnets" {
  for_each          = toset(local.azs)
  vpc_id            = data.aws_vpc.main.id
  availability_zone = each.key
  filter {
    name   = "tag:Layer"
    values = ["app"]
  }
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}
