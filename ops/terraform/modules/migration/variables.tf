variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string) })
}

variable "bb" {
  description = "Blue Button API percentage (0-100) served by the CCS environment"
  type        = number
}

variable "bcda" {
  description = "BCDA percentage (0-100) served by the CCS environment"
  type        = number
}

variable "dpc" {
  description = "DPC percentage (0-100) served by the CCS environment"
  type        = number
}

variable "mct" {
  description = "MCT percentage (0-100) served by the CCS environment"
  type        = number
}


