variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string, azs = list(string) })
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
