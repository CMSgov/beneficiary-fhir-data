variable "db_config" {
  description       = "All the high-level configuration needed to setup an RDS instances"
  type              = object({instance_class = string, allocated_storage=number, iops = number})
}

variable "db_import_mode" {
  description       = "Enable or disable parameters that optimize bulk data imports"
  type              = object({enabled = bool, maintenance_work_mem = string})
}

variable "env_config" {
  description       = "All high-level info for the whole vpc"
  type              = object({env=string, tags=map(string)})
}

variable "enable_victor_ops" {
  description       = "Enable cloudwatch alarms to be forwarded to VictorOps"
  type              = boolean
  default           = false
}
