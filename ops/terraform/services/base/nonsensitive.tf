locals {
  # High precedence. Establish override behavior by removing overrides if they're not provided, i.e. they're `null`
  common_nonsensitive_override_raw = {
    "/bfd/${local.env}/common/nonsensitive/rds_snapshot_identifier" = local.is_ephemeral_env ? data.aws_db_cluster_snapshot.seed[0].id : null
  }

  # NOTE: null values are illegal, so we must strip them out if they should exist
  common_nonsensitive_override = { for key, value in local.common_nonsensitive_override_raw : key => value if value != null }

  # Normal precedence. Values stored in YAML files.
  yaml_file = local.is_ephemeral_env ? "ephemeral.yaml" : "${local.env}.yaml"
  # yaml = yamldecode(templatefile("${path.module}/values/${local.yaml_file}", {
  #   env = local.env
  # }))
  yaml          = data.external.eyaml.result
  common_yaml   = { for key, value in local.yaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "common") && strcontains(key, "nonsensitive") && value != "UNDEFINED" }
  migrator_yaml = { for key, value in local.yaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "migrator") && strcontains(key, "nonsensitive") && value != "UNDEFINED" }
  pipeline_yaml = { for key, value in local.yaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "pipeline") && strcontains(key, "nonsensitive") && value != "UNDEFINED" }
  server_yaml   = { for key, value in local.yaml : replace(key, "$${env}", local.env) => value if contains(split("/", key), "server") && strcontains(key, "nonsensitive") && value != "UNDEFINED" }

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
