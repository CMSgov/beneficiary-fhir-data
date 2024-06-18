variable "ami_id_override" {
  default     = null
  description = "BFD Migrator override ami-id. Defaults to latest migrator AMI from `master`."
  type        = string
}

variable "create_migrator_instance" {
  default     = false
  description = "When true, create the migrator instance, security group, and RDS security group rules"
  type        = bool
}
