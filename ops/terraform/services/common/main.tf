locals {
  env            = terraform.workspace
  environment    = coalesce(local.seed_env, local.env)
  service        = "common"
  legacy_service = "admin"
  layer          = "data"

  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name

  # NOTE: AWS Account Roots for Access Log Delivery
  # https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html
  aws_classic_loadbalancer_account_roots = {
    us-east-1 = "arn:aws:iam::127311923021:root"
    us-west-2 = "arn:aws:iam::797873946194:root"
  }

  # ephemeral environment determination is based on the existence of the ephemeral_environment_seed in the common hierarchy
  seed_env         = lookup(local.nonsensitive_config, "ephemeral_environment_seed", null)
  is_ephemeral_env = local.seed_env == null ? false : true

  # TODO: support ephemeral environments... which bucket should the ephemeral environment use for its admin bucket?
  admin_bucket   = "bfd-${local.env}-admin-${local.account_id}"
  logging_bucket = "bfd-${local.env}-logs-${local.account_id}"

  default_tags = {
    Environment    = local.env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/common"
  }

  # Two-step map creation and redefinition creates `config` and `secret` maps of simplified parameter names to values
  nonsensitive_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive.values))
  nonsensitive_config = { for key, value in local.nonsensitive_map : split("/", key)[5] => value }
  sensitive_map       = zipmap(data.aws_ssm_parameters_by_path.sensitive.names, data.aws_ssm_parameters_by_path.sensitive.values)
  sensitive_config    = { for key, value in local.sensitive_map : split("/", key)[5] => value }

  # Supports custom, YAML-encoded, environment-specific parameter groups
  db_cluster_parameter_group_file = fileexists("${path.module}/db-cluster-parameters/${local.env}.yaml") ? "${path.module}/db-cluster-parameters/${local.env}.yaml" : "${path.module}/db-cluster-parameters/default-${local.rds_aurora_family}.yaml"
  db_node_parameter_group_file    = fileexists("${path.module}/db-node-parameters/${local.env}.yaml") ? "${path.module}/db-node-parameters/${local.env}.yaml" : "${path.module}/db-node-parameters/default-${local.rds_aurora_family}.yaml"
  db_cluster_parameters           = toset(yamldecode(file(local.db_cluster_parameter_group_file)))
  db_parameters                   = toset(yamldecode(file(local.db_node_parameter_group_file)))

  # Security Group SSM lookups
  enterprise_tools_security_group = local.nonsensitive_config["enterprise_tools_security_group"]
  management_security_group       = local.nonsensitive_config["management_security_group"]
  vpn_security_group              = local.nonsensitive_config["vpn_security_group"]

  # RDS configuration SSM lookups
  rds_aurora_family                       = local.nonsensitive_config["rds_aurora_family"]
  rds_backup_retention_period             = local.nonsensitive_config["rds_backup_retention_period"]
  rds_cluster_identifier                  = local.nonsensitive_config["rds_cluster_identifier"]
  rds_iam_database_authentication_enabled = local.nonsensitive_config["rds_iam_database_authentication_enabled"]
  rds_instance_class                      = local.nonsensitive_config["rds_instance_class"]
  rds_instance_count                      = local.nonsensitive_config["rds_instance_count"]
  rds_master_password                     = lookup(local.sensitive_config, "rds_master_password", null)
  rds_master_username                     = lookup(local.nonsensitive_config, "rds_master_username", null)
  rds_snapshot_identifier                 = lookup(local.nonsensitive_config, "rds_snapshot_identifier", null)

  # General SSM lookups
  kms_key_alias = local.nonsensitive_config["kms_key_alias"]
  kms_key_id    = data.aws_kms_key.cmk.arn
  vpc_name      = local.nonsensitive_config["vpc_name"]
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

data "aws_ssm_parameters_by_path" "nonsensitive" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}

data "aws_ssm_parameters_by_path" "sensitive" {
  path            = "/bfd/${local.env}/${local.service}/sensitive"
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

data "aws_security_group" "tools" {
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
