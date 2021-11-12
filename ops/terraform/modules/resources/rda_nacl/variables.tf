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
