variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string, azs = list(string) })
}

variable "role" {
  type = string
}

variable "layer" {
  description = "app or data"
  type        = string
}

variable "az" {
  type = string
}

variable "mgmt_config" {
  type = object({ vpn_sg = string, tool_sg = string, remote_sg = string, ci_sg = string })
}

variable "launch_config" {
  type = object({ instance_type = string, volume_size = number, ami_id = string, key_name = string, profile = string, user_data_tpl = string, git_branch = string, git_commit = string })
}

variable "sg_ids" {
  type        = list(string)
  default     = []
  description = "The IDs of the additional security groups that this EC2 instance should be added to."
}

variable "ec2_depends_on_1" {
  type        = string
  default     = null
  description = "The Terraform resource name/ID of a resource that this module's EC2 instance should depend on, if any."
}
