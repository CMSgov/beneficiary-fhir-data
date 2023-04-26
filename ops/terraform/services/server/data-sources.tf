# account number
data "aws_caller_identity" "current" {}

# vpc
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
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
  count = length(local.vpc_peerings)
  tags  = { Name = local.vpc_peerings[count.index] }
}

# s3 buckets
data "aws_s3_bucket" "admin" {
  bucket = "bfd-${local.env}-admin-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${local.env}-logs-${data.aws_caller_identity.current.account_id}"
}

# aurora security group
data "aws_security_group" "aurora_cluster" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-aurora-cluster"]
  }
}

# vpn security group
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpn-private"]
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
    values = ["bfd-${local.env}-enterprise-tools"]
  }
}

# management security group
data "aws_security_group" "remote" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-remote-management"]
  }
}

# ansible vault pw read only policy
data "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:policy/bfd-ansible-vault-pw-ro-s3"
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_service" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}
