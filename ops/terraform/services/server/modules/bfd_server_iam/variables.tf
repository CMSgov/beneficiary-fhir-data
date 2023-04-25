variable "service" {
  description = "Service name for BFD FHIR API server"
  type        = string
  default     = "server"
}

variable "legacy_service" {
  description = "Legacy service name for BFD FHIR API server"
  type        = string
  default     = "fhir"
}
