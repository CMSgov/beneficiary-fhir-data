variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string })
}

variable "role" {
  type = string
}

variable "kms_key_id" {
  type    = string
  default = null
}

variable "log_bucket" {
  type    = string
  default = ""
}

variable "acl" {
  type    = string
  default = "private"
}

