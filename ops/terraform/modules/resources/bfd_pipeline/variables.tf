variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string, azs = list(string) })
}

variable "az" {
  type = string
}

variable "db_config" {
  description = "Setup DB ingress rules if defined"
  type        = object({ db_sg = string })
  default     = null
}

variable "mgmt_config" {
  type = object({ vpn_sg = string, tool_sg = string, remote_sg = string, ci_cidrs = list(string) })
}

variable "launch_config" {
  type = object({ ami_id = string, account_id = string, ssh_key_name = string, git_branch = string, git_commit = string })
}

variable "alarm_notification_arn" {
  description = "The CloudWatch Alarm notification ARN."
  type        = string
  default     = null
}

variable "ok_notification_arn" {
  description = "The CloudWatch OK notification ARN."
  type        = string
  default     = null
}

variable "mpm_rda_cidr_block" {
  description = "CIDR block of hosts available through the MPM VPC Peered environment"
  type        = string
  default     = null
}
