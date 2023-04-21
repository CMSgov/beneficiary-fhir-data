# account number
data "aws_caller_identity" "current" {}

# vpc
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpc"]
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

# dns
data "aws_route53_zone" "local_zone" {
  name         = "bfd-${var.env_config.env}.local"
  private_zone = true
}

# s3 buckets
data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${var.env_config.env}-logs-${data.aws_caller_identity.current.account_id}"
}

# cloudwatch topics
data "aws_sns_topic" "cloudwatch_alarms" {
  name = "bfd-${var.env_config.env}-cloudwatch-alarms"
}
data "aws_sns_topic" "cloudwatch_ok" {
  name = "bfd-${var.env_config.env}-cloudwatch-ok"
}

# aurora security group
data "aws_security_group" "aurora_cluster" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-aurora-cluster"]
  }
}

# vpn security group
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpn-private"]
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
    values = ["bfd-${var.env_config.env}-enterprise-tools"]
  }
}

# management security group
data "aws_security_group" "remote" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-remote-management"]
  }
}

# ansible vault pw read only policy
data "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:policy/bfd-ansible-vault-pw-ro-s3"
}


