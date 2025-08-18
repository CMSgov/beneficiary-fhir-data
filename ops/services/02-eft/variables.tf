variable "outbound_lambda_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the 'sftp-outbound-transfer' container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "outbound_lambda_version_override" {
  default     = null
  description = "Overrides the version for 'sftp-outbound-transfer' container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}

