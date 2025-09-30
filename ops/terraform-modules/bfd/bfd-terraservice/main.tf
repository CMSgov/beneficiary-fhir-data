locals {
  established_envs = ["test", "sandbox", "prod"]

  region = data.aws_region.current.region

  bfd_version = data.external.bfd_version.result.bfd_version

  service = var.service

  parent_env = try(one([for x in local.established_envs : x if can(regex("${x}$$", terraform.workspace))]), null)
  env        = terraform.workspace

  # There are no tags from which we can divine the environment a VPC is associated. Fortunately, we
  # know the name of our VPCs, and they should _never_ change, so it's OK to just check the name of
  # the VPC and return its associated env.
  vpcs_to_env = {
    "bfd-east-test"         = "test"
    "bfd-sandbox-east-prod" = "sandbox"
    "bfd-east-prod"         = "prod"
  }
  env_vpc = one([for v in data.aws_vpc.main : v if local.vpcs_to_env[v.tags["Name"]] == local.parent_env])

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
  ssm_config = zipmap(
    [
      for name in local.ssm_flattened_data.names
      : replace(name, "/((non)*sensitive|${local.env})//", "")
    ],
    local.ssm_flattened_data.values
  )

  platform_key_alias = "alias/bfd-platform-cmk"
  platform_key_arn   = one(data.aws_kms_key.platform[*].arn)
  env_key_alias      = "alias/bfd-${local.parent_env}-cmk"
  env_key_arn        = one(data.aws_kms_key.env[*].arn)

  # OIT/CMS Cloud configured Security Groups that exist in all accounts
  cms_cloud_vpn_sg             = "cmscloud-vpn"
  cms_cloud_security_tools_sg  = "cmscloud-security-tools"
  cms_cloud_shared_services_sg = "cmscloud-shared-services"

  # Specify a set of default AZs -- in this case: <region>a, <region>b, <region>c
  default_azs = {
    for k, v in data.aws_availability_zone.main :
    k => v if contains(["a", "b", "c"], v.name_suffix)
  }

  local_cidrs = toset([for v in data.aws_vpc.main : v.cidr_block])
  all_connections = { for pc_id, pc in data.aws_vpc_peering_connection.main :
    pc.tags["Name"] => {
      connection_id = pc_id
      peering_owner = pc.peer_owner_id
      local_cidr    = contains(local.local_cidrs, pc.cidr_block) ? pc.cidr_block : pc.peer_cidr_block
      foreign_cidr  = contains(local.local_cidrs, pc.cidr_block) ? pc.peer_cidr_block : pc.cidr_block
      env = one([
        for vpc in data.aws_vpc.main
        : local.vpcs_to_env[vpc.tags["Name"]] if contains([pc.vpc_id, pc.peer_vpc_id], vpc.id)
      ])
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

data "external" "bfd_version" {
  program = ["${path.module}/scripts/get_bfd_version.sh"]
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

  tags = {
    GroupName = each.key
  }
}

data "aws_subnet" "main" {
  for_each = toset(flatten([for _, obj in data.aws_subnets.main : obj.ids]))

  id = each.key
}

data "aws_vpc_peering_connections" "main" {
  filter {
    name   = "status-code"
    values = ["active"]
  }
}

data "aws_vpc_peering_connection" "main" {
  for_each = toset(data.aws_vpc_peering_connections.main.ids)
  id       = each.value
}

data "aws_security_group" "cms_cloud_vpn" {
  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_vpn_sg]
  }
}

data "aws_security_group" "cms_cloud_security_tools" {
  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_security_tools_sg]
  }
}

data "aws_security_group" "cms_cloud_shared_services" {
  vpc_id = local.env_vpc.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_shared_services_sg]
  }
}
