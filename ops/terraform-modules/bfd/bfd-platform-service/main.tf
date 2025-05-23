locals {
  account_types = ["prod", "non-prod"]
  account_type  = one([for x in local.account_types : x if terraform.workspace == x])

  service      = var.service
  github_token = coalesce(data.external.github_token.result.github_token, "invalid")

  ssm_hierarchies = flatten([
    for root in var.ssm_hierarchy_roots :
    # TODO: Remove "/ng/" prefix when Greenfield/"next-gen" services are migrated to completely
    ["/ng/${root}/platform/common", "/ng/${root}/platform/${local.service}"]
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
      "/${trimprefix(replace(name, "/((non)*sensitive|platform)//", ""), "/ng/")}"
    ],
    local.ssm_flattened_data.values
  )

  kms_key_alias             = "alias/bfd-platform-data-cmk"
  kms_key_mrk_config        = one(data.aws_kms_key.data[*].multi_region_configuration)
  kms_config_key_alias      = "alias/bfd-platform-config-cmk"
  kms_config_key_mrk_config = one(data.aws_kms_key.config[*].multi_region_configuration)
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "external" "github_token" {
  program = ["${path.module}/scripts/get_github_token.sh"]
}

data "aws_iam_account_alias" "current" {
  lifecycle {
    postcondition {
      condition     = endswith(self.account_alias, terraform.workspace) && !(endswith(self.account_alias, "non-prod") && terraform.workspace != "non-prod")
      error_message = "The current account does not match the selected workspace. Select a workspace that matches the current account type."
    }
  }
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

data "aws_kms_key" "data" {
  count  = local.kms_key_alias != null && var.lookup_kms_keys ? 1 : 0
  key_id = local.kms_key_alias
}

data "aws_kms_key" "config" {
  count  = local.kms_config_key_alias != null && var.lookup_kms_keys ? 1 : 0
  key_id = local.kms_config_key_alias
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
