data "external" "rds" {
  program = [
    "${path.module}/scripts/rds-cluster-config.sh", # helper script
    data.aws_rds_cluster.rds.cluster_identifier     # verified, positional argument to script
  ]
}

data "aws_caller_identity" "current" {}

# the following logic produces a map of ami filters to their filter values:
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
  name_regex  = ".+-${local.service}-.+"

  dynamic "filter" {
    for_each = local.filters
    content {
      name   = filter.key
      values = [filter.value]
    }
  }
}


data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}

data "aws_kms_key" "mgmt_config_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}

data "aws_kms_key" "config_cmk" {
  key_id = local.kms_config_key_alias
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnet" "main" {
  vpc_id            = data.aws_vpc.main.id
  availability_zone = data.external.rds.result["WriterAZ"]
  filter {
    name   = "tag:Layer"
    values = [local.layer]
  }
}

data "aws_security_group" "vpn" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.nonsensitive_common_config["vpn_security_group"]]
  }
}

data "aws_security_group" "enterprise_tools" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.enterprise_tools_security_group]
  }
}

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-aurora-cluster"]
  }
}

# These data sources aren't very useful, but they do verify inputs to terraform
# ensuring that the resources exist before offered to the configuration
data "aws_key_pair" "main" {
  key_name = local.key_pair
}

data "aws_rds_cluster" "rds" {
  cluster_identifier = local.rds_cluster_identifier
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}

data "aws_iam_policy" "ec2_instances_tags_ro" {
  name = "bfd-mgmt-ec2-instances-tags-ro"
}
