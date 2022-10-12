variable "ami_id_override" {
  default     = null
  description = "BFD Pipeline override ami-id. Defaults to latest pipeline/etl AMI from `master`."
  type        = string
}

variable "force_etl_user_creation" {
  default     = false
  description = "Force an etl service account creation; only `prod` typically creates an etl service account."
  type        = string
}
