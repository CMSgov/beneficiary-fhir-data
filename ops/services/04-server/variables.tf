variable "db_environment_override" {
  default     = null
  description = "For use in database maintenance contexts or in ephemeral environments only"
  sensitive   = false
  type        = string
}

variable "ephemeral_regression_override" {
  default     = false
  description = "Enables the execution and creation of the regression-wrapper during CodeDeploy deployment. Applies _only_ to ephemeral environments, ignored in established environments"
  sensitive   = false
  nullable    = false
  type        = bool
}

variable "certstores_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the certstores container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "log_router_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the log_router container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "server_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the server container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "certstores_version_override" {
  default     = null
  description = "Overrides the version for certstores container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}

variable "log_router_version_override" {
  default     = null
  description = "Overrides the version for log_router container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}

variable "server_version_override" {
  default     = null
  description = "Overrides the version for server container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}
