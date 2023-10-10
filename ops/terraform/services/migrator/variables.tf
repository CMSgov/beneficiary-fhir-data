variable "ami_id_override" {
  default     = null
  description = "BFD Migrator override ami-id. Defaults to latest migrator AMI from `git_branch_name|master`"
  type        = string
}

variable "create_migrator_instance" {
  default     = false
  description = "When true, create the migrator instance, security group, and RDS security group rules"
  type        = bool
}

variable "migrator_monitor_enabled_override" {
  default     = null
  description = "When true, migrator system emits signals to SQS. Defaults to `true` via locals"
  type        = bool
}

variable "migrator_monitor_heartbeat_interval_seconds_override" {
  default     = null
  description = "Sets interval for migrator monitor heartbeat in seconds. Defaults to `300` via locals"
  type        = number
}

variable "git_branch_name" {
  default     = "master"
  description = "The name of the git branch to reference during creation. Defaults to `master`"
  type        = string
}
