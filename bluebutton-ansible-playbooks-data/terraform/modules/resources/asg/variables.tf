
variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({env=string, tags=map(string), vpc_id=string, zone_id=string, azs=list(string)})
}

variable "role" {
  type        = string
}

variable "layer" {
  description = "app or data"
  type        = string      
}

variable "asg_config" {
  type        = object({min=number, max=number, desired=number, sns_topic_arn=string})
}

variable "db_config" {
  description = "Setup a db ingress rules if defined"
  type        = object({db_sg=string, role=string})
  default     = null
}

variable "lb_config" {
  description = "Load balancer information"
  type        = object({name=string, tg_arn=string, port=number})
  default     = null
}

variable "mgmt_config" {
  type        = object({vpn_sg=string, tool_sg=string, remote_sg=string, ci_cidrs=list(string)})
}

variable "launch_config" {
  type        = object({instance_type=string, ami_id=string, key_name=string, profile=string})
}


