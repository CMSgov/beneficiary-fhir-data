locals {
  established_envs = [
    "test",
    "mgmt",
    "prod-sbx",
    "prod"
  ]
  is_ephemeral_env = !(contains(local.established_envs, terraform.workspace))
  env              = terraform.workspace
  data_env         = local.is_ephemeral_env ? var.ephemeral_environment_seed : local.env

  # Set of tags that will be applied across all services in related workspaces. These will be used as `default_tags` as
  # well as a source of metadata in other modules/services.
  global_tags = {
    business    = "oeda"
    application = "bfd"
    stack       = local.is_ephemeral_env ? "bfd-${local.env}-${local.data_env}" : "bfd-${local.env}"

    Environment = local.data_env
    Terraform   = true
  }

  # Tags for *this* module.
  this_tags = {
    tf_module_root = "ops/terraform/services/base"
    tf_workspace   = local.env
  }

  # Tags that only get applied in ephemeral environments.
  ephemeral_tags = {
    Ephemeral    = true
    EphemeralPoc = coalesce(var.ephemeral_poc, "bfd-${local.env}")
  }
  default_tags = merge(local.global_tags, local.this_tags, local.is_ephemeral_env ? local.ephemeral_tags : {})
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}

output "default_tags" {
  description = "A set of default tags to be applied across resources in all services."
  value       = merge(local.global_tags, local.is_ephemeral_env ? local.ephemeral_tags : {})
}

output "workspace" {
  description = "The base workspace name."
  value       = terraform.workspace
}
