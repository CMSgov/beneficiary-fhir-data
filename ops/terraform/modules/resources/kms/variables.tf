variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string) })
}

variable "dependency" {
  type        = list(any)
  description = "Fake a depends_on behavior"
}
