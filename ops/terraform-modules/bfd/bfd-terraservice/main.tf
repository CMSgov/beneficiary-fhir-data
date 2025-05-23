locals {
  established_envs = ["test", "prod-sbx", "prod"]

  service      = var.service
  env          = var.environment_name
  github_token = coalesce(data.external.github_token.result.github_token, "invalid")

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
}

data "aws_availability_zones" "main" {}

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

data "aws_vpc" "main" {
  filter {
    name   = "tag:stack"
    values = [var.parent_env]
  }
}

data "aws_subnets" "main" {
  for_each = toset(var.subnet_layers)

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
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

data "aws_security_group" "vpn" {
  count = local.legacy_vpn_sg != null && !var.greenfield ? 1 : 0

  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.legacy_vpn_sg]
  }
}

data "aws_security_group" "management" {
  count = local.legacy_management_sg != null && !var.greenfield ? 1 : 0

  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.legacy_management_sg]
  }
}

data "aws_security_group" "tools" {
  count = local.legacy_tools_sg != null && !var.greenfield ? 1 : 0

  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.legacy_tools_sg]
  }
}

data "aws_security_group" "cms_cloud_vpn" {
  count = local.cms_cloud_vpn_sg != null && var.greenfield ? 1 : 0

  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_vpn_sg]
  }
}

data "aws_security_group" "cms_cloud_security_tools" {
  count = local.cms_cloud_security_tools_sg != null && var.greenfield ? 1 : 0

  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_security_tools_sg]
  }
}

data "aws_security_group" "cms_cloud_shared_services" {
  count = local.cms_cloud_shared_services_sg != null && var.greenfield ? 1 : 0

  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.cms_cloud_shared_services_sg]
  }
}
