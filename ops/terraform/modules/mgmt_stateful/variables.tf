variable "env_config" {
  description       = "All high-level info for the whole vpc"
  type              = object({env=string, tags=map(string), azs=string})
}

variable "app_subnet" {
  description       = "The bfd-mgmt-azX-app tpo use. MGMT is az1 and MGMT-TEST is az3"
  type              = string
}