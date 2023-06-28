locals {
  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", local.env))])
  is_ephemeral_env = !(contains(local.established_envs, local.env))

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


  # TODO: To finalize ephemeral environment support,
  # we need to clarify the logging and admin bucket relationships
  admin_bucket   = "bfd-${local.env}-admin-${local.account_id}"
  logging_bucket = "bfd-${local.env}-logs-${local.account_id}"

  default_tags = {
    Environment    = local.seed_env
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
  rds_cluster_identifier                  = coalesce(var.rds_cluster_identifier_override, local.nonsensitive_config["rds_cluster_identifier"])
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
