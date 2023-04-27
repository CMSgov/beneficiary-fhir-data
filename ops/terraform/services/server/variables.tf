variable "fhir_ami" {
  description = "FHIR server AMI"
  type        = string
}

variable "ssh_key_name" {
  description = "SSH Key"
  type        = string
}

variable "is_public" {
  description = "If true, open the FHIR data end-point to the public Internet"
  type        = bool
  default     = false
}
