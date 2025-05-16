variable "db_environment_override" {
  default     = null
  description = "For use in database maintenance contexts or in ephemeral environments only"
  sensitive   = false
  type        = string
}

variable "pipeline_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the pipeline container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "pipeline_version_override" {
  default     = null
  description = "Overrides the version for pipeline container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}
