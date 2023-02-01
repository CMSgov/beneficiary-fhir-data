data "external" "rds" {
  program = [
    "${path.module}/scripts/rds-cluster-config.sh", # helper script
    data.aws_rds_cluster.rds.cluster_identifier     # verified, positional argument to script
  ]
}

data "aws_caller_identity" "current" {}

data "aws_security_group" "vpn" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.vpn_security_group]
  }
}

data "aws_security_group" "enterprise_tools" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.enterprise_tools_security_group]
  }
}

# TODO: this is a temporary work-around until versioning becomes a reality
# the following logic produces a map of ami filters to their filter values:
# `{"image-id" => "ami-?????????????????"}` when the var.ami_id_override is provided
# `{"tag:Branch" => "master"}` when the var.ami_id_override is not provided
locals {
  filters = { for k, v in {
    "image-id" = var.ami_id_override,
    "tag:Branch" = var.ami_id_override == null ? "master" : null } : k => v if v != null
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

data "aws_kms_key" "cmk" {
  key_id = local.nonsensitive_common_config["kms_key_alias"]
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

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-aurora-cluster"]
  }
}

data "aws_rds_cluster" "rds" {
  cluster_identifier = local.nonsensitive_common_config["rds_cluster_identifier"]
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_shared" {
  path = "/bfd/${local.env}/${local.service}/shared/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_ccw" {
  path = "/bfd/${local.env}/${local.service}/ccw/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_rda" {
  path = "/bfd/${local.env}/${local.service}/rda/nonsensitive"
}

# TODO: this needs to be defined in common
data "aws_sns_topic" "alarm" {
  count = local.is_prod ? 1 : 0
  name  = "bfd-${local.env}-cloudwatch-alarms"
}

# TODO: this needs to be defined in common
data "aws_sns_topic" "ok" {
  count = local.is_prod ? 1 : 0
  name  = "bfd-${local.env}-cloudwatch-ok"
}

# TODO: this needs to be defined in common
# TODO: this will be replaced in BFD-2244
data "aws_sns_topic" "bfd_test_slack_alarm" {
  count = local.is_ephemeral_env ? 0 : 1
  name  = "bfd-${local.env}-cloudwatch-alarms-alert-testing"
}

# TODO: this needs to be defined in common
data "aws_sns_topic" "bfd_notices_slack_alarm" {
  count = local.is_ephemeral_env ? 0 : 1
  name  = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-notices"
}
