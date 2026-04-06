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

variable "consume_idr_events_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the consume-idr-events container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "run_idr_pipeline_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the run-idr-pipeline container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "pipeline_version_override" {
  default     = null
  description = "Overrides the version for pipeline container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}

variable "consume_idr_events_version_override" {
  default     = null
  description = "Overrides the version for consume-idr-events container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}

variable "run_idr_pipeline_version_override" {
  default     = null
  description = "Overrides the version for run-idr-pipeline container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}
