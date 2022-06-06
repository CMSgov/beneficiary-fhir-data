variable "ami_id" {
  description = "Provided AMI ID for the migrator."
  type        = string
}

variable "create_migrator_instance" {
  default     = false
  description = "When true, create the migrator instance"
  type        = bool
}

variable "git_branch_name" {
  description = "Source branch for this migrator deployment"
  type        = string
}

variable "instance_type_override" {
  default     = null
  description = "Valid instance type. See [ec2 instance types](https://aws.amazon.com/ec2/instance-types/)"
  type        = string
}

variable "migrator_monitor_enabled_override" {
  default     = null
  description = "When true, migrator system emits signals to SQS. Defaults to `true`"
  type        = bool
}

variable "migrator_monitor_heartbeat_interval_seconds_override" {
  default     = null
  description = "Sets interval for migrator monitor heartbeat in seconds. Defaults to `300`"
  type        = number
}

variable "rds_cluster_identifier_override" {
  default     = null
  description = "RDS Cluster identifier. Defaults to environment-specific RDS cluster."
  type        = string
}

variable "security_group_ids_extra" {
  default     = []
  description = "Extra security group IDs"
  type        = list(string)
}

variable "sqs_queue_name_override" {
  default     = null
  description = "SQS Queue Name. Defaults to environment-specific SQS Queue."
  type        = string
}

variable "volume_size_override" {
  default     = null
  description = "Root volume size override."
  type        = number
}
