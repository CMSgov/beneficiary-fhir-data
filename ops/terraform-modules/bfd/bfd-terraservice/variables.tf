variable "environment_name" {
  description = "The solution's environment name. Generally, `terraform.workspace`"
  type        = string
  validation {
    // NOTE: validations can only refer to this scope, i.e. `var.environment_name` is valid but `local.established_envs` is not
    // Simple validation ensures that the environment is either one of the established environments or ends with a combined
    // suffix of "-" and an established environment, e.g. `prod-sbx`, `2554-test`, `2554-ii-prod-sbx` are valid, `-prod`, `2554--test` are not
    condition     = one([for x in ["test", "prod-sbx", "prod"] : x if can(regex("^${x}$$|^([a-z0-9]+[a-z0-9-])+([^--])-${x}$$", var.environment_name))]) != null
    error_message = "Invalid environment/workspace name. https://github.com/CMSgov/beneficiary-fhir-data/wiki/Environments#ephemeral-environments for more details."
  }
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
