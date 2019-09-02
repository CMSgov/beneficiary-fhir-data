
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

variable "az" {
  type        = string
}

variable "mgmt_config" {
  type        = object({vpn_sg=string, tool_sg=string, remote_sg=string, ci_cidrs=list(string)})
}

variable "launch_config" {
  type        = object({instance_type=string, ami_id=string, key_name=string, profile=string, user_data_tpl=string})
}


