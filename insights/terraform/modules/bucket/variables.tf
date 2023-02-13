variable "name" {
  description = "Name of the bucket"
  type        = string
}

variable "sensitivity" {
  description = "Sensitivity for the bucket"
  type        = string
  default     = "moderate"
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

variable "full_groups" {
  description = "Group arns to give full access"
  type        = list(string)
  default     = ["bfd-insights-analysts"]
}

variable "athena_groups" {
  description = "Group arns to give athena access to"
  type        = list(string)
  default     = ["bfd-insights-readers", "bfd-insights-authors"]
}

variable "cross_accounts" {
  description = "Arns to give cross_account access to"
  type        = list(string)
  default     = []
}

variable "folders" {
  description = "List of top-level folders to create"
  type        = list(string)
  default     = ["databases"]
}

variable "bucket_key_enabled" {
  description = "Toggle AWS S3 Bucket BucketKey"
  default     = true
  type        = bool

}
