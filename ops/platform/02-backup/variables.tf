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

variable "greenfield" {
  default     = true
  description = "Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed."
}
