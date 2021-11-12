variable "victor_ops_url" {
  description = "VictorOps CloudWatch integration URL"
  type        = string
}

variable "medicare_opt_out_config" {
  description = "Config for medicare opt out S3 bucket"
  type        = object({ read_roles = list(string), write_accts = list(string), admin_users = list(string) })
}

variable "mpm_rda_cidr_block" {
  description = "CIDR block of hosts available through the MPM VPC Peered environment"
  type        = string
  default     = null
}

variable "mpm_enabled" {
  description = "If set to true, MPM network connections over known ports will be accepted"
  type        = bool
  default     = true
}
