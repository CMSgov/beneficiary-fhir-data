# This root tofu.tf is symlink'd to by all platform Terraservices. Changes to this tofu.tf apply to
# _all_ platform Terraservices, so be careful!

locals {
  account_types = ["prod", "non-prod"]
  # Instead of just defaulting to terraform.workspace, we use one() to ensure an error is generated
  # if the workspace does not exactly match one of the above account types. Further validation is
  # handled by the Terraservice module. This is only:necessary if greenfield is true, since we don't
  # use the account_type in the legacy environment
  account_type = var.greenfield ? coalesce(var.account_type, one([for x in local.account_types : x if x == terraform.workspace])) : "invalid-account-type"

  _canary_exists = module.terraservice.canary
}

variable "greenfield" {
  default     = false
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

variable "account_type" {
  description = <<-EOF
  The account type being targeted to create platform resources within. Will correspond with
  `terraform.workspace`. Necessary on `tofu init` and `tofu workspace select` _only_. In all other
  situations, the account type will be divined from `terraform.workspace`.
  EOF
  type        = string
  nullable    = true
  default     = null
  validation {
    condition     = var.account_type == null || one([for x in local.account_types : x if var.account_type == x]) != null
    error_message = "Invalid account type. Must be one of 'prod' or 'non-prod'."
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
    bucket       = !var.greenfield ? "bfd-tf-state" : "bfd-platform-${local.account_type}-tf-state"
    key          = "ops/platform/${local.service}/tofu.tfstate"
    region       = var.region
    encrypt      = true
    kms_key_id   = !var.greenfield ? "alias/bfd-tf-state" : "alias/bfd-platform-cmk"
    use_lockfile = true
  }
}
