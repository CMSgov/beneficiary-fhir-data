variable "aurora_config" {
  description = "Aurora sizing and version config"
  type        = object({ instance_class = string, cluster_nodes = number, engine_version = string, param_version = string })
}

variable "aurora_node_params" {
  description = "Aurora node parameter group config"
  type        = list(object({ name = string, value = string, apply_on_reboot = bool }))
}

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

variable "partner_acct_nums" {
  description = "Map of partner account numbers accessing EFT EFS file systems"
  type        = map
}

variable "partner_subnets" {
  description = "Map of partner subnets requiring access to EFT EFS file systems"
  type        = map(map(list(string)))
}

// add module feature toggles here
variable "module_features" {
  type = map(bool)
}

