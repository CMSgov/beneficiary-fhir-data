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

variable "kms_config_key_alias" {
  description = "Key alias of environment's configuration KMS key"
  type        = string
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}