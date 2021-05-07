variable "aurora_config" {
  description = "Aurora sizing and version config"
  type        = object({ instance_class = string, cluster_nodes = number, engine_version = string, param_version = string })
}

variable "aurora_node_params" {
  description = "Aurora node parameter group config"
  type        = list(object({ name = string, value = string, apply_on_reboot = bool }))
}

# variable "db_config" {
#   description       = "All the high-level configuration needed to setup an RDS instances"
#   type              = object({instance_class = string, allocated_storage=number, iops = number})
# }

# variable "db_params" {
#   description       = "Parameters that populate the default parameter group"
#   type              = list(object({name = string, value = string, apply_on_reboot = bool}))
# }

# variable "db_import_mode" {
#   description       = "Enable or disable parameters that optimize bulk data imports"
#   type              = object({enabled = bool, maintenance_work_mem = string})
# }

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

// add module feature toggles here
variable "module_features" {
  type = map(bool)
}

variable "bfd_packages_bucket" {
  type = string
}
