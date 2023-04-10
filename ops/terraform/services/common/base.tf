# base.tf contains commonly shared data and resources that may be used across multiple services. This should be copied
# into each service's directory and modified (comment/uncomment) as needed. Each service should define a few mandatory
# locals in their main.tf:
#   layer: the layer of the service (e.g. data, app, etc)
#   service: the name of the service
#   stack: append service/role to the base_stack (ie `bfd-prod`, `bfd-2332-test`). E.g. `stack = "${local.base_stack}-foo"`
locals {
  account_id       = data.aws_caller_identity.current.account_id
  region           = data.aws_region.current.name
  env              = terraform.workspace
  data_env         = data.terraform_remote_state.base.outputs.default_tags["Environment"]
  base_stack       = data.terraform_remote_state.base.outputs.default_tags["stack"]
  is_ephemeral_env = lookup(data.terraform_remote_state.base.outputs.default_tags, "Ephemeral", false)

  tags = {
    Layer          = local.layer
    tf_module_root = "ops/terraform/services/${local.service}"
  }
  default_tags = merge(data.terraform_remote_state.base.outputs.default_tags, local.tags)

  # Two-step map creation and redefinition creates `config` and `secret` maps of simplified parameter names to values
  # TODO: consider renaming to something like `base_sensitive_config`, `common_nonsensitive_config`, etc
  nonsensitive_base_map      = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_base.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_base.values))
  nonsensitive_base_config   = { for key, value in local.nonsensitive_base_map : split("/", key)[5] => value }
  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : split("/", key)[5] => value }
  sensitive_base_map         = zipmap(data.aws_ssm_parameters_by_path.sensitive_base.names, data.aws_ssm_parameters_by_path.sensitive_base.values)
  sensitive_base_config      = { for key, value in local.sensitive_base_map : split("/", key)[5] => value }

  # Security Group SSM lookups
  enterprise_tools_security_group = local.nonsensitive_common_config["enterprise_tools_security_group"]
  management_security_group       = local.nonsensitive_common_config["management_security_group"]
  vpn_security_group              = local.nonsensitive_common_config["vpn_security_group"]

  # General SSM lookups
  # TODO: consider renaming to `env_key_*` to clarify its purpose
  kms_key_alias = local.nonsensitive_common_config["kms_key_alias"]
  kms_key_id    = data.aws_kms_key.cmk.arn # TODO: is this right? id vs arn?
  vpc_name      = local.nonsensitive_common_config["vpc_name"]
}

data "aws_availability_zones" "main" {}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnet" "data" {
  count             = 3
  vpc_id            = data.aws_vpc.main.id
  availability_zone = data.aws_availability_zones.main.names[count.index]

  filter {
    name   = "tag:Layer"
    values = ["data"]
  }
}

data "aws_ssm_parameters_by_path" "nonsensitive_base" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}

data "aws_ssm_parameters_by_path" "sensitive_base" {
  path            = "/bfd/${local.env}/${local.service}/sensitive"
  with_decryption = true
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_ssm_parameters_by_path" "sensitive_common" {
  path            = "/bfd/${local.env}/common/sensitive"
  with_decryption = true
}

data "aws_security_group" "vpn" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.vpn_security_group]
  }
}

data "aws_security_group" "management" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.management_security_group]
  }
}

data "aws_security_group" "enterprise_tools" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.enterprise_tools_security_group]
  }
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}

data "aws_iam_role" "monitoring" {
  name = "rds-monitoring-role"
}

data "terraform_remote_state" "base" {
  backend = "s3"

  config = {
    bucket         = "bfd-tf-state"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "bfd-tf-table"
    key            = "env:/${local.env}/services/base/terraform.tfstate"
    kms_key_id     = "alias/bfd-tf-state"
  }
}
