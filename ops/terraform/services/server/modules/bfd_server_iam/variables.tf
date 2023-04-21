variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string) })
}

variable "name" {
  description = "The name part used to create the role and policy"
  type        = string
  default     = "app"
}

variable "s3_bucket_arns" {
  description = "A list of S3 bucket arns to allow access to"
  type        = list(string)
  default     = []
}
