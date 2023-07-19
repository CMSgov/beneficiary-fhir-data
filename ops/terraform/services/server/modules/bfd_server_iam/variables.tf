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

variable "kms_key_alias" {
  description = "Key alias of environment's KMS key"
  type        = string
}

variable "rds_cluster_resource_id" {
  description = "RDS cluster resource ID (not the cluster identifier)"
  type        = string
}
