variable "env_config" {
  description = "All high-level tags and VPC info"
  type        = object({env = string, tags = map(string), vpc_id = string, zone_id = string})
}

variable "aurora_config" {
  description = "Aurora sizing and version config"
  type        = object({instance_class = string, cluster_nodes = number, engine_version = string, param_version = string})
}

variable "aurora_node_params" {
  description = "Aurora node parameter group config"
  type        = list(object({name = string, value = string, apply_on_reboot = bool}))
}

variable "stateful_config" {
  description = "Aurora networking and security config passed from stateful"
  type        = object({azs = list(string), vpc_sg_ids = list(string), subnet_ids = list(string), kms_key_id = string})
}
