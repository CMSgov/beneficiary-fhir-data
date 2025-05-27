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
  value       = data.aws_region.current.name
}

output "account_id" {
  description = "The account ID associated with the current caller identity"
  sensitive   = false
  value       = data.aws_caller_identity.current.account_id
}

output "account_alias" {
  description = "The account ID associated with the current caller identity"
  sensitive   = false
  value       = data.aws_caller_identity.current.account_id
}

output "default_tags" {
  value = merge(var.additional_tags, {
    Environment    = "platform"
    application    = "bfd"
    business       = "oeda"
    stack          = "platform"
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

output "key_alias" {
  description = "Alias name for the platform general-purpose, multi-region CMK."
  sensitive   = false
  value       = local.kms_key_alias
}

output "config_key_alias" {
  description = "Alias name for the platform, configuration-specific, multi-region CMK."
  sensitive   = false
  value       = local.kms_config_key_alias
}

output "key_arns" {
  description = "ARNs of the platform general-purpose, multi-region CMK."
  sensitive   = false
  value = flatten(
    [
      for v in coalesce(local.kms_key_mrk_config, []) :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
}

output "config_key_arns" {
  description = "ARNs of the platform configuration-specific, multi-region CMK."
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
