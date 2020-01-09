variable "env_config" {
  description       = "All high-level info for the whole vpc"
  type              = object({env=string, tags=map(string)})
}

variable "az" {
  description       = "Availability Zone"
  type              = string
}
