variable "env_config" {
  type        = object({ env = string, tags = map(string) })
  description = "All high-level info for the whole vpc"
}

variable "name" {
  type        = string
  description = "What to name the registry (bfd-ecr, ops-ecr, etc)"
}

variable "scan_images_on_push" {
  type        = bool
  description = "Scan images for vulnerabilities after being pushed to the registry"
  default     = true
}

variable "image_tag_mutability" {
  type        = string
  description = "Allow tagged images to be overwritten. Must be one of: MUTABLE or IMMUTABLE"
  default     = "IMMUTABLE"
}

variable "encryption_configuration" {
  type = object({
    encryption_type = string
    kms_key         = any
  })
  description = "ECR encryption config. Must set encryption_type to 'KMS' if encrypting."
  default     = null
}

# note: ensure rules are sorted by rulePriority to prevent triggering terraform
variable "lifecycle_policy" {
  type = string
  description = "ECR Lifecycle Policy"
  default = null
}
