locals {
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", var.environment_name))])
  service          = var.service
  env              = var.environment_name
  github_token     = coalesce(data.external.github_token.result.github_token, "invalid")

  ssm_hierarchies = flatten([
    for root in var.ssm_hierarchy_roots :
    # TODO: Remove "/ng/" prefix when Greenfield/"next-gen" services are migrated to completely
    ["/ng/${root}/${local.env}/common", "/ng/${root}/${local.env}/${local.service}"]
  ])
  ssm_flattened_data = {
    names = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : v.names]
    )
    values = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : nonsensitive(v.values)]
    )
  }
  ssm_config = zipmap(
    [
      for name in local.ssm_flattened_data.names :
      # TODO: Remove trimprefix when Greenfield/"next-gen" services are migrated to completely
      "/${trimprefix(replace(name, "/((non)*sensitive|${local.env})//", ""), "/ng/")}"
    ],
    local.ssm_flattened_data.values
  )

  # Using lookup to ensure that this module can be used in Terraservices that may not have SSM
  # configuration, or could be applied before SSM configuration exists at all
  kms_key_alias             = lookup(local.ssm_config, "/bfd/common/kms_key_alias", null)
  kms_config_key_alias      = lookup(local.ssm_config, "/bfd/common/kms_config_key_alias", null)
  kms_config_key_mrk_config = one(data.aws_kms_key.env_config_cmk[*].multi_region_configuration)
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

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

data "aws_kms_key" "env_cmk" {
  count  = local.kms_key_alias != null ? 1 : 0
  key_id = local.kms_key_alias
}

data "aws_kms_key" "env_config_cmk" {
  count  = local.kms_config_key_alias != null ? 1 : 0
  key_id = local.kms_config_key_alias
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
