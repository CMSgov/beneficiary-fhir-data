variable "service" {
  type        = string
  nullable    = false
  description = "Terraservice name corresponding to SSM service hierarchy."
}

variable "ssm_config" {
  type        = map(string)
  nullable    = false
  description = "bfd-terraservice generated SSM config map from which capacity provider strategy values are pulled."
}

variable "cluster_name" {
  type        = string
  nullable    = false
  description = "ECS Cluster Name to lookup Capacity Providers on."
}
