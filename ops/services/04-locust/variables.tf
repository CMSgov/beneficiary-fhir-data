variable "run_locust_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the 'run-locust' container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "run_locust_version_override" {
  default     = null
  description = "Overrides the version for 'run-locust' container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}

