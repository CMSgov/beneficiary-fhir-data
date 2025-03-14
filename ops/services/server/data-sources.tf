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

data "aws_vpc" "mgmt" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
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

# NLBs must exist in the dmz subnets. This is especially important for prod-sbx as those subnets
# have an IGW in their routing tables
data "aws_subnet" "dmz_subnets" {
  for_each          = toset(local.azs)
  vpc_id            = data.aws_vpc.main.id
  availability_zone = each.key
  filter {
    name   = "tag:Layer"
    values = ["dmz"]
  }
}

data "aws_vpc_peering_connection" "peers" {
  for_each = nonsensitive(toset(jsondecode(local.ssm_config["/bfd/${local.service}/lb_vpc_peerings_json"])))

  tags = {
    Name = each.key
  }
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_ec2_managed_prefix_list" "jenkins" {
  filter {
    name   = "prefix-list-name"
    values = ["bfd-cbc-jenkins"]
  }
}
