variable "ami_id_override" {
  description = "BFD Pipeline override ami-id. Defaults to latest pipeline/etl AMI from `master`."
  type        = string
  default     = null
}
