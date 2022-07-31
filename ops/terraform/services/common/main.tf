provider "aws" {
  region = "us-east-1"
}

locals {
  env     = terraform.workspace
  service = "common"

  shared_tags = {
    Environment = local.env
    application = "bfd"
    business    = "oeda"
    stack       = local.env
  }

  # Two-step map creation and redefinition creates `config` and `secret` maps of simplified parameter names to values
  config_map  = zipmap(data.aws_ssm_parameters_by_path.ns.names, nonsensitive(data.aws_ssm_parameters_by_path.ns.values))
  config      = { for key, value in local.config_map : split("/", key)[5] => value }
  secrets_map = zipmap(data.aws_ssm_parameters_by_path.s.names, data.aws_ssm_parameters_by_path.s.values)
  secrets     = { for key, value in local.secrets_map : split("/", key)[5] => value }

  # Supports custom, YAML-encoded, environment-specific parameter groups
  parameter_group_parameters_file = fileexists("${path.module}/db-parameter-group-parameters/${local.env}.yaml") ? "${path.module}/db-parameter-group-parameters/${local.env}.yaml" : "${path.module}/db-parameter-group-parameters/default.yaml"
  db_parameters                   = toset(yamldecode(file(local.parameter_group_parameters_file)))


  # Security Group SSM lookups
  enterprise_tools_security_group = local.config["enterprise_tools_security_group"]
  management_security_group       = local.config["management_security_group"]
  vpn_security_group              = local.config["vpn_security_group"]

  # RDS configuration SSM lookups
  rds_aurora_family                       = lookup(local.config, "rds_aurora_family", "aurora-postgresql14")
  rds_backup_retention_period             = lookup(local.config, "rds_backup_retention_period", 21) # TODO: 21 is probably a bad default. Fix this.
  rds_cluster_identifier                  = local.config["rds_cluster_identifier"]
  rds_iam_database_authentication_enabled = lookup(local.config, "rds_iam_database_authentication_enabled", false) # TODO: someday
  rds_instance_class                      = local.config["rds_instance_class"]
  rds_instance_count                      = local.config["rds_instance_count"]
  rds_master_password                     = lookup(local.secrets, "rds_master_password", null)
  rds_master_username                     = lookup(local.config, "rds_master_username", null)
  rds_snapshot_identifier                 = lookup(local.config, "rds_snapshot_identifier", null)

  # General SSM lookups
  kms_key_alias = local.config["kms_key_alias"]
  vpc_name      = local.config["vpc_name"]
}

data "aws_availability_zones" "main" {}

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

data "aws_ssm_parameters_by_path" "ns" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive"
}

data "aws_ssm_parameters_by_path" "s" {
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
