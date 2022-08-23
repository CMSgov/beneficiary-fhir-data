variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string, azs = list(string) })
}

variable "dashboard_name" {
  description = "BFD dashboard name"
  type        = string
}

variable "dashboard_namespace" {
  description = "BFD dashboard namespace"
  type        = string
}
