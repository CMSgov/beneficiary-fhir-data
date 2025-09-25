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

variable "lookup_kms_key" {
  default     = true
  description = <<-EOF
  Toggles whether or not this module does data lookups for the platform KMS key.
  If false, the KMS-related outputs will all be null. Set to false for services that create the key
  or are otherwise applied prior to the keys existing
  EOF
  nullable    = false
}
