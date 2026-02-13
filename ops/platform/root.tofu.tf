# This root tofu.tf is symlink'd to by all platform Terraservices. Changes to this tofu.tf apply to
# _all_ platform Terraservices, so be careful!

locals {
  account_types = ["prod", "non-prod"]
  # Instead of just defaulting to terraform.workspace, we use one() to ensure an error is generated
  # if the workspace does not exactly match one of the above account types. Further validation is
  # handled by the Terraservice module.
  account_type = coalesce(var.account_type, one([for x in local.account_types : x if x == terraform.workspace]))

  # This is just a breadcrum - helper variable that is not actually used in code but helps in tracking whether or not we are including this tf.
  # tflint-ignore: terraform_unused_declarations
  _canary_exists = module.terraservice.canary
}

variable "region" {
  default  = "us-east-1"
  nullable = false
  type     = string
}

variable "secondary_region" {
  default  = "us-west-2"
  nullable = false
  type     = string
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

# tflint-ignore: terraform_required_providers
provider "aws" {
  region = var.region
  default_tags {
    tags = local.default_tags
  }
}

# tflint-ignore: terraform_required_providers, terraform_unused_declarations
provider "aws" {
  alias = "secondary"

  region = var.secondary_region
  default_tags {
    tags = local.default_tags
  }
}

# tflint-ignore: terraform_required_version
terraform {
  backend "s3" {
    bucket       = "bfd-platform-${local.account_type}-tf-state"
    key          = "ops/platform/${local.service}/tofu.tfstate"
    region       = var.region
    encrypt      = true
    kms_key_id   = "alias/bfd-platform-cmk"
    use_lockfile = true
  }
}
