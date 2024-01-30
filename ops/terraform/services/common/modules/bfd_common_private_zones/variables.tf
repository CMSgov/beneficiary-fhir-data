variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, vpc_id = string })
}
