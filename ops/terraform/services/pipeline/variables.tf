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

variable "jdbc_suffix" {
  default     = "?logServerErrorDetail=false"
  description = "boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging"
  type        = string
}

variable "deployed_git_branch" {
  default     = null
  description = "The Git branch from which this module was deployed"
  type        = string
}
