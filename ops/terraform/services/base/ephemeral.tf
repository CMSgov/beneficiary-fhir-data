locals {
  # All seed environment hierarchies
  seed = local.is_ephemeral_env ? zipmap(data.aws_ssm_parameters_by_path.seed[0].names, data.aws_ssm_parameters_by_path.seed[0].values) : {}

  # Targeted COMMON hierarchy paths to be "copied" from the seed environment into requested ephemeral environment
  common_seed_paths = local.is_ephemeral_env ? {
    "/bfd/${local.env}/common/nonsensitive/rds_aurora_family"  = "/bfd/${local.seed_env}/common/nonsensitive/rds_aurora_family",
    "/bfd/${local.env}/common/nonsensitive/rds_instance_class" = "/bfd/${local.seed_env}/common/nonsensitive/rds_instance_class",
    "/bfd/${local.env}/common/nonsensitive/rds_instance_count" = "/bfd/${local.seed_env}/common/nonsensitive/rds_instance_count"
  } : {}

  # Targeted MIGRATOR hierarchy paths to be "copied" from the seed environment into requested ephemeral environment
  migrator_seed_paths = local.is_ephemeral_env ? {
    "/bfd/${local.env}/migrator/sensitive/db_migrator_db_username" = "/bfd/${local.seed_env}/migrator/sensitive/db_migrator_db_username",
    "/bfd/${local.env}/migrator/sensitive/db_migrator_db_password" = "/bfd/${local.seed_env}/migrator/sensitive/db_migrator_db_password"
  } : {}

  # Targeted PIPELINE hierarchy paths to be "copied" from the seed environment into requested ephemeral environment
  pipeline_seed_paths = local.is_ephemeral_env ? {
    "/bfd/${local.env}/pipeline/shared/sensitive/data_pipeline_db_username" = "/bfd/${local.seed_env}/pipeline/shared/sensitive/data_pipeline_db_username",
    "/bfd/${local.env}/pipeline/shared/sensitive/data_pipeline_db_password" = "/bfd/${local.seed_env}/pipeline/shared/sensitive/data_pipeline_db_password"
  } : {}
}

data "aws_db_cluster_snapshot" "seed" {
  count                 = local.is_ephemeral_env ? 1 : 0
  db_cluster_identifier = "bfd-${local.seed_env}-aurora-cluster"
  most_recent           = true
  db_cluster_snapshot_identifier = lookup(
    local.common_nonsensitive_ssm,
    "/bfd/${local.env}/common/nonsensitive/rds_snapshot_identifier",
    var.ephemeral_rds_snapshot_id_override
  )
}

# NOTE: Contains *all* seed environment hierarchies including sensitive and nonsensitive values
data "aws_ssm_parameters_by_path" "seed" {
  count           = local.is_ephemeral_env ? 1 : 0
  path            = "/bfd/${local.seed_env}/"
  with_decryption = true
  recursive       = true
}

# Copy targeted COMMON hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_common" {
  for_each  = local.common_seed_paths
  key_id    = contains(split("/", each.key), "sensitive") ? data.aws_kms_key.cmk.arn : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
}

# Copy targeted MIGRATOR hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_migrator" {
  for_each  = local.migrator_seed_paths
  key_id    = contains(split("/", each.key), "sensitive") ? data.aws_kms_key.cmk.arn : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
}

# Copy targeted PIPELINE hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_pipeline" {
  for_each  = local.pipeline_seed_paths
  key_id    = contains(split("/", each.key), "sensitive") ? data.aws_kms_key.cmk.arn : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
}
