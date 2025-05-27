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

variable "subnet_layers" {
  default     = []
  description = "List of subnet 'layers' (app, data, dmz, etc.) from which each subnet associated with that layer will be looked up."
  type        = list(string)
}
