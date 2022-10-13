variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string) })
}

variable "victor_ops_url" {
  description = "VictorOps CloudWatch integration URL"
  type        = string
}

variable "medicare_opt_out_config" {
  description = "Config for medicare opt out S3 bucket"
  type        = object({ read_roles = list(string), write_accts = list(string), admin_users = list(string) })
}

# add module feature toggles here
variable "module_features" {
  type = map(bool)
}
