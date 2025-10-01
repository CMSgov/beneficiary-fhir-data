output "canary" {
  description = "Canary output used to ensure that any Terraservice using the root.tofu.tf also uses this module."
  sensitive   = false
  value       = true
}

output "service" {
  description = "The name of the current Terraservice"
  sensitive   = false
  value       = var.service
}

output "region" {
  description = "The region name associated with the current caller identity"
  sensitive   = false
  value       = local.region
}

output "account_id" {
  description = "The account ID associated with the current caller identity"
  sensitive   = false
  value       = data.aws_caller_identity.current.account_id
}

output "env" {
  description = "The solution's environment name. Generally, `terraform.workspace`"
  sensitive   = false
  value       = local.env
}

output "is_ephemeral_env" {
  description = "Returns true when environment is _ephemeral_, false when _established_"
  sensitive   = false
  value       = local.env != local.parent_env
}

output "default_tags" {
  value = merge(var.additional_tags, {
    Environment    = local.parent_env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    service        = var.service
    Terraform      = true
    tf_module_root = var.relative_module_root
  })
  sensitive = false
}

output "bfd_version" {
  description = "The BFD version that is being deployed. Corresponds to the most recent tag of the checked-out repository"
  sensitive   = false
  value       = local.bfd_version
}

output "ssm_config" {
  description = "Parameter:Value map that elides repetitive keys, e.g. ssm:/bfd/test/common/vpc_name is /bfd/comon/vpc_name"
  sensitive   = true
  value       = local.ssm_config
}

output "platform_key_alias" {
  description = "Alias name for the platform CMK."
  sensitive   = false
  value       = local.platform_key_alias
}

output "platform_key_arn" {
  description = "ARN of the current region's primary platform CMK."
  sensitive   = false
  value       = local.platform_key_arn
}

output "env_key_alias" {
  description = "Alias name for the current environment's CMK."
  sensitive   = false
  value       = local.env_key_alias
}

output "env_key_arn" {
  description = "ARN of the current region's primary environment CMK."
  sensitive   = false
  value       = local.env_key_arn
}

output "default_iam_path" {
  description = "Default path for IAM policies and roles."
  sensitive   = false
  value       = "/delegatedadmin/developer/"
}

output "default_permissions_boundary_arn" {
  description = "ARN of the default permissions boundary for IAM Roles."
  sensitive   = false
  value       = data.aws_iam_policy.permissions_boundary.arn
}

output "vpc" {
  description = "The current environment's VPC (data.aws_vpc)."
  sensitive   = false
  value       = local.env_vpc
}

output "default_azs" {
  description = "Key-value map of AZ names to their attributes (data.aws_availability_zone) of all default AZs that all BFD services exist in."
  sensitive   = false
  value       = local.default_azs
}

output "subnets_map" {
  description = "Map of subnet group to the subnets (data.aws_subnet) in that layer in the current environment's VPC."
  sensitive   = false
  value = {
    for group in var.subnet_layers
    : group => [for _, subnet in data.aws_subnet.main : subnet if subnet.tags["GroupName"] == group]
  }
}

output "all_connections" {
  description = "Map of all peering connections in all VPCs to their properties"
  sensitive   = false
  value       = local.all_connections
}

output "cms_cloud_vpn_sg" {
  description = "The OIT/CMS Cloud provided VPN Security Group (data.aws_security_group)."
  sensitive   = false
  value       = data.aws_security_group.cms_cloud_vpn
}

output "cms_cloud_security_tools_sg" {
  description = "The OIT/CMS Cloud provided Security Tools Security Group (data.aws_security_group)."
  sensitive   = false
  value       = data.aws_security_group.cms_cloud_security_tools
}

output "cms_cloud_shared_services_sg" {
  description = "The OIT/CMS Cloud provided Shared Services Security Group (data.aws_security_group)."
  sensitive   = false
  value       = data.aws_security_group.cms_cloud_shared_services
}
