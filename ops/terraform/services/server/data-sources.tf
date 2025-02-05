# account number
data "aws_caller_identity" "current" {}

# vpc
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

# mgmt vpc
data "aws_vpc" "mgmt" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

# peerings
data "aws_vpc_peering_connection" "peers" {
  count = length(local.lb_vpc_peerings)
  tags  = { Name = local.lb_vpc_peerings[count.index] }
}

# The following logic produces a map of ami filters to their filter values:
# `{"image-id" => "ami-?????????????????"}` when the var.ami_id_override is provided
# `{"tag:Branch" => local.latest_bfd_release}` when the var.ami_id_override is not provided
locals {
  filters = { for k, v in {
    "image-id" = var.ami_id_override,
    "tag:Branch" = var.ami_id_override == null ? local.latest_bfd_release : null } : k => v if v != null
  }
}

data "aws_ami" "main" {
  most_recent = true
  owners      = ["self"]
  name_regex  = ".+-${local.legacy_service}-.+"

  dynamic "filter" {
    for_each = local.filters
    content {
      name   = filter.key
      values = [filter.value]
    }
  }
}

# aurora security group
data "aws_security_groups" "aurora_cluster" {
  filter {
    name = "tag:Name"
    values = toset([
      local.db_cluster_identifier,
      "bfd-${local.seed_env}-aurora-cluster"
    ])
  }
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }
}

# vpn security group
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = [local.vpn_security_group]
  }
}

# get vpn cidr blocks from shared CMS prefix list
data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

# get cbc jenkins cidr block
data "aws_ec2_managed_prefix_list" "jenkins" {
  filter {
    name   = "prefix-list-name"
    values = ["bfd-cbc-jenkins"]
  }
}

# tools security group
data "aws_security_group" "tools" {
  filter {
    name   = "tag:Name"
    values = [local.enterprise_tools_security_group]
  }
}

# management security group
data "aws_security_group" "remote" {
  filter {
    name   = "tag:Name"
    values = [local.management_security_group]
  }
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_service" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}
