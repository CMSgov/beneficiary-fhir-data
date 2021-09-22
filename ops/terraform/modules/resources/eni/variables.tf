variable "name" {
  type = string
  description = "Friendly name for this ENI."
}

variable "description" {
  type = string
  description = "Brief (a few words) describing the purpose for this ENI."
  default = null
}

variable "subnet_id" {
  type = string
  description = "(Required) Subnet ID to create the ENI in."
}

variable "source_dest_check" {
  type = bool
  description = "Whether to enable source destination checking for the ENI. Default true."
  default = true
}

variable "private_ips" {
  type = list(string)
  description = "(Optional) List of private IPs to assign to the ENI."
  default = []
}

variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string })
}
