variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), azs = string })
}

variable "role" {
  type = string
}

variable "layer" {
  description = "app or data"
  type        = string
}
