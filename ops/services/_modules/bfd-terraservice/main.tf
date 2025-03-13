locals {
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", var.environment_name))])
  service          = var.service
  env              = var.environment_name
  github_token     = coalesce(data.external.github_token.result.github_token, "invalid")

  ssm_hierarchies = flatten([
    for root in var.ssm_hierarchy_roots :
    ["/${root}/${local.env}/common", "/${root}/${local.env}/${local.service}"]
  ])
  ssm_flattened_data = {
    names = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : v.names]
    )
    values = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : nonsensitive(v.values)]
    )
  }
}

data "external" "github_token" {
  program = ["${path.module}/scripts/get_github_token.sh"]
}

data "http" "latest_bfd_release" {
  url = "https://api.github.com/repos/CMSgov/beneficiary-fhir-data/releases/latest"

  request_headers = local.github_token != "invalid" ? {
    Authorization = "Bearer ${local.github_token}"
  } : {}
}

data "aws_ssm_parameters_by_path" "params" {
  for_each = toset(local.ssm_hierarchies)

  recursive       = true
  path            = each.value
  with_decryption = true
}
