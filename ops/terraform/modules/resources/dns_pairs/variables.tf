variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string })
}

variable "zone_id" {
  type = string
}

variable "a_alias" {
  type = string
}

variable "a_zone_id" {
  type = string
}

variable "a_set" {
  type = string
}

variable "b_alias" {
  type = string
}

variable "b_zone_id" {
  type = string
}

variable "b_set" {
  type = string
}

variable "weights" {
  description = "For each name, create a pair with a weight."
  type        = map(number)
}
