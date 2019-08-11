variable "db_config" {
  description       = "All the high-level configuration needed to setup an RDS instances"
  type              = object({instance_class = string, allocated_storage=number, iops = number})
}

variable "env_config" {
  description       = "All high-level info for the whole vp"
  type              = object({env=string, tags=map(string)})
}
