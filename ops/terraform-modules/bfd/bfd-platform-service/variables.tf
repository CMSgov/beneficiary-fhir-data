variable "greenfield" {
  default     = false
  description = "Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed."
}

variable "relative_module_root" {
  description = "The solution's relative path from the root of beneficiary-fhir-data repository"
  type        = string
}

variable "additional_tags" {
  default     = {}
  description = "Additional tags to merge into final default_tags output"
  type        = map(string)
}

variable "ssm_hierarchy_roots" {
  default     = ["bfd"]
  description = "List of SSM Hierarchy roots. Module executes a recursive lookup for all roots for `common` and service-specific hierarchies."
  type        = list(string)
}

variable "service" {
  description = "Service _or_ terraservice name."
  type        = string
}

variable "lookup_kms_keys" {
  default     = true
  description = <<-EOF
  Toggles whether or not this module does data lookups for the platform data and config KMS keys.
  If false, the KMS-related outputs will all be null. Set to false for services that create the keys
  or are otherwise applied prior to the keys existing
  EOF
  nullable    = false
}
