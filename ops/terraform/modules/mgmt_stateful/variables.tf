variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), azs = string })
}
variable "bfd_packages_bucket" {
  type = string
}
