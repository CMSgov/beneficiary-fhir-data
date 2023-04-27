variable "fhir_ami" {
  description = "FHIR server AMI"
  type        = string
}

variable "is_public" {
  description = "If true, open the FHIR data end-point to the public Internet"
  type        = bool
  default     = false
}
