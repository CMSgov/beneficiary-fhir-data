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

variable "create_ccw_pipeline" {
  default     = true
  description = "Creates a BFD Pipeline to run CCW Rif jobs; RDA jobs on the pipeline are disabled by default."
  type        = bool
}

variable "create_rda_pipeline" {
  default     = true
  description = "Creates a BFD Pipeline to run RDA jobs; CCW Rif jobs on the pipeline are disabled by default."
  type        = bool
}
