locals {
  # High precedence. Establish override behavior by removing overrides if they're not provided, i.e. they're `null`
  common_nonsensitive_override_raw = {
    "/bfd/${local.env}/common/nonsensitive/rds_snapshot_identifier"    = local.is_ephemeral_env ? data.aws_db_cluster_snapshot.seed[0].id : null
    "/bfd/${local.env}/common/nonsensitive/ephemeral_environment_seed" = local.is_ephemeral_env ? var.ephemeral_environment_seed : null
  }

  # NOTE: null values are illegal, so we must strip them out if they should exist
  common_nonsensitive_override = { for key, value in local.common_nonsensitive_override_raw : key => value if value != null }

  kms_key_alias = local.is_ephemeral_env ? "alias/bfd-${local.seed_env}-cmk" : "alias/bfd-${local.env}-cmk"

  # NOTE: When instantiating an ephemeral environment, `local.common_nonsensitive_ssm` will be empty.
  #       The var.epehemeral_environment_seed **must** be provided on the first run of terraform!
  seed_env = lookup(
    local.common_nonsensitive_ssm,
    "/bfd/${local.env}/common/nonsensitive/ephemeral_environment_seed",
    var.ephemeral_environment_seed
  )

  # Normal precedence. Values stored in YAML files.
  yaml_file = contains(local.established_envs, local.env) ? "${local.env}.yaml" : "ephemeral.yaml"
  yaml = yamldecode(templatefile("${path.module}/values/${local.yaml_file}", {
    env      = local.env
    seed_env = local.seed_env
  }))
  common_yaml   = { for key, value in local.yaml : key => value if contains(split("/", key), "common") }
  migrator_yaml = { for key, value in local.yaml : key => value if contains(split("/", key), "migrator") }
  pipeline_yaml = { for key, value in local.yaml : key => value if contains(split("/", key), "pipeline") }
  server_yaml   = { for key, value in local.yaml : key => value if contains(split("/", key), "server") }

  # Low precedence. These values are already present in SSM but aren't (yet) part of the encoded YAML configuration.
  common_nonsensitive_ssm = zipmap(
    data.aws_ssm_parameters_by_path.common_nonsensitive.names,
    nonsensitive(data.aws_ssm_parameters_by_path.common_nonsensitive.values)
  )

  # Final, precedence ordered, merged configuration as applicable
  # NOTE: ephemeral environments must filter-out local.common_seed_paths (defined in ephemeral.tf) to avoid infinite loops
  common_nonsensitive = merge(
    { for ssm_key, ssm_value in local.common_nonsensitive_ssm : ssm_key => ssm_value if !contains(keys(local.common_seed_paths), ssm_key) }, # Low
    local.common_yaml,                                                                                                                       # Normal
    local.common_nonsensitive_override                                                                                                       # High
  )
  migrator_nonsensitive = local.migrator_yaml
  pipeline_nonsensitive = local.pipeline_yaml
  server_nonsensitive   = local.server_yaml
}

data "aws_ssm_parameters_by_path" "common_nonsensitive" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

resource "aws_ssm_parameter" "common_nonsensitive" {
  for_each = local.common_nonsensitive

  name      = each.key
  overwrite = true
  type      = "String"
  value     = each.value
}

resource "aws_ssm_parameter" "migrator_nonsensitive" {
  for_each = local.migrator_nonsensitive

  name      = each.key
  overwrite = true
  type      = "String"
  value     = each.value
}

resource "aws_ssm_parameter" "pipeline_nonsensitive" {
  for_each = local.pipeline_nonsensitive

  name      = each.key
  overwrite = true
  type      = "String"
  value     = each.value
}

resource "aws_ssm_parameter" "server_nonsensitive" {
  for_each = local.server_nonsensitive

  name      = each.key
  overwrite = true
  type      = "String"
  value     = each.value
}
