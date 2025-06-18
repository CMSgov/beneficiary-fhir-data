# This root tofu.tf is symlink'd to by all per-env Terraservices. Changes to this tofu.tf apply to
# _all_ Terraservices, so be careful!

locals {
  established_envs = !var.greenfield ? ["test", "prod-sbx", "prod"] : ["test", "sandbox", "prod"]
  parent_env = coalesce(
    var.parent_env,
    try(one([for x in local.established_envs : x if can(regex("${x}$$", terraform.workspace))]), "invalid-workspace"),
    "invalid-parent-env"
  )

  _canary_exists = module.terraservice.canary
}

variable "greenfield" {
  default     = true
  description = "Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed."
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
    bucket         = !var.greenfield ? "bfd-tf-state" : "bfd-${local.parent_env}-tf-state"
    key            = !var.greenfield ? "ops/services/${local.service}/terraform.tfstate" : "ops/services/${local.service}/tofu.tfstate"
    region         = var.region
    dynamodb_table = !var.greenfield ? "bfd-tf-table" : null
    encrypt        = true
    kms_key_id     = !var.greenfield ? "alias/bfd-tf-state" : "alias/bfd-${local.parent_env}-cmk"
    use_lockfile   = var.greenfield
  }
}
