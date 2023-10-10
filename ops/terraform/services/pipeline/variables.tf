variable "ami_id_override" {
  default     = null
  description = "BFD Pipeline override ami-id. Defaults to latest pipeline/etl AMI from `git_branch_name|master`."
  type        = string
}

variable "force_etl_user_creation" {
  default     = false
  description = "Force an etl service account creation; only `prod` typically creates an etl service account."
  type        = string
}

variable "force_dashboard_creation" {
  default     = false
  description = "Forces the creation of BFD Pipeline CloudWatch Dashboards; note dashboards are created by-default for established environments"
  type        = bool
}

variable "force_sli_creation" {
  default     = false
  description = "Forces the creation of BFD Pipeline SLI CloudWatch Metrics; required for SLO Alarms"
  type        = bool
}

variable "force_slo_alarms_creation" {
  default     = false
  description = "Forces the creation of BFD Pipeline SLO CloudWatch Alarms; note alarms are created by-default for established environments; requires SLIs"
  type        = bool
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

variable "alert_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Alert SLO alarms"
  type        = string
  default     = null
}

variable "warning_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Warning SLO alarms"
  type        = string
  default     = null
}

variable "alert_ok_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Alert Alarms that transition from ALARM to OK"
  type        = string
  default     = null
}

variable "warning_ok_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Warning Alarms that transition from ALARM to OK"
  type        = string
  default     = null
}

variable "git_branch_name" {
  default     = "master"
  description = "The name of the git branch to reference during creation. Defaults to `master`"
  type        = string
}
