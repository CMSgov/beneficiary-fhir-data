variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string })
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
