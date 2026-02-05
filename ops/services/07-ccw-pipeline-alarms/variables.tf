variable "manifests_verifier_repository_override" {
  default     = null
  description = "Overrides the ECR repository for the manifests-verifier container image. If not provided, the default will be used"
  sensitive   = false
  type        = string
}

variable "manifests_verifier_version_override" {
  default     = null
  description = "Overrides the version for manifests-verifier container image resolution. If not provided, the latest BFD version will be used"
  sensitive   = false
  type        = string
}
