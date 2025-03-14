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
  name_regex  = ".+-${local.legacy_service}-.+"

  dynamic "filter" {
    for_each = local.filters
    content {
      name   = filter.key
      values = [filter.value]
    }
  }
}

data "aws_kms_key" "mgmt_config_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}

data "aws_kms_key" "cmk" {
  key_id = local.nonsensitive_common_config["kms_key_alias"]
}

data "aws_kms_key" "config_cmk" {
  key_id = local.nonsensitive_common_config["kms_config_key_alias"]
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

data "aws_security_groups" "rds" {
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

data "aws_rds_cluster" "rds" {
  cluster_identifier = local.db_cluster_identifier
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_shared" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive/shared"
}

data "aws_ssm_parameters_by_path" "sensitive_ccw" {
  path            = "/bfd/${local.env}/${local.service}/sensitive/ccw"
  with_decryption = true
}

data "aws_ssm_parameters_by_path" "nonsensitive_ccw" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive/ccw"
}

data "aws_ssm_parameters_by_path" "nonsensitive_rda" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive/rda"
}

data "aws_ssm_parameter" "verifier_enabled" {
  name = "/bfd/${local.env}/${local.service}/nonsensitive/ccw/slo/weekend_data_availability/verifier/enabled"
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
data "aws_sns_topic" "bfd_test_slack_alarm" {
  count = local.is_ephemeral_env ? 0 : 1
  name  = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-test"
}

# TODO: this needs to be defined in common
data "aws_sns_topic" "bfd_notices_slack_alarm" {
  count = local.is_ephemeral_env ? 0 : 1
  name  = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-notices"
}

data "aws_iam_policy" "ec2_instance_tags_ro" {
  name = "bfd-mgmt-ec2-instance-tags-ro"
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
