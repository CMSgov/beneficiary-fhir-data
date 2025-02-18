variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ default_tags = map(string), vpc_id = string, azs = list(string) })
}

variable "role" {
  description = "The role of this lb in the application. Will be used to "
  type        = string
}

variable "layer" {
  description = "app or data"
  type        = string
}

variable "is_public" {
  description = "If true, the LB is created as a public LB"
  type        = bool
  default     = false
}

variable "ingress" {
  description = "Ingress port and cidr blocks"
  type        = object({ description = string, port = number, cidr_blocks = list(string), prefix_list_ids = list(string) })
}

variable "egress" {
  description = "Egress port and cidr blocks"
  type        = object({ description = string, port = number, cidr_blocks = list(string) })
}
