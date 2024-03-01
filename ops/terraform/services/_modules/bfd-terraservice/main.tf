locals {
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", var.environment_name))])

  github_token = coalesce(data.external.github_token.result.github_token, "invalid")
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
