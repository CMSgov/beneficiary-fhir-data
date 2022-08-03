locals {
  # Normal precedence. Values stored in YAML files.
  yaml_file = contains(local.established_envs, local.env) ? "${local.env}.yaml" : "default.yaml"
  yaml = yamldecode(templatefile("${path.module}/values/${local.yaml_file}", {
    env = local.env
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

  # Final, merged configuration as applicable.
  common_nonsensitive   = merge(local.common_nonsensitive_ssm, local.common_yaml)
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
