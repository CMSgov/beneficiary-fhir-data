variable "ami_id_override" {
  description = "BFD Server override ami-id. Defaults to latest server/fhir AMI from `master`."
  type        = string
  default     = null
}

variable "jdbc_suffix" {
  default     = "?logServerErrorDetail=false"
  description = "boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging"
  type        = string
}
