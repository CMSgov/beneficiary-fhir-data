variable "env" {
  description = "The BFD SDLC environment to target"
  type        = string
  default     = "test"
}

variable "pii_bucket_config" {
  description = "Config for PII S3 bucket"
  type = object({
    name        = string
    log_bucket  = string
    read_arns   = list(string)
    write_accts = list(string)
    admin_arns  = list(string)
  })
}
