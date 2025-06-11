locals {
  established_envs = ["test", "prod-sbx", "prod"]

  region = data.aws_region.current.name

  service      = var.service
  github_token = coalesce(data.external.github_token.result.github_token, "invalid")

  env        = try(one([for x in local.established_envs : x if can(regex("${x}$$", terraform.workspace))]), null)
  parent_env = terraform.workspace

  env_vpc = one([for _, v in data.aws_vpc.main : v if v.tags["stack"] == local.env])

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

  platform_key_alias = var.greenfield ? "alias/bfd-platform-cmk" : "alias/bfd-mgmt-cmk"
  platform_key_arn   = one(data.aws_kms_key.platform[*].arn)
  env_key_alias      = "alias/bfd-${local.env}-cmk"
  env_key_arn        = one(data.aws_kms_key.env[*].arn)

  # OIT/CMS Cloud configured Security Groups that exist only in the legacy account
  legacy_tools_sg      = lookup(local.ssm_config, "/bfd/common/enterprise_tools_security_group", null)
  legacy_management_sg = lookup(local.ssm_config, "/bfd/common/management_security_group", null)
  legacy_vpn_sg        = lookup(local.ssm_config, "/bfd/common/vpn_security_group", null)

  # OIT/CMS Cloud configured Security Groups that exist in the Greenfield accounts
  cms_cloud_vpn_sg             = "cmscloud-vpn"
  cms_cloud_security_tools_sg  = "cmscloud-security-tools"
  cms_cloud_shared_services_sg = "cmscloud-shared-services"

  # Specify a set of default AZs -- in this case: <region>a, <region>b, <region>c
  default_azs = {
    for k, v in data.aws_availability_zone.main :
    k => v if contains(["a", "b", "c"], v.name_suffix)
  }

  local_cidrs = toset([for vpc in data.aws_vpc.main : vpc.cidr_block])
  all_connections = { for k, v in data.aws_vpc_peering_connection.main :
    v.tags["Name"] => {
      connection_id = k
      peering_owner = v.peer_owner_id
      local_cidr    = contains(local.local_cidrs, v.cidr_block) ? v.cidr_block : v.peer_cidr_block
      foreign_cidr  = contains(local.local_cidrs, v.cidr_block) ? v.peer_cidr_block : v.cidr_block
      env           = one([for _, vpc in data.aws_vpc.main : vpc.tags["stack"] if contains([v.vpc_id, v.peer_vpc_id], vpc.id)])
    }
  }
}

data "aws_availability_zones" "main" {
  # Unfortunately, there is no better place to put these precondition checks. We want to fail-fast
  # and early when the workspace is invalid, but there is no construct in OpenTofu to do these sorts
  # of checks early outside of data resources. There is the 'null_data_source' data resource, but
  # that resource is marked as deprecated with no replacement. So, given that we have no real
  # choice, these conditions live on this unrelated data resource.
  lifecycle {
    precondition {
      // Simple validation ensures that the environment is either one of the established environments or ends with a combined
      // suffix of "-" and an established environment, e.g. `prod-sbx`, `2554-test`, `2554-ii-prod-sbx` are valid, `-prod`, `2554--test` are not
      condition     = try(one([for x in local.established_envs : x if can(regex("^${x}$$|^([a-z0-9]+[a-z0-9-])+([^--])-${x}$$", local.env))]) != null, false)
      error_message = "Invalid environment/workspace name. https://github.com/CMSgov/beneficiary-fhir-data/wiki/Environments#ephemeral-environments for more details."
    }

    precondition {
      condition     = local.parent_env != null
      error_message = "Invalid parent environment. Must be one of: ${join(", ", local.established_envs)}"
    }
  }
}

data "aws_availability_zone" "main" {
  for_each = toset(data.aws_availability_zones.main.names)

  name = each.key
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

data "aws_kms_key" "env" {
  count  = var.lookup_kms_keys ? 1 : 0
  key_id = local.env_key_alias
}

data "aws_kms_key" "platform" {
  count  = var.lookup_kms_keys ? 1 : 0
  key_id = local.platform_key_alias
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}

data "aws_vpcs" "main" {}

data "aws_vpc" "main" {
  for_each = toset(data.aws_vpcs.main.ids)
  id       = each.value
}

data "aws_subnets" "main" {
  for_each = toset(var.subnet_layers)

  filter {
    name   = "vpc-id"
    values = [local.env_vpc.id]
  }

  tags = !var.greenfield ? {
    Layer = each.key
    } : {
    GroupName = each.key
  }
}

data "aws_subnet" "main" {
  for_each = toset(flatten([for _, obj in data.aws_subnets.main : obj.ids]))

  id = each.key
}

data "aws_vpc_peering_connections" "main" {}

data "aws_vpc_peering_connection" "main" {
  for_each = toset(data.aws_vpc_peering_connections.main.ids)
  id       = each.value
}

data "aws_security_group" "vpn" {
  count = local.legacy_vpn_sg != null && !var.greenfield ? 1 : 0

  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.legacy_vpn_sg]
  }
}

data "aws_security_group" "management" {
  count = local.legacy_management_sg != null && !var.greenfield ? 1 : 0

  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.legacy_management_sg]
  }
}

data "aws_security_group" "tools" {
  count = local.legacy_tools_sg != null && !var.greenfield ? 1 : 0

  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.legacy_tools_sg]
  }
}

data "aws_security_group" "cms_cloud_vpn" {
  count = local.cms_cloud_vpn_sg != null && var.greenfield ? 1 : 0

  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_vpn_sg]
  }
}

data "aws_security_group" "cms_cloud_security_tools" {
  count = local.cms_cloud_security_tools_sg != null && var.greenfield ? 1 : 0

  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_security_tools_sg]
  }
}

data "aws_security_group" "cms_cloud_shared_services" {
  count = local.cms_cloud_shared_services_sg != null && var.greenfield ? 1 : 0

  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_shared_services_sg]
  }
}
