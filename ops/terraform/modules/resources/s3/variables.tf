variable "env_config" {
  description = "Environment description"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string })
}

variable "role" {
  description = "(REQUIRED) Role of the bucket. Ie 'logs', 'data', 'artifacts', etc."
  type        = string
}

# everything below this line is optional and/or has a default values set

variable "kms_key_id" {
  description = "KMS key (arn) to use for encryption. Defaults to the AWS managed S3 key."
  type        = string
  default     = "alias/aws/s3"
}

variable "acl" {
  description = "The canned ACL to apply to the bucket. Defaults to private."
  type        = string
  default     = "private"
}

variable "id" {
  description = "Explicitly set the bucket id."
  type        = string
  default     = null
}

variable "logging_enabled" {
  description = "Enable bucket logging. Bucket logs are stored in the designated environment's 'logs' bucket."
  type        = bool
  default     = true
}

variable "logging_bucket" {
  description = "Explicitly set a logging bucket."
  type        = string
  default     = null
}

variable "logging_prefix" {
  description = "Explicitly set a logging prefix."
  type        = string
  default     = null
}

variable "versioning_enabled" {
  description = "Enable bucket versioning."
  type        = bool
  default     = true
}

variable "lifecycle_enabled" {
  description = "Enable bucket lifecycle rules."
  type        = bool
  default     = false # TODO: enable by default after reviewing with team
}

variable "lifecycle_config" {
  description = "Default bucket lifecycle settings. Transitioning numbers are in days or null to ignore. Any 'noncurrent' rules requires versioning to be enabled."
  type        = object({
    enabled = bool
    transition_objects_to_ia_days = number
    transition_noncurrent_versions_to_ia_days = number
    expire_noncurrent_versions_days = number
  })
  default = {
    enabled = false
    transition_objects_to_ia_days = 90
    transition_noncurrent_versions_to_ia_days = 7
    expire_noncurrent_versions_days = 60
  }
}

variable "ignore_public_acls" {
  description = "Whether Amazon S3 should ignore public ACLs for this bucket. Defaults to true."
  type        = bool
  default     = true
}

variable "restrict_public_buckets" {
  description = "Whether Amazon S3 should restrict public bucket policies for this bucket. Defaults to true."
  type        = bool
  default     = true
}

variable "block_public_acls" {
  description = "Whether Amazon S3 should block public ACLs for this bucket. Defaults to true."
  type        = bool
  default     = true
}

variable "block_public_policy" {
  description = "Whether Amazon S3 should block public bucket policies for this bucket. Defaults to true."
  type        = bool
  default     = true
}
