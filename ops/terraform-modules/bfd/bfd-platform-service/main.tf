locals {
  region = data.aws_region.current.region

  account_types = ["prod", "non-prod"]
  account_type  = try(one([for x in local.account_types : x if terraform.workspace == x]), null)

  bfd_version = data.external.bfd_version.result.bfd_version

  service = var.service

  ssm_hierarchies = flatten([
    for root in var.ssm_hierarchy_roots :
    ["/${root}/platform/common", "/${root}/platform/${local.service}"]
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
      for name in local.ssm_flattened_data.names
      : replace(name, "/((non)*sensitive|platform)//", "")
    ],
    local.ssm_flattened_data.values
  )

  kms_key_alias = "alias/bfd-platform-cmk"
  kms_key_arn   = one(data.aws_kms_key.platform[*].arn)
}

data "aws_region" "current" {
  # Unfortunately, there is no better place to put these precondition checks. We want to fail-fast
  # and early when the workspace is invalid, but there is no construct in OpenTofu to do these sorts
  # of checks early outside of data resources. There is the 'null_data_source' data resource, but
  # that resource is marked as deprecated with no replacement. So, given that we have no real
  # choice, this condition lives on this unrelated data resource.
  lifecycle {
    precondition {
      condition     = local.account_type != null
      error_message = "Invalid account type. Must be one of: ${join(", ", local.account_types)}"
    }
  }
}

data "aws_caller_identity" "current" {}

data "external" "bfd_version" {
  program = ["${path.module}/scripts/get_bfd_version.sh"]
}

data "aws_iam_account_alias" "current" {
  lifecycle {
    postcondition {
      condition     = endswith(self.account_alias, terraform.workspace) && !(endswith(self.account_alias, "non-prod") && terraform.workspace != "non-prod")
      error_message = "The current account does not match the selected workspace. Select a workspace that matches the current account type."
    }
  }
}

data "aws_ssm_parameters_by_path" "params" {
  for_each = toset(local.ssm_hierarchies)

  recursive       = true
  path            = each.value
  with_decryption = true
}

data "aws_kms_key" "platform" {
  count  = var.lookup_kms_key ? 1 : 0
  key_id = local.kms_key_alias
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
