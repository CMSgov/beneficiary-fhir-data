variable "greenfield" {
  default     = false
  description = "Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed."
}

variable "parent_env" {
  description = <<-EOF
  The solution's parent environment name. Corresponds with `terraform.workspace` and
  `var.environment.name`.
  EOF
  type        = string

  validation {
    condition     = one([for x in local.established_envs : x if var.parent_env == x]) != null
    error_message = "Parent environment must be a valid parent environment."
  }
}

variable "environment_name" {
  description = "The solution's environment name. Generally, `terraform.workspace`"
  type        = string
  validation {
    // Simple validation ensures that the environment is either one of the established environments or ends with a combined
    // suffix of "-" and an established environment, e.g. `prod-sbx`, `2554-test`, `2554-ii-prod-sbx` are valid, `-prod`, `2554--test` are not
    condition     = one([for x in local.established_envs : x if can(regex("^${x}$$|^([a-z0-9]+[a-z0-9-])+([^--])-${x}$$", var.environment_name))]) != null && can(regex("${var.parent_env}$$", var.environment_name))
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

variable "subnet_layers" {
  default     = []
  description = "List of subnet 'layers' (app, data, dmz, etc.) from which each subnet associated with that layer will be looked up."
  type        = list(string)
}
