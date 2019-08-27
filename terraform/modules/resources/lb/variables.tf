variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({env=string, tags=map(string), vpc_id=string, zone_id=string, azs=list(string)})
}

variable "role" {
  description = "The role of this lb in the application. Will be used to "
  type        = string
}

variable "load_balancer_type" {
  type        = string
  default     = "application"
}

variable "layer" {
  description = "app or data"
  type        = string 
}

variable "log_bucket" {
  type        = string
}

variable "ingress_port" {
  type        = number
}

variable "egress_port" {
  type        = number
}

