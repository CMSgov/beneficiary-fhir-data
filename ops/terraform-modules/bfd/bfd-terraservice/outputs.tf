output "service" {
  description = "The name of the current Terraservice"
  sensitive   = false
  value       = var.service
}

output "region" {
  description = "The region name associated with the current caller identity"
  sensitive   = false
  value       = data.aws_region.current.name
}

output "account_id" {
  description = "The account ID associated with the current caller identity"
  sensitive   = false
  value       = data.aws_caller_identity.current.account_id
}

output "env" {
  description = "The solution's environment name. Generally, `terraform.workspace`"
  sensitive   = false
  value       = var.environment_name
}

output "is_ephemeral_env" {
  description = "Returns true when environment is _ephemeral_, false when _established_"
  sensitive   = false
  value       = var.environment_name != local.seed_env
}

output "seed_env" {
  description = "The solution's source environment. For established environments this is equal to the environment's name"
  sensitive   = false
  value       = local.seed_env
}

output "default_tags" {
  value = merge(var.additional_tags, {
    Environment    = local.seed_env
    application    = "bfd"
    business       = "oeda"
    stack          = var.environment_name
    service        = var.service
    Terraform      = true
    tf_module_root = var.relative_module_root
  })
  sensitive = false
}

output "latest_bfd_release" {
  description = "This is the latest CMSgov/beneficiary-fhir-data release. Excludes Pre-Releases."
  sensitive   = false
  value       = try(jsondecode(data.http.latest_bfd_release.response_body).tag_name, null)
}

output "ssm_config" {
  description = "Parameter:Value map that elides repetitive keys, e.g. ssm:/bfd/test/common/vpc_name is /bfd/comon/vpc_name"
  sensitive   = true
  value       = local.ssm_config
}

output "env_key_alias" {
  description = "Alias name for the current environment's general-purpose CMK."
  sensitive   = false
  value       = local.kms_key_alias
}

output "env_config_key_alias" {
  description = "Alias name for the current environment's configuration-specific, multi-region CMK."
  sensitive   = false
  value       = local.kms_config_key_alias
}

output "env_key_arn" {
  description = "ARN of the current environment's general-purpose CMK."
  sensitive   = false
  value       = one(data.aws_kms_key.env_cmk[*].arn)
}

output "env_config_key_arns" {
  description = "ARNs of the current environment's configuration-specific, multi-region CMK."
  sensitive   = false
  value = flatten(
    [
      for v in coalesce(local.kms_config_key_mrk_config, []) :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
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
  value       = data.aws_vpc.main
}

output "subnets_map" {
  description = "Map of subnet layers to the subnets (data.aws_subnet) in that layer in the current environment's VPC."
  sensitive   = false
  value = {
    for layer in var.subnet_layers
    : layer => [for _, subnet in data.aws_subnet.main : subnet if subnet.tags["Layer"] == layer]
  }
}
