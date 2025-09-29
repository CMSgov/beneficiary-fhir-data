# This root tofu.tf is symlink'd to by all per-env Terraservices. Changes to this tofu.tf apply to
# _all_ Terraservices, so be careful!

locals {
  established_envs = ["test", "sandbox", "prod"]
  parent_env = coalesce(
    var.parent_env,
    try(one([for x in local.established_envs : x if can(regex("${x}$$", terraform.workspace))]), "invalid-workspace"),
    "invalid-parent-env"
  )

  _canary_exists = module.terraservice.canary
}

variable "region" {
  default  = "us-east-1"
  nullable = false
}

variable "secondary_region" {
  default  = "us-west-2"
  nullable = false
}

variable "parent_env" {
  description = <<-EOF
  The parent environment of the current solution. Will correspond with `terraform.workspace`".
  Necessary on `tofu init` and `tofu workspace select` _only_. In all other situations, parent env
  will be divined from `terraform.workspace`.
  EOF
  type        = string
  nullable    = true
  default     = null
  validation {
    condition     = var.parent_env == null || one([for x in local.established_envs : x if var.parent_env == x && endswith(terraform.workspace, x)]) != null
    error_message = "Invalid parent environment name."
  }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = local.default_tags
  }
}

provider "aws" {
  alias = "secondary"

  region = var.secondary_region
  default_tags {
    tags = local.default_tags
  }
}

terraform {
  backend "s3" {
    bucket       = "bfd-${local.parent_env}-tf-state"
    key          = "ops/services/${local.service}/tofu.tfstate"
    region       = var.region
    encrypt      = true
    kms_key_id   = "alias/bfd-${local.parent_env}-cmk"
    use_lockfile = true
  }
}
