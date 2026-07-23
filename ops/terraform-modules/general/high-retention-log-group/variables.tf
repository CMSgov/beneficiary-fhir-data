variable "log_group_name" {
  type    = string
}

variable "log_retention_days" {
  type    = number
  default = 2557

  validation {
    condition     = var.log_retention_days >= 2557
    error_message = "log_retention_days must be at least 2557."
  }
}

variable "kms_key_id" {
  type        = string
  default     = null
  description = "Optional KMS key ARN for encrypting the log group."
}

variable "tags" {
  type        = map(string)
  default     = {}
  description = "Tags to apply to the CloudWatch log group."
}

variable "prevent_destroy" {
  type        = bool
  default     = true
  description = "Set true to prevent accidental deletion of the log group."
}