variable "ami_id" {
  description = "Provided AMI ID for the migrator."
  type        = string
  default     = null
}

variable "create_migrator_instance" {
  default     = false
  description = "When true, create the migrator instance"
  type        = bool
}

variable "git_repo_version" {
  description = "Branch, tag, or hash. [Details on ansible's `git` module parameter version](https://docs.ansible.com/ansible/2.9/modules/git_module.html#parameter-version)"
  type        = string
  default     = "master"
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
